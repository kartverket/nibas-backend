package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockkStatic
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.REPOSITORY_EXTENSIONS_FILEPATH
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate
import java.time.Month
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.mockito.Mockito.`when` as whenever

@SpringBootTest(classes = [KommuneService::class])
class KommuneServiceTest {

    @Autowired
    lateinit var kommuneService: KommuneService

    @MockkBean
    private lateinit var kommuneRepositoryKotlinMock: KommuneRepository

    @MockBean
    private lateinit var kommuneRepositoryMock: KommuneRepository

    @MockBean
    private lateinit var fylkeRepository: FylkeRepository

    @MockBean
    private lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
    }

    @Test
    fun hentGrenserForKommuneReturnsNullWhenKommuneNotFound() {
        whenever(kommuneRepositoryMock.findById(anyString())).thenReturn(Optional.empty())
        assertThat(kommuneService.hentGrenserForKommune(Identifikator(Lokalid("1")))).isEmpty()
    }

    @Test
    fun hentGrenserForKommuneReturnsTheCorrectBorders() {
        val kommuneIdentifikator = Identifikator(lokalid = Lokalid("1"))

        val kommune = KommuneBuilder()
            .withGrunnkrets(GrunnkretsBuilder()
                .withRandomGrenser(
                    Pair(Grensetype.Kommunegrense, 5),
                    Pair(Grensetype.Fylkesgrense, 4),
                    Pair(Grensetype.Riksgrense, 3),
                    Pair(Grensetype.Territorialgrense, 2),
                    Pair(Grensetype.AvtaltAvgrensningslinje, 1)
                )
                .build())
            .build()
        every { kommuneRepositoryKotlinMock.findGyldigObjektForIdentifikator(kommuneIdentifikator) } returns kommune

        val grenserForKommune = kommuneService.hentGrenserForKommune(kommuneIdentifikator)
        assertEquals(15, grenserForKommune.size, "Expected 15 borders")

        assertGrensekoblinger(grenserForKommune, Grensetype.AvtaltAvgrensningslinje, 1, "Expected 1 AvtaltAvgrensningslinje")
        assertGrensekoblinger(grenserForKommune, Grensetype.Territorialgrense, 2, "Expected 2 Territorialgrense")
        assertGrensekoblinger(grenserForKommune, Grensetype.Riksgrense, 3, "Expected 3 Riksgrense")
        assertGrensekoblinger(grenserForKommune, Grensetype.Fylkesgrense, 4, "Expected 4 Fylkesgrense")
        assertGrensekoblinger(grenserForKommune, Grensetype.Kommunegrense, 5, "5 Kommunegrense")
    }

    @Test
    fun thatOppdaterKommuneReturnsNullWhenKommuneNotFound() {
        whenever(kommuneRepositoryMock.findById(anyString())).thenReturn(Optional.empty())
        assertNull(
            kommuneService.oppdaterKommune(Lokalid("1"), KommuneRequestBuilder.DEFAULT_REQUEST),
            "Expected to not find the kommune"
        )
    }

    @Test
    fun thatOppdaterKommuneSavesAnUpdatedVersion() {
        val lokalIdValue = "1000"
        val navn = "Bergen"

        val eksisterende = KommuneBuilder.DEFAULT_KOMMUNE
        val oppdatertKommune =
            KommuneRequestBuilder().withLokalId(lokalIdValue).withNavn(navn).build()

        val lokalid = Lokalid(lokalIdValue)
        val kommuneIdentifikator = Identifikator(lokalid = lokalid)
        every { kommuneRepositoryKotlinMock.findGyldigObjektForIdentifikator(kommuneIdentifikator) } returns eksisterende

        kommuneService.oppdaterKommune(lokalid, oppdatertKommune)

        val forventetNavnToSave = eksisterende.administrativenhetnavn.first()
        forventetNavnToSave.navn = navn

        verify(administrativenhetnavnRepository).save(forventetNavnToSave)
    }

    /**
     * Kommune med 3 grunnkretser.
     * 1 Storvatn
     * 1 nåværende og 1 framtidig versjon av Moi.
     *
     * Later som dagens dato er 23.11.22 og da skal kommunes grunnkretser kun returnere
     * Storvatn og nåværende Moi.
     */
    @Test
    fun hentKommunesGrunnkretserReturnererBareGyldigeNaavaerendeVersjoner() {
        val august1 = LocalDate.of(2022, Month.AUGUST, 1)
        val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)
        val todayIsNovember22In2022 = LocalDate.of(2022, Month.NOVEMBER, 22)

        val kommuneId = "12"
        val lokalIdGrunnkretsStorvatn = Lokalid("98")
        val lokalIdGrunnkretsMoi = Lokalid("99")

        val grunnkretsStorvatn = GrunnkretsBuilder(
            id = "1",
            lokalId = lokalIdGrunnkretsStorvatn,
            gyldigFra = august1
        ).build()
        val grunnkretsMoi = GrunnkretsBuilder(
            id = "2",
            lokalId = lokalIdGrunnkretsMoi,
            gyldigFra = august1,
            gyldigTil = jan1In2024
        ).build()
        val grunnkretsMoiFremtidigVersjon = GrunnkretsBuilder(
            id = "3",
            lokalId = lokalIdGrunnkretsMoi,
            gyldigFra = jan1In2024
        ).build()

        val kommune = KommuneBuilder(
            id = kommuneId,
            lokalId = Lokalid("1000"),
            navn = "Bergen",
            grunnkretser = mutableListOf(grunnkretsStorvatn, grunnkretsMoi, grunnkretsMoiFremtidigVersjon)
        ).build()

        val kommuneIdentifikator = Identifikator(
            lokalid = Lokalid(kommuneId),
            gyldighetsdato = todayIsNovember22In2022)
        every { kommuneRepositoryKotlinMock.findGyldigObjektForIdentifikator(kommuneIdentifikator) } returns kommune

        val kommunesGrunnkretser = kommuneService.hentKommunesGrunnkretser(kommuneIdentifikator)

        val expectedListOfGrunnkretsIds = listOf("1", "2")
        assertThat(kommunesGrunnkretser.map { it.id }).isEqualTo(expectedListOfGrunnkretsIds)
    }
}
