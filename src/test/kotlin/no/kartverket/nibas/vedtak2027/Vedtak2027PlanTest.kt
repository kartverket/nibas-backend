package no.kartverket.nibas.vedtak2027

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Vedtak2027PlanTest {
    // Feiler hvis en midlertidig dato for lokal testing blir med inn på main
    @Test
    fun `Liabygda har virkningsdato 1 januar 2027`() {
        assertThat(Vedtak2027Plan.LIABYGDA.virkningsdato).isEqualTo(LocalDate.of(2027, 1, 1))
    }
}
