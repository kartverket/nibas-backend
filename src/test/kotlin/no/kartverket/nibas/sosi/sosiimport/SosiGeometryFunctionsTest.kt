package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SosiGeometryFunctionsTest {

    @Test
    fun selfIntersectingPolygonFailsValidation() {
        val a = Coordinate(167_390.0, 6_524_000.0)
        val b = Coordinate(167_380.0, 6_523_900.0)
        val c = Coordinate(167_370.0, 6_524_100.0)
        val d = Coordinate(167_370.0, 6_524_000.0)

        val grenseAB = grunnkretsGrenseKobling(listOf(a, b), 1)
        val grenseBC = grunnkretsGrenseKobling(listOf(b, c), 2)
        val grenseCD = grunnkretsGrenseKobling(listOf(c, d), 3)
        val grenseDA = grunnkretsGrenseKobling(listOf(d, a), 4) // D-A krysser B-C

        val grenseKoblinger = setOf(grenseAB, grenseBC, grenseCD, grenseDA)

        assertThatThrownBy { SosiGeometryFunctions.beregnFlate(grenseKoblinger) }
            .isInstanceOf(UgyldigGeometriException::class.java)
            .hasMessageContaining("Geometri-objekt er ikke gyldig: POLYGON ((167390 6524000")
            .hasMessageContaining("Validation-feil: Self-intersection.")
            .hasMessageContaining("Feil-lokasjon: (167375.0, 6524000.0, NaN)")
    }

    @Test
    fun thatFlaterDannesAvGrenserMedEnkelGeometri() {
        val geometryFactory = GeometryFactory()
        val grenser = mutableListOf(
            geometryFactory.createLineString(arrayOf(
                Coordinate(1.0, 1.0),
                Coordinate(2.0, 1.0),
                Coordinate(2.0, 2.0),
            )),
            geometryFactory.createLineString(arrayOf(
                Coordinate(2.0, 2.0),
                Coordinate(1.0, 2.0),
                Coordinate(1.0, 1.0)
            ))
        )

        val flate: MultiPolygon = SosiGeometryFunctions.dannFlaterFraGrenser(grenser)

        val coordinates = geometryFactory.createMultiLineString(grenser.toTypedArray()).coordinates
        val coordinateList = CoordinateList(coordinates, false)

        val linearRing = geometryFactory.createLinearRing(coordinateList.toCoordinateArray())
        val polygon = listOf(geometryFactory.createPolygon(linearRing))
        val multiPolygon = geometryFactory.createMultiPolygon(polygon.toTypedArray())

        assertEquals(multiPolygon, flate)
    }

    @Test
    fun thatFlaterDannesAvGrenseSomBiterSegSelvIHalen() {
        val geometryFactory = GeometryFactory()

        // En grense som er selv-lukkende
        val grenser = mutableListOf(
            geometryFactory.createLineString(arrayOf(
                Coordinate(1.0, 1.0),
                Coordinate(2.0, 1.0),
                Coordinate(2.0, 2.0),
                Coordinate(2.0, 2.0),
                Coordinate(1.0, 2.0),
                Coordinate(1.0, 1.0)
            ))
        )

        val faktiskFlate = SosiGeometryFunctions.dannFlaterFraGrenser(grenser)

        val linearRing = geometryFactory.createLinearRing(geometryFactory.createMultiLineString(grenser.toTypedArray()).coordinates)
        val polygon = listOf(geometryFactory.createPolygon(linearRing))
        val forventetFlate = geometryFactory.createMultiPolygon(polygon.toTypedArray())

        assertTrue(forventetFlate.equalsTopo(faktiskFlate))
    }

    @Test
    @Disabled("Denne testen feiler fordi det opprettes tre polygoner som så pakkes inn i et multipolygon, mulig vi må utvide til å hensynta skall og hull for å få ekte likhet")
    fun thatFlaterDannesAvGrenserSomDannerFlaterMedHull() {
        val geometryFactory = GeometryFactory()

        // 4 grenser, hvor to danner et skall og de to siste danner to hull i skallet
        val grense1 = geometryFactory.createLineString(arrayOf(
            Coordinate(1.0, 1.0),
            Coordinate(7.0, 1.0),
            Coordinate(7.0, 4.0)
        ))
        val grense2 = geometryFactory.createLineString(arrayOf(
            Coordinate(7.0, 4.0),
            Coordinate(1.0, 4.0),
            Coordinate(1.0, 1.0)
        ))
        val grense3 = geometryFactory.createLineString(arrayOf(
            Coordinate(2.0, 2.0),
            Coordinate(3.0, 2.0),
            Coordinate(3.0, 3.0),
            Coordinate(2.0, 3.0),
            Coordinate(2.0, 2.0)
        ))

        val grense4 = geometryFactory.createLineString(arrayOf(
            Coordinate(4.0, 2.0),
            Coordinate(6.0, 2.0),
            Coordinate(6.0, 3.0),
            Coordinate(4.0, 3.0),
            Coordinate(4.0, 2.0)
        ))

        val grenser = mutableListOf( grense3, grense4, grense1, grense2)

        val faktiskFlate = SosiGeometryFunctions.dannFlaterFraGrenser(grenser)

//        val linearRing = geometryFactory.createLinearRing(geometryFactory.createMultiLineString(grenser.toTypedArray()).coordinates)
//        val polygon = listOf(geometryFactory.createPolygon(linearRing))
        val hullLineStrings = listOf(listOf(grense3), listOf(grense4))
        val polygon = SosiGeometryFunctions.lagFlate(listOf(grense1, grense2), hullLineStrings)// geometryFactory.createMultiPolygon(polygon.toTypedArray())
        val forventetFlate = geometryFactory.createMultiPolygon(listOf(polygon).toTypedArray())

        assertTrue(forventetFlate.equalsTopo(faktiskFlate))
    }

    @Test
    fun thatFlaterDannesAvGrenserSomIkkeErSortert() {
        val geometryFactory = GeometryFactory()

        val grense1 = geometryFactory.createLineString(arrayOf( //grense 1
            Coordinate(1.0, 1.0),
            Coordinate(2.0, 1.0)
        ))
        val grense2 = geometryFactory.createLineString(arrayOf( //grense 2
            Coordinate(2.0, 1.0),
            Coordinate(3.0, 2.0)
        ))
        val grense3 = geometryFactory.createLineString(arrayOf( //grense 3
            Coordinate(3.0, 2.0),
            Coordinate(4.0, 2.0)
        ))
        val grense4 = geometryFactory.createLineString(arrayOf( //grense 4
            Coordinate(4.0, 2.0),
            Coordinate(4.0, 4.0)
        ))
        val grense5 = geometryFactory.createLineString(arrayOf( //grense 5
            Coordinate(4.0, 4.0),
            Coordinate(2.0, 4.0)
        ))
        val grense6 = geometryFactory.createLineString(arrayOf( //grense 6
            Coordinate(2.0, 4.0),
            Coordinate(1.0, 1.0)
        ))
        val usorterteGrenser = mutableListOf(
            grense6,
            grense2,
            grense5,
            grense4,
            grense1,
            grense3
        )

        val sorterteGrenser = mutableListOf(
            grense1,
            grense2,
            grense3,
            grense4,
            grense5,
            grense6
        )

        val faktiskFlate = SosiGeometryFunctions.dannFlaterFraGrenser(usorterteGrenser)

        val linearRing = geometryFactory.createLinearRing(geometryFactory.createMultiLineString(sorterteGrenser.toTypedArray()).coordinates)
        val polygon = geometryFactory.createPolygon(linearRing)
        val forventetFlate = geometryFactory.createMultiPolygon(listOf(polygon).toTypedArray())

        assertTrue(forventetFlate.equalsTopo(faktiskFlate))
    }

    /**
     * 4 punkter, 3 kanter. Mangler grensesnutt fra D - A, slik at ringen er ikke lukket.
     */
    @Test
    fun ikkeLukketRingFailsCreationOfGeometry() {
        val a = Coordinate(167_390.0, 6_524_000.0)
        val b = Coordinate(167_370.0, 6_524_100.0)
        val c = Coordinate(167_370.0, 6_524_100.0)
        val d = Coordinate(167_385.0, 6_524_000.0)

        val grenseAB = grunnkretsGrenseKobling(listOf(a, b), 1)
        val grenseBC = grunnkretsGrenseKobling(listOf(b, c), 2)
        val grenseCD = grunnkretsGrenseKobling(listOf(c, d), 3)

        val grenseKoblinger = setOf(grenseAB, grenseBC, grenseCD)

        assertThatThrownBy { SosiGeometryFunctions.beregnFlate(grenseKoblinger) }
            .isInstanceOf(IkkeSammenhengendePolygonException::class.java)
            .hasMessageContaining("Polygon som skal dannes har ikke-sammenhengende punkter, sluttpunkt: (167385.0, 6524000.0, NaN), startPunkt: (167390.0, 6524000.0, NaN). Validation-feil: Interior is disconnected.")
    }
    /**
     * Ringen har grenser som ikke henger sammen i et knutepunkt, ringen regnes da ikke som lukket
     */
    @Test
    fun manglendeKnutepunkterFailsCreationOfGeometry() {
        val a = Coordinate(167_390.0, 6_524_000.0)
        val b = Coordinate(167_370.0, 6_524_100.0)
        val c = Coordinate(167_370.0, 6_524_000.0)
        val d = Coordinate(167_385.0, 6_524_000.0)

        val grenseAB = grunnkretsGrenseKobling(listOf(a, b), 1)
        val grenseBC = grunnkretsGrenseKobling(listOf(b, c), 2)
        val grenseDA = grunnkretsGrenseKobling(listOf(d, a), 4)

        val grenseKoblinger = setOf(grenseAB, grenseBC, grenseDA)

        assertThatThrownBy { SosiGeometryFunctions.beregnFlate(grenseKoblinger) }
            .isInstanceOf(IkkeSammenhengendePolygonException::class.java)
            .hasMessageContaining("Polygon som skal dannes har ikke-sammenhengende punkter, sluttpunkt: (167370.0, 6524000.0, NaN), startPunkt: (167385.0, 6524000.0, NaN). Validation-feil: Interior is disconnected.")
    }

    @Test
    fun repeterendePunkterFjernesIFlate() {
        val a = Coordinate(0.0, 0.0)
        val b = Coordinate(100.0, 0.0)
        val c = Coordinate(100.0, 100.0)
        val d = Coordinate(0.0, 100.0)

        val grenseAB = grunnkretsGrenseKobling(listOf(a, b), 1)
        val grenseBC = grunnkretsGrenseKobling(listOf(b, c), 2)
        val grenseCD = grunnkretsGrenseKobling(listOf(c, d), 3)
        val grenseDA = grunnkretsGrenseKobling(listOf(d, a), 4)

        val grenseKoblinger = setOf(grenseAB, grenseBC, grenseCD, grenseDA)
        val flate = SosiGeometryFunctions.beregnFlate(grenseKoblinger)
        assertThat(flate.numPoints).isEqualTo(5) // De 4 koordinatene, samt at flaten lukkes og får med start-koordinatet helt til slutt
    }


    @Test
    fun thatDekkerReturnsTrueWhenCoverdByBothInBufferAndOnGeometry() {
        val skalDekke = SosiGeometryFunctions.getGeometryFactory().createLineString(arrayOf(Coordinate(0.0,0.0), Coordinate(0.0, 1.0), Coordinate(0.0, 2.0), Coordinate(0.0, 3.0)))
        val paaLinjen = SosiGeometryFunctions.getGeometryFactory().createPoint(Coordinate(0.0, 1.0))
        val innenforBuffer = SosiGeometryFunctions.getGeometryFactory().createPoint(Coordinate(5.0, 2.0))
        val utenforBuffer = SosiGeometryFunctions.getGeometryFactory().createPoint(Coordinate(5.5, 2.0))

        assertThat(SosiGeometryFunctions.dekker(skalDekke, paaLinjen, 5.0)).isTrue
        assertThat(SosiGeometryFunctions.dekker(skalDekke, innenforBuffer, 5.0)).isTrue
        assertThat(SosiGeometryFunctions.dekker(skalDekke, utenforBuffer, 5.0)).isFalse
    }

    private fun grunnkretsGrenseKobling(coordinates: List<Coordinate>, rekkefoelge: Int) =
        GrunnkretsGrenseKobling(GrunnkretsBuilder().build(), GrenseBuilder()
            .withGrenseGeometri(coordinates)
            .buildGrunnkretsgrense(),
            GrenseKoblingAttributter(rekkefoelge = rekkefoelge))
}


