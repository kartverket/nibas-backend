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
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiKurve
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
    fun importerSosiStemmekrets(flater: List<SosiFlate>, kurver: List<SosiKurve>, admenhetRoot: SosiObjekt) {
        // Henter egenskaper fra første flate. Antar at all metadata er lik
        val forsteFlate = flater.first()
        val sosiIdent = forsteFlate.getIdent()

        if (stemmekretsRepository.findByLokalid(sosiIdent?.getLokalId()!!) != null) {
            logger.info("Stemmekrets med lokalId ${sosiIdent.getLokalId()!!} er allerede importert.")
        } else {
            val stemmekrets = kommunalKretsImporter.importer(forsteFlate) as Stemmekrets
            val stemmekretsId = forsteFlate.getStemmekretsId()
            stemmekrets.stemmekretsnummer = stemmekretsId?.getStemmekretsnummer()
            stemmekrets.stemmekretsnavn = stemmekretsId?.getStemmekretsnavn()
            stemmekrets.tellekretsnummer = stemmekretsId?.getTellekretsnummer()
            stemmekrets.tellekretsnavn = stemmekretsId?.getTellekretsnavn()
            stemmekrets.valgdistriktsnummer = stemmekretsId?.getValgdistriktsnummer()

            stemmekrets.posisjon = SosiGeometryFunctions.sosiKoordinatToPoint(forsteFlate.getAlleSosiNordOest(), forsteFlate.koordsys)

            // Hent grensene til kommunen som stemmekretsen er en del av (for å kun gjøre oppslaget èn gang)
            val kommuneFlater = admenhetRoot.getFlater().filter { it.getObjType() == SosiKoder.KOMMUNE && it.getKommunenummer() == forsteFlate.getKommunenummer() }
            kommuneFlater.forEach { it.lastReferanser(admenhetRoot) }

            // Setter kommunenummerId fra GeoNorge samt kobling til kommune
            val kommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(forsteFlate.getKommunenummer())
            stemmekrets.kommunenummer = kommunenummerId
            stemmekrets.kommune = kommuneRepository.findByKommunenummer(kommunenummerId)!!

            stemmekrets.informasjon = forsteFlate.getInformasjon()

            flater.forEachIndexed { skall_index, gkFlate ->
                gkFlate.getSkallReferanser().forEachIndexed { rekkefoelge, ref ->
                    grenseImporter.importerGrenseRef(stemmekrets, ref, kurver, kommuneFlater, skall_index, rekkefoelge)
                }

                gkFlate.getHullReferanser().forEachIndexed { hullIndex, liste ->
                    liste.forEachIndexed { rekkefoelge, ref ->
                        grenseImporter.importerGrenseRef(stemmekrets, ref, kurver, kommuneFlater, skall_index, rekkefoelge, hullIndex)
                    }
                }
            }

            stemmekrets.oppdaterFlate()
            stemmekretsRepository.save(stemmekrets)
        }
    }
}
