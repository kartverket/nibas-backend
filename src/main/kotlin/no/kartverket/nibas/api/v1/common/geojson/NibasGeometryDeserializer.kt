/*******************************************************************************
 * Copyright (c) 2015 VoyagerSearch and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0 which
 * accompanies this distribution and is available at
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package no.kartverket.nibas.api.v1.common.geojson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.locationtech.jts.geom.Geometry
import org.locationtech.spatial4j.context.jts.JtsSpatialContext
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory
import org.locationtech.spatial4j.io.jackson.ShapeDeserializer
import java.io.IOException

class NibasGeometryDeserializer : JsonDeserializer<Geometry?>() {
    companion object {
        // Create a context that will allow any JTS shape
        var JTS: JtsSpatialContext? = null

        init {
            val factory = JtsSpatialContextFactory()
            factory.geo = false
            factory.useJtsLineString = true
            factory.useJtsMulti = true
            factory.useJtsPoint = true
            factory.srid = 25833
            JTS = JtsSpatialContext(factory)
        }
    }

    private val dser: ShapeDeserializer = ShapeDeserializer(JTS)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Geometry? {
        val shape = dser.deserialize(jp, ctxt)
        return if (shape != null) {
            JTS!!.shapeFactory.getGeometryFrom(shape)
        } else null
    }

}