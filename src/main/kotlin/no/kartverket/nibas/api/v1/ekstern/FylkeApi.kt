package no.kartverket.nibas.api.v1.ekstern

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.ekstern.response.EksternFylkeRef
import no.kartverket.nibas.api.v1.ekstern.response.EksternFylkeResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/ekstern/fylker")
@Tag(
    name = "v1/ekstern/fylker",
    description = "Endepunkter for å hente informasjon om fylker i Nasjonal inndelingsbase"
)
interface FylkeApi {

    @Operation(summary = "Hent et fylke med gitt id", description = "Henter fylke med gitt id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = EksternFylkeResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFylke(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable(name = "id") lokalid: String,
                  @Parameter(description = "Eventuell gyldighetsdato for fylke (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                  @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): EksternFylkeResponse

    @Operation(summary = "Hent alle fylker", description = "Henter alle fylker i Nasjonal inndelingsbase.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = EksternFylkeRef::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFylker(@Parameter(description = "Eventuell gyldighetsdato for fylkene (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                   @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<EksternFylkeRef>
}
