package no.kartverket.nibas.controller.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.builders.KretsResponseBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.config.WebSecurityConfig
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.service.StemmekretsService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import org.mockito.Mockito.`when` as whenever

const val EKSTERN_STEMMEKRETSER_ENDPOINT = "/v1/ekstern/stemmekretser"

@ImportAutoConfiguration(WebSecurityConfig::class)
@WebMvcTest(StemmekretsController::class)
internal class StemmekretsControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var stemmekretsService: StemmekretsService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun beforeEach() {
        whenever(kodelisteServiceMock.getCodeValueByKommunenummerId(anyString())).thenReturn("0301")
    }

    @Test
    fun hentStemmekretsReturns200_OK() {
        whenever(stemmekretsService.hentStemmekrets(any(Identifikator::class.java))).thenReturn(StemmekretsBuilder.DEFAULT_STEMMEKRETS)
        get("$EKSTERN_STEMMEKRETSER_ENDPOINT/1", status().isOk)
    }

    @Test
    fun hentStemmekretserForEnIdGir200_OK() {
        val stemmekrets = stemmekrets("1")
        val stemmekretsResponse = stemmekretsKretsResponse(stemmekrets)
        val stemmekretsResponseList = listOf(stemmekretsResponse)
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        whenever(stemmekretsService.hentStemmekretserForIds(anySet(), any(LocalDate::class.java)))
            .thenReturn(listOf(stemmekrets))

        val responseAsString = invokeAndExpectStatusOK("$EKSTERN_STEMMEKRETSER_ENDPOINT?ids=1")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    /** Sender inn id 2,1 og forventer sortert respons 1,2 */
    @Test
    fun hentStemmekretserForToIdsGir200_OK() {
        val stemmekrets1 = stemmekrets("1")
        val stemmekrets2 = stemmekrets("2")
        val stemmekretsResponse1 = stemmekretsKretsResponse(stemmekrets1)
        val stemmekretsResponse2 = stemmekretsKretsResponse(stemmekrets2)
        val stemmekretsResponseList = listOf(stemmekretsResponse1, stemmekretsResponse2)
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        whenever(stemmekretsService.hentStemmekretserForIds(anySet(), any(LocalDate::class.java)))
            .thenReturn(listOf(stemmekrets1, stemmekrets2))

        val responseAsString = invokeAndExpectStatusOK("$EKSTERN_STEMMEKRETSER_ENDPOINT?ids=2,1&gyldighetsdato=2023-02-27")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    private fun stemmekrets(id: String): Stemmekrets {
        return StemmekretsBuilder(id)
            .build()
    }

    private fun stemmekretsKretsResponse(stemmekrets: Stemmekrets): KretsResponse {
        return KretsResponseBuilder(
            stemmekrets = stemmekrets)
            .buildForStemmekrets()
    }

    private fun asJson(someObject: Any): String? = objectMapper.writeValueAsString(someObject)

    private fun invokeAndExpectStatusOK(url: String): String {
        return mvc.perform(
            MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .getContentAsString(Charsets.UTF_8)
    }
}

