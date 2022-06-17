package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.builders.NasjonRequestBuilder
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.NasjonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.mockito.Mockito.`when` as whenever

fun assertGrensekoblinger(grenseKoblinger: Set<GrenseKobling>, grensetype: Grensetype, size: Int, msg: String) {
    assertThat(grenseKoblinger.map { it.grense }
        .filter { it.grensetype == grensetype })
        .hasSize(size)
        .withFailMessage(msg)
}

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
        assertThat(nasjonService.hentGrenserForNasjon("1")).isEmpty()
    }

    @Test
    fun hentGrenserForNasjonReturnsTheCorrectBorders() {
        val nasjon = NasjonBuilder()
            .withFylke(FylkeBuilder()
                .withKommune(KommuneBuilder()
                    .withGrunnkrets(GrunnkretsBuilder()
                        .withGrenser(
                            Grense(Grensetype.Riksgrense),
                            Grense(Grensetype.Riksgrense),
                            Grense(Grensetype.Riksgrense),
                            Grense(Grensetype.Territorialgrense),
                            Grense(Grensetype.Territorialgrense),
                            Grense(Grensetype.AvtaltAvgrensningslinje),
                        )
                        .build())
                    .build())
                .build())
            .build()

        whenever(nasjonRepository.findById(anyString())).thenReturn(Optional.of(nasjon))

        val grenserForNasjon = nasjonService.hentGrenserForNasjon("1")
        assertEquals(6, grenserForNasjon.size, "Expected six borders")
        assertGrensekoblinger(grenserForNasjon, Grensetype.AvtaltAvgrensningslinje, 1, "Expected 1 AvtaltAvgrensningslinje")
        assertGrensekoblinger(grenserForNasjon, Grensetype.Territorialgrense, 2, "Expected 2 Territorialgrense")
        assertGrensekoblinger(grenserForNasjon, Grensetype.Riksgrense, 3, "Expected 3 Riksgrense")
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
