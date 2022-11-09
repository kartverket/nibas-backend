package no.kartverket.nibas.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.kartverket.nibas.config.AsynchronousSpringEventsConfig
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.inndelinger.HistorikkService
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.PostCommitDeleteEventListener
import org.hibernate.event.spi.PostCommitInsertEventListener
import org.hibernate.event.spi.PostCommitUpdateEventListener
import org.hibernate.event.spi.PostDeleteEvent
import org.hibernate.event.spi.PostInsertEvent
import org.hibernate.event.spi.PostUpdateEvent
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.persister.entity.EntityPersister
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.logging.Logger
import javax.annotation.PostConstruct
import javax.persistence.EntityManagerFactory


@Component
@ConditionalOnProperty(value = ["spring.cloud.gcp.pubsub.enabled"], havingValue = "true")
class NibasEventListener constructor(private val messagingGateway: AsynchronousSpringEventsConfig.PubsubOutboundGateway) {

    @Value("\${spring.cloud.gcp.pubsub.enabled:true}")
    val pubSubEnabled: Boolean = true

    val log = Logger.getLogger(this::class.java.name)

    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    @EventListener(value = [NibasEvent::class])
    fun onApplicationEvent(event: NibasEvent) {
        if(pubSubEnabled) {
            messagingGateway.sendToPubsub(mapper.writeValueAsString(event))
        } else {
            log.fine("PubSub disabled - not sending event for ${event.id} - ${event.type} - ${event.target}")
        }
    }
}

@Component
class NibasEventPublisher constructor(
    val aep: ApplicationEventPublisher,
    val historikkService: HistorikkService
) {

    fun publishNibasEvent(enhet: Flate, eventType: EventType = EventType.MODIFIED) {
        aep.publishEvent(
            NibasEvent(
                type = eventType,
                target = EventTarget.fromFlatetype(enhet.type),
                id = enhet.id,
                revision = historikkService.hentGjeldendeRevisjonsId(enhet.id, enhet::class.java)
            )
        )
    }
}

@Component
class HibernateEntitySavesListener constructor(
    private val nibasEventPublisher: NibasEventPublisher,
    private val jpaEntityManagerFactory: EntityManagerFactory
) : PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {
    override fun requiresPostCommitHanding(persister: EntityPersister?): Boolean = true
    override fun requiresPostCommitHandling(persister: EntityPersister?): Boolean = true

    override fun onPostDelete(event: PostDeleteEvent) = publish(event.entity, EventType.DELETED)
    @Suppress("EmptyFunctionBlock")
    override fun onPostDeleteCommitFailed(event: PostDeleteEvent) {}

    override fun onPostUpdate(event: PostUpdateEvent) = publish(event.entity, EventType.MODIFIED)
    @Suppress("EmptyFunctionBlock")
    override fun onPostUpdateCommitFailed(event: PostUpdateEvent) {}

    override fun onPostInsert(event: PostInsertEvent) = publish(event.entity, EventType.ADDED)
    @Suppress("EmptyFunctionBlock")
    override fun onPostInsertCommitFailed(event: PostInsertEvent) {}

    private fun publish(entity: Any, eventType: EventType) {
        if(entity is Flate){
            nibasEventPublisher.publishNibasEvent(entity, eventType)
        }
    }

    @PostConstruct
    @Suppress("UnusedPrivateMember")
    private fun postConstruct() {
        val eventListenerRegistry = jpaEntityManagerFactory.unwrap(SessionFactoryImpl::class.java)
            .serviceRegistry.getService(EventListenerRegistry::class.java)

        eventListenerRegistry.appendListeners(org.hibernate.event.spi.EventType.POST_COMMIT_INSERT, this)
        eventListenerRegistry.appendListeners(org.hibernate.event.spi.EventType.POST_COMMIT_UPDATE, this)
        eventListenerRegistry.appendListeners(org.hibernate.event.spi.EventType.POST_COMMIT_DELETE, this)
    }
}
