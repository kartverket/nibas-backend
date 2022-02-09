package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.KommuneRepository
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

@SpringBootTest(classes = [KommuneService::class])
class KommuneServiceTest {

    @Autowired
    lateinit var kommuneService: KommuneService

    @MockBean
    private lateinit var kommuneRepository: KommuneRepository

    @MockBean
    private lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Test
    fun hentGrenserForKommuneReturnsNullWhenKommuneNotFound() {
        whenever(kommuneRepository.findById(anyString())).thenReturn(Optional.empty())
        assertNull(kommuneService.hentGrenserForKommune("1"), "Expected to not find the kommune")
    }

    @Test
    fun hentGrenserForKommuneReturnsTheCorrectBorders() {
        val kommune = KommuneBuilder()
            .withKommunegrenser(Kommunegrense(), Kommunegrense(), Kommunegrense(), Kommunegrense(), Kommunegrense())
            .withFylkesgrenser(Fylkesgrense(), Fylkesgrense(), Fylkesgrense(), Fylkesgrense())
            .withRiksgrenser(Riksgrense(), Riksgrense(), Riksgrense())
            .withTerritorialgrenser(Territorialgrense(), Territorialgrense())
            .withAvtaltavgrensningslinjer(Avtaltavgrensningslinje())
            .build()

        whenever(kommuneRepository.findById(anyString())).thenReturn(Optional.of(kommune))

        when (val grenserForKommune = kommuneService.hentGrenserForKommune("1")) {
            null -> fail("Expected grenser to have a value")
            else -> {
                assertEquals(15, grenserForKommune.size, "Expected 15 borders")
                assertEquals(
                    1,
                    grenserForKommune.filterIsInstance<Avtaltavgrensningslinje>().size,
                    "Expected 1 AvtaltAvgrensningslinje"
                )
                assertEquals(
                    2,
                    grenserForKommune.filterIsInstance<Territorialgrense>().size,
                    "Expected 2 Territorialgrense"
                )
                assertEquals(
                    3,
                    grenserForKommune.filterIsInstance<Riksgrense>().size,
                    "Expected 3 Riksgrense"
                )
                assertEquals(
                    4,
                    grenserForKommune.filterIsInstance<Fylkesgrense>().size,
                    "Expected 4 Fylkesgrense"
                )
                assertEquals(
                    5,
                    grenserForKommune.filterIsInstance<Kommunegrense>().size,
                    "Expected 5 Kommunegrense"
                )
            }
        }
    }

    @Test
    fun thatOppdaterKommuneReturnsNullWhenKommuneNotFound() {
        whenever(kommuneRepository.findById(anyString())).thenReturn(Optional.empty())
        assertNull(
            kommuneService.oppdaterKommune("1", KommuneRequestBuilder.DEFAULT_REQUEST),
            "Expected to not find the kommune"
        )
    }

    @Test
    fun thatOppdaterKommuneSavesAnUpdatedVersion() {
        val navnerom = "no.kartverket.nibas"
        val lokalId = "1000"
        val navn = "Bergen"

        val eksisterende = KommuneBuilder.DEFAULT_KOMMUNE
        val oppdatertKommune =
            KommuneRequestBuilder().withNavnerom(navnerom).withLokalId(lokalId).withNavn(navn).build()

        whenever(kommuneRepository.findById(anyString())).thenReturn(Optional.of(eksisterende))
        kommuneService.oppdaterKommune("1", oppdatertKommune)

        val forventetNavnToSave = eksisterende.administrativenhetnavn.first()
        forventetNavnToSave.navn = navn

        eksisterende.navnerom = navnerom
        eksisterende.lokalid = lokalId

        verify(administrativenhetnavnRepository).save(forventetNavnToSave)
        verify(kommuneRepository).save(eksisterende)

    }
}
