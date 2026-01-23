package no.kartverket.nibas.api.v1.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class GrensetypeTest {

    @Test
    fun fromValue() {
        assertThat(Grensetype.fromValue("Stemmekretsgrense")).isEqualTo(Grensetype.Stemmekretsgrense)
    }

    @Test
    fun fromValueNotMatching() {
        assertThatThrownBy { Grensetype.fromValue("Grense2") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun fromDbKey() {
        assertThat(Grensetype.fromDbKey("Stemmekretsgrense")).isEqualTo(Grensetype.Stemmekretsgrense)
    }

    @Test
    fun fromDbKeyNotMatching() {
        assertThatThrownBy { Grensetype.fromDbKey("Grense2") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}


