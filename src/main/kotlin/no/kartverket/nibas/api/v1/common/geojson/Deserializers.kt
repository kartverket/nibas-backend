package no.kartverket.nibas.api.v1.common.geojson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.locationtech.jts.geom.Geometry
import org.locationtech.spatial4j.context.jts.JtsSpatialContext
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory
import org.locationtech.spatial4j.io.jackson.ShapeDeserializer
import org.locationtech.spatial4j.shape.Shape
import java.io.IOException

private fun createJtsSpatialContextFactory(): JtsSpatialContextFactory {
    val factory = JtsSpatialContextFactory()
    factory.geo = false
    factory.useJtsLineString = true
    factory.useJtsMulti = true
    factory.useJtsPoint = true
    factory.srid = SosiGeometryFunctions.defaultSrid
    return factory
}

class NibasGeometryDeserializer : JsonDeserializer<Geometry?>() {
    // Create a context that will allow any JTS shape
    private val jtsSpatialContext: JtsSpatialContext = JtsSpatialContext(createJtsSpatialContextFactory())
    private val shapeDeserializer: ShapeDeserializer = ShapeDeserializer(jtsSpatialContext)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Geometry? {
        val shape: Shape? = shapeDeserializer.deserialize(jp, ctxt)
        return if (shape != null) {
            jtsSpatialContext.shapeFactory.getGeometryFrom(shape)
        } else {
            null
        }
    }

}
