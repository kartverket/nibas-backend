package no.kartverket.nibas.api.v1.common.geojson

import io.swagger.v3.oas.annotations.media.Schema

class FeatureCollection (
    @Schema(description = "Toppnivå for geojson-strukturen, definert av en konstant med navn type og verdi FeatureCollection")
    val type: String = "FeatureCollection",

    @Schema(description = "Liste av features som holder på dataene")
    val features: MutableList<Feature> = mutableListOf()
) {
    fun add(feature: Feature): Boolean {
        return features.add(feature)
    }
}