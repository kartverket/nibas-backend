package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockkStatic
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.FylkeRequestBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.REPOSITORY_EXTENSIONS_FILEPATH
import no.kartverket.nibas.repository.UnikLokalidRepository
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

@SpringBootTest(classes = [FylkeService::class])
class FylkeServiceTest {

    @Autowired
    lateinit var fylkeService: FylkeService

    @MockkBean
    private lateinit var fylkeRepositoryMock: FylkeRepository

    @MockkBean
    @Suppress("unused")
    private lateinit var unikLokalidRepository: UnikLokalidRepository

    @MockkBean
    @Suppress("unused")
    private lateinit var nasjonRepository: NasjonRepository

    @MockkBean
    @Suppress("unused")
    private lateinit var hrefProvider: HrefProvider

    @MockkBean
    @Suppress("unused")
    private lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @MockkBean
    @Suppress("unused")
    private lateinit var administrativenhetnavnService: AdministrativenhetnavnService

    @MockkBean
    @Suppress("unused")
    private lateinit var kodelisteServiceMock: KodelisteService

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
    }

    @Test
    fun hentGrenserForFylkeReturnsEmptyCollectionWhenFylkeNotFound() {
        val fylkeIdentifikator = Identifikator(lokalid = Lokalid("1"))
        every { fylkeRepositoryMock.findAllByLokalid(any()) } returns emptyList()

        assertThat(fylkeService.hentGrenserForFylke(fylkeIdentifikator)).isEmpty()
    }

    @Test
    fun hentGrenserForFylkeReturnsTheCorrectBorders() {
        val fylkeIdentifikator = Identifikator(lokalid = Lokalid("1"))
        val fylke = FylkeBuilder()
            .withKommune(KommuneBuilder()
                .withGrunnkrets(GrunnkretsBuilder()
                    .withRandomGrenser(
                        Pair(Grensetype.Fylkesgrense, 4),
                        Pair(Grensetype.Riksgrense, 3),
                        Pair(Grensetype.Territorialgrense, 2),
                        Pair(Grensetype.AvtaltAvgrensningslinje, 1))
                    .build())
                .build())
            .build()
        every { fylkeRepositoryMock.findGyldigObjektForIdentifikator(fylkeIdentifikator) } returns fylke

        val grenserForFylke = fylkeService.hentGrenserForFylke(fylkeIdentifikator)
        assertEquals(10, grenserForFylke.size, "Expected ten borders")

        assertGrensekoblinger(grenserForFylke, Grensetype.AvtaltAvgrensningslinje, 1, "Expected 1 AvtaltAvgrensningslinje")
        assertGrensekoblinger(grenserForFylke, Grensetype.Territorialgrense, 2, "Expected 2 Territorialgrense")
        assertGrensekoblinger(grenserForFylke, Grensetype.Riksgrense, 3, "Expected 3 Riksgrense")
        assertGrensekoblinger(grenserForFylke, Grensetype.Fylkesgrense, 4, "Expected 4 Fylkesgrense")
    }

    @Test
    fun thatOppdaterFylkeReturnsNullWhenFylkeNotFound() {
        every { fylkeRepositoryMock.findAllByLokalid(any()) } returns emptyList()

        Assertions.assertThatThrownBy {
            fylkeService.oppdaterFylke(Lokalid("1"), LocalDate.now(), Endringstype.FASTSETTING, FylkeRequestBuilder.DEFAULT_REQUEST)
        }
            .isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fylke med Identifikator")
            .hasMessageContaining("ble ikke funnet")
    }

}
