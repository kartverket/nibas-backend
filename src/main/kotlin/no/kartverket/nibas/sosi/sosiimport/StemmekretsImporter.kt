package no.kartverket.nibas.sosi.sosiimport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.model.SosiReferanse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger

@Component
class StemmekretsImporter @Autowired constructor(private val kommunalKretsImporter: KommunalKretsImporter,
                                                 private val grenseImporter: GrenseImporter,
                                                 private val stemmekretsRepository: StemmekretsRepository,
                                                 private val kommuneRepository: KommuneRepository,
                                                 private val kodelisteService: KodelisteService) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiStemmekrets(flater: List<SosiFlate>, root: SosiObjekt) {
        // Henter egenskaper fra første flate. Antar at all metadata er lik
        val forsteFlate = flater.first()

        val stemmekrets = kommunalKretsImporter.importer(forsteFlate) as Stemmekrets
        val stemmekretsId = forsteFlate.getStemmekretsId()
        stemmekrets.stemmekretsnummer = stemmekretsId?.getStemmekretsnummer()
        stemmekrets.stemmekretsnavn = stemmekretsId?.getStemmekretsnavn()
        stemmekrets.tellekretsnummer = stemmekretsId?.getTellekretsnummer()
        stemmekrets.tellekretsnavn = stemmekretsId?.getTellekretsnavn()
        stemmekrets.valgdistriktsnummer = stemmekretsId?.getValgdistriktsnummer()

        stemmekrets.posisjon = SosiGeometryFunctions.sosiKoordinatToPoint(forsteFlate.getAlleSosiNordOest(), forsteFlate.koordsys)

        // Setter kommunenummerId fra GeoNorge samt kobling til kommune
        val kommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(forsteFlate.getKommunenummer())
        stemmekrets.kommunenummer = kommunenummerId
        stemmekrets.kommune = kommuneRepository.findByKommunenummer(kommunenummerId)!!

        stemmekrets.informasjon = forsteFlate.getInformasjon()

        runBlocking {
            withContext(Dispatchers.Default) {
                flater.forEachIndexed { skall_index, flate ->
                    val skall: List<Job> = flate.getSkallReferanser().mapIndexed { rekkefoelge, ref ->
                        launch {
                            importerStemmekretsRef(stemmekrets, ref, root, skall_index, rekkefoelge, null)
                        }
                    }

                    val hull: List<List<Job>> = flate.getHullReferanser().mapIndexed { hullIndex, liste ->
                        liste.mapIndexed { rekkefoelge, ref ->
                            launch {
                                importerStemmekretsRef(stemmekrets, ref, root, skall_index, rekkefoelge, hullIndex)
                            }
                        }
                    }

                    skall.joinAll()
                    hull.forEach { it.joinAll() }
                }
            }
        }

        stemmekrets.oppdaterFlate()
        stemmekretsRepository.save(stemmekrets)
    }

    fun importerStemmekretsRef(stemmekrets: Stemmekrets, ref: SosiReferanse, root: SosiObjekt, flateIndex: Int, rekkefoelge: Int, hullIndex: Int? = null) {
        val referanse = ref.getVerdiUtenRetningOgParentes()

        when (val grenseRef = root.getObjektMedId(referanse)) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                stemmekrets.addGrensekobling(
                    grense = grenseImporter.importerGrense(grenseRef),
                    grenseKoblingAttributter = GrenseKoblingAttributter(ref.erRetningMedKlokken(), rekkefoelge, flateIndex, hullIndex)
                )
            }
        }
    }
}
