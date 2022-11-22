package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.RevisjonRef
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate
import java.util.*

@RequestMapping("/v1/grunnkretser")
@Tag(
    name = "v1/grunnkretser",
    description = "Endepunkter for å hente informasjon om grunnkretser i Nasjonal inndelingsbase"
)
interface GrunnkretsApi {

    @Operation(summary = "Hent en grunnkrets med gitt id", description = "Henter grunnkrets med gitt id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GrunnkretsResponse::class)
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrunnkrets(
        @Parameter(description = "ID-en til grunnkretsen man vil hente") @PathVariable id: String,
        @Parameter(description = "Eventuell Gyldig Dato for grunnkrets (default = datens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldigDato") gyldigDato: Optional<LocalDate>
    ): GrunnkretsResponse

    @Operation(
        summary = "Hent grensene til en gitt grunnkrets",
        description = "Henter grensene til en grunnkrets med gitt id"
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenserForGrunnkrets(
        @Parameter(description = "ID-en til grunnkretsen man vil hente") @PathVariable id: String,
        @Parameter(description = "Eventuell Gyldig Dato for grunnkrets (default = datens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false) gyldigDato: Optional<LocalDate>
    ): FeatureCollection

    @Operation(
        summary = "Hent historiske revisjoner for en gitt grunnkrets",
        description = "Henter historiske revisjoner til en grunnkrets med gitt id"
    )
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/{id}/revisjoner", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrunnkretsRevisjoner(@Parameter(description = "ID-en til grunnkretsen man vil hente") @PathVariable id: String): List<RevisjonRef>

    @Operation(
        summary = "Hent en historisk revisjon av en grunnkrets",
        description = "Henter en gitt historisk revisjon av en grunnkrets"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GrunnkretsResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}/revisjoner/{revisjon}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrunnkretsRevisjon(
        @Parameter(description = "ID-en til grunnkretsen man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): GrunnkretsResponse

}
