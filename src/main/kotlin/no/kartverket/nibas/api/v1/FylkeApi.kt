package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.response.FylkeResponse
import no.kartverket.nibas.api.v1.response.FylkeRef
import no.kartverket.nibas.api.v1.response.HistorikkRef
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RequestMapping("/v1/fylker")
@Tag(
    name = "v1/fylker",
    description = "Endepunkter for å hente informasjon om fylker i Nasjonal inndelingsbase"
)
interface FylkeApi {

    @Operation(summary = "Hent alle fylker", description = "Henter alle fylker i nasjonal inndelingsbase.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = FylkeRef::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlle(): List<FylkeRef>

    @Operation(summary = "Hent et fylke med gitt id", description = "Henter fylke med gitt id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = FylkeRef::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentEn(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: Int): FylkeResponse

    @Operation(
        summary = "Hent grensene til et gitt fylke",
        description = "Henter grensene til et fylke med gitt id"
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenser(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: Int): FeatureCollection


    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterFylke(
        @Parameter(description = "ID til fylket man vil oppdatere") @PathVariable id: Int,
        @RequestBody oppdatertFylke: FylkeRequest
    ): FylkeResponse

    @Operation(
        summary = "Hent historikk for et gitt fylke",
        description = "Henter historikken til et fylke med gitt id"
    )
    @DefaultHistorikkRefListApiResponses
    @GetMapping("/{id}/historikk", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentHistorikkForEn(@Parameter(description = "ID-en til fylket man vil hente historikk for") @PathVariable id: Int): List<HistorikkRef>

    @Operation(
        summary = "Hent en historisk versjon av et fylke",
        description = "Henter en gitt revisjon av et historisk fylke"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = FylkeResponse::class)
            )]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}/historikk/{revision}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentHistorikkRevisjon(
        @Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: Int,
        @Parameter(description = "RevisjonsId til revisjonen man vil hente") @PathVariable revision: Int
    ): FylkeResponse
}