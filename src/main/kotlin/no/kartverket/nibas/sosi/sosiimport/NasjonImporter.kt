package no.kartverket.nibas.sosi.sosiimport

import kotlinx.coroutines.*
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.NasjonInformasjonRepository
import no.kartverket.nibas.repository.NasjonRepository
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
class NasjonImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var nasjonRepository: NasjonRepository

    @Autowired
    lateinit var nasjonInformasjonRepository: NasjonInformasjonRepository

    @Autowired
    lateinit var spraakkodeRepository: SpraakkodeRepository

    @Autowired
    lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Autowired
    lateinit var grenseImporter: GrenseImporter

    @Autowired
    lateinit var administrativEnhetImporter: AdministrativEnhetImporter

    @Transactional
    fun importerSosiNasjon(flate: SosiFlate, root: SosiObjekt) {
        when (nasjonRepository.findByLokalid(flate.getIdent()?.getLokalId()!!)) {
            null -> {
                var nasjon = administrativEnhetImporter.importer(flate) as Nasjon

                runBlocking {
                    withContext(Dispatchers.Default) {
                        val deferredSkall = flate.getSkallReferanser().mapIndexed { rekkefoelge, ref ->
                            async {
                                importerNasjonRef(nasjon, ref, root, rekkefoelge)
                            }
                        }

                        val deferredHull = flate.getHullReferanser().mapIndexed { hullIndex, liste ->
                            liste.mapIndexed { rekkefoelge, ref ->
                                async {
                                    importerNasjonRef(nasjon, ref, root, rekkefoelge, hullIndex)
                                }
                            }
                        }

                        val skall = deferredSkall.awaitAll()
                        val hull = deferredHull.map { it.awaitAll() }

                        nasjon.avtaltavgrensningslinjer = (skall.flatMap { it.avtaltavgrensningslinjer } + hull.flatMap { it.flatMap { i -> i.avtaltavgrensningslinjer } }).toMutableSet()
                        nasjon.territorialgrenser = (skall.flatMap { it.territorialgrenser } + hull.flatMap { it.flatMap { i -> i.territorialgrenser } }).toMutableSet()
                        nasjon.riksgrenser = (skall.flatMap { it.riksgrenser } + hull.flatMap { it.flatMap { i -> i.riksgrenser } }).toMutableSet()


                        nasjon.flate = SosiGeometryFunctions.lagFlate(
                            skall.map { it.lineString!! },
                            hull.map { it.map { i -> i.lineString!! } },
                            flate.koordsys
                        )
                    }
                }

                nasjon = nasjonRepository.save(nasjon)

                if (flate.getInformasjon() != null) {
                    val nasjonInformasjon = NasjonInformasjon()
                    nasjonInformasjon.informasjon = flate.getInformasjon()
                    nasjonInformasjon.nasjonFk = nasjon
                    nasjonInformasjonRepository.save(nasjonInformasjon)
                }

                flate.getNasjonnavn().forEach { sosiNasjonnavn ->
                    val nasjonnavn = Administrativenhetnavn()
                    nasjonnavn.navn = sosiNasjonnavn.getNavn()
                    nasjonnavn.spraak = sosiNasjonnavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
                    nasjonnavn.rekkefoelge = sosiNasjonnavn.getRekkefolge()
                    nasjonnavn.nasjonFk = nasjon
                    administrativenhetnavnRepository.save(nasjonnavn)
                }
            }
            else -> {}
        }
    }

    private fun importerNasjonRef(nasjon: Nasjon, ref: SosiReferanse, root: SosiObjekt, rekkefoelge: Int, hullIndex: Int? = null): NasjonReferanseImportResult {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                val result = NasjonReferanseImportResult()
                when (importertGrense) {
                    is Avtaltavgrensningslinje -> result.avtaltavgrensningslinjer.add(NasjonAvtaltavgrensningslinje(nasjon, importertGrense, GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, hullIndex)))
                    is Territorialgrense -> result.territorialgrenser.add(NasjonTerritorialgrense(nasjon, importertGrense, GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, hullIndex)))
                    is Riksgrense -> result.riksgrenser.add(NasjonRiksgrense(nasjon, importertGrense, GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, hullIndex)))
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

data class NasjonReferanseImportResult(
    val avtaltavgrensningslinjer: MutableSet<NasjonAvtaltavgrensningslinje> = mutableSetOf(),
    val territorialgrenser: MutableSet<NasjonTerritorialgrense> = mutableSetOf(),
    val riksgrenser: MutableSet<NasjonRiksgrense> = mutableSetOf(),
    var lineString: LineString? = null
)