package no.kartverket.nibas.service.autosync

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point as JtsPoint

/**
 * Spatial coordinate point in EUREF89 UTM zone 33N (EPSG:25833).
 * Immutable value type with proper equality semantics.
 */
data class Point(val x: Double, val y: Double) {

    /**
     * Calculate Euclidean distance to another point in meters.
     */
    fun distanceTo(other: Point): Double {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Convert to JTS Point for spatial operations.
     */
    fun toJtsPoint(factory: GeometryFactory = GeometryFactory()): JtsPoint {
        return factory.createPoint(Coordinate(x, y))
    }

    /**
     * Convert to list format [x, y] for API compatibility.
     */
    fun toList(): List<Double> = listOf(x, y)

    companion object {
        /**
         * Create Point from list format [x, y].
         */
        fun fromList(coords: List<Double>): Point {
            require(coords.size >= 2) { "Coordinate list must have at least 2 elements, got ${coords.size}" }
            return Point(coords[0], coords[1])
        }

        /**
         * Create Point from JTS Point.
         */
        fun fromJtsPoint(point: JtsPoint): Point {
            return Point(point.x, point.y)
        }
    }
}
