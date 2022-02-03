package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.builders.NasjonRequestBuilder
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.NasjonRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail
import org.mockito.Mockito.`when` as whenever

@SpringBootTest(classes = [NasjonService::class])
class NasjonServiceTest {

    @Autowired
    lateinit var nasjonService: NasjonService

    @MockBean
    private lateinit var nasjonRepository: NasjonRepository

    @MockBean
    private lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Test
    fun hentGrenserForNasjonReturnsNullWhenNasjonNotFound() {
        whenever(nasjonRepository.findById(anyString())).thenReturn(Optional.empty())
        assertNull(nasjonService.hentGrenserForNasjon("1"), "Expected to not find the nasjon")
    }

    @Test
    fun hentGrenserForNasjonReturnsTheCorrectBorders() {
        val nasjon = NasjonBuilder()
            .withRiksgrenser(Riksgrense(), Riksgrense(), Riksgrense())
            .withTerritorialgrenser(Territorialgrense(), Territorialgrense())
            .withAvtaltavgrensningslinjer(Avtaltavgrensningslinje())
            .build()

        whenever(nasjonRepository.findById(anyString())).thenReturn(Optional.of(nasjon))

        when (val grenserForNasjon = nasjonService.hentGrenserForNasjon("1")) {
            null -> fail("Expected grenser to have a value")
            else -> {
                assertEquals(6, grenserForNasjon.size, "Expected six borders")
                assertEquals(
                    1,
                    grenserForNasjon.filterIsInstance<Avtaltavgrensningslinje>().size,
                    "Expected 1 AvtaltAvgrensningslinje"
                )
                assertEquals(
                    2,
                    grenserForNasjon.filterIsInstance<Territorialgrense>().size,
                    "Expected 2 Territorialgrense"
                )
                assertEquals(
                    3,
                    grenserForNasjon.filterIsInstance<Riksgrense>().size,
                    "Expected 3 Riksgrense"
                )
            }
        }
    }

    @Test
    fun thatOppdaterFylkeReturnsNullWhenFylkeNotFound() {
        whenever(nasjonRepository.findById(anyString())).thenReturn(Optional.empty())
        assertNull(
            nasjonService.oppdaterNasjon("1", NasjonRequestBuilder.DEFAULT_NASJON),
            "Expected to not find the nasjon"
        )
    }

    @Test
    fun thatOppdaterNasjonSavesAnUpdatedVersion() {
        val navnerom = "no.kartverket.nibas"
        val lokalId = "1000"
        val navn = "Sverige"

        val eksisterende = NasjonBuilder.DEFAULT_NASJON
        val oppdatertNasjon = NasjonRequestBuilder().withNavnerom(navnerom).withLokalId(lokalId).withNavn(navn).build()

        whenever(nasjonRepository.findById(anyString())).thenReturn(Optional.of(eksisterende))
        nasjonService.oppdaterNasjon("1", oppdatertNasjon)

        val forventetNavnToSave = eksisterende.administrativenhetnavn.first()
        forventetNavnToSave.navn = navn

        eksisterende.navnerom = navnerom
        eksisterende.lokalid = lokalId

        verify(administrativenhetnavnRepository).save(forventetNavnToSave)
        verify(nasjonRepository).save(eksisterende)

    }
}
