package no.kartverket.nibas.nibasserverapi.controller

import no.kartverket.nibas.nibasserverapi.api.v1.fromKommuneRequest
import no.kartverket.nibas.nibasserverapi.api.v1.request.Kommune as KommuneRequest
import no.kartverket.nibas.nibasserverapi.api.v1.response.Kommune as KommuneResponse
import no.kartverket.nibas.nibasserverapi.api.v1.toKommuneResponse
import no.kartverket.nibas.nibasserverapi.api.v1.KommuneApi as KommuneApiV1
import no.kartverket.nibas.nibasserverapi.service.KommuneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class KommuneKontroller : KommuneApiV1 {

    @Autowired
    lateinit var kommuneService: KommuneService

    override fun getAll() = kommuneService.hentAlleKommuner().map { toKommuneResponse(it) }

    override fun getOne(kommuneId: Int): KommuneResponse {
        return when (val kommune = kommuneService.hentMedId(kommuneId)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toKommuneResponse(kommune)
        }
    }

    override fun oppdaterKommune(kommuneId: Int, oppdatertKommune: KommuneRequest): KommuneResponse {
        kommuneService.oppdaterKommune(fromKommuneRequest(kommuneId, oppdatertKommune))
        return getOne(kommuneId)
    }

}