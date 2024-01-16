package no.kartverket.nibas.utkast

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.utkast.OperasjonerValidering
import no.kartverket.nibas.service.utkast.UtkastValidatorService
import no.kartverket.nibas.service.utkast.UtkastValidering
import no.kartverket.nibas.utkast.UtkastServiceTest.Companion.operasjonerMedIkkeRedigerbareGrenser
import no.kartverket.nibas.utkast.UtkastServiceTest.Companion.operasjonerMedKunRedigerbareGrenser
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
    private lateinit var kodelisteServiceMock: KodelisteService

    @MockK
    private lateinit var grenseServiceMock: GrenseService

    @MockK
    private lateinit var currentTime: CurrentTime

    private lateinit var utkastValidatorService: UtkastValidatorService

    @BeforeEach
    fun setup () {
        MockKAnnotations.init(this)
        every { currentTime.asLocalDate() }  returns LocalDate.of(2022, 6, 1)
        every { grunnkretsServiceMock.hentGrunnkrets(any()) }  returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS
        utkastValidatorService = UtkastValidatorService(grunnkretsServiceMock, grenseServiceMock, stemmekretsServiceMock, kodelisteServiceMock, currentTime)
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
    fun `valider operasjoner med ikke-redigerbare grenser gir ugyldige operasjoner`() {
        val operasjoner = operasjonerMedIkkeRedigerbareGrenser()

        val validateGyldigeOperasjoner = utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner).isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at metadataoperasjoner med blankt grunnkretsnavn gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf(
                        "1" to GrunnkretsRequestBuilder(navn = "gyldigNavn").build(),
                        "2" to GrunnkretsRequestBuilder(navn = "").build(),
                    )
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at metadataoperasjoner med blankt stemmekretsnavn gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnavn = "").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at sammenslaaingsoperasjoner med blankt stemmekretsnavn gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                stemmekretsSammenslaaingsendring =
                StemmekretsSammenslaaingsendringRequest(
                    viderefoertStemmekrets = IdentifikatorMedVersjon("1", 5),
                    stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("2", 1)),
                    stemmekretsNavn = "",
                    stemmekretsNummer = "1234567"
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at metadataoperasjoner med blankt grunnkretsnummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at metadataoperasjoner med 0 som stemmekretsnummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "0").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at sammenslaaingsoperasjoner med stemmekretsnummer med bokstaver gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                stemmekretsSammenslaaingsendring =
                StemmekretsSammenslaaingsendringRequest(
                    viderefoertStemmekrets = IdentifikatorMedVersjon("1", 5),
                    stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("2", 1)),
                    stemmekretsNavn = "gyldigNavn",
                    stemmekretsNummer = "ugyldigNummer"
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at grunnkretsnummer med mindre enn 8 siffer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "1234567").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at grunnkretsnummer med mer enn 8 siffer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "123456789").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at negativt grunnkretsnummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "-12345678").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at stemmekretsnummer med mer enn 4 siffer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "12345").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at negativt stemmekretsnummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "-123").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at grunnkretsnummer som ikke matcher med kommunenummer gir ugyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "12345678").build())
                )
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.UgyldigeOperasjoner::class.java)
    }

    @Test
    fun `valider at godkjente operasjoner gir gyldige operasjoner`() {
        val operasjoner =
            Operasjoner(
                metadataendringer =
                Metadataendringer(
                    grunnkretsendringer =
                    mapOf(
                        "1" to GrunnkretsRequestBuilder.DEFAULT_REQUEST,
                        "2" to GrunnkretsRequestBuilder(lokalId = "2").build(),
                    ),
                    stemmekretsendringer =
                    mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "123").build())
                ),
                stemmekretsSammenslaaingsendring =
                StemmekretsSammenslaaingsendringRequest(
                    viderefoertStemmekrets = IdentifikatorMedVersjon("1", 5),
                    stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("2", 1)),
                    stemmekretsNavn = "gyldigNavn",
                    stemmekretsNummer = "4"
                ),
                grenseendringer = operasjonerMedKunRedigerbareGrenser().grenseendringer
            )

        val validateGyldigeOperasjoner =
            utkastValidatorService.validateGyldigeOperasjoner(operasjoner)
        assertThat(validateGyldigeOperasjoner)
            .isInstanceOf(OperasjonerValidering.GyldigeOperasjoner::class.java)
    }
}
