package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.response.RevisjonRef
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/v1/grenser")
@Tag(
    name = "v1/grenser",
    description = "Endepunkt for å oppdatere et sett med grenser (FeatureCollection) i Nasjonal inndelingsbase"
)
interface GrenseApi {

    @Operation(summary = "Hent grense", description = "Henter grense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/{id}")
    fun hentGrense(@Parameter(description = "ID-en til grensen man vil hente") @PathVariable id: String): Feature

    @Operation(summary = "Hent en historisk revisjon for grense", description = "Henter gitt historisk revisjon for gitt grense")
    @DefaultFeatureApiResponses
    @GetMapping("/{id}/revisjoner/{revisjon}")
    fun hentGrenseRevisjon(
        @Parameter(description = "ID til grensen man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): Feature

    @Operation(summary = "Hent historiske revisjoner for grense", description = "Henter historiske revisjoner for grense med gitt id")
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/{id}/revisjoner")
    fun hentGrenseRevisjoner(@Parameter(description = "ID-en til grensen man vil hente") @PathVariable id: String): List<RevisjonRef>

}

