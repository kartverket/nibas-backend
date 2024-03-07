package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SosiGeometryFunctionsTest {
    @Test
    fun thatFlaterDannesAvGrenserMedEnkelGeometri() {
        val geometryFactory = GeometryFactory()
        val grenser = mutableListOf(
            GrenseMedLokalid(
                geometryFactory.createLineString(arrayOf(
                    Coordinate(1.0, 1.0),
                    Coordinate(2.0, 1.0),
                    Coordinate(2.0, 2.0)
                )), Lokalid("123")
            ),
            GrenseMedLokalid(
                geometryFactory.createLineString(arrayOf(
                    Coordinate(2.0, 2.0),
                    Coordinate(1.0, 2.0),
                    Coordinate(1.0, 1.0)
                )), Lokalid("234")
            ),
        )

        val flate: MultiPolygon = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(grenser)

        val coordinateList = CoordinateList(geometryFactory.createMultiLineString(grenser.map { it.grense }.toTypedArray()).coordinates, false)
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
            GrenseMedLokalid(
                geometryFactory.createLineString(arrayOf(
                    Coordinate(1.0, 1.0),
                    Coordinate(2.0, 1.0),
                    Coordinate(2.0, 2.0),
                    Coordinate(2.0, 2.0),
                    Coordinate(1.0, 2.0),
                    Coordinate(1.0, 1.0)
            )), Lokalid("123")
            )
        )

        val faktiskFlate = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(grenser)

        val linearRing = geometryFactory.createLinearRing(geometryFactory.createMultiLineString(grenser.map{ it.grense }.toTypedArray()).coordinates)
        val polygon = listOf(geometryFactory.createPolygon(linearRing))
        val forventetFlate = geometryFactory.createMultiPolygon(polygon.toTypedArray())

        assertTrue(forventetFlate.equalsTopo(faktiskFlate))
    }

    @Test
    fun thatFlaterDannesAvGrenserSomDannerFlaterMedHull() {
        val geometryFactory = GeometryFactory()

        // 4 grenser, hvor to danner et skall og de to siste danner to hull i skallet
        val grense1 = geometryFactory.createLineString(arrayOf(
            Coordinate(100.0, 100.0),
            Coordinate(700.0, 100.0),
            Coordinate(700.0, 400.0)
        ))
        val grense2 = geometryFactory.createLineString(arrayOf(
            Coordinate(700.0, 400.0),
            Coordinate(100.0, 400.0),
            Coordinate(100.0, 100.0)
        ))
        val grense3 = geometryFactory.createLineString(arrayOf(
            Coordinate(200.0, 200.0),
            Coordinate(300.0, 200.0),
            Coordinate(300.0, 300.0),
            Coordinate(200.0, 300.0),
            Coordinate(200.0, 200.0)
        ))

        val grense4 = geometryFactory.createLineString(arrayOf(
            Coordinate(400.0, 200.0),
            Coordinate(600.0, 200.0),
            Coordinate(600.0, 300.0),
            Coordinate(400.0, 300.0),
            Coordinate(400.0, 200.0)
        ))

        val grenserMedLokalid = mutableListOf(
            GrenseMedLokalid(grense1, Lokalid("1")),
            GrenseMedLokalid(grense2, Lokalid("2")),
            GrenseMedLokalid(grense3, Lokalid("3")),
            GrenseMedLokalid(grense4, Lokalid("4"))
        )

        val faktiskFlate = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(grenserMedLokalid)

        val hullLineStrings = listOf(listOf(grense3), listOf(grense4))
        val polygon = SosiGeometryFunctions.lagFlate(listOf(grense1, grense2), hullLineStrings)
        val forventetFlate = geometryFactory.createMultiPolygon(listOf(polygon).toTypedArray())

        assertTrue(forventetFlate.equalsTopo(faktiskFlate))
    }

    @Test
    fun thatIkkeLukketFlateFeiler() {
        val geometryFactory = GeometryFactory()

        // 4 grenser, hvor de to første skal danne et skall, men dette skallet er ikke komplett
        val grense1 = geometryFactory.createLineString(arrayOf(
            Coordinate(100.0, 100.0),
            Coordinate(700.0, 100.0),
            Coordinate(700.0, 400.0)
        ))
        val grense2 = geometryFactory.createLineString(arrayOf(
            Coordinate(700.0, 400.0),
            Coordinate(100.0, 400.0)
        ))
        val grense3 = geometryFactory.createLineString(arrayOf(
            Coordinate(200.0, 200.0),
            Coordinate(300.0, 200.0),
            Coordinate(300.0, 300.0),
            Coordinate(200.0, 300.0),
            Coordinate(200.0, 200.0)
        ))

        val grense4 = geometryFactory.createLineString(arrayOf(
            Coordinate(400.0, 200.0),
            Coordinate(600.0, 200.0),
            Coordinate(600.0, 300.0),
            Coordinate(400.0, 300.0),
            Coordinate(400.0, 200.0)
        ))

        val grenserMedLokalid = mutableListOf(
            GrenseMedLokalid(grense1, Lokalid("1")),
            GrenseMedLokalid(grense2, Lokalid("2")),
            GrenseMedLokalid(grense3, Lokalid("3")),
            GrenseMedLokalid(grense4, Lokalid("4"))
        )

        assertThatThrownBy { SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(grenserMedLokalid) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Klarte ikke å danne flate grunnet ukomplette flater, antall som feiler: 1")

    }

    @Test
    fun thatIkkeLukketHullFeiler() {
        val geometryFactory = GeometryFactory()

        // 4 grenser, hvor to danner et skall og de to siste danner to hull i skallet, men begge disse har hull
        val grense1 = geometryFactory.createLineString(arrayOf(
            Coordinate(100.0, 100.0),
            Coordinate(700.0, 100.0),
            Coordinate(700.0, 400.0)
        ))
        val grense2 = geometryFactory.createLineString(arrayOf(
            Coordinate(700.0, 400.0),
            Coordinate(100.0, 400.0),
            Coordinate(100.0, 100.0)
        ))
        val grense3 = geometryFactory.createLineString(arrayOf(
            Coordinate(200.0, 200.0),
            Coordinate(300.0, 200.0),
            Coordinate(300.0, 300.0),
            Coordinate(200.0, 300.0)
        ))

        val grense4 = geometryFactory.createLineString(arrayOf(
            Coordinate(400.0, 200.0),
            Coordinate(600.0, 200.0),
            Coordinate(600.0, 300.0),
            Coordinate(400.0, 300.0)
        ))

        val grenserMedLokalid = mutableListOf(
            GrenseMedLokalid(grense1, Lokalid("1")),
            GrenseMedLokalid(grense2, Lokalid("2")),
            GrenseMedLokalid(grense3, Lokalid("3")),
            GrenseMedLokalid(grense4, Lokalid("4"))
        )

        assertThatThrownBy { SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(grenserMedLokalid) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Klarte ikke å danne flate grunnet ukomplette flater, antall som feiler: 2")

    }

    @Test
    fun thatFlaterDannesAvGrenserSomIkkeErSortert() {
        val geometryFactory = GeometryFactory()

        val grense1 =  GrenseMedLokalid(geometryFactory.createLineString(arrayOf(
            Coordinate(1.0, 1.0),
            Coordinate(2.0, 1.0)
        )), Lokalid("1"))
        val grense2 = GrenseMedLokalid(geometryFactory.createLineString(arrayOf(
            Coordinate(2.0, 1.0),
            Coordinate(3.0, 2.0)
        )), Lokalid("2"))
        val grense3 = GrenseMedLokalid(geometryFactory.createLineString(arrayOf(
            Coordinate(3.0, 2.0),
            Coordinate(4.0, 2.0)
        )), Lokalid("3"))
        val grense4 = GrenseMedLokalid(geometryFactory.createLineString(arrayOf(
            Coordinate(4.0, 2.0),
            Coordinate(4.0, 4.0)
        )), Lokalid("4"))
        val grense5 = GrenseMedLokalid(geometryFactory.createLineString(arrayOf(
            Coordinate(4.0, 4.0),
            Coordinate(2.0, 4.0)
        )), Lokalid("5"))
        val grense6 = GrenseMedLokalid(geometryFactory.createLineString(arrayOf(
            Coordinate(2.0, 4.0),
            Coordinate(1.0, 1.0)
        )), Lokalid("5"))
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

        val faktiskFlate = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(usorterteGrenser)

        val linearRing = geometryFactory.createLinearRing(geometryFactory.createMultiLineString(sorterteGrenser.map{ it.grense }.toTypedArray()).coordinates)
        val polygon = geometryFactory.createPolygon(linearRing)
        val forventetFlate = geometryFactory.createMultiPolygon(listOf(polygon).toTypedArray())

        assertTrue(forventetFlate.equalsTopo(faktiskFlate))
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
            .withGrensetype(Grensetype.Grunnkretsgrense).build()
        )
}


