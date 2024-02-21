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
import no.kartverket.nibas.api.v1.klient.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.klient.response.KommuneRef
import no.kartverket.nibas.api.v1.klient.response.KommuneResponse
import no.kartverket.nibas.api.v1.klient.response.StemmekretsResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/kommuner")
@Tag(
    name = "v1/kommuner",
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

    @Operation(
        summary = "Hent grensene til en gitt kommune",
        description = "Henter grensene til en kommune med gitt id",
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenserForKommune(@Parameter(description = "ID-en til kommunen man vil hente") @PathVariable(name = "id") lokalid: String,
                              @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                              @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): FeatureCollection

    @Operation(summary = "Hent kommunes grunnkretser", description = "Henter alle grunnkretser som tilhører en kommune.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = GrunnkretsResponse::class))
            )]
        )
    )
    @GetMapping("/{id}/grunnkretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesGrunnkretser(
        @Parameter(description = "ID til kommunen man vil hente grunnkretsene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<GrunnkretsResponse>

    @Operation(summary = "Henter alle grunnkretsgrensene til en gitt kommune")
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grunnkretsgrenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesGrunnkretsgrenser(
        @Parameter(description = "ID til kommunen man vil hente grunnkretsgrensene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): FeatureCollection

    @Operation(summary = "Hent kommunes stemmekretser", description = "Henter alle stemmekretser som tilhører en kommune.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = StemmekretsResponse::class))
            )]
        )
    )
    @GetMapping("/{id}/stemmekretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesStemmekretser(
        @Parameter(description = "ID til kommunen man vil hente stemmekretsene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<StemmekretsResponse>

    @Operation(summary = "Henter alle stemmekretsgrensene til en gitt kommune")
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/stemmekretsgrenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesStemmekretsgrenser(
        @Parameter(description = "ID til kommunen man vil hente stemmekretsgrensene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): FeatureCollection
}
