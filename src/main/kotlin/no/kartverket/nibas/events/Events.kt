package no.kartverket.nibas.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.kartverket.nibas.config.AsynchronousSpringEventsConfig
import no.kartverket.nibas.domain.Flate
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component


@Component
@ConditionalOnProperty(value = ["spring.cloud.gcp.pubsub.enabled"], havingValue = "true")
class NibasEventListener constructor(private val messagingGateway: AsynchronousSpringEventsConfig.PubsubOutboundGateway) {

    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())

    @EventListener(value = [NibasEvent::class])
    fun onApplicationEvent(event: NibasEvent) {
        messagingGateway.sendToPubsub(mapper.writeValueAsString(event))
    }
}

@Component
class NibasEventPublisher (private val nibasEventsService: NibasEventsService) {

    fun publishNibasEvent(enhet: Flate, eventType: EventType) {
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
