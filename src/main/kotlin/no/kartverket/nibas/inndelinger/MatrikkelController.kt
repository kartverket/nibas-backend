package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.MatrikkelApi
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController

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
        return ids.map { fetchStemmekrets(Lokalid(it)) }.sortedBy { it.id }
    }

    fun hentAlleStemmekretserForKommune(numeriskKommunenummer: String): List<Stemmekrets> {
        return hentKommuneFraNumeriskNummer(numeriskKommunenummer).stemmekretser
            .sortedBy { it.id }
    }

    fun hentGrunnkretserForIds(ids: Set<String>): List<Grunnkrets> {
        return ids.map { fetchGrunnkrets(Lokalid(it)) }.sortedBy { it.id }
    }

    fun hentAlleGrunnkretserForKommune(kommunenummer: String): List<Grunnkrets> {
        return hentKommuneFraNumeriskNummer(kommunenummer).grunnkretser
            .sortedBy { it.id }
    }

    private fun fetchStemmekrets(lokalid: Lokalid): Stemmekrets {
        val identifikator = Identifikator(lokalid)
        return stemmekretsService.hentStemmekrets(identifikator)
            ?: throw NotFoundException("Stemmekrets med $identifikator ble ikke funnet.")
    }

    private fun fetchGrunnkrets(lokalid: Lokalid): Grunnkrets {
        val identifikator = Identifikator(lokalid)
        return grunnkretsService.hentGrunnkrets(identifikator)
            ?: throw NotFoundException("Grunnkrets med $identifikator ble ikke funnet.")
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
