package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.request.UtkastRequest
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
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
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Successful operation",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = String::class)
                )
            ]
        ),
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettUtkast(@Parameter(description = "Utkastet som ønskes opprettet")
                      @RequestBody utkastRequest: UtkastRequest
    ): UtkastOpprettetResponse

}
