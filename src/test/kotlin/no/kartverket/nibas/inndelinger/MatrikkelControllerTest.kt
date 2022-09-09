package no.kartverket.nibas.inndelinger

import com.fasterxml.jackson.databind.ObjectMapper
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.response.GrunnkretsResponse
import no.kartverket.nibas.api.v1.response.StemmekretsResponse
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.GrunnkretsResponseBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.builders.StemmekretsResponseBuilder
import no.kartverket.nibas.config.ApiKeyWebSecurityConfig
import no.kartverket.nibas.kodelister.KodelisteService
import org.assertj.core.api.Assertions.assertThat
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
@ImportAutoConfiguration(ApiKeyWebSecurityConfig::class)
@WebMvcTest(MatrikkelController::class)
internal class MatrikkelControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var matrikkelService: MatrikkelService

    @MockBean
    private lateinit var grunnkretsControllerServiceMock: GrunnkretsControllerService

    @MockBean
    private lateinit var stemmekretsControllerServiceMock: StemmekretsControllerService

    @MockBean
    private lateinit var kodelisteServiceMock: KodelisteService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun hentStemmekretserForEnIdGir200_OK() {
        val stemmekretsResponse = stemmekretsResponse("1")
        val stemmekretsResponseList = listOf(stemmekretsResponse)
        whenever(matrikkelService.hentStemmekretserForIds(anySet())).thenReturn(stemmekretsResponseList)
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        val responseAsString = invokeAndExpectStatusOK("$STEMMEKRETSER_ENDPOINT?id=1")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    /**
     * Sender inn id 2,1 og forventer sortert respons 1,2
     */
    @Test
    fun hentStemmekretserForToIdsGir200_OK() {
        val stemmekretsResponse1 = stemmekretsResponse("1")
        val stemmekretsResponse2 = stemmekretsResponse("2")
        val stemmekretsResponseList = listOf(stemmekretsResponse1, stemmekretsResponse2)
        whenever(matrikkelService.hentStemmekretserForIds(anySet())).thenReturn(stemmekretsResponseList)
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        val responseAsString = invokeAndExpectStatusOK("$STEMMEKRETSER_ENDPOINT?id=2,1")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    @Test
    fun hentGrunnkretserForEnIdGir200_OK() {
        val grunnkretsResponse = grunnkretsResponse("1")
        val grunnkretsResponseList = listOf(grunnkretsResponse)
        whenever(matrikkelService.hentGrunnkretserForIds(anySet())).thenReturn(grunnkretsResponseList)
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        val responseAsString = invokeAndExpectStatusOK("$GRUNNKRETSER_ENDPOINT?id=1")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    /**
     * Sender inn b7,a5 og forventer sortert respons a5,b7
     */
    @Test
    fun hentGrunnkretserForToIdsGir200_OK() {
        val grunnkretsResponse1 = grunnkretsResponse("a5")
        val grunnkretsResponse2 = grunnkretsResponse("b7")
        val grunnkretsResponseList = listOf(grunnkretsResponse1, grunnkretsResponse2)
        whenever(matrikkelService.hentGrunnkretserForIds(anySet())).thenReturn(grunnkretsResponseList)
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        val responseAsString = invokeAndExpectStatusOK("$GRUNNKRETSER_ENDPOINT?id=b7,a5")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    @Test
    fun hentAlleStemmekretserForKommuneGir200_OK() {
        val stemmekretsResponseList = listOf(stemmekretsResponse("1"), stemmekretsResponse("2"))
        whenever(matrikkelService.hentAlleStemmekretserForKommune(anyString())).thenReturn(stemmekretsResponseList)
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        val responseAsString = invokeAndExpectStatusOK("/v1/ekstern/matrikkel/kommuner/0301/stemmekretser")
        assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    @Test
    fun hentAlleGrunnkretserForKommuneGir200_OK() {
        val grunnkretsResponseList = listOf(grunnkretsResponse("a5"), grunnkretsResponse("b7"))
        whenever(matrikkelService.hentAlleGrunnkretserForKommune(anyString())).thenReturn(grunnkretsResponseList)
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

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

    private fun stemmekretsResponse(id: String): StemmekretsResponse {
        return StemmekretsResponseBuilder(kodelisteServiceMock)
            .forStemmekrets(StemmekretsBuilder(id)
                .build())
            .build()
    }

    private fun grunnkretsResponse(id: String): GrunnkretsResponse {
        return GrunnkretsResponseBuilder(kodelisteServiceMock)
            .forGrunnkrets(GrunnkretsBuilder(id)
                .build())
            .build()
    }

    private fun asJson(someObject: Any): String? = objectMapper.writeValueAsString(someObject)

}
