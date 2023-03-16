package no.kartverket.nibas.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/v1/admin")
@Tag(name = "v1/admin", description = "Endepunkter for å gjøre administrative endringer i Nasjonal inndelingsbase")
interface AdminApi {

    @Operation(
        summary = "Invaliderer kodeliste-cache slik at kodelister refreshes.",
        description = "Invaliderer kodeliste-cache slik at kodelister refreshes."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
        )
    )
    @PutMapping("/kodelister/invalidate")
    fun invalidateKodelisteCache()

}
