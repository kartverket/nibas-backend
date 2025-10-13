package no.kartverket.nibas.service.autosync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/v1/create-rettet-utkast")
@Tag(
    name = "v1/create-rettet-utkast",
    description = "Generer utkast automatisk for retting av avvik mellom NIBAS og Matrikkel."
)
interface CreateRettetUtkastApi {

    @Operation(
        summary = "Generer utkast for retting av avvik",
        description = "Genererer utkast automatisk for kommunepar med avvik. Stopper når ønsket antall utkast er opprettet."
    )
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PostMapping("/generer", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun genererRettetUtkast(
        @Parameter(description = "Antall utkast som skal genereres")
        @RequestParam(defaultValue = "5") antall: Int
    ): String

    @Operation(
        summary = "Publiser alle OPPRETTET utkast",
        description = "Publiserer alle utkast med status OPPRETTET."
    )
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PostMapping("/publiser-alle", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun publiserAlleUtkast(): String
}
