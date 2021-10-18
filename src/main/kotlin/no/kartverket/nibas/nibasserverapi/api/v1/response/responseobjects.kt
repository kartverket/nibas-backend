package no.kartverket.nibas.nibasserverapi.api.v1.response

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import org.locationtech.jts.geom.LineString
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer
import org.locationtech.spatial4j.io.jackson.GeometryDeserializer

@Schema(description = "Representasjon av en kommune")
class Kommune(

    @Schema(description = "Unik id for kommunen")
    val id: Int,

    @Schema(description = "Navnet til kommunen")
    val navn: String,

    @Schema(description = "Kommunenummeret")
    val kommunenummer: Int,

    @Schema(description = "Alle grensene for denne kommunen")
    val grenser: List<Kommunegrense> = emptyList()
)

@Schema(description = "Representasjon av en kommunegrense")
class Kommunegrense(

    @Schema(description = "Unik id for kommunegrensen")
    val id: Int,

    @Schema(description = "Navn på kommunegrensen (bare for test)")
    val navn: String,

    @JsonSerialize(using = GeometryAsGeoJSONSerializer::class)
    @JsonDeserialize(using = GeometryDeserializer::class)
    @Schema(description = "Kurven med koordinater som bestemmer grense-elementet")
    val grense: LineString
)
