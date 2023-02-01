package no.kartverket.nibas

import com.google.cloud.spring.pubsub.core.PubSubTemplate
import no.kartverket.nibas.events.NibasEventPublisher
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean

@TestConfiguration
class TestConfiguration {
    @MockBean
    private lateinit var pubsubTemplate: PubSubTemplate

    @MockBean
    private lateinit var nibasEventPublisher: NibasEventPublisher
}
