package no.kartverket.nibas.controller

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.response.AdministrativEnhet
import no.kartverket.nibas.api.v1.toAdministrativEnhetResponse
import no.kartverket.nibas.service.AdministrativEnhetService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import no.kartverket.nibas.api.v1.AdministrativEnhetApi as AdministrativEnhetApiV1
import no.kartverket.nibas.api.v1.request.AdministrativEnhet as AdministrativEnhetRequest

@RestController
class AdministrativEnhetKontroller : AdministrativEnhetApiV1 {

    @Autowired
    lateinit var administrativenhetService: AdministrativEnhetService

    override fun hentAlle(
        type: AdministrativEnhetType
    ): List<AdministrativEnhet> {
        return administrativenhetService.hentAlleAdministrativeEnheter(type).map { toAdministrativEnhetResponse(it, type) }
    }

    override fun hentEn(id: Int, type: AdministrativEnhetType): AdministrativEnhet {
        return when (val administrativEnhet = administrativenhetService.hentAdministrativEnhet(id, type)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toAdministrativEnhetResponse(administrativEnhet, type)
        }
    }

    override fun oppdaterAdministrativEnhet(
        id: Int,
        oppdatertAdministrativEnhet: AdministrativEnhetRequest
    ): AdministrativEnhet {
        return toAdministrativEnhetResponse(administrativenhetService.oppdaterAdministrativEnhet(id, oppdatertAdministrativEnhet), oppdatertAdministrativEnhet.type)
    }

}