package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneRepository
import org.assertj.core.api.Assertions.assertThat
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

@SpringBootTest(classes = [KommuneService::class])
class KommuneServiceTest {

    @Autowired
    lateinit var kommuneService: KommuneService

    @MockBean
    private lateinit var kommuneRepository: KommuneRepository

    @MockBean
    private lateinit var fylkeRepository: FylkeRepository

    @MockBean
    private lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Test
    fun hentGrenserForKommuneReturnsNullWhenKommuneNotFound() {
        whenever(kommuneRepository.findById(anyString())).thenReturn(Optional.empty())
        assertThat(kommuneService.hentGrenserForKommune("1")).isEmpty()
    }

    @Test
    fun hentGrenserForKommuneReturnsTheCorrectBorders() {
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

        whenever(kommuneRepository.findById(anyString())).thenReturn(Optional.of(kommune))

        val grenserForKommune = kommuneService.hentGrenserForKommune("1")
        assertEquals(15, grenserForKommune.size, "Expected 15 borders")

        assertGrensekoblinger(grenserForKommune, Grensetype.AvtaltAvgrensningslinje, 1, "Expected 1 AvtaltAvgrensningslinje")
        assertGrensekoblinger(grenserForKommune, Grensetype.Territorialgrense, 2, "Expected 2 Territorialgrense")
        assertGrensekoblinger(grenserForKommune, Grensetype.Riksgrense, 3, "Expected 3 Riksgrense")
        assertGrensekoblinger(grenserForKommune, Grensetype.Fylkesgrense, 4, "Expected 4 Fylkesgrense")
        assertGrensekoblinger(grenserForKommune, Grensetype.Kommunegrense, 5, "5 Kommunegrense")
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
