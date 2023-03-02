package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.ekstern.KommuneApi
import no.kartverket.nibas.api.v1.response.KommuneResponse
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.toKommuneResponse
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.toKretsResponse
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternKommuneController")
class KommuneController(
    private val kommuneService: KommuneService,
    private val kodelisteService: KodelisteService,
) : KommuneApi {

    override fun hentKommune(lokalid: String, gyldighetsdato: LocalDate?): KommuneResponse {
        val identifikator = createIdentifikator(lokalid, gyldighetsdato)
        return kommuneService.hentKommune(identifikator)?.toKommuneResponse(identifikator, kodelisteService)
            ?: throw NotFoundException("Kommune $identifikator ble ikke funnet.")
    }

    override fun hentAlleStemmekretserForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<KretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleStemmekretserForKommune(kommunenummer, dato).map { it.toKretsResponse(kodelisteService) }
    }

    override fun hentAlleGrunnkretserForKommune(kommunenummer: String, gyldighetsdato: LocalDate?): List<KretsResponse> {
        val dato = gyldighetsdato ?: LocalDate.now()
        return kommuneService.hentAlleGrunnkretserForKommune(kommunenummer, dato).map { it.toKretsResponse(kodelisteService) }
    }
}
