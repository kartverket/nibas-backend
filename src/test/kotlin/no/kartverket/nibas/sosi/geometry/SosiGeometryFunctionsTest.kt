package no.kartverket.nibas.sosi.geometry

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import kotlin.math.hypot

class SosiGeometryFunctionsTest {

    @Test
    fun `bue mot klokka gir punkter paa sirkelen og eksakte endepunkter`() {
        val start = Coordinate(10.0, 0.0)
        val slutt = Coordinate(-10.0, 0.0)
        val punkter = bueTilKoordinater(start, Coordinate(0.0, 10.0), slutt)

        assertThat(punkter.first()).isSameAs(start)
        assertThat(punkter.last()).isSameAs(slutt)
        // Halvsirkel med 32 segmenter per kvartsirkel
        assertThat(punkter).hasSize(65)
        punkter.forEach { assertThat(hypot(it.x, it.y)).isCloseTo(10.0, Offset.offset(1e-9)) }
        assertThat(punkter.map { it.y }).allMatch { it >= -1e-9 }
    }

    @Test
    fun `bue med klokka gaar gjennom midtpunktet og ikke rundt motsatt vei`() {
        val punkter = bueTilKoordinater(Coordinate(-10.0, 0.0), Coordinate(0.0, 10.0), Coordinate(10.0, 0.0))

        assertThat(punkter.map { it.y }).allMatch { it >= -1e-9 }
        assertThat(punkter).hasSize(65)
    }

    @Test
    fun `tre punkter paa linje blir en rett linje`() {
        val punkter = bueTilKoordinater(Coordinate(0.0, 0.0), Coordinate(1.0, 1.0), Coordinate(2.0, 2.0))

        assertThat(punkter).hasSize(3)
    }

    @Test
    fun `liten bue paa UTM-koordinater holder seg paa sirkelen`() {
        val senter = Coordinate(372300.12, 6460050.34)
        val radius = 0.1
        val punkter = bueTilKoordinater(
            Coordinate(senter.x - radius, senter.y),
            Coordinate(senter.x, senter.y + radius),
            Coordinate(senter.x + radius, senter.y),
        )

        punkter.forEach { assertThat(it.distance(senter)).isCloseTo(radius, Offset.offset(1e-6)) }
    }
}
