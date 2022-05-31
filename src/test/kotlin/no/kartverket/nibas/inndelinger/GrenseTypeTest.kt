package no.kartverket.nibas.inndelinger

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class GrenseTypeTest {

    @Test
    fun fromValue() {
        assertThat(GrenseType.fromValue("LovVirkeområdeGrense")).isEqualTo(GrenseType.Lovvirkeomraadegrense)
    }

    @Test
    fun fromValueNotMatching() {
        assertThatThrownBy { GrenseType.fromValue("Grense2") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun fromDbKey() {
        assertThat(GrenseType.fromDbKey("LovVirkeområdeGrense")).isEqualTo(GrenseType.Lovvirkeomraadegrense)
    }

    @Test
    fun fromDbKeyNotMatching() {
        assertThatThrownBy { GrenseType.fromDbKey("Grense2") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}


