package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiKurve
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger

@Component
class GrunnkretsImporter @Autowired constructor(
    private val grenseImporter: GrenseImporter,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val kodelisteService: KodelisteService,
    private val kommuneRepository: KommuneRepository
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiGrunnkrets(flater: List<SosiFlate>, kurver: List<SosiKurve>, admenhetRoot: SosiObjekt) {
        // Henter egenskapene bare fra første flate. Egenskapene er/skal være like på tvers av flatene.
        val flate = flater.first()
        val sosiIdent = flate.getIdent()
        if (grunnkretsRepository.findByLokalid(sosiIdent?.getLokalId()!!) != null) {
            logger.info("Bruker eksisterende flate")
        } else {
            val lokalid: String? = sosiIdent.getValidLokalId()
            val grunnkrets = if (lokalid != null) Grunnkrets(lokalid = lokalid) else Grunnkrets()

            val kommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(flate.getKommunenummer())
            grunnkrets.kommune = kommuneRepository.findByKommunenummer(kommunenummerId)!!

            // Hent grensene til kommunen som grunnkretsen er en del av (for å kun gjøre oppslaget èn gang)
            val kommuneFlater = admenhetRoot.getFlater().filter { it.getObjType() == SosiKoder.KOMMUNE && it.getKommunenummer() == flate.getKommunenummer() }
            kommuneFlater.forEach { it.lastReferanser(admenhetRoot) }

            grunnkrets.datafangstdato = flate.getDatafangstDato()
            grunnkrets.gyldighet = Gyldighet(flate.getGyldigFra(), flate.getGyldigTil())
            grunnkrets.setInfo(flate.getInformasjon())
            grunnkrets.opphav = flate.getOpphav()

            grunnkrets.grunnkretsnummer = flate.getGrunnkretsId()?.getGrunnkretsnummer()
            grunnkrets.grunnkretsnavn = flate.getGrunnkretsId()?.getGrunnkretsnavn()

            grunnkrets.posisjon = SosiGeometryFunctions.sosiKoordinatToPoint(flate.getAlleSosiNordOest(), flate.koordsys)

            grunnkrets.setInfo(flate.getInformasjon())

            flater.forEachIndexed { skall_index, gkFlate ->
                gkFlate.getSkallReferanser().forEachIndexed { _, ref ->
                    grenseImporter.importerGrenseRef(grunnkrets, ref, kurver, kommuneFlater, skall_index)
                }

                gkFlate.getHullReferanser().forEachIndexed { hullIndex, liste ->
                    liste.forEachIndexed { _, ref ->
                        grenseImporter.importerGrenseRef(grunnkrets, ref, kurver, kommuneFlater, skall_index, hullIndex)

                    }
                }
            }

            grunnkrets.oppdaterOmraade()
            grunnkretsRepository.save(grunnkrets)
        }
    }
}
