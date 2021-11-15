package no.kartverket.nibas.nibasserverapi.api.v1.common.geojson

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import org.locationtech.jts.geom.LineString
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer
import org.locationtech.spatial4j.io.jackson.GeometryDeserializer

class Feature (
    @Schema(description = "En konstant, navn: type, verdi: Feature")
    val type: String = "Feature",

    @Schema(description = "Et sett med egenskaper som beskriver denne featuren")
    @JsonProperty("properties")
    var properties: FeatureProperties,

    @JsonSerialize(using = GeometryAsGeoJSONSerializer::class)
    @JsonDeserialize(using = GeometryDeserializer::class)
    @Schema(description = "Geometrien til denne featuren. Pr nå støttes kun LineString-elementer her")
    var geometry: LineString
)