package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.domain.historikk.GeometriHistorikkObjekt
import no.kartverket.nibas.domain.historikk.HistorikkObjekt
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RequestMapping("/v1/administrativ-enhet/historikk")
@Tag(name = "v1/administrativ-enhet/historikk", description = "Endepunkter for å hente historikk på administrative enheter (uten grenser) i Nasjonal inndelingsbase")
interface HistorikkApi {

    @Operation(summary = "Hent historikk for alle administrative enheter av definert type", description = "Henter historikk for alle administrative enheter i nasjonal inndelingsbase av gitt type.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = HistorikkObjekt::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentHistorikkForAlle(@RequestParam(name = "type", required = true) type: AdministrativEnhetType): List<HistorikkObjekt>

    @Operation(summary = "Hent historikk for en administrativ enhet med gitt nummer og av definert type", description = "Henter historikk for angit administrativ enhet med gitt nummer og av definert type")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = HistorikkObjekt::class))
            )]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentHistorikkForEn(@Parameter(description = "ID-en til den adminstrative enheten man vil hente") @PathVariable id: Int,
               @RequestParam(name = "type", required = true) type: AdministrativEnhetType
    ): List<HistorikkObjekt>

    @Operation(summary = "Hent historikk for en grense", description = "Henter historikk for en grense i nasjonal inndelingsbase av gitt type.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = GeometriHistorikkObjekt::class))
            )]
        )
    )
    @GetMapping("/grenser/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentHistorikkForEnGrense(@Parameter(description = "ID-en til den grensa man vil ha historikk for") @PathVariable id: Int, @RequestParam(name = "type", required = true) type: AdministrativEnhetType): List<GeometriHistorikkObjekt>

}