package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import no.kartverket.nibas.api.v1.common.geojson.Geometry as GeometryDto
import no.kartverket.nibas.api.v1.common.geojson.LineString as LineStringDto
import no.kartverket.nibas.api.v1.common.geojson.MultiPolygon as MultiPolygonDto
import no.kartverket.nibas.api.v1.common.geojson.Point as PointDto

fun GeometryDto.toLineString(): LineString {
    if (this !is LineStringDto) {
        throw RuntimeException("Geometry kan ikke utledes til en LineString. Geometry var av type: $this")
    }
    return SosiGeometryFunctions.koordinaterToLineString(this.coordinates, this.srid)
}

fun Geometry.toDto(): GeometryDto {
    return when (this) {
        is Point -> this.toDto()
        is LineString -> this.toDto()
        is MultiPolygon -> this.toDto()
        else -> throw RuntimeException("Ukjent type Geometry: $this")
    }
}

fun Geometry.sridAsString(): String = this.srid.toString()

fun Point.toDto() = PointDto(
    srid = this.sridAsString(),
    coordinates = listOf(this.x, this.y)
)

fun LineString.toDto() = LineStringDto(
    srid = this.sridAsString(),
    coordinates = this.coordinates.map { listOf(it.x, it.y) })

fun MultiPolygon.toDto(): MultiPolygonDto {
    val listOfPolygons: MutableList<List<List<List<Double>>>> = mutableListOf()
    getPolygons().forEach { polygon ->
        val listOfRings: MutableList<List<List<Double>>> = mutableListOf()
        polygon.getRings()
            .forEach { linearRing ->
                val listOfCoordinates: MutableList<List<Double>> = mutableListOf()
                linearRing.coordinatesAsList()
                    .forEach { coordinate ->
                        val listOfXAndY = listOf(coordinate.x, coordinate.y)
                        listOfCoordinates.add(listOfXAndY)
                    }
                listOfRings.add(listOfCoordinates)
            }
        listOfPolygons.add(listOfRings)
    }
    return MultiPolygonDto(
        srid = this.sridAsString(),
        coordinates = listOfPolygons
    )
}

private fun LinearRing.coordinatesAsList(): List<Coordinate> =
    this.coordinates.toList()

private fun MultiPolygon.getPolygons(): List<Polygon> {
    val polygons = mutableListOf<Polygon>()
    for (i in 0 until this.numGeometries) {
        polygons.add(this.getGeometryN(i) as Polygon)
    }
    return polygons
}

private fun Polygon.getRings(): List<LinearRing> {
    val rings = mutableListOf<LinearRing>()
    rings.add(this.exteriorRing)
    for (i in 0 until this.numInteriorRing) {
        rings.add(this.getInteriorRingN(i))
    }
    return rings
}
