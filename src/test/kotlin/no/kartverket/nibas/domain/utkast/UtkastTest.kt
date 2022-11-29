package no.kartverket.nibas.domain.utkast

import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtkastTest {

    @Test
    fun setGyldigFra() {
        val utkast = UtkastBuilder.builder("Mitt utkast", Endringstype.NAVNEENDRING)
            .build()

        assertThatThrownBy {
            utkast.gyldigFra = LocalDate.now().minusWeeks(1)
        }.isInstanceOf(ResponseException.BadRequestException::class.java)

    }
}
