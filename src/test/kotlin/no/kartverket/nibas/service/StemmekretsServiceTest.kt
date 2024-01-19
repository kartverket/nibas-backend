package no.kartverket.nibas.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.repository.REPOSITORY_EXTENSIONS_FILEPATH
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat

internal class StemmekretsServiceTest {
    private val stemmekretsRepositoryMock = mockk<StemmekretsRepository>()
    private val grenseKoblingService = GrenseKoblingService()

    private val stemmekretsService = StemmekretsService(
        stemmekretsRepositoryMock, grenseKoblingService
    )

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
    }

    @Test
    fun hentStemmekretserForIdsGirListeAvStemmekretser() {
        val gyldighetsdato = LocalDate.of(2023, Month.FEBRUARY, 14)
        val id1 = "1"
        val id2 = "2"

        val stemmekrets1 = StemmekretsBuilder(id = id1)
            .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
            .build()
        val stemmekrets2 = StemmekretsBuilder(id = id2)
            .withGyldigFra(LocalDate.of(2019, Month.JANUARY, 1))
            .build()

        val stemmekrets1Identifikator = Identifikator(Lokalid(id1), gyldighetsdato)
        val stemmekrets2Identifikator = Identifikator(Lokalid(id2), gyldighetsdato)

        every { stemmekretsRepositoryMock.findGyldigObjektForIdentifikator(stemmekrets1Identifikator) } returns stemmekrets1
        every { stemmekretsRepositoryMock.findGyldigObjektForIdentifikator(stemmekrets2Identifikator) } returns stemmekrets2

        val stemmekretser = stemmekretsService.hentStemmekretserForIds(setOf("2", "1"), gyldighetsdato)
        assertEquals(listOf(stemmekrets1, stemmekrets2), stemmekretser)
    }

    @Test
    fun `generer varianter av stemmekretsnummer`() {
        assertThat(lagMuligeStemmekretsnumre("1")).containsExactlyInAnyOrder("1", "01", "001", "0001")
        assertThat(lagMuligeStemmekretsnumre("01")).containsExactlyInAnyOrder("1", "01", "001", "0001")
        assertThat(lagMuligeStemmekretsnumre("001")).containsExactlyInAnyOrder("1", "01", "001", "0001")
        assertThat(lagMuligeStemmekretsnumre("0001")).containsExactlyInAnyOrder("1", "01", "001", "0001")

        assertThat(lagMuligeStemmekretsnumre("1001")).containsExactlyInAnyOrder("1001")
        assertThat(lagMuligeStemmekretsnumre("")).isEmpty()
        assertThat(lagMuligeStemmekretsnumre("x")).isEmpty()
    }
}
