package no.kartverket.nibas.config

import com.google.cloud.spring.pubsub.core.PubSubTemplate
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.ServiceActivator

@Configuration
class AsynchronousSpringEventsConfig {

    @Value("\${nibas.pubsub.topic:nibas-events}")
    val pubSubTopic: String = "nibas-events"

    @Bean(name = ["applicationEventMulticaster"])
    fun simpleApplicationEventMulticaster(): ApplicationEventMulticaster {
        val eventMulticaster = SimpleApplicationEventMulticaster()
        eventMulticaster.setTaskExecutor(SimpleAsyncTaskExecutor())
        return eventMulticaster
    }

    @Bean
    @ServiceActivator(inputChannel = "pubsubOutputChannel")
    @ConditionalOnProperty(value = ["spring.cloud.gcp.pubsub.enabled"], havingValue = "true")
    fun messageSender(pubsubTemplate: PubSubTemplate) = PubSubMessageHandler(pubsubTemplate, pubSubTopic)

    @MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
    @ConditionalOnProperty(value = ["spring.cloud.gcp.pubsub.enabled"], havingValue = "true")
    interface PubsubOutboundGateway {
        fun sendToPubsub(text: String)
    }
}
