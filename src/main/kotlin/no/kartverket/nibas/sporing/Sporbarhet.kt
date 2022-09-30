package no.kartverket.nibas.sporing

import no.kartverket.aut.lib.security.DefaultWebSecurityConfig
import no.kartverket.nibas.api.v1.common.geojson.metadata.Sporingsinformasjon
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.AbstractPreDatabaseOperationEvent
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.PreInsertEvent
import org.hibernate.event.spi.PreInsertEventListener
import org.hibernate.event.spi.PreUpdateEvent
import org.hibernate.event.spi.PreUpdateEventListener
import org.hibernate.internal.SessionFactoryImpl
import org.springframework.core.env.Environment
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
class DefaultUserFacade constructor(private val environment: Environment) : UserFacade {

    override fun getAuthenticatedUser(): String {
        if (startedWithSecurityOff()) {
            return "DUMMY"
        }

        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            throw RuntimeException("Ugyldig tilstand. Har ingen innlogget bruker.")
        }
        return authentication.name
    }

    private fun startedWithSecurityOff(): Boolean {
        return listOf(*environment.activeProfiles).contains(DefaultWebSecurityConfig.SECURITY_OFF_PROFILE_STRING)
    }

}

@Component
class AuditComponent(private val jpaEntityManagerFactory: EntityManagerFactory,
                     private val auditPreInsertEventListener: AuditPreInsertEventListener,
                     private val auditPreUpdateEventListener: AuditPreUpdateEventListener) {
    // Litt omstendelig, men må registrere event listeners i EventListenerRegistry
    @PostConstruct
    @Suppress("UnusedPrivateMember")
    private fun postConstruct() {
        val eventListenerRegistry = jpaEntityManagerFactory
            .unwrap(SessionFactoryImpl::class.java)
            .serviceRegistry
            .getService(EventListenerRegistry::class.java)
        eventListenerRegistry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(auditPreInsertEventListener)
        eventListenerRegistry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(auditPreUpdateEventListener)
    }
}

/**
 * Hibernate event listener som ved oppdatering setter sporbarhetsfelter på entiteter som har Sporbar-egenskapen.
 */
@Component
class AuditPreUpdateEventListener(private val userFacade: UserFacade) : PreUpdateEventListener {
    override fun onPreUpdate(event: PreUpdateEvent): Boolean {
        return event.processSporbarEntity(userFacade)
    }
}

/**
 * Hibernate event listener som ved insert setter sporbarhetsfelter på entiteter som har Sporbar-egenskapen.
 */
@Component
class AuditPreInsertEventListener(private val userFacade: UserFacade) : PreInsertEventListener {
    override fun onPreInsert(event: PreInsertEvent): Boolean {
        return event.processSporbarEntity(userFacade)
    }
}

private fun AbstractPreDatabaseOperationEvent.processSporbarEntity(userFacade: UserFacade): Boolean {
    val entity = this.entity
    if (entity is Sporbar) {
        entity.auditInfo.onUpdate(userFacade.getAuthenticatedUser())
    }
    return false
}

interface Sporbar {
    var auditInfo: AuditInfo
}

interface Laasbar {
    var version: Int
    val id: String
    var objtype: String?
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

interface Gyldig {
    var gyldighet: Gyldighet
}
data class Gyldighet(
    @Column(name = "gyldigfra")
    var gyldigFra: LocalDate = LocalDate.now(),

    @Column(name = "gyldigtil")
    var gyldigTil: LocalDate? = null
)

fun AuditInfo.toSporingsinformasjon(): Sporingsinformasjon {
    return Sporingsinformasjon(
        oppdateringsdato = this.oppdateringsdato,
        endretAv = this.endretAv)
}
