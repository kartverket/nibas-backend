package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.findGyldigKommuneForKommunenummer
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
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
    fun importerSosiGrunnkrets(flater: List<SosiFlate>, grunnkretsgrenser: List<SosiGeometri>, erstatningskoordinater: MutableMap<Coordinate, Erstatningskoordinat>, matchedeGrenser: MutableMap<String, List<SplitTilImport>>) {
        // Henter egenskapene bare fra første flate. Egenskapene er/skal være like på tvers av flatene.
        val flate = flater.first()
        val sosiIdent = flate.getIdent()
        if (grunnkretsRepository.findLastByLokalid(sosiIdent?.getLokalId()!!) != null) {
            logger.info("Bruker eksisterende flate")
        } else {
            val lokalid: String? = sosiIdent.getValidLokalId()
            var grunnkrets = if (lokalid != null) Grunnkrets(lokalid = lokalid) else Grunnkrets()

            grunnkrets.datafangstdato = flate.getDatafangstDato()
            grunnkrets.gyldighet = Gyldighet(flate.getGyldigFra(), flate.getGyldigTil())
            grunnkrets.opphav = flate.getOpphav()

            grunnkrets.grunnkretsnummer = flate.getGrunnkretsId()?.getGrunnkretsnummer()
            grunnkrets.grunnkretsnavn = flate.getGrunnkretsId()?.getGrunnkretsnavn()

            grunnkrets.posisjon = SosiGeometryFunctions.sosiKoordinatToPoint(flate.getAlleSosiNordOest(), flate.koordsys)

            grunnkrets.setInfo(flate.getInformasjon())

            flater.forEachIndexed { skall_index, gkFlate ->
                gkFlate.getSkallReferanser().forEachIndexed { rekkefoelge, ref ->
                    grenseImporter.importerGrenseRef(grunnkrets, ref, grunnkretsgrenser, skall_index, rekkefoelge, erstatningskoordinater = erstatningskoordinater, matchedeGrenser = matchedeGrenser)
                }

                gkFlate.getHullReferanser().forEachIndexed { hullIndex, liste ->
                    liste.forEachIndexed { rekkefoelge, ref ->
                        grenseImporter.importerGrenseRef(grunnkrets, ref, grunnkretsgrenser, skall_index, rekkefoelge, hullIndex, erstatningskoordinater, matchedeGrenser)

                    }
                }
            }

            grunnkrets.oppdaterOmraadeOgPosisjon()
            grunnkrets = grunnkretsRepository.save(grunnkrets)

            val kommunenummer = flate.getKommunenummer()
            val gyldighetsdato = grunnkrets.gyldighet.gyldigFra
            val kommune = kommuneRepository.findGyldigKommuneForKommunenummer(kommunenummer, gyldighetsdato)
                ?: throw RuntimeException("Fant ikke kommune med kommunenummer $kommunenummer og gyldig fra $gyldighetsdato")

            kommune.addGrunnkrets(grunnkrets)
        }
    }
}
