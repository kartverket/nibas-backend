package no.kartverket.nibas.sporing

import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.common.ResponseException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class SporbarhetKtTest {

    private val juli31 = LocalDate.of(2022, Month.JULY, 31)
    private val august1 = LocalDate.of(2022, Month.AUGUST, 1)
    private val october16 = LocalDate.of(2022, Month.OCTOBER, 16)
    private val october17 = LocalDate.of(2022, Month.OCTOBER, 17)
    private val october18 = LocalDate.of(2022, Month.OCTOBER, 18)
    private val grunnkretsMedAapentIntervall = grunnkrets(august1, null)
    private val grunnkretsMedLukketIntervall = grunnkrets(august1, october17)

    @Test
    fun setGyldigTilTidligereEnnGyldigFra() {
        val grunnkrets = grunnkrets(august1, october17)
        assertThatThrownBy { grunnkrets.setGyldigTil(juli31) }.isInstanceOf(ResponseException.BadRequestException::class.java)
    }

    @Test
    fun setGyldigTilLikGyldigFra() {
        val grunnkrets = grunnkrets(august1, october17)
        grunnkrets.setGyldigTil(august1)
        assertThat(grunnkrets.gyldighet.gyldigTil).isEqualTo(august1)
    }

    @Test
    fun setGyldigTilEtterGyldigFra() {
        val grunnkrets = grunnkrets(august1, october17)
        grunnkrets.setGyldigTil(october16)
        assertThat(grunnkrets.gyldighet.gyldigTil).isEqualTo(october16)
    }

    @Test
    fun setGyldigFraSenereEnnGyldigTil() {
        val grunnkrets = grunnkrets(august1, october17)
        assertThatThrownBy { grunnkrets.setGyldigFra(october18) }.isInstanceOf(ResponseException.BadRequestException::class.java)
    }

    @Test
    fun setGyldigFraLikGyldigTil() {
        val grunnkrets = grunnkrets(august1, october17)
        grunnkrets.setGyldigFra(october17)
        assertThat(grunnkrets.gyldighet.gyldigFra).isEqualTo(october17)
    }

    @Test
    fun tidspunktEtterGyldigFraMedAapentIntervall() {
        assertThat(grunnkretsMedAapentIntervall.gyldigPaaTidspunkt(october17)).isTrue
    }

    @Test
    fun tidspunktIdentiskMedGyldigFraMedAapentIntervall() {
        assertThat(grunnkretsMedAapentIntervall.gyldigPaaTidspunkt(august1)).isTrue
    }

    @Test
    fun tidspunktTidligereEnnGyldigFraMedAapentIntervall() {
        assertThat(grunnkretsMedAapentIntervall.gyldigPaaTidspunkt(juli31)).isFalse
    }

    @Test
    fun tidspunktDagenFoerGyldigTilMedLukketIntervall() {
        assertThat(grunnkretsMedLukketIntervall.gyldigPaaTidspunkt(october16)).isTrue
    }

    @Test
    fun tidspunktIdentiskMedGyldigTilMedLukketIntervall() {
        assertThat(grunnkretsMedLukketIntervall.gyldigPaaTidspunkt(october17)).isFalse
    }

    @Test
    fun tidspunktDagenEtterGyldigTilMedLukketIntervall() {
        assertThat(grunnkretsMedLukketIntervall.gyldigPaaTidspunkt(october18)).isFalse
    }

    @Test
    fun tidspunktTidligereEnnGyldigFraMedLukketIntervall() {
        assertThat(grunnkretsMedLukketIntervall.gyldigPaaTidspunkt(juli31)).isFalse
    }

    @Test
    fun intervallFor1DagGirIkkeGyldigPaaTidspunkt() {
        val grunnkretsMedIdentiskFraOgTilDato = grunnkrets(august1, august1)
        assertThat(grunnkretsMedIdentiskFraOgTilDato.gyldigPaaTidspunkt(august1)).isFalse
    }

    private fun grunnkrets(gyldigFra: LocalDate, gyldigTil: LocalDate?) = GrunnkretsBuilder()
        .withGyldighetsIntervall(Pair(gyldigFra, gyldigTil))
        .build()
}
