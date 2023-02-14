package no.kartverket.nibas.events

import no.kartverket.nibas.domain.Flate
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class NibasEventPublisher (
    private val nibasEventsService: NibasEventsService,
    private val environment: Environment
) {

    fun publishNibasEvent(enhet: Flate, eventType: EventType) {
        val eventPublishingDisabled = environment.activeProfiles.any { it == "eventsDisabled" }

        if(!eventPublishingDisabled) {
            nibasEventsService.publishEvent(
                NibasEvent(inntreffer = enhet.gyldighet.gyldigFra,
                    eventRader = setOf (
                        EventRad(uuid = enhet.id,
                            eventType = eventType,
                            flateType = enhet.type,
                            lokalid = enhet.lokalid),
                    )
                )
            )
        }
    }
}
