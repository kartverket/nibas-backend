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
import no.kartverket.nibas.api.v1.response.FylkeRef
import no.kartverket.nibas.api.v1.response.FylkeResponse
import no.kartverket.nibas.api.v1.response.ConflictResponse
import no.kartverket.nibas.api.v1.response.RevisjonRef
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

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
                array = ArraySchema(schema = Schema(implementation = FylkeRef::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFylker(): List<FylkeRef>

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
    fun hentFylke(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: String): FylkeResponse

    @Operation(
        summary = "Hent grensene til et gitt fylke",
        description = "Henter grensene til et fylke med gitt id"
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenserForFylke(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: String): FeatureCollection


    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Oppdater fylke med gitt id", description = "Oppdaterer og lagrer metadata for fylket")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successful operation"),
        ApiResponse(
            responseCode = "409",
            description = "Conflict",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = ConflictResponse::class))
            )]
        ))
    fun oppdaterFylke(
        @Parameter(description = "ID til fylket man vil oppdatere") @PathVariable id: String,
        @RequestBody oppdatertFylke: FylkeRequest
    ): FylkeResponse

    @Operation(
        summary = "Hent historiske revisjoner for et gitt fylke",
        description = "Henter historiske revisjoner til et fylke med gitt id"
    )
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/{id}/revisjoner", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFylkesRevisjoner(@Parameter(description = "ID-en til fylket man vil hente historiske revisjoner for") @PathVariable id: String): List<RevisjonRef>

    @Operation(
        summary = "Hent en historisk revisjon av et fylke",
        description = "Henter en gitt historisk revisjon av et fylke"
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
    @GetMapping("/{id}/revisjoner/{revisjon}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFylkesRevisjon(
        @Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): FylkeResponse
}
