package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkStatic
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.extensions.geometry.grenseToFeature
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

}
