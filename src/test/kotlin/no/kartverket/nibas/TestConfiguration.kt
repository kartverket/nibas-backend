package no.kartverket.nibas

import com.google.cloud.spring.pubsub.core.PubSubTemplate
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean

@TestConfiguration
class TestConfiguration {
    @MockBean
    private lateinit var pubsubTemplate: PubSubTemplate
}
