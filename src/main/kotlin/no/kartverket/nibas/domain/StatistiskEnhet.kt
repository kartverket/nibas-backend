package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.StatistiskEnhetType
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class StatistiskEnhet(
    @Transient val type: StatistiskEnhetType,
) : CommonMetadataProperties {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @Column(name = "objtype")
    var objtype: String? = null

    /**
     * Må være satt og skal aldri endre seg i objektets levetid. Så burde ideelt vært immutable.
     */
    @Column(name = "lokalid", nullable = false, updatable = false)
    override var lokalid: String = UUID.randomUUID().toString()

    /**
     * Må være satt ihht standard.
     */
    @Column(name = "navnerom", nullable = false, updatable = false)
    override var navnerom: String = DEFAULT_NAVNEROM

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "datafangstdato")
    override var datafangstdato: LocalDate? = null

    @Column(name = "oppdateringsdato")
    override var oppdateringsdato: LocalDate = LocalDate.now()

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "gyldigfra")
    override var gyldigFra: LocalDate? = null

    @Column(name = "gyldigtil")
    override var gyldigTil: LocalDate? = null

    @Column(name = "informasjon")
    var informasjon: String? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @Embedded
    var endringsflagg: Endringsflagg? = Endringsflagg()

    @Column(name = "opphav")
    override var opphav: String? = null

    @Embedded
    var registreringsversjon: Registreringsversjon? = Registreringsversjon()

    @Embedded
    var kopidata: Kopidata? = Kopidata()

    abstract fun oppdaterFlate()

}
