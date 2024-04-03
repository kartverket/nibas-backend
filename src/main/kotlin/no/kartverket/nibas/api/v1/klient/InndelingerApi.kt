package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.klient.response.InndelingResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/inndelinger")
@Tag(
    name = "v1/inndelinger",
    description = "Endepunkter for å hente informasjon om inndelinger i Nasjonal inndelingsbase"
)
interface InndelingerApi {

    @Operation(summary = "Søk etter inndelinger", description = "Søk etter inndelinger ved å bruke en søkestring")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = InndelingResponse::class))
            )]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun finnInndelinger(
        @Parameter(description = "Søkestring for å finne inndelinger")
        @RequestParam(name = "searchString") searchString: String,
        @Parameter(description = "Eventuell gyldighetsdato for inndelingene (default = dagens dato)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate = LocalDate.now(),
        @Parameter(description = "Maksgrense for antall treff man ønsker seg")
        @RequestParam(required = false, defaultValue = "15", name = "limit") limit: Int = 15
    ): List<InndelingResponse>
}
