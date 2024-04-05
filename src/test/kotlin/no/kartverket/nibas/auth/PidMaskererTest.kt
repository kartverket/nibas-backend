package no.kartverket.nibas.auth

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class PidMaskererTest {
    @Test
    fun `maskerer PID med å bytte tegn med X`() {
        assertThat(PidMaskerer.getMaskedName("01015065689")).isEqualTo("010150XXXXX")
        assertThat(PidMaskerer.getMaskedName(" 01015065689")).isEqualTo("010150XXXXX")
        assertThat(PidMaskerer.getMaskedName("01015065689 ")).isEqualTo("010150XXXXX")
        assertThat(PidMaskerer.getMaskedName("Jens")).isEqualTo("Jens")
    }
}
