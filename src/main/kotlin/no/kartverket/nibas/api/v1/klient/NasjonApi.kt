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
import no.kartverket.nibas.api.v1.klient.response.NasjonRef
import no.kartverket.nibas.api.v1.klient.response.NasjonResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/nasjoner")
@Tag(
    name = "v1/nasjoner",
    description = "Endepunkter for å hente informasjon om nasjoner i Nasjonal inndelingsbase"
)
interface NasjonApi {

    @Operation(summary = "Hent alle nasjoner", description = "Henter alle nasjoner i Nasjonal inndelingsbase.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = NasjonRef::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentNasjoner(@Parameter(description = "Eventuell gyldighetsdato for nasjonene (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                     @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<NasjonRef>

    @Operation(summary = "Hent en nasjon med gitt id", description = "Henter nasjon med gitt id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = NasjonRef::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentNasjon(@Parameter(description = "ID-en til nasjonen man vil hente") @PathVariable(name = "id") lokalid: String,
                   @Parameter(description = "Eventuell gyldighetsdato for nasjon (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                   @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): NasjonResponse

    @Operation(
        summary = "Hent grensene til en gitt nasjon",
        description = "Henter grensene til en nasjon med gitt id"
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenserForNasjon(@Parameter(description = "ID-en til nasjonen man vil hente") @PathVariable(name = "id") lokalid: String,
                             @Parameter(description = "Eventuell gyldighetsdato for nasjon (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                             @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): FeatureCollection

}
