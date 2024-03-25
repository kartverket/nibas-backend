package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.request.PubliserUtkastRequest
import no.kartverket.nibas.api.v1.klient.response.ApiErrorResponse
import no.kartverket.nibas.api.v1.klient.response.OptimistiskLaasWrapper
import no.kartverket.nibas.api.v1.klient.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.klient.response.UtkastRef
import no.kartverket.nibas.api.v1.klient.response.UtkastResponse
import no.kartverket.nibas.service.utkast.UtkastValidering
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@RequestMapping("/v1/utkast")
@Tag(
    name = "v1/utkast",
    description = "Endepunkt for å utføre CRUD-operasjoner mot utkast."
)
interface UtkastApi {

    @Operation(summary = "Oppretter et utkast og returnerer id.", description = "Oppretter et utkast og returnerer id.")
    @Response201Created
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettUtkast(@Parameter(description = "Utkastet som ønskes opprettet")
                      @RequestBody opprettUtkastRequest: OpprettUtkastRequest
    ): UtkastOpprettetResponse

    @Operation(summary = "Hent et utkast med gitt id", description = "Henter utkast med gitt id")
    @Response200OK
    @Response404NotFound
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentUtkast(@Parameter(description = "ID-en til utkastet man vil hente") @PathVariable id: String): UtkastResponse

    @Operation(
        summary = "Valider er utkast med gitt id.",
        description = "Validerer et utkast med gitt id og se alle feil som ble funnet på utkastet."
    )
    @Response200OK
    @Response404NotFound
    @GetMapping("/{id}/valider", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun validerUtkast(
        @Parameter(description = "ID til utkastet man vil validere") @PathVariable id: String,
    ): List<UtkastValidering>

    @Operation(
        summary = "Oppdater angitt utkast.",
        description = "Oppdaterer angitt utkast. Returnerer oppdatert utkast."
    )
    @Response200OK
    @Response400BadRequest
    @Response404NotFound
    @Response409Conflict
    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterUtkast(
        @Parameter(description = "ID til utkastet man vil oppdatere") @PathVariable id: String,
        @RequestBody utkast: OppdaterUtkastRequest
    ): UtkastResponse


    @Operation(
        summary = "Forkast angitt utkast.",
        description = "Forkast angitt utkast."
    )
    @Response200OK
    @Response404NotFound
    @DeleteMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun forkastUtkast(
        @Parameter(description = "ID til utkastet man vil slette") @PathVariable id: String
    )

    @Operation(summary = "Publiserer utkastet med gitt id.", description = "Publiserer utkastet med gitt id.")
    @Response200OK
    @Response400BadRequest
    @Response404NotFound
    @Response409Conflict
    @PostMapping("/{id}/publiser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun publiserUtkast(@Parameter(description = "ID-en til utkastet man vil publisere") @PathVariable id: String, @RequestBody body: PubliserUtkastRequest?)

    @Operation(summary = "Henter alle utkast i status Opprettet sortert på navn.", description = "Henter alle utkast i status Opprettet sortert på navn.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = UtkastRef::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentUtkastIStatusOpprettet(): List<UtkastRef>
}

@ApiResponse(responseCode = "200", description = "Successful operation")
annotation class Response200OK

@ApiResponse(
    responseCode = "201",
    description = "Successful operation",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = String::class)
        )
    ]
)
annotation class Response201Created

@ApiResponse(responseCode = "400", description = "Bad request. Check the request body and path",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = ApiErrorResponse::class)
        )
    ])
annotation class Response400BadRequest

@ApiResponse(responseCode = "404", description = "Not Found",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = ApiErrorResponse::class)
        )
    ]
)
annotation class Response404NotFound

@ApiResponse(
    responseCode = "409", description = "Conflict",
    content = [Content(
        mediaType = "application/json",
        schema = Schema(implementation = OptimistiskLaasWrapper::class)
    )]
)
annotation class Response409Conflict
