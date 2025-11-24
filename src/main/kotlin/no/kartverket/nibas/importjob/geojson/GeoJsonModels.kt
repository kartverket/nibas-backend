package no.kartverket.nibas.importjob.geojson

data class GeoJsonFeatureCollection(
    val type: String,
    val name: String?,
    val crs: GeoJsonCrs?,
    val features: List<GeoJsonFeature>
)

data class GeoJsonCrs(
    val type: String,
    val properties: Map<String, Any?>
)

data class GeoJsonFeature(
    val type: String,
    val properties: Map<String, Any?>,
    val geometry: GeoJsonGeometry
)

data class GeoJsonGeometry(
    val type: String,
    val coordinates: Any
)
