package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findGyldigKommuneForKommunenummer
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.locationtech.jts.geom.Coordinate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StemmekretsImporter @Autowired constructor(private val kommunalKretsImporter: KommunalKretsImporter,
                                                 private val grenseImporter: GrenseImporter,
                                                 private val stemmekretsRepository: StemmekretsRepository,
                                                 private val kommuneRepository: KommuneRepository,
                                                 private val kodelisteService: KodelisteService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun importerSosiStemmekrets(flater: List<SosiFlate>, stemmekretsgrenser: List<SosiGeometri>, erstatningskoordinater: MutableMap<Coordinate, Erstatningskoordinat>, matchedeGrenser: MutableMap<String, List<SplitTilImport>>) {
        // Henter egenskaper fra første flate. Antar at all metadata er lik
        val forsteFlate = flater.first()
        val sosiIdent = forsteFlate.getIdent()

        if (stemmekretsRepository.findLastByLokalid(sosiIdent?.getLokalId()!!) != null) {
            logger.info("Stemmekrets med lokalId ${sosiIdent.getLokalId()!!} er allerede importert, bruker eksisterende stemmekrets")
        } else {
            var stemmekrets = kommunalKretsImporter.importer(forsteFlate) as Stemmekrets
            val stemmekretsId = forsteFlate.getStemmekretsId()
            stemmekrets.stemmekretsnummer = stemmekretsId?.getStemmekretsnummer()
            stemmekrets.stemmekretsnavn = stemmekretsId?.getStemmekretsnavn()
            stemmekrets.tellekretsnummer = stemmekretsId?.getTellekretsnummer()
            stemmekrets.tellekretsnavn = stemmekretsId?.getTellekretsnavn()
            stemmekrets.valgdistriktsnummer = stemmekretsId?.getValgdistriktsnummer()

            stemmekrets.setInfo(forsteFlate.getInformasjon())
            stemmekrets.posisjon = SosiGeometryFunctions.sosiKoordinatToPoint(forsteFlate.getAlleSosiNordOest(), forsteFlate.koordsys)

            flater.forEachIndexed { skall_index, gkFlate ->
                gkFlate.getSkallReferanser().forEachIndexed { rekkefoelge, ref ->
                    grenseImporter.importerGrenseRef(stemmekrets, ref, stemmekretsgrenser, skall_index, rekkefoelge, erstatningskoordinater = erstatningskoordinater, matchedeGrenser = matchedeGrenser)
                }

                gkFlate.getHullReferanser().forEachIndexed { hullIndex, liste ->
                    liste.forEachIndexed { rekkefoelge, ref ->
                        grenseImporter.importerGrenseRef(stemmekrets, ref, stemmekretsgrenser, skall_index, rekkefoelge, hullIndex, erstatningskoordinater, matchedeGrenser)
                    }
                }
            }

            stemmekrets.oppdaterOmraadeOgPosisjon()
            stemmekrets = stemmekretsRepository.save(stemmekrets)

            // Setter kommunenummerId fra GeoNorge samt kobling til kommune
            val kommunenummerUuid = kodelisteService.getKommunenummerIdByCodeValue(forsteFlate.getKommunenummer())
            val gyldighetsdato = stemmekrets.gyldighet.gyldigFra
            val kommune = kommuneRepository.findGyldigKommuneForKommunenummer(kommunenummerUuid, gyldighetsdato)
                ?: throw RuntimeException("Fant ikke kommune med kommunenummerUuid $kommunenummerUuid og gyldig fra $gyldighetsdato")

            kommune.addStemmekrets(stemmekrets)
        }
    }
}
