package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.HistorikkRef
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RequestMapping("/v1/grenser")
@Tag(
    name = "v1/grenser",
    description = "Endepunkt for å oppdatere et sett med grenser (FeatureCollection) i Nasjonal inndelingsbase"
)
interface GrenseApi {

    @Operation(summary = "Oppdater en eller flere grenser.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successful operation"),
        ApiResponse(responseCode = "400", description = "Bad request")
    )
    @PutMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.OK)
    fun oppdaterGrenser(@RequestBody grenser: FeatureCollection)

    @Operation(summary = "Hent kommunegrense", description = "Henter kommunegrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/kommunegrenser/{id}")
    fun hentKommunegrense(@Parameter(description = "ID-en til kommunegrensen man vil hente") @PathVariable id: Int): Feature

    @Operation(summary = "Hent fylkesgrense", description = "Henter fylkesgrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/fylkesgrenser/{id}")
    fun hentFylkesgrense(@Parameter(description = "ID-en til fylkesgrensen man vil hente") @PathVariable id: Int): Feature

    @Operation(summary = "Hent riksgrense", description = "Henter riksgrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/riksgrenser/{id}")
    fun hentRiksgrense(@Parameter(description = "ID-en til riksgrensen man vil hente") @PathVariable id: Int): Feature

    @Operation(summary = "Hent territorialgrense", description = "Henter territorialgrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/territorialgrenser/{id}")
    fun hentTerritorialsgrense(@Parameter(description = "ID-en til territorialgrensen man vil hente") @PathVariable id: Int): Feature

    @Operation(summary = "Hent avtalt avgrensningslinje", description = "Henter avtalt avgrensningslinje med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/avtaltavgrensningslinjer/{id}")
    fun hentAvtaltavgrensningslinje(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: Int): Feature

    @Operation(summary = "Hent historikk for kommunegrense", description = "Henter historikk for kommunegrense med gitt id")
    @DefaultHistorikkRefListApiResponses
    @GetMapping("/kommunegrenser/{id}/historikk")
    fun hentKommunegrenseHistorikk(@Parameter(description = "ID-en til kommunen man vil hente") @PathVariable id: Int): List<HistorikkRef>

    @Operation(summary = "Hent historikk for fylkesgrense", description = "Henter historikk for fylkesgrense med gitt id")
    @DefaultHistorikkRefListApiResponses
    @GetMapping("/fylkesgrenser/{id}/historikk")
    fun hentFylkesgrenseHistorikk(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: Int): List<HistorikkRef>

    @Operation(summary = "Hent historikk for riksgrense", description = "Henter historikk for riksgrense med gitt id")
    @DefaultHistorikkRefListApiResponses
    @GetMapping("/riksgrenser/{id}/historikk")
    fun hentRiksgrenseHistorikk(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: Int): List<HistorikkRef>

    @Operation(summary = "Hent historikk for avtalt avgrensningslinje", description = "Henter historikk for avtalt avgrensningslinje med gitt id")
    @DefaultHistorikkRefListApiResponses
    @GetMapping("/avtaltavgrensningslinjer/{id}/historikk")
    fun hentAvtaltavgrensningslinjeHistorikk(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: Int): List<HistorikkRef>

    @Operation(summary = "Hent historikk for territorialgrense", description = "Henter historikk for territorialgrense med gitt id")
    @DefaultHistorikkRefListApiResponses
    @GetMapping("/territorialgrenser/{id}/historikk")
    fun hentTerritorialgrenseHistorikk(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: Int): List<HistorikkRef>

    @Operation(summary = "Hent en revisjon for kommunegrense", description = "Henter gitt revisjon for gitt kommunegrense")
    @DefaultFeatureApiResponses
    @GetMapping("/kommunegrenser/{id}/historikk/{revision}")
    fun hentKommunegrenseRevisjon(
        @Parameter(description = "ID til kommunegrensen man vil hente") @PathVariable id: Int,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revision: Int
    ): Feature

    @Operation(summary = "Hent en revisjon for fylkesgrense", description = "Henter gitt revisjon for gitt fylkesgrense")
    @DefaultFeatureApiResponses
    @GetMapping("/fylkesgrenser/{id}/historikk/{revision}")
    fun hentFylkesgrenseRevisjon(
        @Parameter(description = "ID til fylkesgrensen man vil hente") @PathVariable id: Int,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revision: Int
    ): Feature

    @Operation(summary = "Hent en revisjon for riksgrense", description = "Henter gitt revisjon for gitt riksgrense")
    @DefaultFeatureApiResponses
    @GetMapping("/riksgrenser/{id}/historikk/{revision}")
    fun hentRiksgrenseRevisjon(
        @Parameter(description = "ID til riksgrense man vil hente") @PathVariable id: Int,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revision: Int
    ): Feature

    @Operation(summary = "Hent en revisjon for avtaltavgrensningslinje", description = "Henter gitt revisjon for gitt avtalt avgrensningslinje")
    @DefaultFeatureApiResponses
    @GetMapping("/avtaltavgrensningslinjer/{id}/historikk/{revision}")
    fun hentAvtaltavgrensningslinjeRevisjon(
        @Parameter(description = "ID til avtaltavgrensningslinje man vil hente") @PathVariable id: Int,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revision: Int
    ): Feature

    @Operation(summary = "Hent en revisjon for territorialgrense", description = "Henter gitt revisjon for gitt territorialgrense")
    @DefaultFeatureApiResponses
    @GetMapping("/territorialgrenser/{id}/historikk/{revision}")
    fun hentTerritorialgrenseRevisjon(
        @Parameter(description = "ID til territorialgrensen man vil hente") @PathVariable id: Int,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revision: Int
    ): Feature
}

