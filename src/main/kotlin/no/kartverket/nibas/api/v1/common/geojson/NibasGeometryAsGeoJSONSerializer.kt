package no.kartverket.nibas.api.v1.common.geojson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import org.locationtech.jts.geom.*
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer
import java.io.IOException

class NibasGeometryAsGeoJSONSerializer : GeometryAsGeoJSONSerializer() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(geometry: Geometry, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName("type")
        gen.writeString(geometry.javaClass.simpleName)
        if (geometry.srid > 0) {
            gen.writeFieldName("srid")
            gen.writeString(geometry.srid.toString())
        }
        when (geometry) {
            is Point -> {
                gen.writeFieldName("coordinates")
                write(gen, geometry.coordinate)
            }
            is Polygon -> {
                gen.writeFieldName("coordinates")
                write(gen, geometry)
            }
            is LineString -> {
                gen.writeFieldName("coordinates")
                write(gen, geometry.coordinateSequence)
            }
            else -> {
                throw UnsupportedOperationException("unknown: $geometry")
            }
        }
        gen.writeEndObject()
    }
}