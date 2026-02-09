package no.kartverket.nibas.importjob.geojson

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel

val precisionModel = PrecisionModel()

fun GeoJsonGeometry.toJTS(srid: Int): Geometry {
    val geomFactory = GeometryFactory(precisionModel, srid)
    @Suppress("UNCHECKED_CAST")
    return when (this.type) {
        "Point" -> geomFactory.createPoint(this.coordinates as List<Double>)
        "LineString" -> geomFactory.createLineString(this.coordinates as List<List<Double>>)
        "Polygon" -> geomFactory.createPolygon(this.coordinates as List<List<List<Double>>>)
        "MultiPoint" -> geomFactory.createMultiPoint(this.coordinates as List<List<Double>>)
        "MultiLineString" -> geomFactory.createMultiLineString(this.coordinates as List<List<List<Double>>>)
        "MultiPolygon" -> geomFactory.createMultiPolygon(this.coordinates as List<List<List<List<Double>>>>)
        else -> throw IllegalArgumentException("Invalid geometry type: ${this.type}")
    }
}

private fun GeometryFactory.createMultiLineString(coordinates: List<List<List<Double>>>): MultiLineString {
    return createMultiLineString(coordinates.map { createLineString(it) }.toTypedArray())
}

private fun GeometryFactory.createMultiPolygon(coordinates: List<List<List<List<Double>>>>): MultiPolygon {
    return createMultiPolygon(coordinates.map { polygon -> createPolygon(polygon) }.toTypedArray())
}

private fun GeometryFactory.createPolygon(coordinates: List<List<List<Double>>>): Polygon {
    val rings = coordinates.map { createLinearRing(it) }
    val shell = rings.first()
    val holes = rings.drop(1).toTypedArray()
    return createPolygon(shell, holes)
}

private fun GeometryFactory.createLinearRing(coordinates: List<List<Double>>): LinearRing {
    return createLinearRing(createCoordinateSequence(coordinates))
}

private fun GeometryFactory.createLineString(coordinates: List<List<Double>>): LineString {
    return createLineString(createCoordinateSequence(coordinates))
}

private fun GeometryFactory.createMultiPoint(coordinates: List<List<Double>>): MultiPoint {
    return createMultiPoint(coordinates.map { createPoint(it) }.toTypedArray())
}

private fun GeometryFactory.createPoint(coordinates: List<Double>): Point {
    return createPoint(
        Coordinate().apply {
            for (i in 0 until coordinates.size) {
                setOrdinate(i, coordinates[i])
            }

    })
}

private fun createCoordinateSequence(coordinates: List<List<Double>>): Array<Coordinate> {
    return coordinates
        .map { coordinates ->
            Coordinate().apply {
                for (i in 0 until coordinates.size) {
                    setOrdinate(i, coordinates[i])
                }
            }
        }
        .toTypedArray()
}
