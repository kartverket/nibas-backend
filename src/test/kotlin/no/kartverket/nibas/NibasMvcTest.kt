package no.kartverket.nibas

import com.fasterxml.jackson.databind.ObjectMapper
import no.kartverket.nibas.config.WebSecurityConfig
import no.kartverket.nibas.inndelinger.HistorikkService
import no.kartverket.nibas.kodelister.KodelisteService
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders


@ActiveProfiles("dev", "security-off")
@ImportAutoConfiguration(WebSecurityConfig::class)
@WebMvcTest
open class NibasMvcTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: ObjectMapper

    @MockBean
    lateinit var kodelisteServiceMock: KodelisteService

    @MockBean
    lateinit var historikkService: HistorikkService

    fun get(url: String, matcher: ResultMatcher) {
        perform(MockMvcRequestBuilders.get(url), matcher)
    }

    fun get(url: String, responseAsserts: MockResponseAsserts) {
        val mvcResult = perform(MockMvcRequestBuilders.get(url), matchers = responseAsserts.resultMatchers)
        assertResponseObject(mvcResult, responseAsserts)
    }

    fun getWithToken(url: String, token: String, matcher: ResultMatcher) = perform(MockMvcRequestBuilders.get(url).header("Authorization", token), matcher)
    fun put(url: String, expected: ResultMatcher) =
        perform(MockMvcRequestBuilders.put(url), expected)

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

    fun post(url: String, requestObject: Any, expected: ResultMatcher) =
        perform(MockMvcRequestBuilders.post(url).content(asJson(requestObject)), expected)

    fun delete(url: String, responseAsserts: MockResponseAsserts) {
        testDelete(url, responseAsserts)
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
}

data class MockResponseAsserts(
    val expectedContentType: String? = MediaType.APPLICATION_JSON_VALUE,
    val expectedResponseObject: Any? = null,
    val resultMatchers: List<ResultMatcher> = emptyList()
)
