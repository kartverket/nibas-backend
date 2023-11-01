package no.kartverket.nibas.api.v1.ekstern

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.KommuneRef
import no.kartverket.nibas.api.v1.response.KommuneResponse
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/ekstern/kommuner")
@Tag(
    name = "v1/ekstern/kommuner",
    description = "Endepunkter for å hente informasjon om kommuner i Nasjonal inndelingsbase"
)
interface KommuneApi {

    @Operation(summary = "Hent alle kommuner", description = "Henter alle kommuner i Nasjonal inndelingsbase.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = KommuneRef::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommuner(
        @Parameter(description = "Valgfri id til et fylke for å filtrere kommuner innenfor et fylke")
        @RequestParam(name = "fylkeid", required = false) fylkeId: String?,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<KommuneRef>

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

    @Operation(summary = "Henter alle stemmekretser for gitt kommunenummer", description = "Henter alle stemmekretser for gitt kommunenummer")
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
    @GetMapping("/{kommunenummer}/stemmekretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleStemmekretserForKommune(@Parameter(description = "Kommunenummer for kommune man ønsker å hente stemmekretsene til") @PathVariable kommunenummer: String,
                                        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<KretsResponse>

    @Operation(summary = "Henter alle grunnkretser for gitt kommunenummer", description = "Henter alle grunnkretser for gitt kommunenummer")
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
    @GetMapping("/{kommunenummer}/grunnkretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleGrunnkretserForKommune(
        @Parameter(description = "Kommunenummer for kommune man ønsker å hente grunnkretsene til") @PathVariable kommunenummer: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<KretsResponse>

    @Operation(summary = "Henter alle stemmekretser med detaljer for gitt kommunenummer", description = "Henter alle stemmekretser med detaljer for gitt kommunenummer. Denne brukes for uthenting av data til dataplattform.")
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
    @GetMapping("/{kommunenummer}/stemmekretsermeddetaljer", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleStemmekretserMedDetaljerForKommune(@Parameter(description = "Kommunenummer for kommune man ønsker å hente stemmekretsene til") @PathVariable kommunenummer: String,
                                        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<StemmekretsResponse>

    @Operation(summary = "Henter alle grunnkretser med detaljer for gitt kommunenummer", description = "Henter alle grunnkretser med detaljer for gitt kommunenummer. Denne brukes for uthenting av data til dataplattform.")
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
    @GetMapping("/{kommunenummer}/grunnkretsermeddetaljer", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlleGrunnkretserMedDetaljerForKommune(
        @Parameter(description = "Kommunenummer for kommune man ønsker å hente grunnkretsene til") @PathVariable kommunenummer: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<GrunnkretsResponse>

}
