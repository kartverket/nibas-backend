package no.kartverket.nibas.controller.klient

import tools.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.sporing.Gyldighet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.Month

@WebMvcTest(FylkeController::class)
class FylkeControllerTest : NibasMvcTest() {

    @MockkBean
    private lateinit var fylkeService: FylkeService

    @MockkBean
    private lateinit var hrefProviderMock: HrefProvider

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun beforeEach() {
        every { hrefProviderMock.getHref(any<Identifikator>()) } returns href
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        val fylke = FylkeBuilder()
            .withNavn("Vestland")
            .withFylkesnummer("1")
            .build()

        every { fylkeService.hentAlleFylker() } returns listOf(fylke)
        get("/v1/fylker", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        every { fylkeService.hentFylke(any()) } returns
            FylkeBuilder()
                .withNavn("Vestland")
                .withFylkesnummerUuid("UUID-123")
                .withFylkesnummer("1")
                .build()

        get("/v1/fylker/1", status().isOk)
    }

    @Test
    fun thatHentAdmnistrativtnavnIsOk() {
        val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
        val historiskNavn = Administrativenhetnavn().apply {
            navn = "Vestlandet"
            spraak = Spraakkode().apply { id = "nor" }
            gyldighet = Gyldighet(gyldigFra = jan1In2023, gyldigTil = LocalDate.now())
        }
        val navn1 = Administrativenhetnavn().apply {
            navn = "Innlandet"
            rekkefoelge = 1
            spraak = Spraakkode().apply { id = "nor" }
        }
        val navn2 = Administrativenhetnavn().apply {
            navn = "Sisdajve"
            rekkefoelge = 2
            spraak = Spraakkode().apply { id = "sma" }
        }

        val fylke = FylkeBuilder(administrativenhetnavn = listOf(historiskNavn, navn1, navn2), gyldigFra = jan1In2023)
            .withFylkesnummerUuid("UUID-123")
            .withFylkesnummer("1")
            .build()
        every { fylkeService.hentFylke(any()) } returns fylke

        val mapper = ObjectMapper()
        val responsTidligereDato = mapper.readTree(get("/v1/fylker/1?gyldighetsdato=${formattedDateString(jan1In2023)}").response.contentAsString)
        val tidligereNavn = responsTidligereDato.get("navn")
        assertThat(tidligereNavn).hasSize(1)
        assertThat(tidligereNavn[0].get("spraak").asText()).isEqualTo("nor")
        assertThat(tidligereNavn[0].get("navn").asText()).isEqualTo("Vestlandet")
        assertThat(tidligereNavn[0].get("rekkefoelge").isNull).isTrue

        val responsDagensDato = mapper.readTree(get("/v1/fylker/1").response.contentAsString)
        val dagensNavn = responsDagensDato.get("navn")
        assertThat(dagensNavn).hasSize(2)

        val foersteNavn = dagensNavn[0]
        assertThat(foersteNavn.get("spraak").asText()).isEqualTo("nor")
        assertThat(foersteNavn.get("navn").asText()).isEqualTo("Innlandet")
        assertThat(foersteNavn.get("rekkefoelge").asInt()).isEqualTo(1)

        val andreNavn = dagensNavn[1]
        assertThat(andreNavn.get("spraak").asText()).isEqualTo("sma")
        assertThat(andreNavn.get("navn").asText()).isEqualTo("Sisdajve")
        assertThat(andreNavn.get("rekkefoelge").asInt()).isEqualTo(2)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        every { fylkeService.hentFylke(any()) } returns null
        get("/v1/fylker/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        every {
            fylkeService.hentGrensekoblingerForFylke(any())
        } returns setOf(
            GrunnkretsGrenseKobling(
                grunnkrets = GrunnkretsBuilder().build(),
                grense = GrenseBuilder().withGrensetype(Grensetype.Fylkesgrense).build(),
            )
        )

        get("/v1/fylker/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        every { fylkeService.hentGrensekoblingerForFylke(any()) } returns emptySet()
        get("/v1/fylker/1/grenser", status().isNotFound)
    }

}
