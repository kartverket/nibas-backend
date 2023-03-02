package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.ekstern.StemmekretsApi
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.toStemmekretsResponse
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.toKretsResponse
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternStemmekretsController")
class StemmekretsController(
    private val stemmekretsService: StemmekretsService,
    private val kodelisteService: KodelisteService,
) : StemmekretsApi {

    override fun hentStemmekrets(lokalid: String, gyldighetsdato: LocalDate?): StemmekretsResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return stemmekretsService.hentStemmekrets(identifikator)?.toStemmekretsResponse(identifikator, kodelisteService)
            ?: throw NotFoundException("Stemmekrets $identifikator ble ikke funnet.")
    }

    override fun hentStemmekretserForIds(ids: Set<String>, gyldighetsdato: LocalDate?): List<KretsResponse> {
        return stemmekretsService.hentStemmekretserForIds(ids, gyldighetsdato ?: LocalDate.now()).map { it.toKretsResponse(kodelisteService) }
    }
}
