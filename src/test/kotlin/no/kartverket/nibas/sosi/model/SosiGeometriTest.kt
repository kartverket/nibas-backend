package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

internal class SosiGeometriTest {

    @Test
    fun testThatDatafangstDatoReturnsDateAtStartOfDayAsDateTime() {
        val geometri = SosiGeometri("STUFF", 0, mutableListOf())
        val dato = SosiObjekt(niva = 0, type = SosiKoder.DATAFANGSTDATO, verdier = mutableListOf(SosiVerdi("20040201")))
        geometri.addBarn(dato)

        val datafangstDato: ZonedDateTime? = geometri.getDatafangstDato()

        assertThat(datafangstDato).isNotNull
        assertThat(datafangstDato!!.year).isEqualTo(2004)
        assertThat(datafangstDato.monthValue).isEqualTo(2)
        assertThat(datafangstDato.dayOfMonth).isEqualTo(1)
        assertThat(datafangstDato.hour).isEqualTo(0)
        assertThat(datafangstDato.minute).isEqualTo(0)
        assertThat(datafangstDato.second).isEqualTo(0)
    }

    @Test
    fun testThatGyldigTilReturnsCorrectDate() {
        val geometri = SosiGeometri("STUFF", 0, mutableListOf())
        val dato = SosiObjekt(niva = 0, type = SosiKoder.GYLDIGTIL, verdier = mutableListOf(SosiVerdi("20040201")))
        geometri.addBarn(dato)

        val gyldigTil: LocalDate? = geometri.getGyldigTil()

        assertThat(gyldigTil).isNotNull
        assertThat(gyldigTil!!.year).isEqualTo(2004)
        assertThat(gyldigTil.monthValue).isEqualTo(2)
        assertThat(gyldigTil.dayOfMonth).isEqualTo(1)
    }
}
