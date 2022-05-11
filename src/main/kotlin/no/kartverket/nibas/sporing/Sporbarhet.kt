package no.kartverket.nibas.sporing

import no.kartverket.nibas.api.v1.common.geojson.metadata.Sporingsinformasjon
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.PreUpdateEvent
import org.hibernate.event.spi.PreUpdateEventListener
import org.hibernate.internal.SessionFactoryImpl
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.annotation.PostConstruct
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EntityManagerFactory


interface UserFacade {
    fun getAuthenticatedUser(): String
}

@Component
class DefaultUserFacade : UserFacade {

    override fun getAuthenticatedUser(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            throw RuntimeException("Ugyldig tilstand. Har ingen innlogget bruker.")
        }
        return authentication.name
    }

}

/**
 * Hibernate event listener som ved oppdatering setter sporbarhetsfelter på entiteter som har Sporbar-egenskapen.
 */
@Component
class AuditPreUpdateEventListener constructor(private val userFacade: UserFacade,
                                              private val jpaEntityManagerFactory: EntityManagerFactory) : PreUpdateEventListener {

    // Litt omstendelig, men må registrere denne event listener i EventListenerRegistry
    @PostConstruct
    private fun postConstruct() {
        jpaEntityManagerFactory.unwrap(SessionFactoryImpl::class.java)
            .serviceRegistry.getService(EventListenerRegistry::class.java)
            .getEventListenerGroup(EventType.PRE_UPDATE)
            .appendListener(this)
    }

    override fun onPreUpdate(event: PreUpdateEvent): Boolean {
        val entity = event.entity
        if (entity is Sporbar) {
            entity.auditInfo.onUpdate(userFacade.getAuthenticatedUser())
        }
        return false
    }

}

interface Sporbar {
    var auditInfo: AuditInfo
}

@Embeddable
data class AuditInfo(
    @Column(name = "oppdateringsdato", nullable = false) var oppdateringsdato: LocalDate = LocalDate.now(),
    @Column(name = "endret_av", nullable = true) var endretAv: String? = null,
) {
    fun onUpdate(user: String) {
        endretAv = user
        oppdateringsdato = LocalDate.now()
    }
}

fun AuditInfo.toSporingsinformasjon(): Sporingsinformasjon {
    return Sporingsinformasjon(
        oppdateringsdato = this.oppdateringsdato,
        endretAv = this.endretAv)
}
