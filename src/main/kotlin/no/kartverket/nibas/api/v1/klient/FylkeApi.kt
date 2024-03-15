package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.DefaultFeatureCollectionApiResponses
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.response.FylkeResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/fylker")
@Tag(
    name = "v1/fylker",
    description = "Endepunkter for å hente informasjon om fylker i Nasjonal inndelingsbase"
)
interface FylkeApi {

    @Operation(summary = "Hent alle fylker", description = "Henter alle fylker i Nasjonal inndelingsbase.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = FylkeResponse::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFylker(@Parameter(description = "Eventuell gyldighetsdato for fylkene (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                   @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<FylkeResponse>

    @Operation(summary = "Hent et fylke med gitt id", description = "Henter fylke med gitt id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = FylkeResponse::class))]
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
    ): FylkeResponse

    @Operation(
        summary = "Hent grensene til et gitt fylke",
        description = "Henter grensene til et fylke med gitt id"
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenserForFylke(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable(name = "id") lokalid: String,
                            @Parameter(description = "Eventuell gyldighetsdato for fylke (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): FeatureCollection

}
