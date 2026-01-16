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
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.FylkeBuilder
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
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.events.NibasEvent
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.kodelister.KodelisteItem
import no.kartverket.nibas.kodelister.KodelisteRespons
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.KodelisteType
import no.kartverket.nibas.repository.BopliktomraadeRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.service.BopliktomraadeService
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
    private val grunnkretsRepositoryMock = mockk<GrunnkretsRepository>()

    private val stemmekretsServiceMock = mockk<StemmekretsService>()
    private val stemmekretsRepositoryMock = mockk<StemmekretsRepository>()

    private val bopliktomraadeServiceMock = mockk<BopliktomraadeService>()

    private val kodelisteServiceMock = mockk<KodelisteService>()

    private val currentTime = mockk<CurrentTime>()

    private val grenseServiceMock = mockk<GrenseService>()

    private val nibasEventPublisherMock = mockk<NibasEventPublisher>()

    private lateinit var utkastService: UtkastService

    @BeforeEach
    fun beforeEach() {
        every { currentTime.asLocalDate() } returns (LocalDate.of(2022, 6, 1))
        every { grenseServiceMock.validerLaasbareForGrenseendringerFinnesOgLagListeMedLaasbarOgOensketVersjon(any(), any()) } returns emptyList()
        every { kodelisteServiceMock.getMaalemetodeKoder() } returns KodelisteRespons(
            type = KodelisteType.MAALEMETODE_KODE,
            items = listOf(
                KodelisteItem(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, kode = "93", label = "Terrengmålt")
            )
        )
        val utkastValidatorService = UtkastValidatorService(grunnkretsServiceMock, grenseServiceMock, stemmekretsServiceMock, bopliktomraadeServiceMock, kodelisteServiceMock, currentTime)
        utkastService = UtkastService(utkastRepositoryMock, nasjonServiceMock, fylkeServiceMock, kommuneServiceMock, grunnkretsServiceMock,
            stemmekretsServiceMock, grenseServiceMock, nibasEventPublisherMock, utkastValidatorService, currentTime,
            stemmekretsRepositoryMock, grunnkretsRepositoryMock)
    }

    @Test
    fun `opprett utkast med tomt navn gir bad request`() {
        assertThatThrownBy { utkastService.opprettUtkast(OpprettUtkastRequestBuilder().withNavn("     ").build()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkastet mangler navn")
    }

    @Test
    fun `opprett utkast med tom endringstype gir bad request`() {
        assertThatThrownBy { utkastService.opprettUtkast(OpprettUtkastRequestBuilder().withEndringstype("").build()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkastet mangler endringstype")
    }

    @Test
    fun `opprett utkast med tomt navn og tom endringstype gir bad request`() {
        assertThatThrownBy { utkastService.opprettUtkast(OpprettUtkastRequestBuilder().withNavn("").withEndringstype("").build()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkastet mangler navn. Utkastet mangler endringstype.")
    }

    @Test
    fun `oppdater-valider gyldig utkast`() {
        val feb3 = LocalDate.of(2025, Month.FEBRUARY, 3)
        val utkastBuilder = UtkastBuilder.builder("Utkast 3", gyldigFra = feb3)
            .withFylkeRequests(mapOf("Fylke-UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST))
            .withKommuneRequests(mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST))
        val utkast = utkastBuilder.build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)
        every { fylkeServiceMock.hentFylke(any()) } returns FylkeBuilder.DEFAULT_FYLKE
        every { kommuneServiceMock.hentKommune(any()) } returns KommuneBuilder.DEFAULT_KOMMUNE

        val utkastEtterSave = utkastBuilder.build()

        every { utkastRepositoryMock.save(any()) } returns utkastEtterSave

        val oppdatertUtkast = utkastService.oppdaterUtkast(utkast.id, UtkastRequestBuilder()
            .withGyldigFra(feb3)
            .build())
        assertThat(oppdatertUtkast).isNotNull

        val validerUtkast = utkastService.validerUtkast(utkast.id, UtkastRequestBuilder()
            .withGyldigFra(feb3)
            .build())
        assertThat(validerUtkast).isTrue
    }

    @Test
    fun `forkast utkast med status Publisert gir BadRequest`() {
        val utkastBuilder = UtkastBuilder.builder("Utkast 3", gyldigFra = LocalDate.now())
            .withKommuneRequests(mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST))
            .withStatus(UtkastStatus.PUBLISERT)
        val utkast = utkastBuilder.build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)
        every { kommuneServiceMock.hentKommune(any()) } returns KommuneBuilder.DEFAULT_KOMMUNE

        assertThatThrownBy { utkastService.forkastUtkast(utkast.id) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kan ikke forkaste et utkast som er publisert")
    }

    @Test
    fun `oppdater-valider utkast med status Publisert gir BadRequest`() {
        val utkastBuilder = UtkastBuilder.builder("Utkast 3", gyldigFra = LocalDate.now())
            .withKommuneRequests(mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST))
            .withStatus(UtkastStatus.PUBLISERT)
        val utkast = utkastBuilder.build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)
        every { kommuneServiceMock.hentKommune(any()) } returns KommuneBuilder.DEFAULT_KOMMUNE

        assertThatThrownBy { utkastService.oppdaterUtkast(utkast.id, UtkastRequestBuilder().build()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kan ikke oppdatere et utkast med status PUBLISERT")

        assertThatThrownBy { utkastService.validerUtkast(utkast.id, UtkastRequestBuilder().build()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kan ikke oppdatere et utkast med status PUBLISERT")
    }

    @Test
    fun `oppdater-valider utkast med status Forkastet gir BadRequest`() {
        val utkastBuilder = UtkastBuilder.builder("Utkast 3", gyldigFra = LocalDate.now())
            .withKommuneRequests(mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST))
            .withStatus(UtkastStatus.FORKASTET)
        val utkast = utkastBuilder.build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)
        every { kommuneServiceMock.hentKommune(any()) } returns KommuneBuilder.DEFAULT_KOMMUNE

        assertThatThrownBy { utkastService.oppdaterUtkast(utkast.id, UtkastRequestBuilder().build()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kan ikke oppdatere et utkast med status FORKASTET")

        assertThatThrownBy { utkastService.validerUtkast(utkast.id, UtkastRequestBuilder().build()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kan ikke oppdatere et utkast med status FORKASTET")
    }

    @Test
    fun `oppdater-valider-publiser utkast med tomt navn gir bad request`() {
        val utkastBuilder = UtkastBuilder.builder("lovlig utkast", gyldigFra = LocalDate.now())
            .withKommuneRequests(mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST))
        val utkast = utkastBuilder.build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)
        every { kommuneServiceMock.hentKommune(any()) } returns KommuneBuilder.DEFAULT_KOMMUNE

        assertThatThrownBy { utkastService.oppdaterUtkast(utkast.id, UtkastRequestBuilder().withNavn("").build()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkastet mangler navn")

        assertThatThrownBy { utkastService.validerUtkast(utkast.id, UtkastRequestBuilder().withNavn("").build()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkastet mangler navn")

        assertThatThrownBy { utkastService.publiserUtkast(utkast.id) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkastet mangler navn")
    }

    @Test
    fun `oppdater-valider utkast med blankt stemmekretsnavn gir bad request`() {
        val utkast = UtkastRequestBuilder()
            .withOperasjoner(
                Operasjoner(
                    metadataendringer = Metadataendringer(
                        stemmekretsendringer = mapOf("1" to StemmekretsRequestBuilder(stemmekretsnummer = "aa", stemmekretsnavn = "").build())
                    )
                )
            )
            .build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(Utkast(navn = "Gyldig", gyldigFra = LocalDate.now()))
        every { stemmekretsServiceMock.hentStemmekrets(any()) } returns StemmekretsBuilder.DEFAULT_STEMMEKRETS
        every { stemmekretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()

        assertThatThrownBy { utkastService.oppdaterUtkast("123", utkast) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kretsnavn kan ikke være tomt.")

        assertThatThrownBy { utkastService.validerUtkast("123", utkast) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kretsnavn kan ikke være tomt.")
    }

    @Test
    fun `oppdater-valider utkast med ikke-editerbare grenser gir bad request`() {
        val utkast = UtkastRequestBuilder()
            .withOperasjoner(operasjonerMedIkkeEditerbareGrenser())
            .build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(Utkast(navn = "Gyldig", gyldigFra = LocalDate.now()))
        every { grenseServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()
        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()

        assertThatThrownBy { utkastService.oppdaterUtkast("123", utkast) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkastet inneholder 1 ikke-redigerbare grenser")

        assertThatThrownBy { utkastService.validerUtkast("123", utkast) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Utkastet inneholder 1 ikke-redigerbare grenser")
    }

    @Test
    fun `oppdater-valider utkast med ugyldig grunnkretsnummer gir bad request`() {
        val utkast = UtkastRequestBuilder()
            .withNavn("Utkast med for kort grunnkretsnummer")
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    grunnkretsendringer = mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "1234567").build())
                )
            ))
            .build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(Utkast(navn = "Gyldig", gyldigFra = LocalDate.now()))
        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()
        every { grunnkretsServiceMock.hentGrunnkrets(any()) } returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS

        assertThatThrownBy { utkastService.oppdaterUtkast("123", utkast) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Grunnkretsnummer (1234567) må være på åtte tegn.")

        assertThatThrownBy { utkastService.validerUtkast("123", utkast) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Grunnkretsnummer (1234567) må være på åtte tegn.")
    }

    @Test
    fun `oppdater-valider utkast med ikke-numerisk grunnkretsnummer gir bad request`() {
        val utkastRequest = UtkastRequestBuilder()
            .withNavn("Utkast med for ikke-numerisk grunnkretsnummer")
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    grunnkretsendringer = mapOf("1" to GrunnkretsRequestBuilder(grunnkretsnummer = "grunnkretsnummer").build())
                )
            ))
            .build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(Utkast(navn = "Gyldig", gyldigFra = LocalDate.now()))
        every { grunnkretsServiceMock.hentGrunnkrets(any()) } returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS
        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()

        assertThatThrownBy { utkastService.oppdaterUtkast("123", utkastRequest) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kretsnummer (grunnkretsnummer) kan bare inneholde sifre.")

        assertThatThrownBy { utkastService.validerUtkast("123", utkastRequest) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kretsnummer (grunnkretsnummer) kan bare inneholde sifre.")
    }

    @Test
    fun `publiser utkast med status Publisert gir BadRequest`() {
        val publisertUtkast = UtkastBuilder.builder("Utkast for publisering", gyldigFra = LocalDate.now())
            .withStatus(UtkastStatus.PUBLISERT)
            .build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(publisertUtkast)

        assertThatThrownBy { utkastService.publiserUtkast(publisertUtkast.id) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kan ikke publisere et utkast med status PUBLISERT")
    }

    @Test
    fun `publiser utkast med status Forkastet gir BadRequest`() {
        val publisertUtkast = UtkastBuilder.builder("Utkast for publisering", gyldigFra = LocalDate.now())
            .withStatus(UtkastStatus.FORKASTET)
            .build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(publisertUtkast)

        assertThatThrownBy { utkastService.publiserUtkast(publisertUtkast.id) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kan ikke publisere et utkast med status FORKASTET")
    }

    @Test
    fun publiserUtkastUtenOperasjonerGirBadRequestEx() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering", gyldigFra = LocalDate.now())
            .build()
        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkastUtenOperasjoner)

        assertThatThrownBy { utkastService.publiserUtkast(utkastUtenOperasjoner.id) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kan ikke publisere et utkast uten endringer")

        assertThatThrownBy { utkastService.validerUtkast(utkastUtenOperasjoner.id, UtkastRequestBuilder()
                .withOperasjoner(Operasjoner())
                .build())
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Kan ikke publisere et utkast uten endringer")
    }

    @Test
    fun publiserUtkast() {
        val utkast = UtkastBuilder.builder("Utkast for publisering", gyldigFra = LocalDate.now())
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
        every { nasjonServiceMock.hentNasjon(any()) } returns NasjonBuilder.DEFAULT_NASJON
        every { fylkeServiceMock.hentFylke(any()) } returns FylkeBuilder.DEFAULT_FYLKE
        every { kommuneServiceMock.hentKommune(any()) } returns KommuneBuilder.DEFAULT_KOMMUNE
        every { stemmekretsServiceMock.hentStemmekrets(any()) } returns StemmekretsBuilder.DEFAULT_STEMMEKRETS
        every { grunnkretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()
        every { stemmekretsServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()
        every { grenseServiceMock.hentAlleVersjonerMedGyldigFraEtterGittDato(any(), any()) } returns listOf()

        every { nasjonServiceMock.oppdaterNasjon(any(), any(), any(), any()) } returns Nasjon()
        every { fylkeServiceMock.oppdaterFylke(any(), any(), any(), any()) } returns Fylke()
        every { kommuneServiceMock.oppdaterKommune(any(), any(), any(), any()) } returns Kommune()
        every { grunnkretsServiceMock.oppdaterGrunnkrets(any(), any(), any(), any()) } returns Grunnkrets()
        every { stemmekretsServiceMock.oppdaterStemmekrets(any(), any(), any(), any()) } returns Stemmekrets()
        every { grenseServiceMock.lagreGrenser(any(), any(), any()) } returns emptySet()
        every { nibasEventPublisherMock.publishNibasEvent(any(), any()) } returns NibasEvent(LocalDate.of(2024, Month.JANUARY, 1), emptySet())
        every { grenseServiceMock.validerLaasbareForGrenseendringerFinnesOgLagListeMedLaasbarOgOensketVersjon(any(), any()) } returns emptyList()

        every { stemmekretsRepositoryMock.findById(any()) } returns Optional.of(StemmekretsBuilder.DEFAULT_STEMMEKRETS)
        every { grunnkretsRepositoryMock.findById(any()) } returns Optional.of(GrunnkretsBuilder.DEFAULT_GRUNNKRETS)
        every { grunnkretsServiceMock.finnesAnnenGrunnkretsMedSammeGrunnkretsnummer(any()) } returns false
        every { stemmekretsServiceMock.finnesAnnenStemmekretsMedSammeKommuneOgStemmekretsnummer(any()) } returns false

        val validerUtkast = utkastService.validerUtkast(utkast.id, UtkastRequestBuilder()
            .withNavn(utkast.navn)
            .withGyldigFra(utkast.gyldigFra)
            .withOperasjoner(utkast.operasjoner!!)
            .build()
        )
        assertThat(validerUtkast).isTrue

        val publiserUtkast = utkastService.publiserUtkast(utkast.id)
        assertThat(publiserUtkast).isNotNull

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
        val utkast = UtkastBuilder.builder("Utkast for forkasting", gyldigFra = LocalDate.now())
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
            UtkastBuilder.builder("Utkast for publisering", gyldigFra = LocalDate.of(2024, Month.JANUARY, 1))
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

        every { stemmekretsRepositoryMock.findById(any()) } returns Optional.of(StemmekretsBuilder.DEFAULT_STEMMEKRETS)
        every { grunnkretsRepositoryMock.findById(any()) } returns Optional.of(GrunnkretsBuilder.DEFAULT_GRUNNKRETS)
        every { grunnkretsServiceMock.finnesAnnenGrunnkretsMedSammeGrunnkretsnummer(any()) } returns false
        every { stemmekretsServiceMock.finnesAnnenStemmekretsMedSammeKommuneOgStemmekretsnummer(any()) } returns false

        val redigerteFlater = utkastService.publiserEndringerOgValiderUnikeKretsnumre(utkast.operasjoner!!, utkast, LocalDate.of(2024, Month.JANUARY, 1))
        assertThat(redigerteFlater).hasSize(10)
    }

    private val sammenslaaingsendringRequest =
        StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon("Stemmekrets-UUID-1", 1),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon("Stemmekrets-UUID-2", 1)),
            navn = "Sammenslått krets",
            nummer = "nytt nummer",
            informasjon = "Informasjon om sammenslaaingsendring",
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
                    FeatureBuilder("8").withGrensetype(Grensetype.Stemmekretsgrense).build(),
                    FeatureBuilder("9").withGrensetype(Grensetype.Grunnlinje).build()
                )
            )
        )

        fun operasjonerMedKunEditerbareGrenser() = Operasjoner(
            grenseendringer = Grenseendringer(
                endredeFeatures = listOf(
                    FeatureBuilder("1").withGrensetype(Grensetype.Grunnkretsgrense).build(),
                    FeatureBuilder("2").withGrensetype(Grensetype.Delomraadegrense).build(),
                    FeatureBuilder("3").withGrensetype(Grensetype.Stemmekretsgrense).build(),
                    FeatureBuilder("4").withGrensetype(Grensetype.Kommunegrense).build(),
                    FeatureBuilder("5").withGrensetype(Grensetype.Fylkesgrense).build(),
                    FeatureBuilder("6").withGrensetype(Grensetype.AvtaltAvgrensningslinje).build(),
                    FeatureBuilder("7").withGrensetype(Grensetype.Riksgrense).build(),
                    FeatureBuilder("8").withGrensetype(Grensetype.Territorialgrense).build()
                )
            )
        )
    }

}
