package no.kartverket.nibas.nibasserverapi.api.v1.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import org.locationtech.jts.geom.LineString
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer
import org.locationtech.spatial4j.io.jackson.GeometryDeserializer
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Schema(description = "Representasjon av en kommune")
class Kommune(

    @Schema(description = "Navnet til kommunen")
    @NotBlank
    @Size(min = 0)
    val navn: String,

    @Schema(description = "Kommunenummeret")
    @NotBlank
    val kommunenummer: Int,

    @Schema(description = "Alle grensene for denne kommunen")
    val grenser: List<Kommunegrense> = emptyList()
)

@Schema(description = "Representasjon av en kommunegrense")
class Kommunegrense(

    @Schema(description = "Unik id for kommunegrensen")
    @NotBlank
    val id: Int,

    @Schema(description = "Navn på kommunegrensen (bare for test)")
    @NotBlank
    val navn: String,

    @JsonSerialize(using = GeometryAsGeoJSONSerializer::class)
    @JsonDeserialize(using = GeometryDeserializer::class)
    @Schema(description = "Kurven med koordinater som bestemmer grense-elementet")
    @NotBlank
    val grense: LineString
)
