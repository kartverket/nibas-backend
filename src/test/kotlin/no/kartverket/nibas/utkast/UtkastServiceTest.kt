package no.kartverket.nibas.utkast

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.KretsNavnOgNummer
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.FylkeRequestBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.builders.NasjonRequestBuilder
import no.kartverket.nibas.builders.OpprettUtkastRequestBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.NasjonService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.utkast.UtkastService
import no.kartverket.nibas.service.utkast.UtkastValidatorService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.util.*

internal class UtkastServiceTest {


    private val utkastRepositoryMock = mockk<UtkastRepository>()

    private val nasjonServiceMock = mockk<NasjonService>()

    private val fylkeServiceMock = mockk<FylkeService>()

    private val kommuneServiceMock = mockk<KommuneService>()

    private val grunnkretsServiceMock = mockk<GrunnkretsService>()

    private val stemmekretsServiceMock = mockk<StemmekretsService>()

    private val kodelisteServiceMock = mockk<KodelisteService>()

    private val currentTime = mockk<CurrentTime>()

    private val grenseServiceMock = mockk<GrenseService>()

    private val nibasEventPublisherMock = mockk<NibasEventPublisher>()

    private lateinit var utkastService: UtkastService

    @BeforeEach
    fun beforeEach() {
        every { currentTime.asLocalDate() } returns (LocalDate.of(2022, 6, 1))
        val utkastValidatorService = UtkastValidatorService(grunnkretsServiceMock, grenseServiceMock, stemmekretsServiceMock, kodelisteServiceMock, currentTime)
        utkastService = UtkastService(utkastRepositoryMock, nasjonServiceMock, fylkeServiceMock, kommuneServiceMock, grunnkretsServiceMock,
            stemmekretsServiceMock, grenseServiceMock, nibasEventPublisherMock, utkastValidatorService)
    }

    @Test
    fun opprettUtkast() {
        val navn = "Grenseendring i Oslo"
        val utkast = UtkastBuilder.builder(navn)
            .build()
        every { utkastRepositoryMock.save(any()) } returns utkast

        val idForOpprettetUtkast = utkastService.opprettUtkast(
            OpprettUtkastRequestBuilder()
                .withNavn(navn)
                .build())
        assertThat(utkast.id).isEqualTo(idForOpprettetUtkast)
    }

