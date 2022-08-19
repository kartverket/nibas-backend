package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.MatrikkelApi
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class MatrikkelController(private val grunnkretsControllerService: GrunnkretsControllerService,
                          private val stemmekretsControllerService: StemmekretsControllerService)
    : MatrikkelApi {
    override fun hentStemmekretserForIds(id: Set<String>): List<StemmekretsResponse> {
        return id.map { stemmekretsControllerService.hentEn(it) }.sortedBy { it.id }
    }

    override fun hentGrunnkretserForIds(id: Set<String>): List<GrunnkretsResponse> {
        return id.map { grunnkretsControllerService.hentEn(it) }.sortedBy { it.id }
    }
}
