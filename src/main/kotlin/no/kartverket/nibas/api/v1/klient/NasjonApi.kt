package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.klient.response.NasjonResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/nasjon")
@Tag(
    name = "v1/nasjon",
    description = "Endepunkter for å hente informasjon om nasjon (Norge) i Nasjonal inndelingsbase"
)
interface NasjonApi {

    @Operation(summary = "Hent nasjon", description = "Henter nasjon på en gitt dato")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = NasjonResponse::class)
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentNasjon(
        @Parameter(description = "Eventuell gyldighetsdato for nasjonen (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): NasjonResponse
}
