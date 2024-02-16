package no.kartverket.nibas.controller.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.KretsResponseBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.utils.IdType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(KommuneController::class)
class KommuneControllerTest : NibasMvcTest() {

    @MockkBean
    private lateinit var kommuneService: KommuneService

    @MockkBean
    private lateinit var hrefProviderMock: HrefProvider

    @Autowired private lateinit var objectMapper: ObjectMapper

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun setupMocks() {
        every { hrefProviderMock.getHref(any<Identifikator>()) } returns href
        every { kommuneService.hentAntallGyldigeVersjonerFramITidFor(any())} returns 1
    }

    @Test
    fun `json-struktur for hent alle kommuner validerer`() {
        val oslo = KommuneBuilder(unikLokalid = UnikLokalid(lokalid = "2f8abeb3-f180-4800-84a3-df6653fc9468", Flatetype.KOMMUNE))
            .withNavn(navn = "Oslo", spraak = "nor")
            .build()
        val vegaarshei = KommuneBuilder(unikLokalid = UnikLokalid(lokalid = "25c2b192-c6b5-4d2c-ba4e-08ab31b4432d", Flatetype.KOMMUNE))
            .withNavn(navn = "Vegårshei", spraak = "nor")
            .build()

        every { kommuneService.hentAlleKommuner() } returns listOf(oslo, vegaarshei)
        val faktiskRespons = get("/v1/ekstern/kommuner")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentAlleKommuner.json", IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for hent en kommune validerer`() {
        val oslo = KommuneBuilder(unikLokalid = UnikLokalid(lokalid = "2f8abeb3-f180-4800-84a3-df6653fc9468", Flatetype.KOMMUNE))
            .withNavn(navn = "Oslo", spraak = "nor")
            .build()
        every { kommuneService.hentKommune(any()) } returns oslo

        val faktiskRespons = get("/v1/ekstern/kommuner/123")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentEnKommune.json", IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for alle stemmekretser for en kommune validerer`() {
        val tangen = StemmekretsBuilder(stemmekretsnavn = "Tangen").withLokalId("dadd20dc-170c-4c9a-9507-dc571c4d0a3b").build()
        val myra = StemmekretsBuilder(stemmekretsnavn = "Myra").withLokalId("cc680e25-3bd5-4427-bbae-fb47f3d8ce88").build()

        every {
            kommuneService.hentAlleStemmekretserForKommune(any(), any())
        } returns listOf(tangen, myra)

        val faktiskRespons = get("/v1/ekstern/kommuner/123/stemmekretser")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentAlleStemmekretserForKommune.json")
    }

    @Test
    fun `json-struktur for alle stemmekretser med detaljer for en kommune validerer`() {
        val tangen = StemmekretsBuilder(stemmekretsnavn = "Tangen").withLokalId("dadd20dc-170c-4c9a-9507-dc571c4d0a3b").build()
        val myra = StemmekretsBuilder(stemmekretsnavn = "Myra").withLokalId("cc680e25-3bd5-4427-bbae-fb47f3d8ce88").build()

        every {
            kommuneService.hentAlleStemmekretserForKommune(any(), any())
        } returns listOf(tangen, myra)

        val faktiskRespons = get("/v1/ekstern/kommuner/123/stemmekretsermeddetaljer")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentAlleStemmekretserMedDetaljerForEnKommune.json", IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for alle grunnkretser for en kommune validerer`() {
        val tangen = GrunnkretsBuilder(grunnkretsnavn = "Tangen").withLokalId("3efb5575-22e3-45da-829e-707a4d01b919").build()
        val myraVest = GrunnkretsBuilder(grunnkretsnavn = "Myra vest").withLokalId("d0dee964-2b38-4311-aa92-5e0aeeab3440").build()

        every {
            kommuneService.hentAlleGrunnkretserForKommune(any(), any())
        } returns listOf(tangen, myraVest)

        val faktiskRespons = get("/v1/ekstern/kommuner/123/grunnkretser")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentAlleGrunnkretserForKommune.json")
    }

    @Test
    fun `json-struktur for alle grunnkretser med detaljer for en kommune validerer`() {
        val tangen = GrunnkretsBuilder(grunnkretsnavn = "Tangen").withLokalId("3efb5575-22e3-45da-829e-707a4d01b919").build()
        val myraVest = GrunnkretsBuilder(grunnkretsnavn = "Myra vest").withLokalId("d0dee964-2b38-4311-aa92-5e0aeeab3440").build()

        every {
            kommuneService.hentAlleGrunnkretserForKommune(any(), any())
        } returns listOf(tangen, myraVest)

        val faktiskRespons = get("/v1/ekstern/kommuner/123/grunnkretsermeddetaljer")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentAlleGrunnkretserMedDetaljerForEnKommune.json", IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        val kommune = KommuneBuilder("1")
            .withNavn("Oslo")
            .build()

        every { kommuneService.hentAlleKommuner() } returns listOf(kommune)
        get("/v1/ekstern/kommuner", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        every { kommuneService.hentKommune(any()) } returns KommuneBuilder.DEFAULT_KOMMUNE
        get("/v1/ekstern/kommuner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        every { kommuneService.hentKommune(any()) } returns null
        get("/v1/ekstern/kommuner/1", status().isNotFound)
    }

    @Test
    fun hentAlleStemmekretserForKommuneGir200_OK() {
        val stemmekrets1 = stemmekrets("1")
        val stemmekrets2 = stemmekrets("2")
        val stemmekretsResponseList =
            listOf(stemmekretsKretsResponse(stemmekrets1), stemmekretsKretsResponse(stemmekrets2))
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        every {
            kommuneService.hentAlleStemmekretserForKommune(any(), any())
        } returns listOf(stemmekrets1, stemmekrets2)

        val responseAsString = invokeAndExpectStatusOK("/v1/ekstern/kommuner/0301/stemmekretser")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    @Test
    fun hentAlleGrunnkretserForKommuneGir200_OK() {
        val grunnkrets1 = grunnkrets("a5")
        val grunnkrets2 = grunnkrets("b7")
        val grunnkretsResponseList = listOf(grunnkretsKretsResponse(grunnkrets1), grunnkretsKretsResponse(grunnkrets2))
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        every { kommuneService.hentAlleGrunnkretserForKommune(any(), any()) } returns listOf(grunnkrets1, grunnkrets2)

        val responseAsString = invokeAndExpectStatusOK("/v1/ekstern/kommuner/0301/grunnkretser")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    private fun invokeAndExpectStatusOK(url: String): String {
        return mvc.perform(MockMvcRequestBuilders.get(url).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .getContentAsString(Charsets.UTF_8)
    }

    private fun stemmekretsKretsResponse(stemmekrets: Stemmekrets): KretsResponse {
        return KretsResponseBuilder(stemmekrets = stemmekrets).buildForStemmekrets()
    }

    private fun grunnkretsKretsResponse(grunnkrets: Grunnkrets): KretsResponse {
        return KretsResponseBuilder(grunnkrets = grunnkrets).buildForGrunnkrets()
    }

    private fun grunnkrets(id: String): Grunnkrets {
        return GrunnkretsBuilder(id).build()
    }

    private fun stemmekrets(id: String): Stemmekrets {
        return StemmekretsBuilder(id).build()
    }

    private fun asJson(someObject: Any): String? = objectMapper.writeValueAsString(someObject)
}
