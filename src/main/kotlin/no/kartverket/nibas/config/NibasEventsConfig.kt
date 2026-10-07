package no.kartverket.nibas.config

import no.kartverket.nibas.events.NibasEventsService
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class NibasEventsConfig (
    private val environment: Environment,
) {
    companion object NibasEventsDefault {
        const val DEFAULT_HOST: String = "http://localhost:8082"
        const val URI: String = "/v1/events"
    }

    @Bean
    fun getNibasEventsService(): NibasEventsService {
        return NibasEventsService(buildWebClient(), URI)
    }

    fun buildWebClient(): WebClient {
        val apiKeyPublisher = environment.getProperty("api.key.publisher")
        val requestId = MDC.get("requestId")

        if(apiKeyPublisher == null && inProduction()) {
            throw IllegalStateException("ApiKey for å publisere events er ikke satt.")
        }

        return WebClient.builder()
            .apply {
                it.baseUrl(environment.getProperty("nibas-events.host", DEFAULT_HOST))
                it.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")

                if (requestId != null) {
                    it.defaultHeader("X-KV-REQUEST-ID", requestId)
                }

                if (apiKeyPublisher != null) {
                    it.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic $apiKeyPublisher")
                }
            }.build()
    }

    fun inProduction(): Boolean {
        return environment.activeProfiles.toList().contains("production")
    }
}
