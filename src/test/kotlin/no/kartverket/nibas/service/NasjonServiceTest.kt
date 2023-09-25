package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockkStatic
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.builders.NasjonRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.REPOSITORY_EXTENSIONS_FILEPATH
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @MockkBean
    private lateinit var nasjonRepositoryKotlinMock: NasjonRepository

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
    }

    @Test
    fun hentGrenserForNasjonReturnsNullWhenNasjonNotFound() {
        val nasjonIdentifikator = returnNullForLokalid1()
        assertThat(nasjonService.hentGrenserForNasjon(nasjonIdentifikator)).isEmpty()
    }

    @Test
    fun hentGrenserForNasjonReturnsTheCorrectBorders() {
        val nasjonIdentifikator = Identifikator(lokalid = Lokalid("1"))

        val nasjon = NasjonBuilder()
            .withFylke(FylkeBuilder()
                .withKommune(KommuneBuilder()
                    .withGrunnkrets(GrunnkretsBuilder()
                        .withGrenser(
                            Grense(grensetype = Grensetype.Riksgrense),
                            Grense(grensetype = Grensetype.Riksgrense),
                            Grense(grensetype = Grensetype.Riksgrense),
                            Grense(grensetype = Grensetype.Territorialgrense),
                            Grense(grensetype = Grensetype.Territorialgrense),
                            Grense(grensetype = Grensetype.AvtaltAvgrensningslinje),
                        )
                        .build())
                    .build())
                .build())
            .build()
        every { nasjonRepositoryKotlinMock.findGyldigObjektForIdentifikator(nasjonIdentifikator) } returns nasjon

        val grenserForNasjon = nasjonService.hentGrenserForNasjon(nasjonIdentifikator)
        assertEquals(6, grenserForNasjon.size, "Expected six borders")
        assertGrensekoblinger(grenserForNasjon, Grensetype.AvtaltAvgrensningslinje, 1, "Expected 1 AvtaltAvgrensningslinje")
        assertGrensekoblinger(grenserForNasjon, Grensetype.Territorialgrense, 2, "Expected 2 Territorialgrense")
        assertGrensekoblinger(grenserForNasjon, Grensetype.Riksgrense, 3, "Expected 3 Riksgrense")
    }

    @Test
    fun thatOppdaterFylkeReturnsNullWhenFylkeNotFound() {
        returnNullForLokalid1()
        assertNull(
            nasjonService.oppdaterNasjon(Lokalid("1"), NasjonRequestBuilder.DEFAULT_NASJON),
            "Expected to not find the nasjon"
        )
    }

    @Test
    fun thatOppdaterNasjonSavesAnUpdatedVersion() {
        val lokalIdValue = "1000"
        val navn = "Sverige"

        val eksisterende = NasjonBuilder.DEFAULT_NASJON
        val oppdatertNasjon = NasjonRequestBuilder().withLokalId(lokalIdValue).withNavn(navn).build()

        val lokalid = Lokalid(lokalIdValue)
        val nasjonIdentifikator = Identifikator(lokalid = lokalid)
        every { nasjonRepositoryKotlinMock.findGyldigObjektForIdentifikator(nasjonIdentifikator) } returns eksisterende

        nasjonService.oppdaterNasjon(lokalid, oppdatertNasjon)

        val forventetNavnToSave = eksisterende.nasjonLokalid.administrativenhetnavn.first()
        forventetNavnToSave.navn = navn

        verify(administrativenhetnavnRepository).save(forventetNavnToSave)
        verify(nasjonRepository).save(eksisterende)

    }

    private fun returnNullForLokalid1(): Identifikator {
        val nasjonIdentifikator = Identifikator(lokalid = Lokalid("1"))
        every { nasjonRepositoryKotlinMock.findGyldigObjektForIdentifikator(nasjonIdentifikator) } returns null
        return nasjonIdentifikator
    }
}
