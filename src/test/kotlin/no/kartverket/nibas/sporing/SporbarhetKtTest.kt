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
    fun oppretteGyldighetMedIntervall0DagerGirFeil() {
        assertThatThrownBy { grunnkrets(august1, august1) }
            .isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Gyldighetsintervall er ugyldig")
    }

    @Test
    fun setGyldigTilTidligereEnnGyldigFra() {
        val grunnkrets = grunnkrets(august1, october17)
        assertThatThrownBy { grunnkrets.setGyldigTil(juli31) }
            .isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Gyldighetsintervall er ugyldig")
    }

    // Tillater å sette gyldig til lik gyldig fra, tilstanden blir da: slettet = true
    @Test
    fun setGyldigTilLikGyldigFraGirSlettetGyldighet() {
        val grunnkrets = grunnkrets(august1, october17)
        grunnkrets.setGyldigTil(august1)
        assertThat(grunnkrets.gyldighet.slettet).isTrue
    }

    // Tillater ikke å sette gyldigFra lik gyldigTil.
    @Test
    fun setGyldigFraLikGyldigTilGirFeil() {
        val grunnkrets = grunnkrets(august1, october17)
        assertThatThrownBy {
            grunnkrets.setGyldigFra(october17)
        }.isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Gyldighetsintervall er ugyldig")
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
        assertThatThrownBy { grunnkrets.setGyldigFra(october18) }
            .isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Gyldighetsintervall er ugyldig")
    }

    @Test
    fun tidspunktEtterGyldigFraMedAapentIntervall() {
        assertThat(grunnkretsMedAapentIntervall.gyldigPaaDato(october17)).isTrue
    }

    @Test
    fun tidspunktIdentiskMedGyldigFraMedAapentIntervall() {
        assertThat(grunnkretsMedAapentIntervall.gyldigPaaDato(august1)).isTrue
    }

    @Test
    fun tidspunktTidligereEnnGyldigFraMedAapentIntervall() {
        assertThat(grunnkretsMedAapentIntervall.gyldigPaaDato(juli31)).isFalse
    }

    @Test
    fun tidspunktDagenFoerGyldigTilMedLukketIntervall() {
        assertThat(grunnkretsMedLukketIntervall.gyldigPaaDato(october16)).isTrue
    }

    @Test
    fun tidspunktIdentiskMedGyldigTilMedLukketIntervall() {
        assertThat(grunnkretsMedLukketIntervall.gyldigPaaDato(october17)).isFalse
    }

    @Test
    fun tidspunktDagenEtterGyldigTilMedLukketIntervall() {
        assertThat(grunnkretsMedLukketIntervall.gyldigPaaDato(october18)).isFalse
    }

    @Test
    fun tidspunktTidligereEnnGyldigFraMedLukketIntervall() {
        assertThat(grunnkretsMedLukketIntervall.gyldigPaaDato(juli31)).isFalse
    }

    @Test
    fun ugyldigIntervallGirIkkeGyldigPaaTidspunkt() {
        assertThat(grunnkrets(august1, october17, slettet = true).gyldigPaaDato(august1)).isFalse
    }

    @Test
    fun setUgyldig() {
        val grunnkrets = grunnkrets(august1, october17)
        assertThat(grunnkrets.gyldighet.slettet).isFalse

        grunnkrets.setSlettet()
        assertThat(grunnkrets.gyldighet.slettet).isTrue
        assertThat(grunnkrets.gyldigPaaDato(august1)).isFalse
    }

    @Test
    fun `overlapping av gyldigheter`() {
        var gyldighetA = Gyldighet(juli31)
        var gyldighetB = Gyldighet(juli31)

        //Gyldigheter med lik start (31.7.) og åpen slutt overlapper
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isTrue
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isTrue

        //Gyldigheter med ulik start (31.7. og 1.8.) og åpen slutt overlapper
        assertThat(gyldighetA.setGyldigFra(august1).overlapperMed(gyldighetB)).isTrue
        assertThat(gyldighetB.overlapperMed(gyldighetA.setGyldigFra(august1))).isTrue

        //Slettet gyldighet overlapper ikke
        assertThat(gyldighetA.setSlettet().overlapperMed(gyldighetB)).isFalse
        assertThat(gyldighetB.overlapperMed(gyldighetA.setSlettet())).isFalse

        gyldighetA = Gyldighet(juli31, august1)
        gyldighetB = Gyldighet(juli31, august1)
        val gyldighetC = Gyldighet(juli31)

        //Gyldigheter med lik start (31.7.) og lik slutt (1.8.) overlapper
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isTrue
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isTrue

        //Gyldigheter med lik start (31.7.) og ulik slutt (1.8., 2.8. og null) overlapper
        gyldighetA = Gyldighet(juli31, august1.plusDays(1))
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isTrue
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isTrue
        assertThat(gyldighetA.overlapperMed(gyldighetC)).isTrue
        assertThat(gyldighetC.overlapperMed(gyldighetA)).isTrue

        //Gyldighetene 31.7. - 1.8. og 30.7. - 1.8. overlapper
        gyldighetA = Gyldighet(juli31.minusDays(1), august1)
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isTrue
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isTrue

        //Gyldighetene 31.7. -> og 30.7. - 1.8. overlapper
        assertThat(gyldighetA.overlapperMed(gyldighetC)).isTrue
        assertThat(gyldighetC.overlapperMed(gyldighetA)).isTrue

        //Gyldighetene 31.7. - 1.8. og 30.7. - 2.8. overlapper
        gyldighetA = Gyldighet(juli31.minusDays(1), august1.plusDays(1))
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isTrue
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isTrue

        //Gyldighetene 31.7. - 2.8. og 30.7. - 1.8. overlapper
        gyldighetA = Gyldighet(juli31.minusDays(1), august1)
        gyldighetB = Gyldighet(juli31, august1.plusDays(1))
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isTrue
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isTrue

        //Gyldighetene 30.7. - 1.8. og 1.8. - 17.10. overlapper ikke
        gyldighetB = Gyldighet(august1, october17)
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isFalse
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isFalse

        //Gyldighetene 30.7. - 1.8. og 1.8. -> overlapper ikke
        gyldighetB = Gyldighet(august1)
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isFalse
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isFalse

        //Gyldighetene 30.7. - 1.8. og 16.10. - 17.10. overlapper ikke
        gyldighetB = Gyldighet(october16, october17)
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isFalse
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isFalse

        //Gyldighetene 30.7. - 1.8. og 16.10. -> overlapper ikke
        gyldighetB = Gyldighet(october16)
        assertThat(gyldighetA.overlapperMed(gyldighetB)).isFalse
        assertThat(gyldighetB.overlapperMed(gyldighetA)).isFalse
    }

    private fun grunnkrets(gyldigFra: LocalDate, gyldigTil: LocalDate?, slettet: Boolean = false) = GrunnkretsBuilder(slettet = slettet)
        .withGyldighetsIntervall(Pair(gyldigFra, gyldigTil))
        .build()
}
