package no.kartverket.nibas.domain.historikk

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.NibasGeometryAsGeoJSONSerializer
import no.kartverket.nibas.api.v1.common.geojson.NibasGeometryDeserializer
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

enum class HistorikkStatus {
    NY, ENDRET, SLETTET
}

data class HistorikkObjekt (
    val navn: String,
    val objekt: InfoObjekt,
    @JsonSerialize(using = NibasGeometryAsGeoJSONSerializer::class)
    @JsonDeserialize(using = NibasGeometryDeserializer::class)
    val posisjon: Point,
    @JsonSerialize(using = NibasGeometryAsGeoJSONSerializer::class)
    @JsonDeserialize(using = NibasGeometryDeserializer::class)
    val flate: Polygon,
    val historikkStatus: HistorikkStatus,
)

data class GeometriHistorikkObjekt (
    val objekt: InfoObjekt,
    @JsonSerialize(using = NibasGeometryAsGeoJSONSerializer::class)
    @JsonDeserialize(using = NibasGeometryDeserializer::class)
    val geometri: LineString,
    val historikkStatus: HistorikkStatus,
)

data class InfoObjekt(
    val type: AdministrativEnhetType,
    val id: Int
)