package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.klient.response.AuthZStatusResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/v1/authz")
@Tag(
    name = "v1/authz",
    description = "Halla"
)
interface AuthZApi {
    @Operation(description = "Henter status for autorisering")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Bruker er autorisert",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = AuthZStatusResponse::class)
            )]
        ),
        ApiResponse(
            responseCode = "403",
            description = "Bruker er ikke autorisert"
        )
    )
    @GetMapping("/status", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getStatus(): AuthZStatusResponse
}
