package no.kartverket.nibas.api.v1.ekstern

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.DefaultFeatureCollectionApiResponses
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.ekstern.response.EksternBopliktomraadeRespons
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/ekstern/bopliktomraader")
@Tag(
    name = "v1/ekstern/bopliktomraader",
    description = "Endepunkter for å hente informasjon om bopliktomraader i Nasjonal inndelingsbase"
)
interface BopliktomraadeApi {

    @Operation(summary = "Henter alle bopliktområder")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "OK",
            content = [
                Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = EksternBopliktomraadeRespons::class))
                )
            ]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleBopliktomraader(
        @RequestParam(required = false, name = "gyldighetsdato")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        gyldighetsdato: LocalDate?
    ): List<EksternBopliktomraadeRespons>

    @Operation(summary = "Henter ett bopliktområde basert på ID")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "OK",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = EksternBopliktomraadeRespons::class)
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentBopliktomraade(
        @PathVariable("id") lokalid: String,
        @RequestParam(required = false, name = "gyldighetsdato")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        gyldighetsdato: LocalDate?
    ): EksternBopliktomraadeRespons

    @Operation(
        summary = "Hent grensene til et gitt bopliktområde",
        description = "Henter grensene til et bopliktområde med gitt id"
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenserForBopliktomraade(
        @PathVariable(name = "id") lokalid: String,
        @RequestParam(required = false, name = "gyldighetsdato")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        gyldighetsdato: LocalDate?
    ): FeatureCollection
}

