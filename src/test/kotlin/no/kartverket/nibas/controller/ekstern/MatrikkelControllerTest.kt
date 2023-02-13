package no.kartverket.nibas.controller.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KretsResponseBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.config.WebSecurityConfig
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.service.MatrikkelService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anySet
import org.mockito.Mockito.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever

const val GRUNNKRETSER_ENDPOINT = "/v1/ekstern/matrikkel/grunnkretser"
const val STEMMEKRETSER_ENDPOINT = "/v1/ekstern/matrikkel/stemmekretser"


/**
 * Tester med security-off at tjenestene returnerer som forventet.
 */
@ImportAutoConfiguration(WebSecurityConfig::class)
@WebMvcTest(MatrikkelController::class)
internal class MatrikkelControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var matrikkelService: MatrikkelService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun beforeEach() {
        whenever(kodelisteServiceMock.getCodeValueByKommunenummerId(anyString())).thenReturn("1787")
    }

    @Test
    fun hentStemmekretserForEnIdGir200_OK() {
        val stemmekrets = stemmekrets("1")
        val stemmekretsResponse = stemmekretsKretsResponse(stemmekrets)
        val stemmekretsResponseList = listOf(stemmekretsResponse)
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        whenever(matrikkelService.hentStemmekretserForIds(anySet())).thenReturn(listOf(stemmekrets))

        val responseAsString = invokeAndExpectStatusOK("$STEMMEKRETSER_ENDPOINT?id=1")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    /**
     * Sender inn id 2,1 og forventer sortert respons 1,2
     */
    @Test
    fun hentStemmekretserForToIdsGir200_OK() {
        val stemmekrets1 = stemmekrets("1")
        val stemmekrets2 = stemmekrets("2")
        val stemmekretsResponse1 = stemmekretsKretsResponse(stemmekrets1)
        val stemmekretsResponse2 = stemmekretsKretsResponse(stemmekrets2)
        val stemmekretsResponseList = listOf(stemmekretsResponse1, stemmekretsResponse2)
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        whenever(matrikkelService.hentStemmekretserForIds(anySet())).thenReturn(listOf(stemmekrets1, stemmekrets2))

        val responseAsString = invokeAndExpectStatusOK("$STEMMEKRETSER_ENDPOINT?id=2,1")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    @Test
    fun hentGrunnkretserForEnIdGir200_OK() {
        val grunnkrets = grunnkrets("1")
        val grunnkretsResponse = grunnkretsKretsResponse(grunnkrets)
        val grunnkretsResponseList = listOf(grunnkretsResponse)
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        whenever(matrikkelService.hentGrunnkretserForIds(anySet())).thenReturn(listOf(grunnkrets))

        val responseAsString = invokeAndExpectStatusOK("$GRUNNKRETSER_ENDPOINT?id=1")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    /**
     * Sender inn b7,a5 og forventer sortert respons a5,b7
     */
    @Test
    fun hentGrunnkretserForToIdsGir200_OK() {
        val grunnkrets1 = grunnkrets("a5")
        val grunnkrets2 = grunnkrets("b7")
        val grunnkretsResponse1 = grunnkretsKretsResponse(grunnkrets1)
        val grunnkretsResponse2 = grunnkretsKretsResponse(grunnkrets2)
        val grunnkretsResponseList = listOf(grunnkretsResponse1, grunnkretsResponse2)
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        whenever(matrikkelService.hentGrunnkretserForIds(anySet())).thenReturn(listOf(grunnkrets1, grunnkrets2))

        val responseAsString = invokeAndExpectStatusOK("$GRUNNKRETSER_ENDPOINT?id=b7,a5")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    @Test
    fun hentAlleStemmekretserForKommuneGir200_OK() {
        val stemmekrets1 = stemmekrets("1")
        val stemmekrets2 = stemmekrets("2")
        val stemmekretsResponseList = listOf(stemmekretsKretsResponse(stemmekrets1), stemmekretsKretsResponse(stemmekrets2))
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        whenever(matrikkelService.hentAlleStemmekretserForKommune(anyString())).thenReturn(listOf(stemmekrets1, stemmekrets2))

        val responseAsString = invokeAndExpectStatusOK("/v1/ekstern/matrikkel/kommuner/0301/stemmekretser")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    @Test
    fun hentAlleGrunnkretserForKommuneGir200_OK() {
        val grunnkrets1 = grunnkrets("a5")
        val grunnkrets2 = grunnkrets("b7")
        val grunnkretsResponseList = listOf(grunnkretsKretsResponse(grunnkrets1), grunnkretsKretsResponse(grunnkrets2))
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        whenever(matrikkelService.hentAlleGrunnkretserForKommune(anyString())).thenReturn(listOf(grunnkrets1, grunnkrets2))

        val responseAsString = invokeAndExpectStatusOK("/v1/ekstern/matrikkel/kommuner/0301/grunnkretser")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    private fun invokeAndExpectStatusOK(url: String): String {
        return mvc.perform(
            MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .getContentAsString(Charsets.UTF_8)
    }

    private fun stemmekretsKretsResponse(stemmekrets: Stemmekrets): KretsResponse {
        return KretsResponseBuilder(
            stemmekrets = stemmekrets)
            .buildForStemmekrets()
    }

    private fun grunnkretsKretsResponse(grunnkrets: Grunnkrets): KretsResponse {
        return KretsResponseBuilder(
            grunnkrets = grunnkrets)
            .buildForGrunnkrets()
    }

    private fun grunnkrets(id: String): Grunnkrets {
        return GrunnkretsBuilder(id)
            .build()
    }

    private fun stemmekrets(id: String): Stemmekrets {
        return StemmekretsBuilder(id)
            .build()
    }

    private fun asJson(someObject: Any): String? = objectMapper.writeValueAsString(someObject)

}
