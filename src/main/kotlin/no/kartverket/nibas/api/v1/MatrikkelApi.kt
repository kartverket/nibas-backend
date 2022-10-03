package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/v1/ekstern/matrikkel")
@Tag(
    name = "v1/ekstern/matrikkel",
    description = "Endepunkter som tilbys til Matrikkel."
)
interface MatrikkelApi {
    @Operation(summary = "Henter stemmekretser med gitte id-er", description = "Henter stemmekretser med gitte id-er")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = StemmekretsResponse::class))
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/stemmekretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentStemmekretserForIds(@Parameter(description = "ID-ene til stemmekretsene man vil hente") @RequestParam id: Set<String>): List<StemmekretsResponse>

    @Operation(summary = "Henter alle stemmekretser for gitt kommunenummer", description = "Henter alle stemmekretser for gitt kommunenummer")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = StemmekretsResponse::class))
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/kommuner/{kommunenummer}/stemmekretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleStemmekretserForKommune(@Parameter(description = "Kommunenummer for kommune man ønsker å hente stemmekretsene til") @PathVariable kommunenummer: String): List<StemmekretsResponse>


    @Operation(summary = "Henter grunnkretser med gitte id-er", description = "Henter grunnkretser med gitte id-er")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = GrunnkretsResponse::class))
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/grunnkretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrunnkretserForIds(@Parameter(description = "ID-ene til grunnkretsene man vil hente") @RequestParam id: Set<String>): List<GrunnkretsResponse>

    @Operation(summary = "Henter alle grunnkretser for gitt kommunenummer", description = "Henter alle grunnkretser for gitt kommunenummer")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = GrunnkretsResponse::class))
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/kommuner/{kommunenummer}/grunnkretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleGrunnkretserForKommune(
        @Parameter(description = "Kommunenummer for kommune man ønsker å hente grunnkretsene til") @PathVariable kommunenummer: String
    ): List<GrunnkretsResponse>

}
