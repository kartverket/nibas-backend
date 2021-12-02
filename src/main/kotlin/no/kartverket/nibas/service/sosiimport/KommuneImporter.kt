package no.kartverket.nibas.service.sosiimport

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.KommuneInformasjon
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
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
    private val kommuneRepository: KommuneRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
    private val spraakkodeRepository: SpraakkodeRepository,
    private val grenseImporter: GrenseImporter,
    private val administrativEnhetImporter: AdministrativEnhetImporter
) {

    val logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiKommune(flate: SosiFlate, root: SosiObjekt) {
        val lokalid = flate.getIdent()?.getLokalId()!!
        when (kommuneRepository.findByLokalid(lokalid)) {
            null -> {
                var kommune = administrativEnhetImporter.importer(flate) as Kommune
                kommune.kommunenummer =
                    kodelisteService.getKommunenummerIdByCodeValue(
                        flate.getKommunenummer()
                            ?: throw RuntimeException("Kommunenummer ikke satt i modellen for kommune med flate-id $lokalid")
                    )
                kommune.samiskforvaltningsomraade = flate.isSamiskForvaltningsomrade()

                runBlocking {
                    withContext(Dispatchers.Default) {
                        val deferredSkall: List<Deferred<KommuneReferanseImportResult>> =
                            flate.getSkallReferanser().map { ref ->
                                async {
                                    importerKommuneRef(ref, root)
                                }
                            }

                        val deferredHull: List<List<Deferred<KommuneReferanseImportResult>>> =
                            flate.getHullReferanser().map { liste ->
                                liste.map { ref ->
                                    async {
                                        importerKommuneRef(ref, root)
                                    }
                                }
                            }

                        val skall: List<KommuneReferanseImportResult> = deferredSkall.awaitAll()
                        val hull: List<List<KommuneReferanseImportResult>> = deferredHull.map { it.awaitAll() }

                        kommune.kommunegrenser =
                            (skall.flatMap { it.kommunegrenser } + hull.flatMap { it.flatMap { i -> i.kommunegrenser } }).toMutableSet()
                        kommune.fylkesgrenser =
                            (skall.flatMap { it.fylkesgrenser } + hull.flatMap { it.flatMap { i -> i.fylkesgrenser } }).toMutableSet()
                        kommune.avtaltavgrensningslinjer =
                            (skall.flatMap { it.avtaltavgrensningslinjer } + hull.flatMap { it.flatMap { i -> i.avtaltavgrensningslinjer } }).toMutableSet()
                        kommune.territorialgrenser =
                            (skall.flatMap { it.territorialgrenser } + hull.flatMap { it.flatMap { i -> i.territorialgrenser } }).toMutableSet()
                        kommune.riksgrenser =
                            (skall.flatMap { it.riksgrenser } + hull.flatMap { it.flatMap { i -> i.riksgrenser } }).toMutableSet()

                        kommune.flate = SosiGeometryFunctions.lagFlate(
                            skall.map { it.lineString!! },
                            hull.map { it.map { i -> i.lineString!! } },
                            flate.koordsys
                        )
                    }
                }

                kommune = kommuneRepository.save(kommune)

                if (flate.getInformasjon() != null) {
                    val kommuneInformasjon = KommuneInformasjon()
                    kommuneInformasjon.informasjon = flate.getInformasjon()
                    kommuneInformasjon.kommuneFk = kommune
                    kommuneInformasjonRepository.save(kommuneInformasjon)
                }

                flate.getKommunenavn().forEach { sosiKommunenavn ->
                    val kommunenavn = Administrativenhetnavn()
                    kommunenavn.navn = sosiKommunenavn.getNavn()
                    kommunenavn.spraak = sosiKommunenavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
                    kommunenavn.rekkefoelge = sosiKommunenavn.getRekkefolge()
                    kommunenavn.kommuneFk = kommune
                    administrativenhetnavnRepository.save(kommunenavn)
                }
            }
            else -> {}
        }
    }

    private fun importerKommuneRef(ref: SosiReferanse, root: SosiObjekt): KommuneReferanseImportResult {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                val result = KommuneReferanseImportResult()
                when (importertGrense) {
                    is Kommunegrense -> result.kommunegrenser.add(importertGrense)
                    is Fylkesgrense -> result.fylkesgrenser.add(importertGrense)
                    is Avtaltavgrensningslinje -> result.avtaltavgrensningslinjer.add(importertGrense)
                    is Territorialgrense -> result.territorialgrenser.add(importertGrense)
                    is Riksgrense -> result.riksgrenser.add(importertGrense)
                }
                when (ref.isRetningReversed()) {
                    true -> result.lineString = importertGrense.grense!!.reverse()
                    false -> result.lineString = importertGrense.grense!!
                }
                return result
            }
        }
    }
}

data class KommuneReferanseImportResult(
    val kommunegrenser: MutableSet<Kommunegrense> = mutableSetOf(),
    val fylkesgrenser: MutableSet<Fylkesgrense> = mutableSetOf(),
    val avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf(),
    val territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf(),
    val riksgrenser: MutableSet<Riksgrense> = mutableSetOf(),
    var lineString: LineString? = null
)
