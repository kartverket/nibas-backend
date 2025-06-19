package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.geojson.MultiPolygon
import no.kartverket.nibas.common.geometry.getGeometryFactory
import no.kartverket.nibas.extensions.geometry.multiPolygonToDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Polygon

internal class GeometryExtensionsKtTest {

    private val factory = getGeometryFactory()

    @Test
    fun multiPolygonToDto() {
        val polygon = polygonFromPairsOfCoordinates(listOf(
            Pair(1.0, 1.0),
            Pair(2.0, 1.0),
            Pair(2.0, 2.0),
            Pair(1.0, 1.0)
        ))
        val polygon2 = polygonFromPairsOfCoordinates(listOf(
            Pair(3.0123456789, 1.0),
            Pair(4.0, 1.0),
            Pair(4.0, 2.0),
            Pair(3.0123456789, 1.0)
        ))
        val multiPolygon = factory.createMultiPolygon(arrayOf(polygon, polygon2))
        val resultDto = multiPolygon.multiPolygonToDto()

        val expectedDto = MultiPolygon(
            coordinates =
            listOf(
                listOf(
                    listOf(
                        listOf(1.0, 1.0),
                        listOf(2.0, 1.0),
                        listOf(2.0, 2.0),
                        listOf(1.0, 1.0)
                    )
                ),
                listOf(
                    listOf(
                        listOf(3.0123456789, 1.0),
                        listOf(4.0, 1.0),
                        listOf(4.0, 2.0),
                        listOf(3.0123456789, 1.0)
                    )
                )
            )
        )

        assertThat(resultDto).isEqualTo(expectedDto)
    }

    private fun polygonFromPairsOfCoordinates(pairs: List<Pair<Double, Double>>): Polygon {
        val coordinateArray = pairs
            .map { Coordinate(it.first, it.second) }
            .toTypedArray()
        return factory.createPolygon(factory.createLinearRing(coordinateArray))
    }
}
