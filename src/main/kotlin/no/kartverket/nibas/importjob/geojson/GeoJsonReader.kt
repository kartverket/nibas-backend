package no.kartverket.nibas.importjob.geojson

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component

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
