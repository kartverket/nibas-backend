package no.kartverket.nibas.inndelinger

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockkStatic
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.REPOSITORY_EXTENSIONS_FILEPATH
import no.kartverket.nibas.repository.findAllByLokalidFramITid
import no.kartverket.nibas.repository.findGyldigGrunnkretsForTidspunkt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.Month


@SpringBootTest(classes = [GrunnkretsService::class])
class GrunnkretsServiceTest {

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val january1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val january1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
    @MockkBean
    private lateinit var grunnkretsRepositoryMock: GrunnkretsRepository
    @Autowired
    private lateinit var grunnkretsService: GrunnkretsService

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
    }

    @Test
    fun hentFramtidigeVersjonerForGrunnkrets() {

        val lokalId = Lokalid("123")
        val grunnkrets = GrunnkretsBuilder(lokalId = lokalId, gyldigFra = october1In2022, gyldigTil = january1In2023).build()
        val grunnkrets2 = GrunnkretsBuilder(lokalId = lokalId, gyldigFra = january1In2023, gyldigTil = january1In2024).build()
        val grunnkrets3 = GrunnkretsBuilder(lokalId = lokalId, gyldigFra = january1In2024).build()

        every { grunnkretsRepositoryMock.findGyldigGrunnkretsForTidspunkt(lokalId, october1In2022) } returns grunnkrets
        every { grunnkretsRepositoryMock.findAllByLokalidFramITid(lokalId, october1In2022) } returns listOf(grunnkrets2, grunnkrets3)

        val naavarendeOgFramtidigeVersjoner = grunnkretsService.hentFramtidigeVersjonerForGrunnkrets(lokalId, october1In2022)
        val resultIds = naavarendeOgFramtidigeVersjoner.map { it.id }
        val expectedIds = listOf(grunnkrets.id, grunnkrets2.id, grunnkrets3.id)
        assertThat(resultIds).isEqualTo(expectedIds)
    }
}
