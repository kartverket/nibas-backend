package no.kartverket.nibas.api.v1.ekstern

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.ekstern.response.GrenseResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

/**
 * API for å hente Kommunegrense, Fylkesgrense, Riksgrense, Territorialgrense og AvtaltAvgrensningslinje fra Nasjonal inndelingsbase
 */
@RequestMapping("/v1/ekstern/grenser")
@Tag(
    name = "v1/ekstern/grenser",
    description = "Endepunkter for å hente informasjon om Kommunegrense, Fylkesgrense, Riksgrense,Territorialgrense og AvtaltAvgrensningslinje i Nasjonal inndelingsbase"
)
interface GrenserApi {

    /**
     * Henter Kommunegrense, Fylkesgrense, Riksgrense, Territorialgrense og AvtaltAvgrensningslinje.
     *
     * @param gyldighetsdato Dato for gyldighet av grensene (standard er dagens dato)
     * @param side Sidenummer for paginering (starter på 0)
     * @param antall Antall resultater per side (standard er 10)
     * @return Liste med Kommunegrense, Fylkesgrense, Riksgrense, Territorialgrense og AvtaltAvgrensningslinje
     */
    @Operation(summary = "Hent grenser", description = "Henter Kommunegrense, Fylkesgrense, Riksgrense, Territorialgrense og AvtaltAvgrensningslinje")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Vellykket operasjon",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = PaginertRespons::class)
            )]
        ),
        ApiResponse(responseCode = "404", description = "Ikke funnet")
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenser(
        @Parameter(description = "Dato for gyldighet av grensene (standard er dagens dato)")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false) gyldighetsdato: LocalDate = LocalDate.now(),

        @Parameter(description = "Sidenummer for paginering (starter på 0)")
        @RequestParam(required = false, defaultValue = "0") side: Int = 0,

        @Parameter(description = "Antall resultater per side (standard er 10)")
        @RequestParam(required = false, defaultValue = "10") antall: Int = 10
    ): PaginertRespons<GrenseResponse>

    /**
     * Henter en spesifikk grense basert på lokalid som er gyldig for dagens dato
     *
     * @param lokalid Lokal ID for grensen som skal hentes
     * @return Grense med angitt lokalid som er gyldig for dagens dato
     */
    @Operation(summary = "Hent grense med angitt lokalid", description = "Henter en spesifikk grense basert på lokalid")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Vellykket operasjon",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = GrenseResponse::class)
            )]
        ),
        ApiResponse(responseCode = "404", description = "Ikke funnet")
    )
    @GetMapping("/{lokalid}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrense(
        @Parameter(description = "Lokal ID for grensen som skal hentes")
        @PathVariable(name = "lokalid") lokalid: String
    ): GrenseResponse
}

data class PaginertRespons<T>(
    val innhold: List<T>,
    val side: Int,
    val antallPerSide: Int,
    val totaltAntall: Long,
    val totaltAntallSider: Int
)
