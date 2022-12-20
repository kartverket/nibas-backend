package no.kartverket.nibas.inndelinger

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockkStatic
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.FylkeRequestBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
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
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.mockito.Mockito.`when` as whenever

@SpringBootTest(classes = [FylkeService::class])
class FylkeServiceTest {

    @Autowired
    lateinit var fylkeService: FylkeService

    @MockBean
    private lateinit var fylkeRepository: FylkeRepository

    @MockkBean
    private lateinit var fylkeRepositoryKotlinMock: FylkeRepository

    @MockBean
    private lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
    }

    @Test
    fun hentGrenserForFylkeReturnsEmptyCollectionWhenFylkeNotFound() {
        val fylkeIdentifikator = Identifikator(lokalid = Lokalid("1"))
        every { fylkeRepositoryKotlinMock.findGyldigObjektForIdentifikator(fylkeIdentifikator) } returns null

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
        every { fylkeRepositoryKotlinMock.findGyldigObjektForIdentifikator(fylkeIdentifikator) } returns fylke

        val grenserForFylke = fylkeService.hentGrenserForFylke(fylkeIdentifikator)
        assertEquals(10, grenserForFylke.size, "Expected ten borders")

        assertGrensekoblinger(grenserForFylke, Grensetype.AvtaltAvgrensningslinje, 1, "Expected 1 AvtaltAvgrensningslinje")
        assertGrensekoblinger(grenserForFylke, Grensetype.Territorialgrense, 2, "Expected 2 Territorialgrense")
        assertGrensekoblinger(grenserForFylke, Grensetype.Riksgrense, 3, "Expected 3 Riksgrense")
        assertGrensekoblinger(grenserForFylke, Grensetype.Fylkesgrense, 4, "Expected 4 Fylkesgrense")
    }

    @Test
    fun thatOppdaterFylkeReturnsNullWhenFylkeNotFound() {
        whenever(fylkeRepository.findById(anyString())).thenReturn(Optional.empty())
        assertNull(fylkeService.oppdaterFylke(Lokalid("1"), FylkeRequestBuilder.DEFAULT_REQUEST), "Expected to not find the fylke")
    }

    @Test
    fun thatOppdaterFylkeSavesAnUpdatedVersion() {
        val lokalIdValue = "1000"
        val navn = "Oslo"

        val eksisterende = FylkeBuilder.DEFAULT_FYLKE
        val oppdatertFylke = FylkeRequestBuilder().withLokalId(lokalIdValue).withNavn(navn).build()

        val lokalid = Lokalid(lokalIdValue)
        val fylkeIdentifikator = Identifikator(lokalid = lokalid)
        every { fylkeRepositoryKotlinMock.findGyldigObjektForIdentifikator(fylkeIdentifikator) } returns eksisterende

        fylkeService.oppdaterFylke(lokalid, oppdatertFylke)

        val forventetNavnToSave = eksisterende.administrativenhetnavn.first()
        forventetNavnToSave.navn = navn

        verify(administrativenhetnavnRepository).save(forventetNavnToSave)
        verify(fylkeRepository).save(eksisterende)
    }
}
