package no.kartverket.nibas.service.utkast

import io.mockk.every
import io.mockk.mockk
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.KretsNavnOgNummer
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.LOKALID
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.Lokalid
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
                .buildUtenKommuneOgGrenser()
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

        val utkast = UtkastBuilder.builder("test", gyldigFra = datoSomVilGiFremtidEndringKonflikt)
            .withGrunnkretsRequests(mapOf(
                "lokalId" to GrunnkretsRequest(
                    "navn",
                    "03013456",
                    Identifikasjon(lokalid = "lokalid"),
                    0
                )
            ))
            .build()

        val result = utkastValidatorService.validerFremtidigeEndringer(utkast, gyldighetDato = utkast.gyldigFra)
        assertThat(result).isInstanceOf(FeilVedOppdatering.OppdateringException::class.java)

        val oppdateringFeil = result as FeilVedOppdatering.OppdateringException
        assertThat(oppdateringFeil.oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.GRUNNKRETS)
    }

    @Test
    fun `skal gi feil om det finnes nyere endringer paa grunnkrets ved deling`() {
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

        val delingEndringRequest = KretsDelingEndringRequest(
            opprinneligKrets = IdentifikatorMedVersjon(lokalId = LOKALID, version = 0),
            kommuneId = ObjektIdentifikator(Lokalid("lokalid"), eksisterendeTingGyldigFra),
            flatetype = Flatetype.GRUNNKRETS,
            nyeKretser = listOf(KretsNavnOgNummer("nyGrunnkrets", "1"))
        )

        val utkast = UtkastBuilder.builder("test", gyldigFra = datoSomVilGiFremtidEndringKonflikt)
            .withOperasjoner(Operasjoner(kretsDelingEndringer = listOf(delingEndringRequest)))
            .build()

        val result = utkastValidatorService.validerFremtidigeEndringer(utkast, utkast.gyldigFra)
        assertThat(result).isInstanceOf(FeilVedOppdatering.OppdateringException::class.java)

        val oppdateringFeil = result as FeilVedOppdatering.OppdateringException
        assertThat(oppdateringFeil.oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.GRUNNKRETS)
    }

    @Test
    fun `skal gi feil om det finnes nyere endringer paa stemmekrets ved deling`() {
        every { stemmekretsServiceMock.hentStemmekrets(any()) } returns StemmekretsBuilder.DEFAULT_STEMMEKRETS.apply {
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

        val delingEndringRequest = KretsDelingEndringRequest(
            opprinneligKrets = IdentifikatorMedVersjon(lokalId = LOKALID, version = 0),
            kommuneId = ObjektIdentifikator(Lokalid("lokalid"), eksisterendeTingGyldigFra),
            flatetype = Flatetype.STEMMEKRETS,
            nyeKretser = listOf(KretsNavnOgNummer("nyStemmekrets", "1"))
        )

        val utkast = UtkastBuilder.builder("test", gyldigFra = datoSomVilGiFremtidEndringKonflikt)
            .withOperasjoner(Operasjoner(kretsDelingEndringer = listOf(delingEndringRequest)))
            .build()

        val result = utkastValidatorService.validerFremtidigeEndringer(utkast, utkast.gyldigFra)
        assertThat(result).isInstanceOf(FeilVedOppdatering.OppdateringException::class.java)

        val oppdateringFeil = result as FeilVedOppdatering.OppdateringException
        assertThat(oppdateringFeil.oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.STEMMEKRETS)
    }

    @Test
    fun `skal gi feil om det finnes nyere endringer paa stemmekrets ved sammenslaaing`() {
        every { stemmekretsServiceMock.hentStemmekrets(any()) } returns StemmekretsBuilder.DEFAULT_STEMMEKRETS.apply {
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

        val stemmekretsSammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(lokalId = LOKALID, version = 1),
            stemmekretserTilSammenslaaing = listOf(),
            navn = "Navn etter endring",
            nummer = "22",
            informasjon = "Informasjon om sammenslaaingsendring",
        )

        val utkast = UtkastBuilder.builder("test", gyldigFra = datoSomVilGiFremtidEndringKonflikt)
            .withOperasjoner(Operasjoner(stemmekretsSammenslaaingsendring = stemmekretsSammenslaaingsendringRequest))
            .build()

        val result = utkastValidatorService.validerFremtidigeEndringer(utkast, utkast.gyldigFra)
        assertThat(result).isInstanceOf(FeilVedOppdatering.OppdateringException::class.java)

        val oppdateringFeil = result as FeilVedOppdatering.OppdateringException
        assertThat(oppdateringFeil.oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.STEMMEKRETS)
    }

    @Test
    fun `skal gi feil om det finnes flere endringer på grenser med samme ID`() {
        val utkast = UtkastBuilder.builder("test", gyldigFra = datoSomVilGiFremtidEndringKonflikt)
            .withOperasjoner(
                Operasjoner(
                    grenseendringer = Grenseendringer(
                        endredeFeatures = listOf(
                            FeatureBuilder()
                                .withLokalid("123")
                                .withGrensetype(Grensetype.Grunnkretsgrense)
                                .build(),
                            FeatureBuilder()
                                .withLokalid("123")
                                .withGrensetype(Grensetype.Grunnkretsgrense)
                                .build()
                        )
                    )
                )
            )
            .build()

        val result = utkastValidatorService.validateUtkastKlartForPublisering(utkast, utkast.gyldigFra)
        assertThat(result).contains("Utkastet inneholder grenseendringer for samme grense(r) flere ganger. Grensene (representert ved lokalid) det gjelder er: [123].")
    }

    @Test
    fun `skal ikke gi feil om det er flere nye features i samme utkast`() {
        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(
                Operasjoner(
                    grenseendringer = Grenseendringer(
                        endredeFeatures = listOf(
                            FeatureBuilder()
                                .withLokalid(null)
                                .withGrensetype(Grensetype.Grunnkretsgrense)
                                .build(),
                            FeatureBuilder()
                                .withLokalid(null)
                                .withGrensetype(Grensetype.Grunnkretsgrense)
                                .build()
                        )
                    )
                )
            )
            .build()

        val result = utkastValidatorService.validateUtkastKlartForPublisering(utkast, utkast.gyldigFra)
        assertThat(result).isEmpty()
    }

    @Test
    fun `skal gi feil om det finnes nyere endringer paa stemmekrets`() {
        val utkast = UtkastBuilder.builder("test", gyldigFra = datoSomVilGiFremtidEndringKonflikt)
            .withStemmekretsRequests(mapOf(
                "lokalId" to StemmekretsRequest(
                    identifikasjon = Identifikasjon(lokalid = "lokalid"),
                    nummer = "0123",
                    navn = "navn",
                    version = 0,
                    valgdistriktsnummer = "01",
                    tellekretsnavn = "tellekrets",
                    tellekretsnummer = "01",
                    kommunenummer = Kommunenummer(
                        "1",
                        "1"
                    )
                )
            ))
            .build()

        val result = utkastValidatorService.validerFremtidigeEndringer(utkast, utkast.gyldigFra)
        assertThat(result).isInstanceOf(FeilVedOppdatering.OppdateringException::class.java)

        val oppdateringFeil = result as FeilVedOppdatering.OppdateringException
        assertThat(oppdateringFeil.oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.STEMMEKRETS)
    }

    @Test
    fun `skal gi feil om det finnes nyere endringer paa grense`() {
        val utkast = UtkastBuilder.builder("test", gyldigFra = datoSomVilGiFremtidEndringKonflikt)
            .withOperasjoner(
                Operasjoner(
                    grenseendringer = Grenseendringer(
                        endredeFeatures = listOf(
                            FeatureBuilder()
                                .withGrensetype(Grensetype.Grunnkretsgrense)
                                .build()
                        )
                    )
                )
            )
            .build()

        val result = utkastValidatorService.validerFremtidigeEndringer(utkast, utkast.gyldigFra)
        assertThat(result).isInstanceOf(FeilVedOppdatering.OppdateringException::class.java)

        val oppdateringFeil = result as FeilVedOppdatering.OppdateringException
        assertThat(oppdateringFeil.oppdateringInfo.type).isEqualTo(FeilVedOppdatering.Type.GRENSE)
    }
}
