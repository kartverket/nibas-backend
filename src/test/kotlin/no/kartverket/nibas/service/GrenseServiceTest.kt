package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkStatic
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.extensions.geometry.grenseToFeature
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.Month

@SpringBootTest
class GrenseServiceTest {

    @Autowired
    lateinit var grenseService: GrenseService

    @MockkBean
    private lateinit var stemmekretsServiceMock: StemmekretsService

    @MockkBean
    private lateinit var grunnkretsServiceMock: GrunnkretsService

    @MockkBean
    private lateinit var flateServiceMock: FlateService

    @MockkBean
    private lateinit var grenseRepositoryKotlinMock: GrenseRepository

    @MockkBean
    private lateinit var stemmekretsRepositoryKotlinMock: StemmekretsRepository

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
    }

    @Test
    fun thatIncomingFeatureWithUnknownIdThrowsException() {
        val gyldighetsdato = LocalDate.of(2022, Month.DECEMBER, 8)

        // Har en grunnkretsgrense med lokalid 34
        val grense = GrenseBuilder(id = "3400",
            lokalId = Lokalid("34"))
            .withGrensetype(Grensetype.Grunnkretsgrense).build()
        val grenseIdentifikator = Identifikator(Lokalid("34"), gyldighetsdato)

        every { grenseRepositoryKotlinMock.findAllGyldigeObjekterForIdentifikatorer(allAny()) } returns mapOf(grenseIdentifikator to grense)

        // Innkommende feature på ukjent grenseId
        val featureWithUnknownId = FeatureBuilder("33").withKontekstEgenskaper(listOf())
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .build()

        assertThatThrownBy { grenseService.lagreGrenser(
            FeatureCollection(features = listOf(featureWithUnknownId)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.FASTSETTING
        ) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("En eller flere grenser du har endret på eksisterer ikke lengre. For å sørge for at du endrer på en gjeldende grense må du dessverre opprette et nytt utkast og gjennomføre endringene dine på nytt.")
    }

    @Test
    fun hentGrenseReturnsNullInCaseOfNoMatch() {
        val grenseIdentifikator = Identifikator(Lokalid("1"))
        every { grenseRepositoryKotlinMock.findGyldigObjektForIdentifikator(grenseIdentifikator) } returns null

        assertThat(grenseService.hentGrense(grenseIdentifikator)).isNull()
    }

    @Test
    fun `lagreGrenser skal feile om man kun sender inn 1 gyldig krets`() {

        val gyldighetsdato = LocalDate.now()
        val lokalid = "grenseVestLokalid"

        val grenserForKrets = GrenseHelper.getGrenserForVestligKrets(gyldighetsdato, Grensetype.Stemmekretsgrense)

        val gyldigStemmekrets = StemmekretsBuilder()
            .withGrenser(grenserForKrets)
            .build()

        val ugyldigStemmekrets = StemmekretsBuilder()
            .withGyldigFra(LocalDate.now().minusDays(10))
            .withGyldigTil(LocalDate.now().minusDays(5))
            .build()

        //Grense som skal oppdateres får sett med koblinger til begge kretser
        val grenseSomSkalOppdateres = grenserForKrets.find { it.lokalid == lokalid }

        grenseSomSkalOppdateres?.let { grense ->
            val koblinger = mutableSetOf<StemmekretsGrenseKobling>()
            koblinger.add(
                StemmekretsGrenseKobling(
                    gyldigStemmekrets,
                    grense,
                )
            )

            koblinger.add(
                StemmekretsGrenseKobling(
                    ugyldigStemmekrets,
                    grense,
                )
            )

            grense.stemmekretser = koblinger
        }

        val feature = grenseSomSkalOppdateres!!.grenseToFeature(gyldighetsdato)

        val nyGrense = Grense(id = "nyGrense", lokalid = lokalid, grensetype = Grensetype.Stemmekretsgrense)

        val grenseIdentifikator = Identifikator(Lokalid(lokalid), gyldighetsdato)
        every { stemmekretsServiceMock.hentStemmekrets(any()) } returns gyldigStemmekrets
        every { grenseRepositoryKotlinMock.findAllGyldigeObjekterForIdentifikatorer(allAny()) } returns mapOf(grenseIdentifikator to grenseSomSkalOppdateres)
        every { grenseRepositoryKotlinMock.findAllByLokalid(lokalid) } returns listOf(grenseSomSkalOppdateres, nyGrense)
        every { grenseRepositoryKotlinMock.saveAndFlush(allAny()) } returns nyGrense
        every { stemmekretsRepositoryKotlinMock.findGyldigObjektForIdentifikator(any())} returns gyldigStemmekrets
        every { stemmekretsServiceMock.createNyVersjonAvStemmekrets(any(), any(), any()) } returns gyldigStemmekrets
        justRun { grenseRepositoryKotlinMock.flush() }
        justRun { flateServiceMock.oppdaterFlaterFor(any()) }

        assertThatThrownBy {
            grenseService.lagreGrenser(FeatureCollection(features = listOf(feature)), endringstype = Endringstype.VEDTATT_GRENSEJUSTERING)
        }.hasMessageContaining("Grense med id grenseVestLokalid har feil antall tilhørigheter")

    }

    @Test
    fun `lagreGrenser skal ikke feile om man sender inn 2 nye grunnkretser med ulike grunnkretsnummer`() {
        val localdate2022 = LocalDate.of(2022, 1, 1)
        val grense = GrenseBuilder().withGyldigFra(localdate2022).build()
        val grenseFeature = grense.grenseToFeature()

        val grunnkrets1 = GrunnkretsBuilder().withGrunnkretsnummer("12345678").build()
        val grunnkrets2 = GrunnkretsBuilder().withGrunnkretsnummer("87654321").build()

        every { grenseRepositoryKotlinMock.findAllByLokalid(any()) } returns listOf(grense)
        every { grenseRepositoryKotlinMock.findAllGyldigeObjekterForIdentifikatorer(any()) } returns mapOf(Identifikator(Lokalid(grense.lokalid)) to grense)
        every { grunnkretsServiceMock.hentGrunnkretsForGrunnkretsNummer("12345678", any()) } returns grunnkrets1
        every { grunnkretsServiceMock.hentGrunnkretsForGrunnkretsNummer("87654321", any()) } returns grunnkrets2
        every { grenseRepositoryKotlinMock.saveAndFlush(allAny()) } returns grense
        justRun { flateServiceMock.oppdaterFlaterFor(any()) }

        val kontekstGrunnkrets1 = KontekstEgenskaper(
            kommuneId = ObjektIdentifikator(Lokalid("kommune"), localdate2022),
            type = KontekstEnhetType.GRUNNKRETS,
            kretsNummer = "12345678",
            version = 1
        )

        val kontekstGrunnkrets2 = KontekstEgenskaper(
            kommuneId = ObjektIdentifikator(Lokalid("kommune"), localdate2022),
            type = KontekstEnhetType.GRUNNKRETS,
            kretsNummer = "87654321",
            version = 1
        )

        grenseFeature.properties = grenseFeature.properties.copy(kontekstEgenskaper = listOf(kontekstGrunnkrets1, kontekstGrunnkrets2))

        assertDoesNotThrow {
            grenseService.lagreGrenser(FeatureCollection(features = listOf(grenseFeature)), endringstype = Endringstype.FASTSETTING)
        }
    }

    @Test
    fun `lagreGrenser skal feile om man sender inn 2 nye grunnkretser med sammme grunnkretsnummer`() {
        val localdate2022 = LocalDate.of(2022, 1, 1)
        val grense = GrenseBuilder().withGyldigFra(localdate2022).build()
        val grenseFeature = grense.grenseToFeature()

        val grunnkrets1 = GrunnkretsBuilder().withGrunnkretsnummer("12345678").build()

        every { grenseRepositoryKotlinMock.findAllByLokalid(any()) } returns listOf(grense)
        every { grenseRepositoryKotlinMock.findAllGyldigeObjekterForIdentifikatorer(any()) } returns mapOf(Identifikator(Lokalid(grense.lokalid)) to grense)
        every { grunnkretsServiceMock.hentGrunnkretsForGrunnkretsNummer("12345678", any()) } returns grunnkrets1
        every { grenseRepositoryKotlinMock.saveAndFlush(allAny()) } returns grense
        justRun { flateServiceMock.oppdaterFlaterFor(any()) }

        val kontekstGrunnkrets1 = KontekstEgenskaper(
            kommuneId = ObjektIdentifikator(Lokalid("kommune"), localdate2022),
            type = KontekstEnhetType.GRUNNKRETS,
            kretsNummer = "12345678",
            version = 1
        )

        val kontekstGrunnkrets2 = KontekstEgenskaper(
            kommuneId = ObjektIdentifikator(Lokalid("kommune"), localdate2022),
            type = KontekstEnhetType.GRUNNKRETS,
            kretsNummer = "12345678",
            version = 1
        )

        grenseFeature.properties = grenseFeature.properties.copy(kontekstEgenskaper = listOf(kontekstGrunnkrets1, kontekstGrunnkrets2))

        assertThatThrownBy {
            grenseService.lagreGrenser(FeatureCollection(features = listOf(grenseFeature)), endringstype = Endringstype.FASTSETTING)
        }.isInstanceOf(BadRequestException::class.java).hasMessageContaining("Grense med id 123 har begge tilhørighetene satt til samme krets.")
    }

    @Test
    fun `lagreGrenser skal ikke feile om man sender inn 2 nye stemmekretser med sammme stemmekretsnummer men i ulik kommune`() {
        val localdate2022 = LocalDate.of(2022, 1, 1)
        val grense = GrenseBuilder().withGyldigFra(localdate2022).build()
        val grenseFeature = grense.grenseToFeature()

        val kommune1 = KommuneBuilder(gyldigFra = localdate2022, unikLokalid = UnikLokalid(lokalid = "kommune1", Flatetype.KOMMUNE))
            .withKommunenummer("kommune1")
            .build()

        val kommune2 = KommuneBuilder(gyldigFra = localdate2022, unikLokalid = UnikLokalid(lokalid = "kommune2", Flatetype.KOMMUNE))
            .withKommunenummer("kommune2")
            .build()

        val stemmekrets1 = StemmekretsBuilder(stemmekretsnummer = "22", kommune = kommune1).build()
        val stemmekrets2 = StemmekretsBuilder(stemmekretsnummer = "22", kommune = kommune2).build()

        every { grenseRepositoryKotlinMock.findAllByLokalid(any()) } returns listOf(grense)
        every { grenseRepositoryKotlinMock.findAllGyldigeObjekterForIdentifikatorer(any()) } returns mapOf(Identifikator(Lokalid(grense.lokalid)) to grense)
        every { stemmekretsServiceMock.hentStemmekretsForStemmekretsNummerIKommune(ObjektIdentifikator(Lokalid("kommune1"), localdate2022), "22",  any()) } returns stemmekrets1
        every { stemmekretsServiceMock.hentStemmekretsForStemmekretsNummerIKommune(ObjektIdentifikator(Lokalid("kommune2"), localdate2022), "22",  any()) } returns stemmekrets2
        every { grenseRepositoryKotlinMock.saveAndFlush(allAny()) } returns grense
        justRun { flateServiceMock.oppdaterFlaterFor(any()) }

        val kontekstStemmekrets1 = KontekstEgenskaper(
            kommuneId = ObjektIdentifikator(Lokalid("kommune1"), localdate2022),
            type = KontekstEnhetType.STEMMEKRETS,
            kretsNummer = "22",
            version = 1
        )

        val kontekstStemmekrets2 = KontekstEgenskaper(
            kommuneId = ObjektIdentifikator(Lokalid("kommune2"), localdate2022),
            type = KontekstEnhetType.STEMMEKRETS,
            kretsNummer = "22",
            version = 1
        )

        grenseFeature.properties = grenseFeature.properties.copy(kontekstEgenskaper = listOf(kontekstStemmekrets1, kontekstStemmekrets2))

        assertDoesNotThrow {
            grenseService.lagreGrenser(FeatureCollection(features = listOf(grenseFeature)), endringstype = Endringstype.FASTSETTING)
        }
    }
}
