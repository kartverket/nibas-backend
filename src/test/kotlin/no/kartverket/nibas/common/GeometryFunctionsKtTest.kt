package no.kartverket.nibas.common

import no.kartverket.nibas.common.geometry.avrundLineString
import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.geometry.toLineStrings
import no.kartverket.nibas.common.geometry.toMultiPolygon
import org.assertj.core.api.Assertions.assertThat
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import java.util.*
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

        val multiPolygon = result.toMultiPolygon()
        assertThat(multiPolygon.numGeometries).isEqualTo(1)
        assertThat((multiPolygon.getGeometryN(0) as Polygon).numInteriorRing).isEqualTo(1)

        val lineStringsForMultipolygon = multiPolygon.toLineStrings()
        val linestringsIRiktigRekkefoelge = getGrenserIRiktigRekkefoelge(lineStringsForMultipolygon)
        val nyttMultiPolygon = linestringsIRiktigRekkefoelge.toMultiPolygon()
        assertThat(nyttMultiPolygon).isEqualTo(multiPolygon)
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

        val multiPolygon = result.toMultiPolygon()
        assertThat(multiPolygon.numGeometries).isEqualTo(2)

        val lineStringsForMultipolygon = multiPolygon.toLineStrings()
        val linestringsIRiktigRekkefoelge = getGrenserIRiktigRekkefoelge(lineStringsForMultipolygon)
        val nyttMultiPolygon = linestringsIRiktigRekkefoelge.toMultiPolygon()
        assertThat(nyttMultiPolygon).isEqualTo(multiPolygon)
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

        val multiPolygon = result.toMultiPolygon()
        assertThat(multiPolygon.numGeometries).isEqualTo(1)

        val lineStringsForMultipolygon = multiPolygon.toLineStrings()
        val linestringsIRiktigRekkefoelge = getGrenserIRiktigRekkefoelge(lineStringsForMultipolygon)
        val nyttMultiPolygon = linestringsIRiktigRekkefoelge.toMultiPolygon()
        assertThat(nyttMultiPolygon).isEqualTo(multiPolygon)
    }

    @Test
    fun `getGrenserIRiktigRekkefoelge skal kunne bygge opp grensene for en krets med to flater som henger sammen i ett punkt som 2 polygoner`() {
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

            val multiPolygon = result.toMultiPolygon()
            assertThat(multiPolygon.numGeometries).isEqualTo(2)

            val lineStringsForMultipolygon = multiPolygon.toLineStrings()
            val linestringsIRiktigRekkefoelge = getGrenserIRiktigRekkefoelge(lineStringsForMultipolygon)
            val nyttMultiPolygon = linestringsIRiktigRekkefoelge.toMultiPolygon()
            assertThat(nyttMultiPolygon).isEqualTo(multiPolygon)
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
        val hullGrenser2 = listOf(
            lagLineString(
                Coordinate(150.0, 50.0),
                Coordinate(150.0, 25.0),
                Coordinate(125.0, 25.0),
                Coordinate(150.0, 50.0)
            )
        )

        val grenser = listOf(skallGrenser, hullGrenser, hullGrenser2)

        val result = grenser.toMultiPolygon()
        assertThat(result.numGeometries).isEqualTo(1)

        val geometry = result.getGeometryN(0)
        assertThat(geometry.geometryType).isEqualTo("Polygon")

        val polygon = geometry as Polygon

        assertThat(polygon.exteriorRing.coordinates).containsAll(listOf(Coordinate(0.0, 0.0), Coordinate(200.0, 0.0), Coordinate(100.0, 200.0)))
        assertThat(polygon.numInteriorRing).isEqualTo(2)

        val hull = polygon.getInteriorRingN(0)
        val hull2 = polygon.getInteriorRingN(1)
        val koordinaterForHull1 = listOf(Coordinate(50.0, 50.0), Coordinate(100.0, 50.0), Coordinate(75.0, 100.0), Coordinate(50.0, 50.0))
        val koordinaterForHull2 = listOf(Coordinate(150.0, 50.0), Coordinate(150.0, 25.0), Coordinate(125.0, 25.0), Coordinate(150.0, 50.0))
        val koordinatlister = listOf(koordinaterForHull1, koordinaterForHull2)

        //Kan vel ikke være sikker at hullene kommer i samme rekkefølge hver gang
        assertThat(koordinatlister).contains(hull.coordinates.toList())
        assertThat(koordinatlister).contains(hull2.coordinates.toList())
        assertThat(hull).isNotEqualTo(hull2)

        val lineStringsForMultipolygon = result.toLineStrings()
        assertThat(lineStringsForMultipolygon).hasSize(3)

        val linestringsIRiktigRekkefoelge = getGrenserIRiktigRekkefoelge(lineStringsForMultipolygon)
        val nyttMultiPolygon = linestringsIRiktigRekkefoelge.toMultiPolygon()
        assertThat(nyttMultiPolygon).isEqualTo(result)
    }

    @Test
    fun `test avrund koordinater`() {
        val coordinates = arrayOf(Coordinate(3.0123456789, 0.0))
        val result = avrundLineString(coordinates)
        assertThat(result).isEqualTo(arrayOf(Coordinate(3.01, 0.0)))
            .isNotEqualTo(coordinates)

        val koordinatSomIkkeSkalAvrundes = Coordinate(7867303.31, 1121940.59)
        assertThat(avrundLineString(arrayOf(koordinatSomIkkeSkalAvrundes))).isEqualTo(arrayOf(koordinatSomIkkeSkalAvrundes))

        val negativKoordinatSomSkalAvrundes = Coordinate(6496929.842403338, -24600.90321480068)
        assertThat(avrundLineString(arrayOf(negativKoordinatSomSkalAvrundes))).isEqualTo(arrayOf(Coordinate(6496929.84, -24600.905)))

        val koordinaterSomSkalAvrundes = arrayOf(Coordinate(6549092.035, 269445.332588278),
            Coordinate(6548349.2081234, 272351.3374123),
            Coordinate(6548787.587500001, 273395.99912121))
        assertThat(avrundLineString(koordinaterSomSkalAvrundes))
            .isEqualTo(arrayOf(Coordinate(6549092.035, 269445.335),
                Coordinate(6548349.21, 272351.335),
                Coordinate(6548787.59, 273396.0)))
    }

    private fun lagLineString(vararg koordinater: Coordinate): LineString =
        GeometryFactory().createLineString(koordinater)
}
