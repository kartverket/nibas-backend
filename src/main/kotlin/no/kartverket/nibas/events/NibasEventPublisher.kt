package no.kartverket.nibas.events

import no.kartverket.nibas.events.EventType.*
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component

@Component
class NibasEventPublisher(
    private val nibasEventsService: NibasEventsService,
    private val environment: Environment
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun publishNibasEvent(redigerteFlater: List<EventRad>, inntreffer: LocalDate): NibasEvent {
        validate(redigerteFlater)

        val eventPublishingDisabled = environment.activeProfiles.any { it == "eventsDisabled" }

        if (redigerteFlater.isEmpty()) logger.warn("Ingen modifiserte flater funnet, event blir ikke sendt.") //burde denne kaste feil istedet?

        // Sender kun en oppdatering per flate, filtrert på viktighet (EXPIRED > MODIFIED f. eks)
        val eventRader = redigerteFlater.sortedBy { it.eventType }.distinctBy { it.lokalid }
        val eventsForPublisering = NibasEvent(inntreffer = inntreffer, eventRader = eventRader.toSet())

        if (!eventPublishingDisabled) {
            nibasEventsService.publishEvent(
                eventsForPublisering)
        }

        return eventsForPublisering
    }

    private fun validate(rader: List<EventRad>) {
        if (rader.isEmpty()) throw PublishEventException.EventFormatException("Event må inneholde minst 1 EventRad")
        // en flate kan ikke oppstå eller fjernes uten en endret flate
        if (rader.size == 1) {
            if (rader[0].eventType == ADDED || rader[0].eventType == EXPIRED)
                throw PublishEventException.EventFormatException("EventRad med type ${rader[0].eventType} kan ikke oppstå alene")
        }
    }
}

sealed class PublishEventException(errorCause: String): RuntimeException("Feil ved publisering av event: $errorCause") {
    class NibasEventsResponseException(statusCode: HttpStatusCode): PublishEventException(statusCode.toString())
    class EventFormatException(errorMsg: String): PublishEventException(errorMsg)
}
