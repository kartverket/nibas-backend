package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
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
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.service.FlateService
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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.time.Month
import java.util.*
import org.mockito.Mockito.`when` as whenever


internal class UtkastServiceTest {

    @Mock
    private lateinit var utkastRepositoryMock: UtkastRepository

    @Mock
    private lateinit var nasjonServiceMock: NasjonService

    @Mock
    private lateinit var fylkeServiceMock: FylkeService

    @Mock
    private lateinit var kommuneServiceMock: KommuneService

    @Mock
    private lateinit var grunnkretsServiceMock: GrunnkretsService

    @Mock
    private lateinit var stemmekretsServiceMock: StemmekretsService

    @Mock
    private lateinit var kodelisteServiceMock: KodelisteService

    @Mock
    private lateinit var currentTime: CurrentTime

    @Mock
    private lateinit var grenseServiceMock: GrenseService

    @Mock
    private lateinit var nibasEventPublisherMock: NibasEventPublisher

    @Mock
    private lateinit var utkastService: UtkastService

    @Mock
    private lateinit var flateService: FlateService

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)

        whenever(currentTime.asLocalDate()).thenReturn(LocalDate.of(2022, 6, 1))
        val utkastValidatorService = UtkastValidatorService(grunnkretsServiceMock, grenseServiceMock, stemmekretsServiceMock, kodelisteServiceMock, currentTime)
        utkastService = UtkastService(utkastRepositoryMock, nasjonServiceMock, fylkeServiceMock, kommuneServiceMock, grunnkretsServiceMock,
            stemmekretsServiceMock, grenseServiceMock, nibasEventPublisherMock, utkastValidatorService, flateService)
    }

    @Test
    fun opprettUtkast() {
        val navn = "Grenseendring i Oslo"
        val utkast = UtkastBuilder.builder(navn)
            .withFylkeRequests(mapOf("UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST))
            .build()
        whenever(utkastRepositoryMock.save(any(Utkast::class.java))).thenReturn(utkast)

        val idForOpprettetUtkast = utkastService.opprettUtkast(
            OpprettUtkastRequestBuilder()
                .withNavn(navn)
                .build())
        assertThat(utkast.id).isEqualTo(idForOpprettetUtkast)
    }

    @Test
    fun opprettUtkastMedIkkeEditerbareGrenserGirBadRequest() {
        val opprettUtkastRequest = OpprettUtkastRequestBuilder()
            .withNavn("Utkast med ikke-editerbare grenser")
            .withOperasjoner(operasjonerMedIkkeEditerbareGrenser())
            .build()

        assertThatThrownBy {utkastService.opprettUtkast(opprettUtkastRequest)}
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkast inneholder 5 ikke-redigerbare grenser")
    }

    @Test
    fun opprettUtkastMedBlanktStemmekretsnavnGirBadRequest() {
        val opprettUtkastRequest = OpprettUtkastRequestBuilder()
            .withNavn("Utkast med blankt stemmekretsnavn")
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    stemmekretsendringer = mapOf("1" to StemmekretsRequestBuilder(stemmekretsnavn = "").build())
                )
            ))
            .build()

        assertThatThrownBy {utkastService.opprettUtkast(opprettUtkastRequest)}
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("ugyldige metadataendringer")
    }

    @Test
    fun oppdaterUtkast() {
        val utkastBuilder = UtkastBuilder.builder("Utkast 3")
            .withFylkeRequests(mapOf("Fylke-UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST))
            .withGyldigFra(LocalDate.of(2024, Month.JANUARY, 1))
            .withKommuneRequests(mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST))
        val utkast = utkastBuilder.build()

        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(utkast))

        val feb3 = LocalDate.of(2025, Month.FEBRUARY, 3)
        val utkastEtterSave = utkastBuilder.withGyldigFra(feb3)
            .build()

        whenever(utkastRepositoryMock.save(any(Utkast::class.java))).thenReturn(utkastEtterSave)

        val oppdatertUtkast = utkastService.oppdaterUtkast(utkast.id, UtkastRequestBuilder()
            .withGyldigFra(feb3)
            .build())
        assertThat(oppdatertUtkast).isNotNull
        assertThat(oppdatertUtkast!!.gyldigFra).isEqualTo(feb3)
    }

    @Test
    fun oppdaterUtkastMedIkkeEditerbareGrenserGirBadRequest() {
        val utkast = UtkastRequestBuilder()
            .withOperasjoner(operasjonerMedIkkeEditerbareGrenser())
            .build()
        assertThatThrownBy {utkastService.oppdaterUtkast("123", utkast)}
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkast inneholder 5 ikke-redigerbare grenser")
    }

    @Test
    fun oppdaterUtkastMedUgyldigGrunnkretsnummerGirBadRequest() {
        val opprettUtkastRequest = OpprettUtkastRequestBuilder()
            .withNavn("Utkast med for kort grunnkretsnummer")
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    grunnkretsendringer = mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "1234567").build())
                )
            ))
            .build()

        assertThatThrownBy {utkastService.opprettUtkast(opprettUtkastRequest)}
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("ugyldige metadataendringer")
    }

    @Test
    fun oppdaterUtkastMedIkkeNumeriskGrunnkretsnummerGirBadRequest() {
        val opprettUtkastRequest = OpprettUtkastRequestBuilder()
            .withNavn("Utkast med for ikke-numerisk grunnkretsnummer")
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    grunnkretsendringer = mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "grunnkretsnummer").build())
                )
            ))
            .build()

        assertThatThrownBy {utkastService.opprettUtkast(opprettUtkastRequest)}
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("ugyldige metadataendringer")
    }

    @Test
    fun publiserUtkastMedAnnenStatusEnnOpprettetGirBadRequestEx() {
        val publisertUtkast = UtkastBuilder.builder("Utkast for publisering")
            .withStatus(UtkastStatus.PUBLISERT)
            .build()
        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(publisertUtkast))

        assertThatThrownBy {
            utkastService.publiserUtkast(publisertUtkast.id)
        }.isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun publiserUtkastUtenOperasjonerGirBadRequestEx() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering")
            .build()
        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(utkastUtenOperasjoner))

        assertThatThrownBy {
            utkastService.publiserUtkast(utkastUtenOperasjoner.id)
        }.isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun publiserUtkastMedDatoIFortidenGirBadRequestEx() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering")
            .withGyldigFra(LocalDate.of(2021, 1, 1))
            .build()
        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(utkastUtenOperasjoner))

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
                    endredeFeatures = mapOf("56" to FeatureBuilder("56").withGrensetype(Grensetype.Grunnkretsgrense).build())
                )
            ))
            .build()
        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(utkast))
        whenever(grunnkretsServiceMock.hentGrunnkrets(any(Identifikator::class.java))).thenReturn(GrunnkretsBuilder.DEFAULT_GRUNNKRETS)

        utkastService.publiserUtkast(utkast.id)

        assertThat(utkast.status).isEqualTo(UtkastStatus.PUBLISERT)
        assertThat(utkast.operasjoner).isNull()

        verify(nasjonServiceMock).oppdaterNasjon(any(Lokalid::class.java), any(LocalDate::class.java), any(Endringstype::class.java), any(NasjonRequest::class.java))
        verify(fylkeServiceMock).oppdaterFylke(any(Lokalid::class.java), any(LocalDate::class.java), any(Endringstype::class.java), any(FylkeRequest::class.java))
        verify(kommuneServiceMock).oppdaterKommune(any(Lokalid::class.java), any(LocalDate::class.java), any(Endringstype::class.java), any(KommuneRequest::class.java))
        verify(grunnkretsServiceMock).oppdaterGrunnkrets(any(Lokalid::class.java), any(LocalDate::class.java), any(Endringstype::class.java), any(GrunnkretsRequest::class.java))
        verify(stemmekretsServiceMock).oppdaterStemmekrets(any(Lokalid::class.java), any(LocalDate::class.java), any(Endringstype::class.java), any(StemmekretsRequest::class.java))
        verify(grenseServiceMock).lagreGrenser(any(FeatureCollection::class.java), any(LocalDate::class.java), any(Endringstype::class.java))
        verify(nibasEventPublisherMock).publishNibasEvent(anyList(), any(LocalDate::class.java))
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
                    endredeFeatures = mapOf("56" to FeatureBuilder("56").build())
                )
            ))
            .build()
        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(utkast))

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
                            endredeFeatures = mapOf("56" to FeatureBuilder("56").build())
                        )
                    )
                )
                .build()

        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(utkast))
        whenever(
            nasjonServiceMock.oppdaterNasjon(
                any(Lokalid::class.java),
                any(LocalDate::class.java),
                any(Endringstype::class.java),
                any(NasjonRequest::class.java)
            )
        )
            .thenReturn(NasjonBuilder.DEFAULT_NASJON)
        whenever(
            grunnkretsServiceMock.oppdaterGrunnkrets(
                any(Lokalid::class.java),
                any(LocalDate::class.java),
                any(Endringstype::class.java),
                any(GrunnkretsRequest::class.java)
            )
        )
            .thenReturn(GrunnkretsBuilder.DEFAULT_GRUNNKRETS)
        whenever(
            stemmekretsServiceMock.oppdaterStemmekrets(
                any(Lokalid::class.java),
                any(LocalDate::class.java),
                any(Endringstype::class.java),
                any(StemmekretsRequest::class.java)
            )
        )
            .thenReturn(StemmekretsBuilder.DEFAULT_STEMMEKRETS)

        whenever(
            stemmekretsServiceMock.slaaSammenStemmekretser(
                any(StemmekretsSammenslaaingsendringRequest::class.java),
                any(LocalDate::class.java),
                any(Endringstype::class.java)
            )
        )
            .thenReturn(
                Pair(
                    StemmekretsBuilder().withLokalId("2").build(),
                    listOf(StemmekretsBuilder.DEFAULT_STEMMEKRETS)
                )
            )

        whenever(
            grenseServiceMock.lagreGrenser(
                any(FeatureCollection::class.java),
                any(LocalDate::class.java),
                any(Endringstype::class.java)
            )
        )
            .thenReturn(
                setOf(
                    GrunnkretsBuilder().withLokalId("2grunn").build(),
                    KommuneBuilder.DEFAULT_KOMMUNE
                )
            )

        val redigerteFlater = utkastService.publiserEndringer(utkast.operasjoner!!, utkast)
        assertThat(redigerteFlater).hasSize(8)
    }

    private fun <T> any(clazz: Class<T>): T = Mockito.any(clazz)

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
                endredeFeatures = mapOf(
                    "1" to FeatureBuilder("1").withGrensetype(Grensetype.Fylkesgrense).build(),
                    "2" to FeatureBuilder("2").withGrensetype(Grensetype.Kommunegrense).build(),
                    "3" to FeatureBuilder("3").withGrensetype(Grensetype.AvtaltAvgrensningslinje).build(),
                    "4" to FeatureBuilder("4").withGrensetype(Grensetype.Riksgrense).build(),
                    "5" to FeatureBuilder("5").withGrensetype(Grensetype.Territorialgrense).build(),
                    "6" to FeatureBuilder("6").withGrensetype(Grensetype.Grunnkretsgrense).build(),
                    "7" to FeatureBuilder("7").withGrensetype(Grensetype.Delomraadegrense).build(),
                    "8" to FeatureBuilder("8").withGrensetype(Grensetype.Stemmekretsgrense).build()
                )
            )
        )

        fun operasjonerMedKunEditerbareGrenser() = Operasjoner(
            grenseendringer = Grenseendringer(
                endredeFeatures = mapOf(
                    "1" to FeatureBuilder("1").withGrensetype(Grensetype.Grunnkretsgrense).build(),
                    "2" to FeatureBuilder("2").withGrensetype(Grensetype.Delomraadegrense).build(),
                    "3" to FeatureBuilder("3").withGrensetype(Grensetype.Stemmekretsgrense).build()
                )
            )
        )
    }

}
