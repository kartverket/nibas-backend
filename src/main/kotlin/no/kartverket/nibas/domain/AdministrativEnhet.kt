package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Sporbar
import org.hibernate.envers.Audited
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import java.time.LocalDate
import java.util.*
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class AdministrativEnhet(
    @Transient override val type: Flatetype,
) : Flate, Sporbar {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

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

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @Column(name = "opphav")
    override var opphav: String? = null

    @Column(name = "produkt")
    var produkt: String? = null

    @Column(name = "versjon")
    var versjon: String? = null

    @Column(name = "gyldigfra")
    override var gyldigFra: LocalDate? = null

    @Column(name = "gyldigtil")
    override var gyldigTil: LocalDate? = null

    @Column(name = "omraadeid")
    var omraadeid: Int? = null

    @Column(name = "originaldatavert")
    var originaldatavert: String? = null

    @Column(name = "kopidato")
    var kopidato: LocalDate? = null

    @Column(name = "datafangstdato")
    override var datafangstdato: LocalDate? = null

    @ManyToOne
    @JoinColumn(name = "land")
    var land: Landkode? = null

    @ManyToOne
    @JoinColumn(name = "nivaanummer")
    var nivaanummer: Nivaanummer? = null

    @Column(name = "navn")
    var navn: String? = null

    @ManyToOne
    @JoinColumn(name = "spraak")
    var spraak: Spraakkode? = null

    @Column(name = "rekkefoelge")
    var rekkefoelge: Int? = null

    @Column(name = "administrativkode")
    var administrativkode: String? = null

    @Column(name = "posisjon")
    var posisjon: Point? = null

    @Column(name = "flate")
    override var flate: MultiPolygon? = null

    @javax.persistence.Transient
    var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    @Suppress("UNCHECKED_CAST")
    fun <T: GrenseKobling> oppdaterSamling(grense: Grense, attributter: GrenseKoblingAttributter, samling: MutableSet<T>, default: GrenseKobling) {
        val kobling: T  = when(val kobling = samling.find { it.getGrense().id == grense.id }) {
            null -> default as T
            else -> kobling
        }

        kobling.oppdaterAttributter(attributter)
        samling.add(kobling)
    }
}
