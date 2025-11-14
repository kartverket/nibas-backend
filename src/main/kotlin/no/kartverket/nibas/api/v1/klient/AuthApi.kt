package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.klient.response.User
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/v1/auth")
@Tag(
    name = "v1/auth",
    description = "Endepunkter for å hente informasjon om autentisering og autorisering"
)
interface AuthApi {

    @Operation(summary = "Hent innlogget bruker", description = "Henter informasjon om innlogget bruker. Dette hentes fra headere satt av ztoperator")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = User::class)
            )]
        )
    )
    @GetMapping("/user", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getUser(): User

}
