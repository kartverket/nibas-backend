package no.kartverket.nibas.api.v1.ekstern

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.response.KommuneResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate
import java.util.*

@RequestMapping("/v1/ekstern/kommuner")
@Tag(
    name = "v1/ekstern/kommuner",
    description = "Endepunkter for å hente informasjon om kommuner i Nasjonal inndelingsbase"
)
interface KommuneApi {

    @Operation(summary = "Hent en kommune med gitt id", description = "Henter kommune med gitt id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = KommuneResponse::class)
            )]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommune(@Parameter(description = "ID-en til kommunen man vil hente") @PathVariable(name = "id") lokalid: String,
                    @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): KommuneResponse
}
