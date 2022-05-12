package no.kartverket.nibas.api.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.response.RevisjonRef
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@RequestMapping("/v1/grenser")
@Tag(
    name = "v1/grenser",
    description = "Endepunkt for å oppdatere et sett med grenser (FeatureCollection) i Nasjonal inndelingsbase"
)
interface GrenseApi {

    @Operation(summary = "Lagrer grenser som sendes inn. Støtter både nye og oppdaterte grenser.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successful operation"),
        ApiResponse(responseCode = "400", description = "Bad request")
    )
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.OK)
    fun lagreGrenser(@RequestBody grenser: FeatureCollection)

    @Operation(summary = "Hent kommunegrense", description = "Henter kommunegrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/kommunegrenser/{id}")
    fun hentKommunegrense(@Parameter(description = "ID-en til kommunegrensen man vil hente") @PathVariable id: String): Feature

    @Operation(summary = "Hent fylkesgrense", description = "Henter fylkesgrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/fylkesgrenser/{id}")
    fun hentFylkesgrense(@Parameter(description = "ID-en til fylkesgrensen man vil hente") @PathVariable id: String): Feature

    @Operation(summary = "Hent riksgrense", description = "Henter riksgrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/riksgrenser/{id}")
    fun hentRiksgrense(@Parameter(description = "ID-en til riksgrensen man vil hente") @PathVariable id: String): Feature

    @Operation(summary = "Hent territorialgrense", description = "Henter territorialgrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/territorialgrenser/{id}")
    fun hentTerritorialsgrense(@Parameter(description = "ID-en til territorialgrensen man vil hente") @PathVariable id: String): Feature

    @Operation(summary = "Hent avtalt avgrensningslinje", description = "Henter avtalt avgrensningslinje med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/avtaltavgrensningslinjer/{id}")
    fun hentAvtaltavgrensningslinje(@Parameter(description = "ID-en til fylket man vil hente") @PathVariable id: String): Feature

    @Operation(summary = "Hent grunnkretsgrense", description = "Henter grunnkretsgrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/grunnkretsgrenser/{id}")
    fun hentGrunnkretsgrense(@Parameter(description = "ID-en til grunnkretsgrensen man vil hente") @PathVariable id: String): Feature

    @Operation(summary = "Hent delområdegrense", description = "Henter delområdegrense med gitt id")
    @DefaultFeatureApiResponses
    @GetMapping("/delomraadegrenser/{id}")
    fun hentDelomraadegrense(@Parameter(description = "ID-en til delområdegrensen man vil hente") @PathVariable id: String): Feature

    @Operation(summary = "Hent historiske revisjoner for kommunegrense", description = "Henter historiske revisjoner for kommunegrense med gitt id")
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/kommunegrenser/{id}/revisjoner")
    fun hentKommunegrenseRevisjoner(@Parameter(description = "ID-en til kommunegrensen man vil hente") @PathVariable id: String): List<RevisjonRef>

    @Operation(summary = "Hent historiske revisjoner for fylkesgrense", description = "Henter historiske revisjoner for fylkesgrense med gitt id")
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/fylkesgrenser/{id}/revisjoner")
    fun hentFylkesgrenseRevisjoner(@Parameter(description = "ID-en til fylkesgrensen man vil hente") @PathVariable id: String): List<RevisjonRef>

    @Operation(summary = "Hent historiske revisjoner for riksgrense", description = "Henter historiske revisjoner for riksgrense med gitt id")
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/riksgrenser/{id}/revisjoner")
    fun hentRiksgrenseRevisjoner(@Parameter(description = "ID-en til riksgrensen man vil hente") @PathVariable id: String): List<RevisjonRef>

    @Operation(summary = "Hent historiske revisjoner for avtalt avgrensningslinje", description = "Henter historiske revisjoner for avtalt avgrensningslinje med gitt id")
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/avtaltavgrensningslinjer/{id}/revisjoner")
    fun hentAvtaltavgrensningslinjeRevisjoner(@Parameter(description = "ID-en til den avtale avgrensningslinjen man vil hente") @PathVariable id: String): List<RevisjonRef>

    @Operation(summary = "Hent historiske revisjoner for territorialgrense", description = "Henter historiske revisjoner for territorialgrense med gitt id")
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/territorialgrenser/{id}/revisjoner")
    fun hentTerritorialgrenseRevisjoner(@Parameter(description = "ID-en til territorialgrensen man vil hente") @PathVariable id: String): List<RevisjonRef>

    @Operation(summary = "Hent historiske revisjoner for grunnkretsgrense", description = "Henter historiske revisjoner for grunnkretsgrense med gitt id")
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/grunnkretsgrenser/{id}/revisjoner")
    fun hentGrunnkretsgrenseRevisjoner(@Parameter(description = "ID-en til grunnkretsgrensen man vil hente") @PathVariable id: String): List<RevisjonRef>

    @Operation(summary = "Hent historiske revisjoner for delområdegrense", description = "Henter historiske revisjoner for delområdegrense med gitt id")
    @DefaultRevisjonRefListApiResponses
    @GetMapping("/delomraadegrenser/{id}/revisjoner")
    fun hentDelomraadegrenseRevisjoner(@Parameter(description = "ID-en til delområdegrensen man vil hente") @PathVariable id: String): List<RevisjonRef>

    @Operation(summary = "Hent en historisk revisjon for kommunegrense", description = "Henter gitt historisk revisjon for gitt kommunegrense")
    @DefaultFeatureApiResponses
    @GetMapping("/kommunegrenser/{id}/revisjoner/{revisjon}")
    fun hentKommunegrenseRevisjon(
        @Parameter(description = "ID til kommunegrensen man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): Feature

    @Operation(summary = "Hent en historisk revisjon for fylkesgrense", description = "Henter gitt historisk revisjon for gitt fylkesgrense")
    @DefaultFeatureApiResponses
    @GetMapping("/fylkesgrenser/{id}/revisjoner/{revisjon}")
    fun hentFylkesgrenseRevisjon(
        @Parameter(description = "ID til fylkesgrensen man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): Feature

    @Operation(summary = "Hent en historisk revisjon for riksgrense", description = "Henter gitt historisk revisjon for gitt riksgrense")
    @DefaultFeatureApiResponses
    @GetMapping("/riksgrenser/{id}/revisjoner/{revisjon}")
    fun hentRiksgrenseRevisjon(
        @Parameter(description = "ID til riksgrense man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): Feature

    @Operation(summary = "Hent en historisk revisjon for avtaltavgrensningslinje", description = "Henter gitt historisk revisjon for gitt avtalt avgrensningslinje")
    @DefaultFeatureApiResponses
    @GetMapping("/avtaltavgrensningslinjer/{id}/revisjoner/{revisjon}")
    fun hentAvtaltavgrensningslinjeRevisjon(
        @Parameter(description = "ID til avtaltavgrensningslinje man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): Feature

    @Operation(summary = "Hent en historisk revisjon for territorialgrense", description = "Henter gitt historisk revisjon for gitt territorialgrense")
    @DefaultFeatureApiResponses
    @GetMapping("/territorialgrenser/{id}/revisjoner/{revisjon}")
    fun hentTerritorialgrenseRevisjon(
        @Parameter(description = "ID til territorialgrensen man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): Feature

    @Operation(summary = "Hent en historisk revisjon for grunnkretsgrense", description = "Henter gitt historisk revisjon for gitt grunnkretsgrense")
    @DefaultFeatureApiResponses
    @GetMapping("/grunnkretsgrenser/{id}/revisjoner/{revisjon}")
    fun hentGrunnkretsgrenseRevisjon(
        @Parameter(description = "ID til grunnkretsgrensen man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): Feature

    @Operation(summary = "Hent en historisk revisjon for delområdegrense", description = "Henter gitt historisk revisjon for gitt delområdegrense")
    @DefaultFeatureApiResponses
    @GetMapping("/delomraadegrenser/{id}/revisjoner/{revisjon}")
    fun hentDelomraadegrenseRevisjon(
        @Parameter(description = "ID til delområdegrensen man vil hente") @PathVariable id: String,
        @Parameter(description = "ID til revisjonen man vil hente") @PathVariable revisjon: Int
    ): Feature
}

