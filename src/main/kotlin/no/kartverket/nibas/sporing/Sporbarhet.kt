package no.kartverket.nibas.sporing

import no.kartverket.nibas.api.v1.common.geojson.metadata.Sporingsinformasjon
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.service.TransactionTimeService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate


interface UserFacade {
    fun getAuthenticatedUser(): String
}

@Component
class DefaultUserFacade(
) : UserFacade {

    override fun getAuthenticatedUser(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            throw IllegalStateException("No authenticated user found")
        }

        return authentication.principal as? String
            ?: throw IllegalStateException("Authenticated principal is not a String")
    }
}

@Component
class SporbarEntityListener(
    private val userFacade: UserFacade,
    private val transactionTimeService: TransactionTimeService
) {
    @PrePersist
    @PreUpdate
    fun setAuditInfo(entity: Any) {
        if (entity is Sporbar) {
            entity.auditInfo.run {
                endretAv = userFacade.getAuthenticatedUser()
                endretdato = transactionTimeService.transactionTime.atZone(ZONE_ID)
            }
        }
    }
    companion object {
        val ZONE_ID = ZoneId.of("Europe/Oslo")
    }
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
)

interface Gyldig {
    // Inklusiv gyldigFra, eksklusiv gyldigTil
    var gyldighet: Gyldighet
    fun gyldigPaaDato(dato: LocalDate): Boolean {
        return gyldighet.gyldigPaaDato(dato)
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

    fun gyldigPaaDato(dato: LocalDate): Boolean {
        return if (!slettet) {
            (dato == gyldigFra || dato.isAfter(gyldigFra)) && (gyldigTil == null || (dato.isBefore(gyldigTil)))
        } else {
            false
        }
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
