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
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class KommunalKrets(@Transient override val type: Flatetype) : Flate, CommonMetadataProperties, Sporbar {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @Column(name = "objtype")
    var objtype: String? = type.name

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    /**
     * Må være satt og skal aldri endre seg i objektets levetid. Så burde ideelt vært immutable.
     */
    @Column(name = "lokalid", nullable = false, updatable = false)
    override var lokalid: String = UUID.randomUUID().toString()

    @Column(name = "navnerom", nullable = false, updatable = false)
    override var navnerom: String = DEFAULT_NAVNEROM

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "datafangstdato")
    override var datafangstdato: LocalDate? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @Embedded
    var endringsflagg: Endringsflagg? = Endringsflagg()

    @Column(name = "gyldigfra")
    override var gyldigFra: LocalDate? = null

    @Column(name = "gyldigtil")
    override var gyldigTil: LocalDate? = null

    @Column(name = "informasjon")
    var informasjon: String? = null

    @Embedded
    var kopidata: Kopidata? = Kopidata()

    @Column(name = "opphav")
    override var opphav: String? = null

    @Embedded
    var registreringsversjon: Registreringsversjon? = Registreringsversjon()

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "posisjon")
    var posisjon: Point? = null

    @Column(name = "flate")
    override var flate: MultiPolygon? = null

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
