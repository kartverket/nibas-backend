package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate

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


