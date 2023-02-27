package no.kartverket.nibas.events

import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class NibasEventPublisher(
    private val nibasEventsService: NibasEventsService,
    private val environment: Environment
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun publishNibasEvent(redigerteFlater: List<EventRad>, inntreffer: LocalDate) {
        val eventPublishingDisabled = environment.activeProfiles.any { it == "eventsDisabled" }

        if (eventPublishingDisabled) { return }
        if (redigerteFlater.isEmpty()) logger.warn("Ingen modifiserte flater funnet, event blir ikke sendt.") //burde denne kaste feil istedet?

        // Sender kun en oppdatering per flate, filtrert på viktighet (EXPIRED > MODIFIED f. eks)
        val eventRader = redigerteFlater.sortedBy { it.eventType }.distinctBy { it.lokalid }
        nibasEventsService.publishEvent(
            NibasEvent(inntreffer = inntreffer, eventRader = eventRader.toSet())
        )
    }
}
