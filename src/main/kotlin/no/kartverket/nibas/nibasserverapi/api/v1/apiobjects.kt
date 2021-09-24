package no.kartverket.nibas.nibasserverapi.api.v1

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import org.locationtech.jts.geom.Polygon
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer
import org.locationtech.spatial4j.io.jackson.GeometryDeserializer
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Schema(description = "Representasjon av en kommune")
class Kommune(

    @Schema(description = "Unik id for kommunen")
    @NotBlank
    val id: Int,

    @Schema(description = "Navnet til kommunen")
    @NotBlank
    @Size(min = 0)
    val name: String,

    @JsonSerialize(using = GeometryAsGeoJSONSerializer::class)
    @JsonDeserialize(using = GeometryDeserializer::class)
    @Schema(description = "Polygonet med koordinater som avgrenser kommunen")
    @NotBlank
    val omrade: Polygon
)

