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
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.response.StemmekretsRef
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.api.v1.response.RevisjonRef
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/v1/stemmekretser")
@Tag(
    name = "v1/stemmekretser",
    description = "Endepunkter for å hente informasjon om stemmekretser i Nasjonal inndelingsbase"
)
interface StemmekretsApi {

    @Operation(summary = "Hent alle stemmekretser", description = "Henter alle stemmekretser i Nasjonal inndelingsbase.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = StemmekretsRef::class))
                )
            ]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlle(
        @Parameter(description = "Valgfri kommune-ID til en kommune for å filtrere stemmekretser innenfor en kommune") @RequestParam(
            name = "kommuneid",
            required = false
        ) kommuneid: String?
    ): List<StemmekretsRef>

    @Operation(summary = "Hent en stemmekrets med gitt id", description = "Henter stemmekrets med gitt id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = StemmekretsResponse::class)
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentEn(@Parameter(description = "ID-en til stemmekretsen man vil hente") @PathVariable id: String): StemmekretsResponse

    @Operation(
        summary = "Hent grensene til en gitt stemmekrets",
        description = "Henter grensene til en stemmekrets med gitt id"
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenser(@Parameter(description = "ID-en til stemmekretsen man vil hente") @PathVariable id: String): FeatureCollection

    @Operation(
        summary = "Hent historiske revisjoner for en gitt stemmekrets",
        description = "Henter historiske revisjoner til en stemmekrets med gitt id"
    )
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/{id}/revisjoner", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentStemmekretsRevisjoner(@Parameter(description = "ID-en til stemmekretsen man vil hente") @PathVariable id: String): List<RevisjonRef>

    @Operation(
        summary = "Hent en historisk revisjon av en stemmekrets",
        description = "Henter en gitt historisk revisjon av en stemmekrets"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = StemmekretsResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}/revisjoner/{revisjon}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentStemmekretsRevisjon(
        @Parameter(description = "ID-en til stemmekrets man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): StemmekretsResponse

    @Operation(
        summary = "Oppdater angitt stemmekrets",
        description = "Oppdaterer angitt stemmekrets. Returnerer den oppdaterte stemmekretsen"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = StemmekretsResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad request. Check the request body and path"
        )
    )
    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterStemmekrets(
        @Parameter(description = "ID til stemmekretsen man vil oppdatere") @PathVariable id: String,
        @RequestBody oppdatertStemmekrets: StemmekretsRequest
    ): StemmekretsResponse
}
