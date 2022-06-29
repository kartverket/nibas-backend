package no.kartverket.nibas

import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders


@ActiveProfiles("dev", "security-off")
@WebMvcTest
open class NibasMvcTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: ObjectMapper

    fun get(url: String, matcher: ResultMatcher) = perform(MockMvcRequestBuilders.get(url), matcher)
    fun getWithToken(url: String, token: String, matcher: ResultMatcher) = perform(MockMvcRequestBuilders.get(url).header("Authorization", token), matcher)
    fun put(url: String, expected: ResultMatcher) =
        perform(MockMvcRequestBuilders.put(url), expected)

    fun put(url: String, request: Any, expected: ResultMatcher) =
        perform(MockMvcRequestBuilders.put(url).content(this.mapper.writeValueAsString(request)), expected)

    fun post(url: String, request: Any, vararg matchers: ResultMatcher) =
        perform(MockMvcRequestBuilders.post(url).content(this.mapper.writeValueAsString(request)), matchers = matchers)

    private fun perform(action: MockHttpServletRequestBuilder, vararg matchers: ResultMatcher) {
        val resultActions = mvc.perform(action.contentType(MediaType.APPLICATION_JSON))
        matchers.forEach { resultActions.andExpect(it) }
    }

    fun <T> any(clazz: Class<T>): T = Mockito.any(clazz)

}
