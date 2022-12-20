package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.common.Lokalid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class GrenseTest {

    private val grensetyper = setOf(Grensetype.Grunnkretsgrense)
    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28) // Dagens dato
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    @Test
    fun finnRelevantGrenseForTyperOgDato_FinnerGrensaSelv() {
        val grunnkretsgrense = GrenseBuilder(
            id = "17",
            gyldigFra = october1In2022
        ).buildGrunnkretsgrense()
        assertThat(grunnkretsgrense.finnRelevantGrenseForTyperOgDato(grensetyper, november28In2022)!!.id).isEqualTo("17")
        assertThat(grunnkretsgrense.erRelevantGrenseForTyperOgDato(grensetyper, november28In2022)).isTrue
    }

    @Test
    fun finnRelevantGrenseForTyperOgDato_FinnerGrensasNesteVersjon() {
        val lokalidForGrense = Lokalid("888")

        val grunnkretsgrenseGyldigI2024 = GrenseBuilder(
            id = "2002",
            lokalId = lokalidForGrense,
            gyldigFra = jan1In2024
        ).buildGrunnkretsgrense()

        val grunnkretsgrenseGyldigI2023 = GrenseBuilder(
            id = "2001",
            lokalId = lokalidForGrense,
            gyldigFra = jan1In2023,
            gyldigTil = jan1In2024,
            nesteVersjon = grunnkretsgrenseGyldigI2024
        ).buildGrunnkretsgrense()

        val grunnkretsgrense = GrenseBuilder(
            id = "2000",
            lokalId = lokalidForGrense,
            gyldigFra = october1In2022,
            gyldigTil = jan1In2023,
            nesteVersjon = grunnkretsgrenseGyldigI2023
        ).buildGrunnkretsgrense()

        assertThat(grunnkretsgrense.finnRelevantGrenseForTyperOgDato(grensetyper, jan1In2023)!!.id).isEqualTo("2001")
        assertThat(grunnkretsgrense.erRelevantGrenseForTyperOgDato(grensetyper, jan1In2023)).isTrue
    }
}
