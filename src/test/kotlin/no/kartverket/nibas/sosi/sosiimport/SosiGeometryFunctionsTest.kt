package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
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

    private fun grunnkretsGrenseKobling(coordinates: List<Coordinate>, rekkefoelge: Int) =
        GrunnkretsGrenseKobling(GrunnkretsBuilder().build(), GrenseBuilder()
            .withGrenseGeometri(coordinates)
            .buildGrunnkretsgrense(),
            GrenseKoblingAttributter(rekkefoelge = rekkefoelge))
}

