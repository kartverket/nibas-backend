package no.kartverket.nibas.api.v1.ekstern

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.response.KretsResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/ekstern/grunnkretser")
@Tag(
    name = "v1/ekstern/grunnkretser",
    description = "Endepunkter for å hente informasjon om grunnkretser i Nasjonal inndelingsbase"
)
interface GrunnkretsApi {

    @Operation(summary = "Henter grunnkretser med gitte id-er", description = "Henter grunnkretser med gitte id-er")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = KretsResponse::class))
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrunnkretserForIds(@Parameter(description = "ID-ene til grunnkretsene man vil hente") @RequestParam ids: Set<String>,
                               @Parameter(description = "Eventuell gyldighetsdato for grunnkrets (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                               @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?): List<KretsResponse>
}
