package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockkStatic
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.REPOSITORY_EXTENSIONS_FILEPATH
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.repository.findAllGyldigeObjekterForIdentifikatorer
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.service.utkast.UtkastService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.Month
import java.util.*

@SpringBootTest
class GrenseServiceTest {

    @Autowired
    lateinit var grenseService: GrenseService

    @Autowired
    lateinit var utkastService: UtkastService

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

    @MockkBean
    private lateinit var utkastRepositoryMock: UtkastRepository

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
        every { grenseRepositoryKotlinMock.findAllById(allAny()) } returns emptyList()
        every { grenseRepositoryKotlinMock.findAllByLokalid(allAny()) } returns emptyList()

        // Innkommende feature på ukjent grenseId
        val featureWithUnknownId = FeatureBuilder("33").withKontekstEgenskaper(listOf())
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .build()

        val grenseendringer = Grenseendringer(endredeFeatures = listOf(featureWithUnknownId))

        val utkast = UtkastBuilder.builder("test")
            .build()

        every { utkastRepositoryMock.findById(any()) } returns Optional.of(utkast)

        assertThatThrownBy { utkastService.oppdaterUtkast(utkast.id, UtkastRequestBuilder()
            .withOperasjoner(Operasjoner(grenseendringer = grenseendringer)).build())
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("En eller flere grenser du har endret på eksisterer ikke lengre. For å sørge for at du endrer på en gjeldende grense må du dessverre opprette et nytt utkast og gjennomføre endringene dine på nytt.")
    }

    @Test
    fun hentGrenseReturnsNullInCaseOfNoMatch() {
        val grenseIdentifikator = Identifikator(Lokalid("1"))
        every { grenseRepositoryKotlinMock.findGyldigObjektForIdentifikator(grenseIdentifikator) } returns null

        assertThat(grenseService.hentGrense(grenseIdentifikator)).isNull()
    }

}
