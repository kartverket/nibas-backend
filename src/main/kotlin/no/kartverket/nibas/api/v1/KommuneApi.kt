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
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.api.v1.response.KommuneRef
import no.kartverket.nibas.api.v1.response.KommuneResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/v1/kommuner")
@Tag(
    name = "v1/kommuner",
    description = "Endepunkter for å hente informasjon om kommuner i Nasjonal inndelingsbase"
)
interface KommuneApi {

    //TODO: Skulle vi heller vurdert å ha 2 tjenester her, en for hentAlle og en for hentAlleForFylke hvor fylkeId er påkrevet?
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
    fun hentAlle(
        @Parameter(description = "Valgfri id til et fylke for å filtrere kommuner innenfor et fylke") @RequestParam(
            name = "fylkeid",
            required = false
        ) fylkeId: String?
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
    fun hentEn(@Parameter(description = "ID-en til kommunen man vil hente") @PathVariable id: String): KommuneResponse

    @Operation(
        summary = "Hent grensene til en gitt kommune",
        description = "Henter grensene til en kommune med gitt id",
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenser(@Parameter(description = "ID-en til kommunen man vil hente") @PathVariable id: String): FeatureCollection

    @Operation(
        summary = "Hent historiske revisjoner for en gitt kommune",
        description = "Henter historiske revisjoner til en kommune med gitt id"
    )
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/{id}/revisjoner", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommuneRevisjoner(@Parameter(description = "ID-en til kommunen man vil hente historiske revisjoner for") @PathVariable id: String): List<RevisjonRef>

    @Operation(
        summary = "Hent en historisk revisjon av en kommune",
        description = "Henter en gitt historisk revisjon av en kommune"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = KommuneResponse::class)
            )]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}/revisjoner/{revisjon}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommuneRevisjon(
        @Parameter(description = "ID-en til kommunen man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): KommuneResponse

    @Operation(
        summary = "Oppdater angitt kommune",
        description = "Oppdaterer angitt kommune. Returnerer den oppdaterte kommunen"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = KommuneResponse::class)
            )]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad request. Check the request body and path"
        )
    )
    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterKommune(
        @Parameter(description = "ID til kommunen man vil oppdatere") @PathVariable id: String,
        @RequestBody oppdatertKommune: KommuneRequest
    ): KommuneResponse

    @Operation(summary = "Hent kommunes grunnkretser", description = "Henter alle grunnkretser som tilhører en kommune.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = GrunnkretsRef::class))
            )]
        )
    )
    @GetMapping("/{id}/grunnkretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesGrunnkretser(
        @Parameter(description = "ID til kommunen man vil hente grunnkretsene til") @PathVariable id: String
    ): List<GrunnkretsRef>
}
