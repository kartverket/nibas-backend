package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.DefaultFeatureApiResponses
import no.kartverket.nibas.api.v1.common.geojson.Feature
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/grenser")
@Tag(
    name = "v1/grenser",
    description = "Endepunkt for å oppdatere et sett med grenser (FeatureCollection) i Nasjonal inndelingsbase"
)
interface GrenseApi {

    @Operation(summary = "Hent grense", description = "Henter grense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/{id}")
    fun hentGrense(
        @Parameter(description = "ID-en til grensen man vil hente") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for grense (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): Feature

}

