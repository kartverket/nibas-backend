package no.kartverket.nibas.nibasserverapi.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import no.kartverket.nibas.nibasserverapi.api.v1.request.Kommune as KommuneRequest
import no.kartverket.nibas.nibasserverapi.api.v1.response.Kommune as KommuneResponse

@RequestMapping("/v1/kommune")
@Tag(name = "v1/Kommune", description = "Endepunkter for å hente informasjon om kommuner i Nasjonal inndelingsbase")
interface KommuneApi {

    @Operation(summary = "Hent alle kommuner", description = "Henter alle kommuner i nasjonal inndelingsbase.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = KommuneResponse::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll(): Iterable<KommuneResponse>

    @Operation(summary = "Hent en kommune")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = KommuneResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{kommuneId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOne(@Parameter(description = "ID til kommunen man vil hente") @PathVariable kommuneId: Int): KommuneResponse

    @Operation(summary = "Oppdater angitt kommune", description = "Oppdaterer angitt kommune og dens grenser. Returnerer den oppdaterte kommunen")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = KommuneResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad request. Check the request body and path"
        )
    )
    @PutMapping("/{kommuneId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterKommune(@Parameter(description = "ID til kommunen man vil oppdatere") @PathVariable kommuneId: Int,
                        @RequestBody oppdatertKommune: KommuneRequest): KommuneResponse
}