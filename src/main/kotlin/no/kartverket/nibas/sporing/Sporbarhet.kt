package no.kartverket.nibas.sporing

import no.kartverket.aut.lib.security.DefaultWebSecurityConfig
import no.kartverket.nibas.api.v1.common.geojson.metadata.Sporingsinformasjon
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
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
import java.time.ZonedDateTime
import jakarta.annotation.PostConstruct
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EntityManagerFactory


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
            .getService(EventListenerRegistry::class.java) ?: throw RuntimeException("Fant ingen service av typen EventListenerRegistry.")

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
}

@Embeddable
data class AuditInfo(
    @Column(name = "endretdato", nullable = false) var endretdato: ZonedDateTime = ZonedDateTime.now(),
    @Column(name = "endretav", nullable = false) var endretAv: String = "import",
    @Column(name = "typeendring", nullable = false) var typeEndring: Endringstype = Endringstype.IMPORT
) {
    fun onUpdate(user: String) {
        endretAv = user
        endretdato = ZonedDateTime.now()
    }
}

interface Gyldig {
    // Inklusiv gyldigFra, eksklusiv gyldigTil
    var gyldighet: Gyldighet
    fun gyldigPaaDato(dato: LocalDate): Boolean {
        return if (!gyldighet.slettet) {
            val dagenEtterDato = dato.plusDays(1)
            val gyldigTil = gyldighet.gyldigTil
            gyldighet.gyldigFra.isBefore(dagenEtterDato) && (gyldigTil == null || gyldigTil.isAfter(dato))
        } else {
            false
        }
    }

    fun setGyldigTil(gyldigTil: LocalDate) {
        this.gyldighet = this.gyldighet.setGyldigTil(gyldigTil)
    }

    fun setGyldigFra(nyGyldigFra: LocalDate) {
        this.gyldighet = this.gyldighet.setGyldigFra(nyGyldigFra)
    }

    fun setSlettet() {
        this.gyldighet = this.gyldighet.setSlettet()
    }
}

/**
 * Grunnen til å ha både gyldigFra og gyldigTil er at det kan være kjekt å kunne evaluere om en rad er
 * Gyldig.gyldigPaaTidspunkt uten å måtte evaluere flere rader.
 */
@Embeddable
data class Gyldighet(
    @Column(name = "gyldigfra", nullable = false)
    val gyldigFra: LocalDate = LocalDate.now(),

    @Column(name = "gyldigtil")
    val gyldigTil: LocalDate? = null,

    /**
     * Representerer "logisk slettet".
     */
    @Column(name = "slettet", nullable = false)
    val slettet: Boolean = false
) {

    // Kjører alltid en validering etter at objektets konstruktør er kalt
    init {
        validerIntervall()
    }

    fun setSlettet(): Gyldighet {
        return this.copy(slettet = true)
    }

    fun setGyldigFra(nyGyldigFra: LocalDate): Gyldighet {
        return this.copy(gyldigFra = nyGyldigFra)
            .apply { validerIntervall() }
    }

    fun setGyldigTil(gyldigTil: LocalDate): Gyldighet {
        // Forhindrer bruker i å sette en ugyldig til-dato som skaper et ugyldig intervall
        if (gyldigFra.isEqual(gyldigTil)) {
            return setSlettet()
        }
        return this.copy(
            gyldigTil = gyldigTil
        ).apply { validerIntervall() }
    }

    private fun validerIntervall() {
        if (!this.isValidInterval()) {
            throw ResponseException.BadRequestException("Gyldighetsintervall er ugyldig: $this")
        }
    }

    /**
     * Eksempler på gyldige intervall (gyldigFra inklusiv, gyldigTil eksklusiv):
     * - [1.8.22, 16.10.22)
     * - [1.8.22, null)
     *
     * Ugyldig:
     * - [1.8.22, 1.8.22) -> Tomt intervall, må være minst 1 dag
     */
    private fun isValidInterval(): Boolean {
        val gyldigTil = this.gyldigTil
        return gyldigTil == null || gyldigTil.isAfter(this.gyldigFra)
    }

    fun overlapperMed(annenGyldighet: Gyldighet): Boolean {
        //Hvis en av gyldighetene har slettetflagget satt, finnes egentlig ikke gyldigheten, og overlapper derfor ikke
        if (this.slettet || annenGyldighet.slettet) {
            return false
        }
        //Hvis en av gyldighetene har ugyldig intervall, er det ikke noen vits å teste mer. Det vil isåfall komme valideringsfeil fra et annet sted
        if (!this.isValidInterval() || !annenGyldighet.isValidInterval()) {
            return false
        }

        return this.gyldigFra.isBefore(annenGyldighet.gyldigTil ?: LocalDate.MAX) &&
            annenGyldighet.gyldigFra.isBefore(this.gyldigTil ?: LocalDate.MAX)
    }

    override fun toString(): String {
        return "Gyldighet [gyldigFra=$gyldigFra, gyldigTil=$gyldigTil, slettet=$slettet]"
    }
}

fun AuditInfo.toSporingsinformasjon(): Sporingsinformasjon {
    return Sporingsinformasjon(oppdateringsdato = this.endretdato)
}
