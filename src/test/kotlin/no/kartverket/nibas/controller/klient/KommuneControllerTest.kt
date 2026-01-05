package no.kartverket.nibas.controller.klient

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.extensions.flate.toGrunnkretsResponse
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.sporing.Gyldighet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.Month

@WebMvcTest(KommuneController::class)
class KommuneControllerTest : NibasMvcTest() {

    @MockkBean
    private lateinit var kommuneService: KommuneService

    @MockkBean
    private lateinit var grunnkretsService: GrunnkretsService


    @MockkBean
    private lateinit var hrefProviderMock: HrefProvider

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun setupMocks() {
        every { hrefProviderMock.getHref(any<Identifikator>()) } returns href
        every { kommuneService.hentAntallGyldigeVersjonerFramITidFor(any()) } returns 1
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        val kommune = KommuneBuilder("1")
            .withNavn("Oslo")
            .build()

        every { kommuneService.hentAlleKommuner() } returns listOf(kommune)
        get("/v1/kommuner", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        every { kommuneService.hentKommune(any()) } returns KommuneBuilder.DEFAULT_KOMMUNE
        get("/v1/kommuner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        every { kommuneService.hentKommune(any()) } returns null
        get("/v1/kommuner/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        every {
            kommuneService.hentGrensekoblingerForKommune(any())
        } returns setOf(
            GrunnkretsGrenseKobling(
                grunnkrets = GrunnkretsBuilder().build(),
                grense = GrenseBuilder().withGrensetype(Grensetype.Kommunegrense).build()
            )
        )

        get("/v1/kommuner/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        every { kommuneService.hentGrensekoblingerForKommune(any()) } returns emptySet()
        get("/v1/kommuner/1/grenser", status().isNotFound)
    }

    @Test
    fun hentGrunnkretserForKommuneGir200_OK() {
        val grunnkrets = GrunnkretsBuilder(
            lokalId = Lokalid("123"),
            grunnkretsnavn = "Voi",
            grunnkretsnummer = "8989")
            .build()
        every { kommuneService.hentKommunesGrunnkretser(any()) } returns listOf(grunnkrets)
        every { grunnkretsService.hentAntallGyldigeVersjonerFramITidFor(any()) } returns 1

        get("/v1/kommuner/1/grunnkretser?gyldighetsdato=${formattedDateString(LocalDate.now())}",
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = listOf(grunnkrets.toGrunnkretsResponse()),
                resultMatchers = listOf(status().isOk)
            ))
    }

    @Test
    fun hentGrunnkretserForKommuneMedFremtidigeVersjonerGir200_OK() {
        val kommune = KommuneBuilder()
            .withGyldigFra(LocalDate.of(2022, 1, 1))
            .build()

        val grunnkrets = GrunnkretsBuilder(
            lokalId = Lokalid("123"),
            grunnkretsnavn = "Voi",
            grunnkretsnummer = "8989",
            gyldigFra = LocalDate.of(2022, 1, 1),
            gyldigTil = LocalDate.of(2024, 1, 1)
        ).withKommune(kommune).build()
        every { kommuneService.hentKommunesGrunnkretser(any()) } returns listOf(grunnkrets)
        every { grunnkretsService.hentAntallGyldigeVersjonerFramITidFor(any()) } returns 1

        get("/v1/kommuner/1/grunnkretser?gyldighetsdato=${formattedDateString(LocalDate.now())}",
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = listOf(grunnkrets.toGrunnkretsResponse()),
                resultMatchers = listOf(status().isOk)
            ))
    }

    @Test
    fun hentGrunnkretserForKommuneHistoriskeGir200_OK() {
        val kommune = KommuneBuilder().build()
        val grense = GrenseBuilder().withGrensetype(Grensetype.Grunnkretsgrense).build()
        val grunnkrets = GrunnkretsBuilder().withKommune(kommune).build()
        every { kommuneService.hentGrensekoblingerForAlleGrunnkretserIKommuneHistoriske(any()) } returns setOf(GrunnkretsGrenseKobling(grunnkrets, grense))
        get("/v1/kommuner/1/grunnkretsgrenserHistoriske?gyldigTilDate=${formattedDateString(LocalDate.now())}",status().isOk)
    }

    @Test
    fun hentStemmekretserForKommuneHistoriskeGir200_OK() {
        val grense = GrenseBuilder().withGrensetype(Grensetype.Stemmekretsgrense).build()
        val stemmekrets = StemmekretsBuilder().build()
        every { kommuneService.hentGrensekoblingerForAlleStemmekretserIKommuneHistoriske(any()) } returns setOf(StemmekretsGrenseKobling(stemmekrets, grense))
        get("/v1/kommuner/1/stemmekretsgrenserHistoriske?gyldigTilDate=${formattedDateString(LocalDate.now())}",status().isOk)
    }

    @Test
    fun `hent kommunes inndelinger med delt geometri grenser feiler naar ingen grenser er tilgjengelig`() {
        every { kommuneService.hentKommunesInndelingerMedDeltGeometriGrenser(any()) } returns emptyList()

        get("/v1/kommuner/1/inndelingerdeltgeometrigrenser", status().isNotFound)
    }

    @Test
    fun `hent kommunes inndelinger med delt geometri feiler naar ingen inndelinger er tilgjengelig`() {
        every { kommuneService.hentKommunesStemmekretser(any()) } returns emptyList()
        every { kommuneService.hentKommunesGrunnkretser(any()) } returns emptyList()

        get("/v1/kommuner/1/inndelingerdeltgeometri", status().isNotFound)
    }


    @Test
    fun thatHentAdmnistrativtnavnIsOk() {
        val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
        val navn0 = Administrativenhetnavn().apply {
            navn = "Gáivuotna"
            spraak = Spraakkode().apply { id = "sme" }
            rekkefoelge = 1
            gyldighet = Gyldighet(gyldigFra = jan1In2023)
        }
        val navn1 = Administrativenhetnavn().apply {
            navn = "Kåfjord"
            rekkefoelge = 2
            spraak = Spraakkode().apply { id = "nor" }
        }
        val navn2 = Administrativenhetnavn().apply {
            navn = "Kaivuono"
            rekkefoelge = 3
            spraak = Spraakkode().apply { id = "fkv" }
        }

        val kommune = KommuneBuilder(administrativenhetnavn = listOf(navn0, navn1, navn2), gyldigFra = jan1In2023)
            .withKommunenummer("5540")
            .build()
        every { kommuneService.hentKommune(any()) } returns kommune

        val mapper = ObjectMapper()
        val responsTidligereDato = mapper.readTree(get("/v1/kommuner/1?gyldighetsdato=${formattedDateString(jan1In2023)}").response.contentAsString)
        val opprinneligNavn = responsTidligereDato.get("navn")
        assertThat(opprinneligNavn).hasSize(1)
        assertThat(opprinneligNavn[0].get("spraak").asText()).isEqualTo("sme")
        assertThat(opprinneligNavn[0].get("navn").asText()).isEqualTo("Gáivuotna")
        assertThat(opprinneligNavn[0].get("rekkefoelge").asInt()).isEqualTo(1)

        val responsDagensDato = mapper.readTree(get("/v1/kommuner/1").response.contentAsString)
        val dagensNavn = responsDagensDato.get("navn")
        assertThat(dagensNavn).hasSize(3)

        val foersteNavn = dagensNavn[0]
        assertThat(foersteNavn.get("spraak").asText()).isEqualTo("sme")
        assertThat(foersteNavn.get("navn").asText()).isEqualTo("Gáivuotna")
        assertThat(foersteNavn.get("rekkefoelge").asInt()).isEqualTo(1)

        val andreNavn = dagensNavn[1]
        assertThat(andreNavn.get("spraak").asText()).isEqualTo("nor")
        assertThat(andreNavn.get("navn").asText()).isEqualTo("Kåfjord")
        assertThat(andreNavn.get("rekkefoelge").asInt()).isEqualTo(2)

        val tredjeNavn = dagensNavn[2]
        assertThat(tredjeNavn.get("spraak").asText()).isEqualTo("fkv")
        assertThat(tredjeNavn.get("navn").asText()).isEqualTo("Kaivuono")
        assertThat(tredjeNavn.get("rekkefoelge").asInt()).isEqualTo(3)
    }

}
