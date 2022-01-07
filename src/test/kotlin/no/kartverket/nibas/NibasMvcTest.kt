package no.kartverket.nibas

import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@WebMvcTest
open class NibasMvcTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: ObjectMapper

    fun get(url: String, matcher: ResultMatcher) = perform(MockMvcRequestBuilders.get(url), matcher)
    fun put(url: String, request: Any, expected: ResultMatcher) =
        perform(MockMvcRequestBuilders.put(url).content(this.mapper.writeValueAsString(request)), expected)

    private fun perform(action: MockHttpServletRequestBuilder, expected: ResultMatcher) {
        mvc.perform(action.contentType(MediaType.APPLICATION_JSON)).andExpect(expected)
    }

    fun <T> any(clazz: Class<T>): T = Mockito.any(clazz)

}