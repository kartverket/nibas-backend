package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.FylkeRequestBuilder
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail
import org.mockito.Mockito.`when` as whenever

@SpringBootTest(classes = [FylkeService::class])
class FylkeServiceTest {

    @Autowired
    lateinit var fylkeService: FylkeService

    @MockBean
    private lateinit var fylkeRepository: FylkeRepository

    @MockBean
    private lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Test
    fun hentGrenserForFylkeReturnsNullWhenFylkeNotFound() {
        whenever(fylkeRepository.findById(anyString())).thenReturn(Optional.empty())
        assertNull(fylkeService.hentGrenserForFylke("1"), "Expected to not find the fylke")
    }

    @Test
    fun hentGrenserForFylkeReturnsTheCorrectBorders() {
        val fylke = FylkeBuilder()
            .withFylkesgrenser(Fylkesgrense(), Fylkesgrense(), Fylkesgrense(), Fylkesgrense())
            .withRiksgrenser(Riksgrense(), Riksgrense(), Riksgrense())
            .withTerritorialgrenser(Territorialgrense(), Territorialgrense())
            .withAvtaltavgrensningslinjer(Avtaltavgrensningslinje())
            .build()

        whenever(fylkeRepository.findById(anyString())).thenReturn(Optional.of(fylke))

        when (val grenserForFylke = fylkeService.hentGrenserForFylke("1")) {
            null -> fail("Expected grenser to have a value")
            else -> {
                assertEquals(10, grenserForFylke.size, "Expected ten borders")
                val grenserUtenRetning = grenserForFylke.map { it.getGrense() }
                assertEquals(
                    1,
                    grenserUtenRetning.filterIsInstance<Avtaltavgrensningslinje>().size,
                    "Expected 1 AvtaltAvgrensningslinje"
                )
                assertEquals(
                    2,
                    grenserUtenRetning.filterIsInstance<Territorialgrense>().size,
                    "Expected 2 Territorialgrense"
                )
                assertEquals(
                    3,
                    grenserUtenRetning.filterIsInstance<Riksgrense>().size,
                    "Expected 3 Riksgrense"
                )
                assertEquals(
                    4,
                    grenserUtenRetning.filterIsInstance<Fylkesgrense>().size,
                    "Expected 4 Fylkesgrense"
                )
            }
        }
    }

    @Test
    fun thatOppdaterFylkeReturnsNullWhenFylkeNotFound() {
        whenever(fylkeRepository.findById(anyString())).thenReturn(Optional.empty())
        assertNull(fylkeService.oppdaterFylke("1", FylkeRequestBuilder.DEFAULT_REQUEST), "Expected to not find the fylke")
    }

    @Test
    fun thatOppdaterFylkeSavesAnUpdatedVersion() {
        val navnerom = "no.kartverket.nibas"
        val lokalId = "1000"
        val navn = "Oslo"

        val eksisterende = FylkeBuilder.DEFAULT_FYLKE
        val oppdatertFylke = FylkeRequestBuilder().withNavnerom(navnerom).withLokalId(lokalId).withNavn(navn).build()

        whenever(fylkeRepository.findById(anyString())).thenReturn(Optional.of(eksisterende))
        fylkeService.oppdaterFylke("1", oppdatertFylke)

        val forventetNavnToSave = eksisterende.administrativenhetnavn.first()
        forventetNavnToSave.navn = navn

        eksisterende.navnerom = navnerom
        eksisterende.lokalid = lokalId

        verify(administrativenhetnavnRepository).save(forventetNavnToSave)
        verify(fylkeRepository).save(eksisterende)

    }
}
