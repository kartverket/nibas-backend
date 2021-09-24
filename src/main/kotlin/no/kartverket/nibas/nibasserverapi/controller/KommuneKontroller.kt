package no.kartverket.nibas.nibasserverapi.controller

import no.kartverket.nibas.nibasserverapi.api.v1.Kommune
import no.kartverket.nibas.nibasserverapi.api.v1.toApiKommune
import no.kartverket.nibas.nibasserverapi.api.v1.KommuneApi as KommuneApiV1
import no.kartverket.nibas.nibasserverapi.service.KommuneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class KommuneKontroller : KommuneApiV1 {

    @Autowired
    lateinit var kommuneService: KommuneService;

    override fun getAll() =  kommuneService.hentAlleKommuner().map { toApiKommune(it) }

    override fun getOne(kommuneId: Int): Kommune {
        return when (val kommune = kommuneService.hentMedId(kommuneId)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toApiKommune(kommune)
        }
    }
}