package no.kartverket.nibas.common

import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.geometry.toMultiPolygon
import org.assertj.core.api.Assertions.assertThat
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import java.util.Random
import kotlin.test.Test

internal class GeometryFunctionsKtTest {
    @Test
    fun `getGrenserIRiktigRekkefoelge skal returere en sortert liste med grenser`() {
        val grenser = listOf(
            lagLineString(
                Coordinate(0.0, 0.0),
                Coordinate(0.0, 10.0),
                Coordinate(0.0, 20.0),
                Coordinate(10.0, 20.0),
                Coordinate(20.0, 20.0),
            ),
            lagLineString(
                Coordinate(0.0, 0.0),
                Coordinate(10.0, 0.0),
                Coordinate(20.0, 0.0),
                Coordinate(20.0, 10.0),
                Coordinate(20.0, 20.0),
            ),
        )

        val result = getGrenserIRiktigRekkefoelge(grenser)
        assertThat(result).hasSize(1)

        val flate = result.first()
        assertThat(flate).hasSize(2)
        assertThat(flate[0].endPoint).isEqualTo(flate[1].startPoint)
        assertThat(flate.first().startPoint).isEqualTo(flate.last().endPoint)
    }

    @Test
    fun `getGrenserIRiktigRekkefoelge skal returnere flere flater om det er skall og hull`() {
        val skallGrenser = listOf(
            lagLineString(
                Coordinate(0.0, 0.0),
                Coordinate(0.0, 100.0),
                Coordinate(0.0, 200.0),
            ),
            lagLineString(
                Coordinate(200.0, 200.0),
                Coordinate(0.0, 200.0),
            ),
            lagLineString(
                Coordinate(200.0, 200.0),
                Coordinate(200.0, 100.0),
                Coordinate(200.0, 0.0)
            ),
            lagLineString(
                Coordinate(200.0, 0.0),
                Coordinate(0.0, 0.0),
            )
        )

        val hullGrenser = listOf(
            lagLineString(
                Coordinate(50.0, 50.0),
                Coordinate(100.0, 50.0),
                Coordinate(100.0, 100.0),
            ),
            lagLineString(
                Coordinate(50.0, 50.0),
                Coordinate(100.0, 100.0),
            ),
        )

        val result = getGrenserIRiktigRekkefoelge(skallGrenser + hullGrenser)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getGrenserIRiktigRekkefoelge skal returnere flere flater om det er flere adskilte flater`() {
        val skallGrenser = listOf(
            lagLineString(
                Coordinate(0.0, 0.0),
                Coordinate(0.0, 100.0),
                Coordinate(0.0, 200.0),
            ),
            lagLineString(
                Coordinate(200.0, 200.0),
                Coordinate(0.0, 200.0),
            ),
            lagLineString(
                Coordinate(200.0, 200.0),
                Coordinate(200.0, 100.0),
                Coordinate(200.0, 0.0)
            ),
            lagLineString(
                Coordinate(200.0, 0.0),
                Coordinate(0.0, 0.0),
            )
        )

        val skall2Grenser = listOf(
            lagLineString(
                Coordinate(1050.0, 1050.0),
                Coordinate(10100.0, 1050.0),
                Coordinate(10100.0, 10100.0),
            ),
            lagLineString(
                Coordinate(1050.0, 1050.0),
                Coordinate(10100.0, 10100.0),
            ),
        )

        val result = getGrenserIRiktigRekkefoelge(skallGrenser + skall2Grenser)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getGrenserIRiktigRekkefoelge skal fungere med polygoner med kun 1 grense`() {
        val skallGrenser = listOf(
            lagLineString(
                Coordinate(0.0, 0.0),
                Coordinate(0.0, 100.0),
                Coordinate(100.0, 100.0),
                Coordinate(100.0, 0.0),
                Coordinate(0.0, 0.0),
            )
        )

        val result = getGrenserIRiktigRekkefoelge(skallGrenser)
        assertThat(result).hasSize(1)
    }

    @Test
    fun `getGrenserIRiktigRekkefoelge skal håndtere enklaver på kun 1 grense`() {
        val skallGrenser = listOf(
            lagLineString(
                Coordinate(0.0, 0.0),
                Coordinate(0.0, 100.0),
            ),
            lagLineString(
                Coordinate(0.0, 100.0),
                Coordinate(100.0, 100.0),
            ),
            lagLineString(
                Coordinate(100.0, 100.0),
                Coordinate(0.0, 0.0),
            ),
        )

        val skall2Grenser = listOf(
            lagLineString(
                Coordinate(1000.0, 1000.0),
                Coordinate(1000.0, 2000.0),
                Coordinate(2000.0, 2000.0),
                Coordinate(2000.0, 1000.0),
                Coordinate(1000.0, 1000.0),
            ),
        )

        val result = getGrenserIRiktigRekkefoelge(skallGrenser + skall2Grenser)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getGrenserIRiktigRekkefoelge skal klare å bygge opp grensene for en krets med enklave som henger sammen med hovedkretsen i ett punkt som 2 polygoner`() {
        val skallGrenser = listOf(
            lagLineString(
                Coordinate(0.0, 0.0),
                Coordinate(0.0, 100.0),
                Coordinate(0.0, 200.0),
            ),
            lagLineString(
                Coordinate(200.0, 200.0),
                Coordinate(0.0, 200.0),
            ),
            lagLineString(
                Coordinate(200.0, 200.0),
                Coordinate(300.0, 300.0),
            ),
            lagLineString(
                Coordinate(300.0, 300.0),
                Coordinate(300.0, 200.0),
                Coordinate(200.0, 200.0),
            ),
            lagLineString(
                Coordinate(200.0, 200.0),
                Coordinate(200.0, 100.0),
                Coordinate(200.0, 0.0)
            ),
            lagLineString(
                Coordinate(200.0, 0.0),
                Coordinate(0.0, 0.0),
            )
        )

        // La oss forsøke flere ganger med grensene i ulike rekkefølger for å unngå at tilfeldigheter gjør at det blir riktig
        val random = Random(42L)
        for (i in 1 .. 10) {
            val result = getGrenserIRiktigRekkefoelge(skallGrenser.shuffled(random))
            assertThat(result).hasSize(2)
        }

    }

    @Test
    fun `toMultiPolygon skal returnere polygon med hull`() {
        // Skallet er en stor trekant
        val skallGrenser = listOf(
            lagLineString(
                Coordinate(0.0, 0.0),
                Coordinate(100.0, 0.0),
                Coordinate(200.0, 0.0),
            ),
            lagLineString(
                Coordinate(200.0, 0.0),
                Coordinate(100.0, 200.0),
            ),
            lagLineString(
                Coordinate(100.0, 200.0),
                Coordinate(0.0, 0.0),
            )
        )

        // Hullet er en mindre trekant inni den store trekanten
        val hullGrenser = listOf(
            lagLineString(
                Coordinate(50.0, 50.0),
                Coordinate(75.0, 50.0),
                Coordinate(100.0, 50.0),
            ),
            lagLineString(
                Coordinate(100.0, 50.0),
                Coordinate(75.0, 100.0),
            ),
            lagLineString(
                Coordinate(75.0, 100.0),
                Coordinate(50.0, 50.0),
            )
        )

        val grenser = listOf(skallGrenser, hullGrenser)

        val result = grenser.toMultiPolygon()
        assertThat(result.numGeometries).isEqualTo(1)

        val geometry = result.getGeometryN(0)
        assertThat(geometry.geometryType).isEqualTo("Polygon")

        val polygon = geometry as Polygon

        assertThat(polygon.exteriorRing.coordinates).containsAll(listOf(Coordinate(0.0, 0.0), Coordinate(200.0, 0.0), Coordinate(100.0, 200.0)))
        assertThat(polygon.numInteriorRing).isEqualTo(1)

        val hull = polygon.getInteriorRingN(0)
        assertThat(hull.coordinates).containsAll(listOf(Coordinate(50.0, 50.0), Coordinate(100.0, 50.0), Coordinate(75.0, 100.0)))
    }

    private fun lagLineString(vararg koordinater: Coordinate): LineString =
        GeometryFactory().createLineString(koordinater)
}
