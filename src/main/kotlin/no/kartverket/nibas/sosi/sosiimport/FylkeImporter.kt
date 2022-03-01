package no.kartverket.nibas.sosi.sosiimport

import kotlinx.coroutines.*
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeInformasjonRepository
import no.kartverket.nibas.repository.FylkeRepository
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
class FylkeImporter @Autowired constructor(
    private val kodelisteService: KodelisteService,
    private val fylkeRepository: FylkeRepository,
    private val fylkeInformasjonRepository: FylkeInformasjonRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
    private val spraakkodeRepository: SpraakkodeRepository,
    private val grenseImporter: GrenseImporter,
    private val administrativEnhetImporter: AdministrativEnhetImporter
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiFylke(flate: SosiFlate, root: SosiObjekt) {
        val lokalid = flate.getIdent()?.getLokalId()!!
        when (fylkeRepository.findByLokalid(lokalid)) {
            null -> {
                var fylke = administrativEnhetImporter.importer(flate) as Fylke

                fylke.fylkesnummer =
                    kodelisteService.getFylkesnummerIdByCodeValue(
                        flate.getFylkesnummer()
                            ?: throw RuntimeException("Fylkesnummer ikke satt i modellen for fylke med flate-id $lokalid")
                    )
                fylke.samiskforvaltningsomraade = flate.isSamiskForvaltningsomrade()

                runBlocking {
                    withContext(Dispatchers.Default) {
                        val deferredSkall: List<Deferred<FylkeReferanseImportResult>> = flate.getSkallReferanser().mapIndexed { rekkefoelge, ref ->
                            async {
                                importerFylkeRef(fylke, ref, root, rekkefoelge)
                            }
                        }

                        val deferredHull: List<List<Deferred<FylkeReferanseImportResult>>> =
                            flate.getHullReferanser().mapIndexed { hullIndex, liste ->
                                liste.mapIndexed { rekkefoelge, ref ->
                                    async {
                                        importerFylkeRef(fylke, ref, root, rekkefoelge, hullIndex)
                                    }
                                }
                            }

                        val hull = deferredHull.map { it.awaitAll() }
                        val skall = deferredSkall.awaitAll()

                        fylke.fylkesgrenser = (skall.flatMap { it.fylkesgrenser } + hull.flatMap { it.flatMap { i -> i.fylkesgrenser } }).toMutableSet()
                        fylke.avtaltavgrensningslinjer = (skall.flatMap { it.avtaltavgrensningslinjer } + hull.flatMap { it.flatMap { i -> i.avtaltavgrensningslinjer } }).toMutableSet()
                        fylke.territorialgrenser = (skall.flatMap { it.territorialgrenser } + hull.flatMap { it.flatMap { i -> i.territorialgrenser } }).toMutableSet()
                        fylke.riksgrenser = (skall.flatMap { it.riksgrenser } + hull.flatMap { it.flatMap { i -> i.riksgrenser } }).toMutableSet()

                        fylke.flate = SosiGeometryFunctions.lagFlate(
                            skall.map { it.lineString!! },
                            hull.map { it.map { i -> i.lineString!! } },
                            flate.koordsys
                        )
                    }
                }

                fylke = fylkeRepository.save(fylke)

                if (flate.getInformasjon() != null) {
                    val fylkeInformasjon = FylkeInformasjon()
                    fylkeInformasjon.informasjon = flate.getInformasjon()
                    fylkeInformasjon.fylkeFk = fylke
                    fylkeInformasjonRepository.save(fylkeInformasjon)
                }

                flate.getFylkesnavn().forEach { sosiFylkesnavn ->
                    val fylkesnavn = Administrativenhetnavn()
                    fylkesnavn.navn = sosiFylkesnavn.getNavn()
                    fylkesnavn.spraak = sosiFylkesnavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
                    fylkesnavn.rekkefoelge = sosiFylkesnavn.getRekkefolge()
                    fylkesnavn.fylkeFk = fylke
                    administrativenhetnavnRepository.save(fylkesnavn)
                }
            }
            else -> {}
        }
    }

    private fun importerFylkeRef(fylke: Fylke, ref: SosiReferanse, root: SosiObjekt, rekkefoelge: Int, hullIndex: Int? = null): FylkeReferanseImportResult {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                val result = FylkeReferanseImportResult()
                when (importertGrense) {
                    is Fylkesgrense -> result.fylkesgrenser.add(FylkeFylkesgrense(fylke, importertGrense, GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, hullIndex)))
                    is Avtaltavgrensningslinje -> result.avtaltavgrensningslinjer.add(FylkeAvtaltavgrensningslinje(fylke, importertGrense, GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, hullIndex)))
                    is Territorialgrense -> result.territorialgrenser.add(FylkeTerritorialgrense(fylke, importertGrense, GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, hullIndex)))
                    is Riksgrense -> result.riksgrenser.add(FylkeRiksgrense(fylke, importertGrense, GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, hullIndex)))
                }
                when (ref.erRetningMedKlokken()) {
                    true -> result.lineString = importertGrense.grense!!
                    false -> result.lineString = importertGrense.grense!!.reverse()
                }
                return result
            }
        }
    }
}

data class FylkeReferanseImportResult(
    val fylkesgrenser: MutableSet<FylkeFylkesgrense> = mutableSetOf(),
    val avtaltavgrensningslinjer: MutableSet<FylkeAvtaltavgrensningslinje> = mutableSetOf(),
    val territorialgrenser: MutableSet<FylkeTerritorialgrense> = mutableSetOf(),
    val riksgrenser: MutableSet<FylkeRiksgrense> = mutableSetOf(),
    var lineString: LineString? = null
)
