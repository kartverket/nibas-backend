package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.MatrikkelApi
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.utkast.NotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController

@RestController
class MatrikkelController(private val matrikkelService: MatrikkelService)
    : MatrikkelApi {

    override fun hentStemmekretserForIds(id: Set<String>): List<StemmekretsResponse> {
        return matrikkelService.hentStemmekretserForIds(id)
    }

    override fun hentAlleStemmekretserForKommune(kommunenummer: String): List<StemmekretsResponse> {
        return matrikkelService.hentAlleStemmekretserForKommune(kommunenummer)
    }

    override fun hentGrunnkretserForIds(id: Set<String>): List<GrunnkretsResponse> {
        return matrikkelService.hentGrunnkretserForIds(id)
    }

    override fun hentAlleGrunnkretserForKommune(kommunenummer: String): List<GrunnkretsResponse> {
        return matrikkelService.hentAlleGrunnkretserForKommune(kommunenummer)
    }
}

/**
 * Skiller ut egen service for å sørge for at kallene kan markeres med read-only transaksjons-markering.
 */
@Transactional(readOnly = true)
@Service
class MatrikkelService(private val grunnkretsControllerService: GrunnkretsControllerService,
                       private val stemmekretsControllerService: StemmekretsControllerService,
                       private val kodelisteService: KodelisteService,
                       private val kommuneService: KommuneService) {
    private val logger: Logger = LoggerFactory.getLogger(MatrikkelController::class.java)
    fun hentStemmekretserForIds(id: Set<String>): List<StemmekretsResponse> {
        return id.map { stemmekretsControllerService.hentEn(it) }.sortedBy { it.id }
    }

    fun hentAlleStemmekretserForKommune(numeriskKommunenummer: String): List<StemmekretsResponse> {
        return hentKommuneFraNumeriskNummer(numeriskKommunenummer).stemmekretser
            .map { it.toStemmekretsResponse(kodelisteService) }
            .sortedBy { it.id }
    }

    fun hentGrunnkretserForIds(id: Set<String>): List<GrunnkretsResponse> {
        return id.map { grunnkretsControllerService.hentEn(it) }.sortedBy { it.id }
    }

    fun hentAlleGrunnkretserForKommune(kommunenummer: String): List<GrunnkretsResponse> {
        return hentKommuneFraNumeriskNummer(kommunenummer).grunnkretser
            .map { it.toGrunnkretsResponse(kodelisteService) }
            .sortedBy { it.id }
    }

    private fun hentKommuneFraNumeriskNummer(numeriskKommunenummer: String): Kommune {
        try {
            val kommunenummerUUID = kodelisteService.getKommunenummerIdByCodeValue(numeriskKommunenummer)
            return kommuneService.hentKommuneByKommunenummer(kommunenummerUUID)
                ?: throw NotFoundException("Fant ikke kommune med nummer: $numeriskKommunenummer")
        } catch (ex: NoSuchElementException) {
            throw NotFoundException("Fant ikke kommunenummer-id for $numeriskKommunenummer")
        }
    }
}
