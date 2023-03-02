package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.api.v1.ekstern.GrunnkretsApi
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.toKretsResponse
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("EksternGrunnkretsController")
class GrunnkretsController(
    private val grunnkretsService: GrunnkretsService,
    private val kodelisteService: KodelisteService,
) : GrunnkretsApi {

    override fun hentGrunnkretserForIds(ids: Set<String>, gyldighetsdato: LocalDate?): List<KretsResponse> {
        return grunnkretsService.hentGrunnkretserForIds(ids, gyldighetsdato ?: LocalDate.now()).map { it.toKretsResponse(kodelisteService) }
    }
}
