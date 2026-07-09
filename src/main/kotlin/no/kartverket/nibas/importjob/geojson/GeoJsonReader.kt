package no.kartverket.nibas.importjob.geojson

import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class GeoJsonReader(
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader
) {

    fun readFeatureCollectionFromClasspath(path: String): GeoJsonFeatureCollection {
        val resource = resourceLoader.getResource("classpath:$path")
        resource.inputStream.use { input ->
            return objectMapper.readValue(input, GeoJsonFeatureCollection::class.java)
        }
    }
}
