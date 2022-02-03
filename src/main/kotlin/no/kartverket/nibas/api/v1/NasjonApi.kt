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
import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.api.v1.response.HistorikkRef
import no.kartverket.nibas.api.v1.response.NasjonRef
import no.kartverket.nibas.api.v1.response.NasjonResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/v1/nasjoner")
@Tag(
    name = "v1/nasjoner",
    description = "Endepunkter for å hente informasjon om nasjoner i Nasjonal inndelingsbase"
)
interface NasjonApi {

    @Operation(summary = "Hent alle nasjoner", description = "Henter alle nasjoner i nasjonal inndelingsbase.")
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
    fun hentAlle(): List<NasjonRef>

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
    fun hentEn(@Parameter(description = "ID-en til nasjonen man vil hente") @PathVariable id: String): NasjonResponse

    @Operation(
        summary = "Hent grensene til en gitt nasjon",
        description = "Henter grensene til en nasjon med gitt id"
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenser(@Parameter(description = "ID-en til nasjonen man vil hente") @PathVariable id: String): FeatureCollection


    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterNasjon(
        @Parameter(description = "ID til nasjonen man vil oppdatere") @PathVariable id: String,
        @RequestBody oppdatertNasjon: NasjonRequest
    ): NasjonResponse

    @Operation(
        summary = "Hent historikk for en gitt nasjon",
        description = "Henter historikken til en nasjon med gitt id"
    )
    @DefaultHistorikkRefListApiResponses
    @GetMapping("/{id}/historikk", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentHistorikkForEn(@Parameter(description = "ID-en til nasjonen man vil hente historikk for") @PathVariable id: String): List<HistorikkRef>

    @Operation(
        summary = "Hent en historisk versjon av en nasjon",
        description = "Henter en gitt revisjon av en historisk nasjon"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = NasjonResponse::class)
            )]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}/historikk/{revision}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentHistorikkRevisjon(
        @Parameter(description = "Id til nasjonen man vil hente") @PathVariable id: String,
        @Parameter(description = "Id til revisjonen man vil hente") @PathVariable revision: Int
    ): NasjonResponse
}
