package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/v1/kartlag")
@Tag(
    name = "v1/file",
    description = "Endepunkter for å jobbe med filer i nibas-klienten"
)
interface KartlagApi {

    @Operation(description = "Konverterer en SOSI-fil til en GeoJSON-fil")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "SOSI-fil ble konvertert til en GeoJSON-fil",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = FeatureCollection::class)
            )]
        )
    )
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun convertSOSIToGeoJSON(
        @RequestParam(required = true, name = "file") file: MultipartFile
    ): FeatureCollection
}
