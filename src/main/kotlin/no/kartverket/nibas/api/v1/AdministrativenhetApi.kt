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
import no.kartverket.nibas.api.v1.response.AdministrativEnhet as AdministrativEnhetResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import no.kartverket.nibas.api.v1.request.AdministrativEnhet as AdministrativEnhetRequest

@RequestMapping("/v1/administrativ-enhet")
@Tag(name = "v1/administrativ-enhet", description = "Endepunkter for å hente informasjon om administrative enheter (uten grenser) i Nasjonal inndelingsbase")
interface AdministrativEnhetApi {

    @Operation(summary = "Hent alle administrative enheter av definert type", description = "Henter alle administrative enheter i nasjonal inndelingsbase av gitt type.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = AdministrativEnhetResponse::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAlle(@RequestParam(name = "type", required = true) type: AdministrativEnhetType): Iterable<AdministrativEnhetResponse>

    @Operation(summary = "Hent en administrativ enhet med gitt nummer og av definert type", description = "Henter angit administrativ enhet med gitt nummer og av definert type")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AdministrativEnhetResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found"
        )
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentEn(@Parameter(description = "ID-en til den adminstrative enheten man vil hente") @PathVariable id: Int,
               @RequestParam(name = "type", required = true) type: AdministrativEnhetType
    ): AdministrativEnhetResponse

    @Operation(summary = "Oppdater angitt administrativ enhet", description = "Oppdaterer angitt administrativ enhet. Returnerer den oppdaterte administrative enheten")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AdministrativEnhetResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad request. Check the request body and path"
        )
    )
    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterAdministrativEnhet(@Parameter(description = "ID til den administrative enheten man vil oppdatere") @PathVariable id: Int,
                                   @RequestBody oppdatertAdministrativEnhet: AdministrativEnhetRequest): AdministrativEnhetResponse
}