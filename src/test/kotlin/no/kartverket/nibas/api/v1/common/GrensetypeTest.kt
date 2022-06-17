package no.kartverket.nibas.api.v1.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class GrensetypeTest {

    @Test
    fun fromValue() {
        assertThat(Grensetype.fromValue("LovVirkeområdeGrense")).isEqualTo(Grensetype.Lovvirkeomraadegrense)
    }

    @Test
    fun fromValueNotMatching() {
        assertThatThrownBy { Grensetype.fromValue("Grense2") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun fromDbKey() {
        assertThat(Grensetype.fromDbKey("LovVirkeområdeGrense")).isEqualTo(Grensetype.Lovvirkeomraadegrense)
    }

    @Test
    fun fromDbKeyNotMatching() {
        assertThatThrownBy { Grensetype.fromDbKey("Grense2") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}


