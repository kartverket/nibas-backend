package no.kartverket.nibas.controller.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KretsResponseBuilder
import no.kartverket.nibas.config.WebSecurityConfig
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.service.GrunnkretsService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anySet
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

const val EKSTERN_GRUNNKRETSER_ENDPOINT = "/v1/ekstern/grunnkretser"

@ImportAutoConfiguration(WebSecurityConfig::class)
@WebMvcTest(GrunnkretsController::class)
internal class GrunnkretsControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var grunnkretsService: GrunnkretsService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun hentGrunnkretserForEnIdGir200_OK() {
        val grunnkrets = grunnkrets("1")
        val grunnkretsResponse = grunnkretsKretsResponse(grunnkrets)
        val grunnkretsResponseList = listOf(grunnkretsResponse)
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        whenever(grunnkretsService.hentGrunnkretserForIds(anySet(), any(LocalDate::class.java))).thenReturn(listOf(grunnkrets))

        val responseAsString = invokeAndExpectStatusOK("$EKSTERN_GRUNNKRETSER_ENDPOINT?ids=1")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
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

        whenever(grunnkretsService.hentGrunnkretserForIds(anySet(), any(LocalDate::class.java))).thenReturn(listOf(grunnkrets1, grunnkrets2))

        val responseAsString = invokeAndExpectStatusOK("$EKSTERN_GRUNNKRETSER_ENDPOINT?ids=b7,a5&gyldighetsdato=2023-02-27")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

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

    private fun grunnkretsKretsResponse(grunnkrets: Grunnkrets): KretsResponse {
        return KretsResponseBuilder(
            grunnkrets = grunnkrets)
            .buildForGrunnkrets()
    }

    private fun grunnkrets(id: String): Grunnkrets {
        return GrunnkretsBuilder(id)
            .build()
    }

    private fun asJson(someObject: Any): String? = objectMapper.writeValueAsString(someObject)

}

