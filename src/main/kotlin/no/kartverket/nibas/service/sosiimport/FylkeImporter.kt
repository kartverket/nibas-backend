package no.kartverket.nibas.service.sosiimport

import kotlinx.coroutines.*
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.*
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
class FylkeImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var fylkesnummerRepository: FylkesnummerRepository

    @Autowired
    lateinit var fylkeRepository: FylkeRepository

    @Autowired
    lateinit var fylkeInformasjonRepository: FylkeInformasjonRepository

    @Autowired
    lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Autowired
    lateinit var spraakkodeRepository: SpraakkodeRepository

    @Autowired
    lateinit var grenseImporter: GrenseImporter

    @Autowired
    lateinit var administrativEnhetImporter: AdministrativEnhetImporter

    @Transactional
    fun importerSosiFylke(flate: SosiFlate, root: SosiObjekt) {
        when (fylkeRepository.findByLokalid(flate.getIdent()?.getLokalId()!!)) {
            null -> {
                var fylke = administrativEnhetImporter.importer(flate) as Fylke

                fylke.fylkesnummer = flate.getFylkesnummer()?.let { fylkesnummerRepository.findByIdOrNull(it) }
                fylke.samiskforvaltningsomraade = flate.isSamiskForvaltningsomrade()

                runBlocking {
                    withContext(Dispatchers.Default) {
                        val deferredSkall: List<Deferred<FylkeReferanseImportResult>> = flate.getSkallReferanser().map { ref ->
                            async {
                                importerFylkeRef(ref, root)
                            }
                        }

                        val deferredHull: List<List<Deferred<FylkeReferanseImportResult>>> =
                            flate.getHullReferanser().map { liste ->
                                liste.map { ref ->
                                    async {
                                        importerFylkeRef(ref, root)
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

    private fun importerFylkeRef(ref: SosiReferanse, root: SosiObjekt): FylkeReferanseImportResult {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                val result = FylkeReferanseImportResult()
                when (importertGrense) {
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

data class FylkeReferanseImportResult(
    val fylkesgrenser: MutableSet<Fylkesgrense> = mutableSetOf(),
    val avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf(),
    val territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf(),
    val riksgrenser: MutableSet<Riksgrense> = mutableSetOf(),
    var lineString: LineString? = null
)