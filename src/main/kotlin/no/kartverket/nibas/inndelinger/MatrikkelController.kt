package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.MatrikkelApi
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class MatrikkelController(private val matrikkelService: MatrikkelService,
                          private val kodelisteService: KodelisteService) : MatrikkelApi {

    override fun hentStemmekretserForIds(id: Set<String>): List<KretsResponse> {
        return matrikkelService.hentStemmekretserForIds(id).map { it.toKretsResponse(kodelisteService) }
    }

    override fun hentAlleStemmekretserForKommune(kommunenummer: String): List<KretsResponse> {
        return matrikkelService.hentAlleStemmekretserForKommune(kommunenummer).map { it.toKretsResponse(kodelisteService) }
    }

    override fun hentGrunnkretserForIds(id: Set<String>): List<KretsResponse> {
        return matrikkelService.hentGrunnkretserForIds(id).map { it.toKretsResponse(kodelisteService) }
    }

    override fun hentAlleGrunnkretserForKommune(kommunenummer: String): List<KretsResponse> {
        return matrikkelService.hentAlleGrunnkretserForKommune(kommunenummer).map { it.toKretsResponse(kodelisteService) }
    }
}

/**
 * Skiller ut egen service for å sørge for at kallene kan markeres med read-only transaksjons-markering.
 */
@Transactional(readOnly = true)
@Service
class MatrikkelService(private val grunnkretsService: GrunnkretsService,
                       private val stemmekretsService: StemmekretsService,
                       private val kodelisteService: KodelisteService,
                       private val kommuneService: KommuneService) {

    fun hentStemmekretserForIds(ids: Set<String>): List<Stemmekrets> {
        return ids.map { fetchStemmekrets(it) }.sortedBy { it.id }
    }

    fun hentAlleStemmekretserForKommune(numeriskKommunenummer: String): List<Stemmekrets> {
        return hentKommuneFraNumeriskNummer(numeriskKommunenummer).stemmekretser
            .sortedBy { it.id }
    }

    fun hentGrunnkretserForIds(ids: Set<String>): List<Grunnkrets> {
        return ids.map { Lokalid(it) }.map { fetchGrunnkrets(it) }.sortedBy { it.id }
    }

    fun hentAlleGrunnkretserForKommune(kommunenummer: String): List<Grunnkrets> {
        return hentKommuneFraNumeriskNummer(kommunenummer).grunnkretser
            .sortedBy { it.id }
    }

    private fun fetchStemmekrets(id: String): Stemmekrets = stemmekretsService.hentStemmekrets(id)
        ?: throw NotFoundException("Stemmekrets med id '$id' ble ikke funnet.")

    private fun fetchGrunnkrets(id: Lokalid): Grunnkrets = grunnkretsService.hentGrunnkrets(id, LocalDate.now())
        ?: throw NotFoundException("Grunnkrets med id '$id' ble ikke funnet.")

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
