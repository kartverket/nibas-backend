package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.ekstern.MatrikkelApi
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.service.MatrikkelService
import no.kartverket.nibas.service.toKretsResponse
import no.kartverket.nibas.kodelister.KodelisteService
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
