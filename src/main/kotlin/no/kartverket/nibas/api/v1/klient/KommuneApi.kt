package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.DefaultFeatureCollectionApiResponses
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.response.BopliktomraadeResponse
import no.kartverket.nibas.api.v1.klient.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.klient.response.InndelingResponse
import no.kartverket.nibas.api.v1.klient.response.KommuneResponse
import no.kartverket.nibas.api.v1.klient.response.StemmekretsResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@RequestMapping("/v1/kommuner")
@Tag(
    name = "v1/kommuner",
    description = "Endepunkter for å hente informasjon om kommuner i Nasjonal inndelingsbase"
)
interface KommuneApi {

    @Operation(summary = "Hent alle kommuner", description = "Henter alle kommuner i Nasjonal inndelingsbase.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = KommuneResponse::class))
            )]
        )
    )
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommuner(
        @Parameter(description = "Valgfri id til et fylke for å filtrere kommuner innenfor et fylke")
        @RequestParam(name = "fylkeid", required = false) fylkeId: String?,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<KommuneResponse>

    @Operation(summary = "Hent alle kommuner i gitte fylker", description = "Henter alle kommuner i gitte fylker.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = KommuneResponse::class))
            )]
        )
    )
    @GetMapping("/forFylker", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunerForFylker(
        @Parameter(description = "Liste med fylkeid'er for å hente kommuner i flere fylker")
        @RequestParam(name = "fylkeid") fylkeIder: List<String>,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<KommuneResponse>

    @Operation(summary = "Hent en kommune med gitt id", description = "Henter kommune med gitt id")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = KommuneResponse::class)
            )]
        ),
        ApiResponse(responseCode = "404", description = "Not Found")
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommune(@Parameter(description = "ID-en til kommunen man vil hente") @PathVariable(name = "id") lokalid: String,
                    @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): KommuneResponse

    @Operation(
        summary = "Hent grensene til en gitt kommune",
        description = "Henter grensene til en kommune med gitt id",
    )
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentGrenserForKommune(@Parameter(description = "ID-en til kommunen man vil hente") @PathVariable(name = "id") lokalid: String,
                              @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                              @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): FeatureCollection

    @Operation(summary = "Hent kommunes grunnkretser", description = "Henter alle grunnkretser som tilhører en kommune.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = GrunnkretsResponse::class))
            )]
        )
    )
    @GetMapping("/{id}/grunnkretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesGrunnkretser(
        @Parameter(description = "ID til kommunen man vil hente grunnkretsene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<GrunnkretsResponse>

    @Operation(summary = "Henter alle grunnkretsgrensene til en gitt kommune")
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grunnkretsgrenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesGrunnkretsgrenser(
        @Parameter(description = "ID til kommunen man vil hente grunnkretsgrensene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): FeatureCollection

    @Operation(summary = "Henter grunnkretsgrenser som ble historiske på gitt gyldigTil-dato, knyttet til gitt kommune")
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/grunnkretsgrenserHistoriske", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesGrunnkretsgrenserHistoriske(
        @Parameter(description = "ID til kommunen man vil hente grunnkretsgrensene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "gyldigTilDate for da grunnkretsgrensene ble historiske") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = true, name = "gyldigTilDate") gyldigTilDate: LocalDate
    ): FeatureCollection


    @Operation(summary = "Hent kommunes stemmekretser", description = "Henter alle stemmekretser som tilhører en kommune.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = StemmekretsResponse::class))
            )]
        )
    )
    @GetMapping("/{id}/stemmekretser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesStemmekretser(
        @Parameter(description = "ID til kommunen man vil hente stemmekretsene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<StemmekretsResponse>

    @Operation(summary = "Henter alle stemmekretsgrensene til en gitt kommune")
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/stemmekretsgrenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesStemmekretsgrenser(
        @Parameter(description = "ID til kommunen man vil hente stemmekretsgrensene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): FeatureCollection

    @Operation(summary = "Henter stemmekretsgrenser som ble historiske på gitt gyldigTil-dato, knyttet til gitt kommune")
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/stemmekretsgrenserHistoriske", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesStemmekretsgrenserHistoriske(
        @Parameter(description = "ID til kommunen man vil hente stemmekretsgrensene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "gyldigTilDate for da stemmekretsgrensene ble historiske") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = true, name = "gyldigTilDate") gyldigTilDate: LocalDate
    ): FeatureCollection

    @Operation(summary = "Henter alle grenser til inndelinger med delt geometri til kommunegrensa i en gitt kommune")
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/inndelingerdeltgeometrigrenser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesInndelingerMedDeltGeometriGrenser(
        @Parameter(description = "ID til kommunen man vil hente grensene i inndelingene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): FeatureCollection

    @Operation(summary = "Henter alle inndelinger med delt geometri til kommunegrensa i en gitt kommune")
    @DefaultFeatureCollectionApiResponses
    @GetMapping("/{id}/inndelingerdeltgeometri", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesInndelingerMedDeltGeometri(
        @Parameter(description = "ID til kommunen man vil hente inndelingene til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<InndelingResponse>

    @Operation(summary = "Hent kommunes bopliktområde", description = "Henter bopliktområdet som tilhører en kommune.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = BopliktomraadeResponse::class))
            )]
        )
    )
    @GetMapping("/{id}/bopliktomraader", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentKommunesBopliktomraade(
        @Parameter(description = "ID til kommunen man vil hente bopliktområdet til") @PathVariable(name = "id") lokalid: String,
        @Parameter(description = "Eventuell gyldighetsdato for kommune (default = dagens dato") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(required = false, name = "gyldighetsdato") gyldighetsdato: LocalDate?
    ): List<BopliktomraadeResponse>

}
