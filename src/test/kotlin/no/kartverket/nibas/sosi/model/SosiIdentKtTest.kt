package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class SosiIdentKtTest {

    @Test
    fun lokalIdIsInvalidUUID() {
        val invalidUuid = "123456789"
        val sosiIdent = sosiIdentWithLokalId(invalidUuid)
        assertThat(sosiIdent.lokalIdIsValidUUID()).isFalse
    }

    @Test
    fun lokalIdIsValidUUID() {
        val randomValidUUID = UUID.randomUUID().toString()
        val sosiIdent2 = sosiIdentWithLokalId(randomValidUUID)
        assertThat(sosiIdent2.lokalIdIsValidUUID()).isTrue
    }

    private fun sosiIdentWithLokalId(lokalId: String): SosiIdent {
        val sosiIdent = SosiIdent(1, mutableListOf(SosiVerdi("ident1")))
        sosiIdent.addBarn(SosiObjekt(SosiKoder.LOKALID, 1, mutableListOf(SosiVerdi(lokalId))))
        return sosiIdent
    }
}
