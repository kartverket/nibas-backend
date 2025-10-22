package no.kartverket.nibas.service.autosync

import no.kartverket.nibas.domain.Grense
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.index.strtree.STRtree
import org.springframework.stereotype.Service
import kotlin.math.roundToLong

@Service
class CoordinateService {
    private val geometryFactory = GeometryFactory()

    fun roundPoint(point: Point): Point {
        return Point(
            (point.x * SpatialConstants.COORDINATE_SCALE).roundToLong() / SpatialConstants.COORDINATE_SCALE,
            (point.y * SpatialConstants.COORDINATE_SCALE).roundToLong() / SpatialConstants.COORDINATE_SCALE
        )
    }

    fun pointsMatch(p1: Point, p2: Point): Boolean {
        return p1.distanceTo(p2) < SpatialConstants.COORDINATE_TOLERANCE
    }

    fun hasTooManyDecimals(point: Point): Boolean {
        val x = point.x.toString()
        val y = point.y.toString()
        val xDecimals = x.substringAfter('.', "").length
        val yDecimals = y.substringAfter('.', "").length
        return xDecimals > SpatialConstants.MAX_COORDINATE_DECIMALS ||
               yDecimals > SpatialConstants.MAX_COORDINATE_DECIMALS
    }

    fun extractCoordinates(grense: Grense): List<Point> {
        return grense.grense.coordinates.map { Point(it.x, it.y) }
    }

    fun buildSpatialIndex(matrikkelFeatures: List<M22Feature>): STRtree {
        val index = STRtree()

        matrikkelFeatures.forEach { feature ->
            val coords = feature.geometry.coordinates.map {
                Coordinate(it[0], it[1])
            }.toTypedArray()

            val jtsLine = geometryFactory.createLineString(coords)
            val startPoint = geometryFactory.createPoint(coords.first())
            val endPoint = geometryFactory.createPoint(coords.last())

            val indexed = IndexedM22Feature(feature, startPoint, endPoint)
            index.insert(jtsLine.envelopeInternal, indexed)
        }

        index.build()
        return index
    }

    @Suppress("UNCHECKED_CAST")
    fun findFeaturesNearPoint(
        point: Point,
        spatialIndex: STRtree,
        searchRadius: Double = SpatialConstants.COORDINATE_TOLERANCE
    ): List<IndexedM22Feature> {
        val jtsPoint = point.toJtsPoint(geometryFactory)
        val searchEnvelope = jtsPoint.buffer(searchRadius).envelopeInternal
        return spatialIndex.query(searchEnvelope) as List<IndexedM22Feature>
    }

    data class IndexedM22Feature(
        val feature: M22Feature,
        val startPoint: org.locationtech.jts.geom.Point,
        val endPoint: org.locationtech.jts.geom.Point
    )
}
