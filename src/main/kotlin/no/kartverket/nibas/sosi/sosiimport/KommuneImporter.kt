package no.kartverket.nibas.sosi.sosiimport

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.KommuneAvtaltavgrensningslinje
import no.kartverket.nibas.domain.KommuneFylkesgrense
import no.kartverket.nibas.domain.KommuneInformasjon
import no.kartverket.nibas.domain.KommuneKommunegrense
import no.kartverket.nibas.domain.KommuneRiksgrense
import no.kartverket.nibas.domain.KommuneTerritorialgrense
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneInformasjonRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.model.SosiReferanse
import org.locationtech.jts.geom.LineString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger

@Component
class KommuneImporter @Autowired constructor(
    private val kodelisteService: KodelisteService,
    private val kommuneInformasjonRepository: KommuneInformasjonRepository,
    private val fylkeRepository: FylkeRepository,
    private val kommuneRepository: KommuneRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
    private val spraakkodeRepository: SpraakkodeRepository,
    private val grenseImporter: GrenseImporter,
    private val administrativEnhetImporter: AdministrativEnhetImporter
) {

    val logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiKommune(flater: List<SosiFlate>, root: SosiObjekt) {

        // Henter egenskapene bare fra første flate. Egenskapene er/skal være like på tvers av flatene.
        val forsteFlate = flater.first()
        val lokalid = forsteFlate.getIdent()?.getLokalId()!!

        var kommune = administrativEnhetImporter.importer(forsteFlate) as Kommune
        kommune.kommunenummer = kodelisteService.getKommunenummerIdByCodeValue(
            forsteFlate.getKommunenummer()
                ?: throw RuntimeException("Kommunenummer ikke satt i modellen for kommune med flate-id $lokalid")
        )
        kommune.samiskforvaltningsomraade = forsteFlate.isSamiskForvaltningsomrade()

        runBlocking {
            withContext(Dispatchers.Default) {
                val flateLinjer = flater.mapIndexed { skall_index, flate ->
                    val deferredSkallForFlate: List<Deferred<KommuneReferanseImportResult>> =
                        flate.getSkallReferanser().mapIndexed { rekkefoelge, ref ->
                            async {
                                importerKommuneRef(kommune, ref, root, skall_index, rekkefoelge)
                            }
                        }

                    val deferredHullForFlate: List<List<Deferred<KommuneReferanseImportResult>>> =
                        flate.getHullReferanser().mapIndexed { hullIndex, liste ->
                            liste.mapIndexed { rekkefoelge, ref ->
                                async {
                                    importerKommuneRef(kommune, ref, root, skall_index, rekkefoelge, hullIndex)
                                }
                            }
                        }

                    val skallForFlate: List<KommuneReferanseImportResult> = deferredSkallForFlate.awaitAll()
                    val hullForFlate: List<List<KommuneReferanseImportResult>> =
                        deferredHullForFlate.map { it.awaitAll() }
                    Pair(skallForFlate, hullForFlate)
                }

                val skall: List<List<KommuneReferanseImportResult>> = flateLinjer.map { it.first }
                val hull: List<List<List<KommuneReferanseImportResult>>> = flateLinjer.map { it.second }

                kommune.kommunegrenser =
                    (skall.flatMap { it.flatMap { i -> i.kommunegrenser } } + hull.flatMap { it.flatMap { i -> i.flatMap { a -> a.kommunegrenser } } }).toMutableSet()
                kommune.fylkesgrenser =
                    (skall.flatMap { it.flatMap { i -> i.fylkesgrenser } } + hull.flatMap { it.flatMap { i -> i.flatMap { a -> a.fylkesgrenser } } }).toMutableSet()
                kommune.avtaltavgrensningslinjer =
                    (skall.flatMap { it.flatMap { i -> i.avtaltavgrensningslinjer } } + hull.flatMap { it.flatMap { i -> i.flatMap { a -> a.avtaltavgrensningslinjer } } }).toMutableSet()
                kommune.territorialgrenser =
                    (skall.flatMap { it.flatMap { i -> i.territorialgrenser } } + hull.flatMap { it.flatMap { i -> i.flatMap { a -> a.territorialgrenser } } }).toMutableSet()
                kommune.riksgrenser =
                    (skall.flatMap { it.flatMap { i -> i.riksgrenser } } + hull.flatMap { it.flatMap { i -> i.flatMap { a -> a.riksgrenser } } }).toMutableSet()

                kommune.oppdaterFlate()
            }
        }

        // Fylkesnummer er de to første sifferne i kommunenummeret
        kommune.fylkeFk = fylkeRepository.findByFylkesnummer(
            kodelisteService.getFylkesnummerIdByCodeValue(
                forsteFlate.getKommunenummer()!!.substring(0, 2)
            )
        )
        kommune = kommuneRepository.save(kommune)

        if (forsteFlate.getInformasjon() != null) {
            val kommuneInformasjon = KommuneInformasjon()
            kommuneInformasjon.informasjon = forsteFlate.getInformasjon()
            kommuneInformasjon.kommuneFk = kommune
            kommuneInformasjonRepository.save(kommuneInformasjon)
        }

        forsteFlate.getKommunenavn().forEach { sosiKommunenavn ->
            val kommunenavn = Administrativenhetnavn()
            kommunenavn.navn = sosiKommunenavn.getNavn()
            kommunenavn.spraak = sosiKommunenavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
            kommunenavn.rekkefoelge = sosiKommunenavn.getRekkefolge()
            kommunenavn.kommuneFk = kommune
            administrativenhetnavnRepository.save(kommunenavn)
        }
    }

    private fun importerKommuneRef(
        kommune: Kommune,
        ref: SosiReferanse,
        root: SosiObjekt,
        skallIndex: Int,
        rekkefoelge: Int,
        hullIndex: Int? = null
    ): KommuneReferanseImportResult {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                val result = KommuneReferanseImportResult()
                when (importertGrense) {
                    is Kommunegrense -> result.kommunegrenser.add(
                        KommuneKommunegrense(
                            kommune,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                    is Fylkesgrense -> result.fylkesgrenser.add(
                        KommuneFylkesgrense(
                            kommune,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                    is Avtaltavgrensningslinje -> result.avtaltavgrensningslinjer.add(
                        KommuneAvtaltavgrensningslinje(
                            kommune,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                    is Territorialgrense -> result.territorialgrenser.add(
                        KommuneTerritorialgrense(
                            kommune,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                    is Riksgrense -> result.riksgrenser.add(
                        KommuneRiksgrense(
                            kommune,
                            importertGrense,
                            GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, skallIndex, hullIndex)
                        )
                    )
                }

                result.lineString = when(ref.erRetningMedKlokken()) {
                    true -> importertGrense.grense!!
                    false -> importertGrense.grense!!.reverse()
                }

                return result
            }
        }
    }
}

data class KommuneReferanseImportResult(
    val kommunegrenser: MutableSet<KommuneKommunegrense> = mutableSetOf(),
    val fylkesgrenser: MutableSet<KommuneFylkesgrense> = mutableSetOf(),
    val avtaltavgrensningslinjer: MutableSet<KommuneAvtaltavgrensningslinje> = mutableSetOf(),
    val territorialgrenser: MutableSet<KommuneTerritorialgrense> = mutableSetOf(),
    val riksgrenser: MutableSet<KommuneRiksgrense> = mutableSetOf(),
    var lineString: LineString? = null
)
