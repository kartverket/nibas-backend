package no.kartverket.nibas.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.kartverket.nibas.config.AsynchronousSpringEventsConfig
import no.kartverket.nibas.domain.Flate
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
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
class NibasEventPublisher constructor(
    val aep: ApplicationEventPublisher
) {
    @Value("\${spring.cloud.gcp.pubsub.enabled:true}")
    val pubSubEnablet: Boolean = true

    fun publishNibasEvent(enhet: Flate, eventType: EventType) {
        //Må ha sjekk om pubsub er på her, kan ikke ha ConditionalOnProperty på klassen
        if (pubSubEnablet) {
            aep.publishEvent(
                NibasEvent(
                    type = eventType,
                    target = EventTarget.fromFlatetype(enhet.type),
                    lokalid = enhet.lokalid,
                    gyldigFra = enhet.gyldighet.gyldigFra,
                    gyldigTil = enhet.gyldighet.gyldigTil
                )
            )
        }
    }
}
