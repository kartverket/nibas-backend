package no.kartverket.nibas

import com.ninjasquad.springmockk.MockkBean
import no.kartverket.nibas.config.WebSecurityConfig
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.utils.IdType
import no.kartverket.nibas.utils.sammenlignJsonFeltnavn
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import tools.jackson.databind.ObjectMapper
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@ActiveProfiles("humanReadableLog", "localhost")
@ImportAutoConfiguration(WebSecurityConfig::class)
@WebMvcTest
open class NibasMvcTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: ObjectMapper

    @MockkBean
    lateinit var kodelisteServiceMock: KodelisteService

    fun get(url: String): MvcResult {
        return perform(MockMvcRequestBuilders.get(url), emptyList())
    }

    fun get(url: String, matcher: ResultMatcher) {
        perform(MockMvcRequestBuilders.get(url), matcher)
    }

    fun get(url: String, responseAsserts: MockResponseAsserts) {
        val mvcResult = perform(MockMvcRequestBuilders.get(url), matchers = responseAsserts.resultMatchers)
        assertResponseObject(mvcResult, responseAsserts)
    }

    fun put(url: String, request: Any, expected: ResultMatcher) =
        perform(MockMvcRequestBuilders.put(url).content(asJson(request)), expected)

    fun put(url: String, requestObject: Any, responseAsserts: MockResponseAsserts) {
        val mvcResult = perform(
            MockMvcRequestBuilders
                .put(url)
                .content(asJson(requestObject)),
            matchers = responseAsserts.resultMatchers)
        assertResponseObject(mvcResult, responseAsserts)
    }

    fun post(url: String, expected: ResultMatcher) = perform(MockMvcRequestBuilders.post(url), expected)

    // Teste post uten request-objekt, og kunne teste respons-body
    fun post(url: String, responseAsserts: MockResponseAsserts) {
        testPost(url, null, responseAsserts)
    }

    fun post(url: String, requestObject: Any, responseAsserts: MockResponseAsserts) {
        testPost(url, requestObject, responseAsserts)
    }

    fun delete(url: String, responseAsserts: MockResponseAsserts) {
        testDelete(url, responseAsserts)
    }

    fun validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons: MvcResult, filMedForventetRespons: String, idType: IdType = IdType.ID_DIREKTE) {
        val mapper = ObjectMapper()
        val faktiskResponsSomJson = mapper.readTree(faktiskRespons.response.contentAsString)

        val forventetRespons = DefaultResourceLoader().getResource(filMedForventetRespons).getContentAsString(Charset.defaultCharset())
        val forventetResponsSomJson = mapper.readTree(forventetRespons)

        val resultatAvSammenligning = sammenlignJsonFeltnavn(forventetResponsSomJson, faktiskResponsSomJson, idType)
        assertThat(resultatAvSammenligning.erLike)
            .`as`("Resultat: %s", resultatAvSammenligning.resultat)
            .isTrue()
    }

    fun validerAtFaktiskResponsIkkeMatcherForventetRespons(faktiskRespons: MvcResult, forventetMelding: String, filMedForventetRespons: String, idType: IdType = IdType.ID_DIREKTE) {
        val mapper = ObjectMapper()
        val faktiskResponsSomJson = mapper.readTree(faktiskRespons.response.contentAsString)

        val forventetRespons = DefaultResourceLoader().getResource(filMedForventetRespons).getContentAsString(Charset.defaultCharset())
        val forventetResponsSomJson = mapper.readTree(forventetRespons)

        val resultatAvSammenligning = sammenlignJsonFeltnavn(forventetResponsSomJson, faktiskResponsSomJson, idType)
        assertThat(resultatAvSammenligning.erLike)
            .`as`("Resultat: ${resultatAvSammenligning.resultat}")
            .isFalse()
        assertThat(resultatAvSammenligning.resultat.contains(forventetMelding))
            .`as`("Forventet: $forventetMelding, faktisk: ${resultatAvSammenligning.resultat}")
            .isTrue()
    }

    private fun testPost(
        url: String,
        requestObject: Any?,
        responseAsserts: MockResponseAsserts
    ) {
        val postRequest = MockMvcRequestBuilders.post(url)
        if (requestObject != null) {
            postRequest.content(asJson(requestObject))
        }
        val mvcResult = perform(
            action = postRequest,
            matchers = responseAsserts.resultMatchers)
        assertResponseObject(mvcResult, responseAsserts)
    }

    private fun testDelete(
        url: String,
        responseAsserts: MockResponseAsserts
    ) {
        val deleteRequest = MockMvcRequestBuilders.delete(url)
        perform(
            action = deleteRequest,
            matchers = responseAsserts.resultMatchers
        )
    }

    private fun perform(action: MockHttpServletRequestBuilder, matcher: ResultMatcher): MvcResult {
        return perform(action, listOf(matcher))
    }

    private fun perform(action: MockHttpServletRequestBuilder, matchers: List<ResultMatcher>): MvcResult {
        val resultActions = mvc.perform(action.contentType(MediaType.APPLICATION_JSON))
        matchers.forEach { resultActions.andExpect(it) }
        return resultActions.andReturn()
    }

    fun <T> any(clazz: Class<T>): T = Mockito.any(clazz)

    private fun asJson(someObject: Any): String = mapper.writeValueAsString(someObject)

    private fun assertResponseObject(
        mvcResult: MvcResult,
        asserts: MockResponseAsserts
    ) {
        val mockHttpServletResponse = mvcResult.response
        assertThat(mockHttpServletResponse.contentType).isEqualTo(asserts.expectedContentType)

        val expectedResponseObject = asserts.expectedResponseObject
        if (expectedResponseObject != null) {
            val contentAsString = mockHttpServletResponse.getContentAsString(Charsets.UTF_8)
            assertThat(contentAsString).isEqualTo(asJson(expectedResponseObject))
        }
    }

    fun formattedDateString(date: LocalDate): String? = DateTimeFormatter.ISO_DATE.format(date)
}

data class MockResponseAsserts(
    val expectedContentType: String? = MediaType.APPLICATION_JSON_VALUE,
    val expectedResponseObject: Any? = null,
    val resultMatchers: List<ResultMatcher> = emptyList()
)
