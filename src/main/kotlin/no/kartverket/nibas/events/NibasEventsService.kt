package no.kartverket.nibas.events

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

open class NibasEventsService (
    private val nibasEventsWebClient: WebClient, private val publishUri: String
) {
    open fun publishEvent(event: NibasEvent) {
        nibasEventsWebClient.post()
            .uri(publishUri)
            .body(Mono.just(event), NibasEvent::class.java)
            .exchangeToMono {
                if (it.statusCode() == HttpStatus.CREATED) Mono.just(it.statusCode().toString())
                else it.createException().flatMap { exception -> Mono.error(exception) }
            }.block()
    }
}
