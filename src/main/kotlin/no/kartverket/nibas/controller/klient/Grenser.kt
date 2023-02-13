package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.GrenseApi
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.createIdentifikator
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.toFeature
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class GrenseController(
    private val grenseService: GrenseService,
) : GrenseApi {

    override fun hentGrense(lokalid: String, gyldighetsdato: LocalDate?): Feature {
        val identifikatorForGrense = createIdentifikator(lokalid, gyldighetsdato)
        return grenseService.hentGrense(identifikatorForGrense)?.toFeature()
            ?: throw NotFoundException("Grense $identifikatorForGrense ble ikke funnet.")
    }

}
