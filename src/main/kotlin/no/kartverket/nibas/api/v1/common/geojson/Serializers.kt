package no.kartverket.nibas.api.v1.common.geojson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
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
            is LineString -> {
                gen.writeFieldName("coordinates")
                write(gen, geometry.coordinateSequence)
            }
            is MultiPolygon -> {
                gen.writeFieldName("coordinates")
                gen.writeStartArray()
                for (i in 0 until geometry.getNumGeometries()) {
                    write(gen, geometry.getGeometryN(i) as Polygon)
                }
                gen.writeEndArray()
            }
            else -> {
                throw UnsupportedOperationException("unknown: $geometry")
            }
        }
        gen.writeEndObject()
    }
}
