package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RequestMapping("/v1/feature")
@Tag(name = "v1/feature", description = "Endepunkter for å hente informasjon om kommuner som geojson (FeatureCollection) i Nasjonal inndelingsbase")
interface FeatureApi {

    @Operation(summary = "Hent en eller flere administrative enheter av gitt type som en FeatureCollection ")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = FeatureCollection::class)
            )]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/administrative-enheter", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAdministrativeEnheter(
        @RequestParam(name = "type", required = true) type: AdministrativEnhetType,
        @RequestParam(name = "ider", required = true) ider: List<Int>
    ): FeatureCollection

    @Operation(summary = "Oppdater grensene til en eller flere administrative enheter av gitt type. Returnerer de oppdaterte administrative enhetene som en FeatureCollection")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = FeatureCollection::class)
            )]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad request"
        )
    )
    @PutMapping("/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterGrenser(
        @RequestParam(name = "type", required = true) type: AdministrativEnhetType,
        @RequestParam(name = "ider", required = true) ider: List<Int>,
        @RequestBody administratitveEnheter: FeatureCollection
    ): FeatureCollection
}