    @Test
    fun `opprett utkast med tomt navn gir bad request`() {
        assertThatThrownBy {
            utkastService.opprettUtkast(
                OpprettUtkastRequestBuilder()
                    .withNavn("     ")
                    .build())
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Husk å fylle inn både navnet og endringstypen på utkastet.")
    }

    @Test
    fun `utkast med tom endringstype gir bad request`() {
        assertThatThrownBy {
            utkastService.opprettUtkast(
                OpprettUtkastRequestBuilder()
                    .withEndringstype("")
                    .build())
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Husk å fylle inn både navnet og endringstypen på utkastet.")
    }

    @Test
    fun oppdaterUtkast() {
        val utkastBuilder = UtkastBuilder.builder("Utkast 3")
            .withFylkeRequests(mapOf("Fylke-UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST))
            .withGyldigFra(LocalDate.of(2024, Month.JANUARY, 1))
            .withKommuneRequests(mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST))
        val utkast = utkastBuilder.build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)

        val feb3 = LocalDate.of(2025, Month.FEBRUARY, 3)
        val utkastEtterSave = utkastBuilder.withGyldigFra(feb3)
            .build()

        every { utkastRepositoryMock.save(any()) } returns utkastEtterSave

        val oppdatertUtkast = utkastService.oppdaterUtkast(utkast.id, UtkastRequestBuilder()
            .withGyldigFra(feb3)
            .build())
        assertThat(oppdatertUtkast).isNotNull
        assertThat(oppdatertUtkast!!.gyldigFra).isEqualTo(feb3)
    }

    @Test
    fun `oppdater utkast med tomt navn gir bad request`() {
        val utkastBuilder = UtkastBuilder.builder("lovlig utkast")
            .withFylkeRequests(mapOf("Fylke-UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST))
            .withGyldigFra(LocalDate.of(2024, Month.JANUARY, 1))
            .withKommuneRequests(mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST))
        val utkast = utkastBuilder.build()

        assertThatThrownBy {
            utkastService.oppdaterUtkast(utkast.id, UtkastRequestBuilder()
                .withNavn("")
                .build())
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Husk å fylle inn både navnet og endringstypen på utkastet.")
    }

    @Test
    fun oppdaterUtkastMedBlanktStemmekretsnavnGirBadRequest() {
        val utkast = UtkastRequestBuilder()
            .withOperasjoner(
                Operasjoner(
                    metadataendringer = Metadataendringer(
                        stemmekretsendringer = mapOf("1" to StemmekretsRequestBuilder(stemmekretsnavn = "").build())
                    )
                )
            )
            .build()

        assertThatThrownBy { utkastService.oppdaterUtkast("123", utkast) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("ugyldige metadataendringer på stemmekrets")
    }

    @Test
    fun oppdaterUtkastMedIkkeEditerbareGrenserGirBadRequest() {
        val utkast = UtkastRequestBuilder()
            .withOperasjoner(operasjonerMedIkkeEditerbareGrenser())
            .build()
        assertThatThrownBy { utkastService.oppdaterUtkast("123", utkast) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkast inneholder 4 ikke-redigerbare grenser")
    }

    @Test
    fun oppdaterUtkastMedUgyldigGrunnkretsnummerGirBadRequest() {
        val utkast = UtkastRequestBuilder()
            .withNavn("Utkast med for kort grunnkretsnummer")
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    grunnkretsendringer = mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "1234567").build())
                )
            ))
            .build()

        assertThatThrownBy { utkastService.oppdaterUtkast("123", utkast) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("ugyldige metadataendringer")
    }

    @Test
    fun oppdaterUtkastMedIkkeNumeriskGrunnkretsnummerGirBadRequest() {
        val utkastRequest = UtkastRequestBuilder()
            .withNavn("Utkast med for ikke-numerisk grunnkretsnummer")
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    grunnkretsendringer = mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "grunnkretsnummer").build())
                )
            ))
            .build()

        assertThatThrownBy { utkastService.oppdaterUtkast("123", utkastRequest) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("ugyldige metadataendringer")
    }

    @Test
    fun publiserUtkastMedAnnenStatusEnnOpprettetGirBadRequestEx() {
        val publisertUtkast = UtkastBuilder.builder("Utkast for publisering")
            .withStatus(UtkastStatus.PUBLISERT)
            .build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(publisertUtkast)

        assertThatThrownBy {
            utkastService.publiserUtkast(publisertUtkast.id)
        }.isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun publiserUtkastUtenOperasjonerGirBadRequestEx() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering")
            .build()
        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkastUtenOperasjoner)

        assertThatThrownBy {
            utkastService.publiserUtkast(utkastUtenOperasjoner.id)
        }.isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun publiserUtkastMedDatoIFortidenGirBadRequestEx() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering")
            .withGyldigFra(LocalDate.of(2021, 1, 1))
            .build()
        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkastUtenOperasjoner)

        assertThatThrownBy {
            utkastService.publiserUtkast(utkastUtenOperasjoner.id)
        }.isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun publiserUtkast() {
        val utkast = UtkastBuilder.builder("Utkast for publisering")
            .withGyldigFra(LocalDate.of(2024, Month.JANUARY, 1))
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    nasjonsendringer = mapOf("Nasjon-UUID-1" to NasjonRequestBuilder.DEFAULT_NASJON),
                    fylkesendringer = mapOf("Fylke-UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST),
                    kommuneendringer = mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST),
                    grunnkretsendringer = mapOf("Grunnkrets-UUID-1" to GrunnkretsRequestBuilder.DEFAULT_REQUEST),
                    stemmekretsendringer = mapOf("Stemmekrets-UUID-1" to StemmekretsRequestBuilder.DEFAULT_REQUEST)
                ),
                grenseendringer = Grenseendringer(
                    endredeFeatures = listOf(FeatureBuilder("56").withGrensetype(Grensetype.Grunnkretsgrense).build())
                )
            ))
            .build()
        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)
        every { grunnkretsServiceMock.hentGrunnkrets(any()) } returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS
        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()
        every { stemmekretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()
        every { grenseServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()

        every { nasjonServiceMock.oppdaterNasjon(any(), any(), any(), any()) } returns Nasjon()
        every { fylkeServiceMock.oppdaterFylke(any(), any(), any(), any()) } returns Fylke()
        every { kommuneServiceMock.oppdaterKommune(any(), any(), any(), any()) } returns Kommune()
        every { grunnkretsServiceMock.oppdaterGrunnkrets(any(), any(), any(), any()) } returns Grunnkrets()
        every { stemmekretsServiceMock.oppdaterStemmekrets(any(), any(), any(), any()) } returns Stemmekrets()
        every { grenseServiceMock.lagreGrenser(any(), any(), any()) } returns emptySet()
        every { nibasEventPublisherMock.publishNibasEvent(any(), any()) } returns Unit

        utkastService.publiserUtkast(utkast.id)

        assertThat(utkast.status).isEqualTo(UtkastStatus.PUBLISERT)
        assertThat(utkast.operasjoner).isNull()

        verify { nasjonServiceMock.oppdaterNasjon(any(), any(), any(), any()) }
        verify { fylkeServiceMock.oppdaterFylke(any(), any(), any(), any()) }
        verify { kommuneServiceMock.oppdaterKommune(any(), any(), any(), any()) }
        verify { grunnkretsServiceMock.oppdaterGrunnkrets(any(), any(), any(), any()) }
        verify { stemmekretsServiceMock.oppdaterStemmekrets(any(), any(), any(), any()) }
        verify { grenseServiceMock.lagreGrenser(any(), any(), any()) }
        verify { nibasEventPublisherMock.publishNibasEvent(any(), any()) }
    }

    @Test
    fun forkastUtkast() {
        val utkast = UtkastBuilder.builder("Utkast for forkasting")
            .withGyldigFra(LocalDate.of(2024, Month.JANUARY, 1))
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    nasjonsendringer = mapOf("Nasjon-UUID-1" to NasjonRequestBuilder.DEFAULT_NASJON),
                    fylkesendringer = mapOf("Fylke-UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST),
                    kommuneendringer = mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST),
                    grunnkretsendringer = mapOf("Grunnkrets-UUID-1" to GrunnkretsRequestBuilder.DEFAULT_REQUEST),
                    stemmekretsendringer = mapOf("Stemmekrets-UUID-1" to StemmekretsRequestBuilder.DEFAULT_REQUEST)
                ),
                grenseendringer = Grenseendringer(
                    endredeFeatures = listOf(FeatureBuilder("56").build())
                )
            ))
            .build()
        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)

        utkastService.forkastUtkast(utkast.id)

        assertThat(utkast.status).isEqualTo(UtkastStatus.FORKASTET)
        assertThat(utkast.operasjoner).isNull()
    }

    @Test
    fun `lager like mange EventRader som det er av endringer`() {

        val utkast =
            UtkastBuilder.builder("Utkast for publisering")
                .withGyldigFra(LocalDate.of(2024, Month.JANUARY, 1))
                .withOperasjoner(
                    Operasjoner(
                        metadataendringer =
                        Metadataendringer(
                            nasjonsendringer =
                            mapOf("Nasjon-UUID-1" to NasjonRequestBuilder.DEFAULT_NASJON),
                            grunnkretsendringer =
                            mapOf(
                                "Grunnkrets-UUID-1" to
                                    GrunnkretsRequestBuilder.DEFAULT_REQUEST,
                                "Grunnkrets-UUID-2" to
                                    GrunnkretsRequestBuilder.DEFAULT_REQUEST
                            ),
                            stemmekretsendringer =
                            mapOf(
                                "Stemmekrets-UUID-1" to
                                    StemmekretsRequestBuilder.DEFAULT_REQUEST
                            )
                        ),
                        stemmekretsSammenslaaingsendring = sammenslaaingsendringRequest,
                        grenseendringer = Grenseendringer(
                            endredeFeatures = listOf(FeatureBuilder("56").build())
                        ),
                        kretsDelingEndringer = listOf(
                            KretsDelingEndringRequest(
                                IdentifikatorMedVersjon("Grunnkrets-UUID-1", 1),
                                kommuneId = ObjektIdentifikator(Lokalid("1"), LocalDate.of(2023, Month.JANUARY, 1)),
                                flatetype = Flatetype.GRUNNKRETS,
                                nyeKretser = listOf(
                                    KretsNavnOgNummer(kretsNummer = "03015215", kretsNavn = "nyGrunnkrets2"),
                                    KretsNavnOgNummer(kretsNummer = "03015214", kretsNavn = "nyGrunnkrets"))
                            )
                        )
                    )
                )
                .build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)
        every {
            nasjonServiceMock.oppdaterNasjon(
                any(),
                any(),
                any(),
                any()
            )
        } returns NasjonBuilder.DEFAULT_NASJON
        every {
            grunnkretsServiceMock.oppdaterGrunnkrets(
                any(),
                any(),
                any(),
                any()
            )
        } returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS
        every {
            stemmekretsServiceMock.oppdaterStemmekrets(
                any(),
                any(),
                any(),
                any()
            )
        } returns StemmekretsBuilder.DEFAULT_STEMMEKRETS

        every {
            stemmekretsServiceMock.slaaSammenStemmekretser(
                any(),
                any(),
                any()
            )
        } returns
            Pair(
                StemmekretsBuilder().withLokalId("2").build(),
                listOf(StemmekretsBuilder.DEFAULT_STEMMEKRETS)
            )
        every {
            grenseServiceMock.lagreGrenser(
                any(),
                any(),
                any()
            )
        } returns
            setOf(
                GrunnkretsBuilder().withLokalId("2grunn").build(),
                KommuneBuilder.DEFAULT_KOMMUNE
            )

        every {
            grunnkretsServiceMock.delGrunnkrets(
                any(),
                any(),
                any())
        } returns Grunnkrets()

        val redigerteFlater = utkastService.publiserEndringer(utkast.operasjoner!!, utkast)
        assertThat(redigerteFlater).hasSize(10)
    }

    private val sammenslaaingsendringRequest =
        StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon("Stemmekrets-UUID-1", 1),
            stemmekretserTilSammenslaaing =
            listOf(IdentifikatorMedVersjon("Stemmekrets-UUID-2", 1)),
            stemmekretsNavn = "Sammenslått krets",
            stemmekretsNummer = "nytt nummer"
        )

    companion object {
        fun operasjonerMedIkkeEditerbareGrenser() = Operasjoner(
            grenseendringer = Grenseendringer(
                endredeFeatures = listOf(
                    FeatureBuilder("1").withGrensetype(Grensetype.Fylkesgrense).build(),
                    FeatureBuilder("2").withGrensetype(Grensetype.Kommunegrense).build(),
                    FeatureBuilder("3").withGrensetype(Grensetype.AvtaltAvgrensningslinje).build(),
                    FeatureBuilder("4").withGrensetype(Grensetype.Riksgrense).build(),
                    FeatureBuilder("5").withGrensetype(Grensetype.Territorialgrense).build(),
                    FeatureBuilder("6").withGrensetype(Grensetype.Grunnkretsgrense).build(),
                    FeatureBuilder("7").withGrensetype(Grensetype.Delomraadegrense).build(),
                    FeatureBuilder("8").withGrensetype(Grensetype.Stemmekretsgrense).build()
                )
            )
        )

        fun operasjonerMedKunEditerbareGrenser() = Operasjoner(
            grenseendringer = Grenseendringer(
                endredeFeatures = listOf(
                    FeatureBuilder("1").withGrensetype(Grensetype.Grunnkretsgrense).build(),
                    FeatureBuilder("2").withGrensetype(Grensetype.Delomraadegrense).build(),
                    FeatureBuilder("3").withGrensetype(Grensetype.Stemmekretsgrense).build(),
                    FeatureBuilder("4").withGrensetype(Grensetype.Kommunegrense).build()
                )
            )
        )
    }

}
