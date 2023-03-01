package no.kartverket.nibas.utkast

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.utkast.UtkastServiceTest.Companion.operasjonerMedIkkeEditerbareGrenser
import no.kartverket.nibas.utkast.UtkastServiceTest.Companion.operasjonerMedKunEditerbareGrenser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate


class UtkastValideringTest {

    @MockK
    private lateinit var grunnkretsServiceMock: GrunnkretsService

    @MockK
    private lateinit var stemmekretsServiceMock: StemmekretsService

    @MockK
    private lateinit var currentTime: CurrentTime

    private lateinit var utkastValidatorService: UtkastValidatorService

    @BeforeEach
    fun setup () {
        MockKAnnotations.init(this)
        every { currentTime.asLocalDate() }  returns LocalDate.of(2022, 6, 1)
        utkastValidatorService = UtkastValidatorService(grunnkretsServiceMock, stemmekretsServiceMock, currentTime)
    }

    @Test
    fun `valider utkast med annen status enn opprettet gir ugyldig utkast` () { //publiserUtkastMedAnnenStatusEnnOpprettetGirBadRequestEx
        val publisertUtkast = UtkastBuilder.builder("Utkast for publisering")
            .withStatus(UtkastStatus.PUBLISERT)
            .build()

        val validateUtkastKlartForPublisering = utkastValidatorService.validateUtkastKlartForPublisering(publisertUtkast)
        assertThat(validateUtkastKlartForPublisering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
    }

    @Test
    fun `valider utkast uten operasjoner gir ugyldig utkast`() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering")
            .build()

        val validateUtkastKlartForPublisering = utkastValidatorService.validateUtkastKlartForPublisering(utkastUtenOperasjoner)
        assertThat(validateUtkastKlartForPublisering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
    }

    @Test
    fun `valider utkast med gyldig fra i fortiden gir ugyldig utkast`() {
        val utkastMedGyldigFraIFortiden = UtkastBuilder.builder("Utkast for publisering")
            .withGyldigFra(LocalDate.of(2021, 1, 1))
            .build()

        val validateUtkastKlartForPublisering = utkastValidatorService.validateUtkastKlartForPublisering(utkastMedGyldigFraIFortiden)
        assertThat(validateUtkastKlartForPublisering).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)
    }

    @Test
    fun `valider operasjoner med ikke-editerbare grenser gir ugyldige operasjoner`() {
        val operasjoner = operasjonerMedIkkeEditerbareGrenser()

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider operasjoner med kun editerbare grenser gir gyldige operasjoner`() {
        val operasjoner = operasjonerMedKunEditerbareGrenser()

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).isInstanceOf(OperasjonerValidering.GyldigeOperasjoner::class.java)
    }


}
