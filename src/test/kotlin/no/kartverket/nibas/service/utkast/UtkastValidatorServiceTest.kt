package no.kartverket.nibas.service.utkast

import io.mockk.every
import io.mockk.mockk
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.StemmekretsService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate


class UtkastValidatorServiceTest {
    val grunnkretsServiceMock = mockk<GrunnkretsService>()
    val grenseServiceMock = mockk<GrenseService>()
    val stemmekretsServiceMock = mockk<StemmekretsService>()
    val kodelisteServiceMock = mockk<KodelisteService>()
    val currentTimeMock = mockk<CurrentTime>()

    val utkastValidatorService = UtkastValidatorService(
        grunnkretsServiceMock, grenseServiceMock, stemmekretsServiceMock,
        kodelisteServiceMock, currentTimeMock
    )

    val eksisterendeTingGyldigFra = LocalDate.of(2022, 1, 1)
    val datoSomVilGiFremtidEndringKonflikt = LocalDate.of(2021, 1, 1)
    val iDag = LocalDate.of(2020, 1, 1)

    @BeforeEach
    fun setup() {
        every { currentTimeMock.asLocalDate() } returns iDag

        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf(
            GrunnkretsBuilder()
                .withGyldigFra(eksisterendeTingGyldigFra)
                .buildUtenKommune()
        )

        every { stemmekretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf(
            StemmekretsBuilder()
                .withGyldigFra(eksisterendeTingGyldigFra)
                .build()
        )

        every { grenseServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf(
            GrenseBuilder()
                .withGyldigFra(eksisterendeTingGyldigFra)
                .build()
        )
    }

    @Test
    fun `skal gi feil om det finnes nyere endringer paa grunnkrets`() {
        every { grunnkretsServiceMock.hentGrunnkrets(any()) } returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS.apply {
            kommuneLokalid = UnikLokalid(
                lokalid = "lokalid",
                flatetype = Flatetype.KOMMUNE
            ).also {
                it.kommuner = mutableSetOf(KommuneBuilder()
                    .withGyldigFra(eksisterendeTingGyldigFra)
                    .build()
                )
            }
        }

        val utkast = UtkastBuilder.builder("test")
            .withGyldigFra(datoSomVilGiFremtidEndringKonflikt)
            .withGrunnkretsRequests(mapOf(
                "lokalId" to GrunnkretsRequest(
                    "navn",
                    "03013456",
                    Identifikasjon(lokalid = "lokalid"),
                    0)
            ))
            .build()

        val result = utkastValidatorService.validateUtkastKlartForPublisering(utkast)
        assertThat(result).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)

        val ugyldigUtkast = result as UtkastValidering.UgyldigUtkast
        assertThat(ugyldigUtkast.feil).isInstanceOf(FeilVedOppdatering.OppdateringException::class.java)

        val oppdateringFeil = result.feil as FeilVedOppdatering.OppdateringException
        assertThat(oppdateringFeil.oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.GRUNNKRETS)
    }

    @Test
    fun `skal gi feil om det finnes nyere endringer paa stemmekrets`() {
        val utkast = UtkastBuilder.builder("test")
            .withGyldigFra(datoSomVilGiFremtidEndringKonflikt)
            .withStemmekretsRequests(mapOf(
                "lokalId" to StemmekretsRequest(
                    identifikasjon = Identifikasjon(lokalid = "lokalid"),
                    version = 0,
                    stemmekretsnummer = "0123",
                    stemmekretsnavn = "navn",
                    valgdistriktsnummer = "01",
                    tellekretsnavn = "tellekrets",
                    tellekretsnummer = "01"
                )
            ))
            .build()

        val result = utkastValidatorService.validateUtkastKlartForPublisering(utkast)
        assertThat(result).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)

        val ugyldigUtkast = result as UtkastValidering.UgyldigUtkast
        assertThat(ugyldigUtkast.feil).isInstanceOf(FeilVedOppdatering.OppdateringException::class.java)

        val oppdateringFeil = result.feil as FeilVedOppdatering.OppdateringException
        assertThat(oppdateringFeil.oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.STEMMEKRETS)
    }

    @Test
    fun `skal gi feil om det finnes nyere endringer paa grense`() {
        val utkast = UtkastBuilder.builder("test")
            .withGyldigFra(datoSomVilGiFremtidEndringKonflikt)
            .withOperasjoner(
                Operasjoner(
                    grenseendringer = Grenseendringer(
                        endredeFeatures = mapOf(
                            "0101" to FeatureBuilder()
                                .withGrensetype(Grensetype.Grunnkretsgrense)
                                .build()
                        )
                    )
                )
            )
            .build()

        val result = utkastValidatorService.validateUtkastKlartForPublisering(utkast)
        assertThat(result).isInstanceOf(UtkastValidering.UgyldigUtkast::class.java)

        val ugyldigUtkast = result as UtkastValidering.UgyldigUtkast
        assertThat(ugyldigUtkast.feil).isInstanceOf(FeilVedOppdatering.OppdateringException::class.java)

        val oppdateringFeil = result.feil as FeilVedOppdatering.OppdateringException
        assertThat(oppdateringFeil.oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.GRENSE)
    }
}
