package no.kartverket.nibas.sosi.sosiimport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrenseMedAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.updateSosiIdentifikasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.TypeendringRepository
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.sosi.model.SosiKurve
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.model.SosiReferanse
import org.locationtech.jts.geom.LineString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger
import kotlin.math.log
import kotlin.math.min
import kotlin.system.measureTimeMillis

@Component
class GrunnkretsImporter @Autowired constructor(
    private val grenseImporter: GrenseImporter,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val typeendringRepository: TypeendringRepository,
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
            val grunnkrets = Grunnkrets()
            val kommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(flate.getKommunenummer())
            grunnkrets.kommunenummer = kommunenummerId
            grunnkrets.kommune = kommuneRepository.findByKommunenummer(kommunenummerId)!!

            // Hent grensene til kommunen som grunnkretsen er en del av (for å kun gjøre oppslaget èn gang)
            val kommuneFlater = admenhetRoot.getFlater().filter { it.getObjType() == SosiKoder.KOMMUNE && it.getKommunenummer() == flate.getKommunenummer() }
            kommuneFlater.forEach { it.lastReferanser(admenhetRoot) }

            grunnkrets.updateSosiIdentifikasjon(sosiIdent)
            grunnkrets.datafangstdato = flate.getDatafangstDato()
            grunnkrets.endringsflagg?.tidspunktendring = flate.getEndringsflagg()?.getTidspunktEndring()
            grunnkrets.endringsflagg?.typeendring = when (val typeendring = flate.getEndringsflagg()?.getTypeEndring()) {
                null -> null
                else -> typeendringRepository.findByIdOrNull(typeendring)
            }
            grunnkrets.gyldigFra = flate.getGyldigFra()
            grunnkrets.gyldigTil = flate.getGyldigTil()
            grunnkrets.informasjon = flate.getInformasjon()
            grunnkrets.kopidata?.kopidato = flate.getKopidata()?.getKopidato()
            grunnkrets.kopidata?.omraadeid = flate.getKopidata()?.getOmraadeId()
            grunnkrets.kopidata?.originaldatavert = flate.getKopidata()?.getOriginalDatavert()
            grunnkrets.opphav = flate.getOpphav()

            grunnkrets.registreringsversjon?.produkt = flate.getRegistreringsversjon()?.getProdukt()
            grunnkrets.registreringsversjon?.versjon = flate.getRegistreringsversjon()?.getVersjon()

            grunnkrets.sluttdato = flate.getSluttdato()

            grunnkrets.grunnkretsnummer = flate.getGrunnkretsId()?.getGrunnkretsnummer()
            grunnkrets.grunnkretsnavn = flate.getGrunnkretsId()?.getGrunnkretsnavn()

            grunnkrets.posisjon = SosiGeometryFunctions.sosiKoordinatToPoint(flate.getAlleSosiNordOest(), flate.koordsys)

            grunnkrets.informasjon = flate.getInformasjon()

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

            grunnkrets.oppdaterFlate()
            grunnkretsRepository.save(grunnkrets)
        }
    }
}
