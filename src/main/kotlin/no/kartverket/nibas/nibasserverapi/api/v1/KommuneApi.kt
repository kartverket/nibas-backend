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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

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
                array = ArraySchema(schema = Schema(implementation = Kommune::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll(): Iterable<Kommune>

    @Operation(summary = "Hent en kommune")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Kommune::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{kommuneId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOne(@Parameter(description = "ID til kommunen man vil hente") @PathVariable kommuneId: Int): Kommune
}