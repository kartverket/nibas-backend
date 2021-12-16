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
                        val deferredSkall = flate.getSkallReferanser().map { ref ->
                            async {
                                importerNasjonRef(ref, root)
                            }
                        }

                        val deferredHull = flate.getHullReferanser().map { liste ->
                            liste.map { ref ->
                                async {
                                    importerNasjonRef(ref, root)
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

    private fun importerNasjonRef(ref: SosiReferanse, root: SosiObjekt): NasjonReferanseImportResult {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                val result = NasjonReferanseImportResult()
                when (importertGrense) {
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

data class NasjonReferanseImportResult(
    val avtaltavgrensningslinjer: MutableSet<Avtaltavgrensningslinje> = mutableSetOf(),
    val territorialgrenser: MutableSet<Territorialgrense> = mutableSetOf(),
    val riksgrenser: MutableSet<Riksgrense> = mutableSetOf(),
    var lineString: LineString? = null
)