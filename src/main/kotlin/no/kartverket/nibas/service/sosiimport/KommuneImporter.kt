package no.kartverket.nibas.service.sosiimport

import kotlinx.coroutines.*
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.model.SosiRef
import org.locationtech.jts.geom.LineString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger

@Component
class KommuneImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var kommunenummerRepository: KommunenummerRepository

    @Autowired
    lateinit var kommuneInformasjonRepository: KommuneInformasjonRepository

    @Autowired
    lateinit var kommuneRepository: KommuneRepository

    @Autowired
    lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Autowired
    lateinit var spraakkodeRepository: SpraakkodeRepository

    @Autowired
    lateinit var grenseImporter: GrenseImporter

    @Autowired
    lateinit var administrativEnhetImporter: AdministrativEnhetImporter

    @Transactional
    fun importerSosiKommune(flate: SosiFlate, root: SosiObjekt) {
        when (kommuneRepository.findByLokalid(flate.getIdent()?.getLokalId()!!)) {
            null -> {
                var kommune = administrativEnhetImporter.importer(flate) as Kommune
                kommune.kommunenummer = flate.getKommunenummer()?.let { kommunenummerRepository.findByIdOrNull(it) }
                kommune.samiskforvaltningsomraade = flate.isSamiskForvaltningsomrade()

                runBlocking {
                    withContext(Dispatchers.Default) {
                        val deferredSkall = flate.getSkallRef().map { ref ->
                            async {
                                importerKommuneRef(ref, kommune, root)
                            }
                        }

                        val deferredHull = flate.getHullRef().map { liste ->
                            liste.map { ref ->
                                async {
                                    importerKommuneRef(ref, kommune, root)
                                }
                            }
                        }

                        val skall = deferredSkall.awaitAll()
                        val hull = deferredHull.map { it.awaitAll() }
                        kommune.flate = SosiGeometryFunctions.lagFlate(skall, hull, flate.koordsys)
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

    private fun importerKommuneRef(ref: SosiRef, kommune: Kommune, root: SosiObjekt): LineString {
        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                val importertGrense = grenseImporter.importerGrense(grenseRef)
                when (importertGrense) {
                    is Kommunegrense -> kommune.kommunegrenser.add(importertGrense)
                    is Fylkesgrense -> kommune.fylkesgrenser.add(importertGrense)
                    is Avtaltavgrensningslinje -> kommune.avtaltavgrensningslinjer.add(importertGrense)
                    is Territorialgrense -> kommune.territorialgrenser.add(importertGrense)
                    is Riksgrense -> kommune.riksgrenser.add(importertGrense)
                }
                return when (ref.isRetningReversed()) {
                    true -> importertGrense.grense!!.reverse()
                    false -> importertGrense.grense!!
                }
            }
        }
    }
}