package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.matrikkel.MatrikkelKodelisterRespons
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/v1/matrikkelkodelister")
@Tag(
    name = "v1/matrikkelkodelister",
    description = "Endepunkter for å hente utvalgte kodelister fra matrikkelen"
)
interface MatrikkelKodelisteApi {

    /**
     * Slår opp kodeverdier fra matrikkelen. Pr nå er det målemetoder og hjelpelinjetyper vi henter
     */
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fetchMatrikkelKodelister(): MatrikkelKodelisterRespons

}
