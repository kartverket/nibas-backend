package no.kartverket.nibas.controller.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.KretsResponseBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.service.KommuneService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when` as whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(KommuneController::class)
class KommuneControllerTest : NibasMvcTest() {

    @MockBean private lateinit var kommuneService: KommuneService

    @MockBean private lateinit var hrefProviderMock: HrefProvider

    @Autowired private lateinit var objectMapper: ObjectMapper

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun mockKodelisteService() {
        whenever(kodelisteServiceMock.getCodeValueByKommunenummerId(anyString())).thenReturn("0301")
        whenever(hrefProviderMock.getHref(any(Identifikator::class.java))).thenReturn(href)
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        val kommune = KommuneBuilder("1")
            .withNavn("Oslo")
            .build()

        whenever(kommuneService.hentAlleKommuner()).thenReturn(listOf(kommune))
        get("/v1/ekstern/kommuner", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(kommuneService.hentKommune(any(Identifikator::class.java)))
            .thenReturn(KommuneBuilder.DEFAULT_KOMMUNE)
        get("/v1/ekstern/kommuner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(kommuneService.hentKommune(any(Identifikator::class.java))).thenReturn(null)
        get("/v1/ekstern/kommuner/1", status().isNotFound)
    }

    @Test
    fun hentAlleStemmekretserForKommuneGir200_OK() {
        val stemmekrets1 = stemmekrets("1")
        val stemmekrets2 = stemmekrets("2")
        val stemmekretsResponseList =
            listOf(stemmekretsKretsResponse(stemmekrets1), stemmekretsKretsResponse(stemmekrets2))
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        whenever(
                kommuneService.hentAlleStemmekretserForKommune(
                    anyString(),
                    any(LocalDate::class.java)
                )
            )
            .thenReturn(listOf(stemmekrets1, stemmekrets2))

        val responseAsString = invokeAndExpectStatusOK("/v1/ekstern/kommuner/0301/stemmekretser")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    @Test
    fun hentAlleGrunnkretserForKommuneGir200_OK() {
        val grunnkrets1 = grunnkrets("a5")
        val grunnkrets2 = grunnkrets("b7")
        val grunnkretsResponseList =
            listOf(grunnkretsKretsResponse(grunnkrets1), grunnkretsKretsResponse(grunnkrets2))
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        whenever(
                kommuneService.hentAlleGrunnkretserForKommune(
                    anyString(),
                    any(LocalDate::class.java)
                )
            )
            .thenReturn(listOf(grunnkrets1, grunnkrets2))

        val responseAsString =
            invokeAndExpectStatusOK("/v1/ekstern/kommuner/0301/grunnkretser")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
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
