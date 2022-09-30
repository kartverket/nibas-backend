package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
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
import javax.persistence.Version

@MappedSuperclass
@Audited
abstract class StatistiskEnhet(@Transient override val type: Flatetype) : Flate, Sporbar, Laasbar {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Embedded
    override var gyldighet: Gyldighet = Gyldighet()

    @Version
    override var version: Int = 1

    @Column(name = "objtype")
    override var objtype: String? = null

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

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "informasjon")
    var informasjon: String? = null

    @Embedded
    var endringsflagg: Endringsflagg? = Endringsflagg()

    @Column(name = "opphav")
    override var opphav: String? = null

    @Embedded
    override var registreringsversjon: Registreringsversjon? = Registreringsversjon()

    @Embedded
    override var kopidata: Kopidata? = Kopidata()

    @Column(name = "posisjon")
    var posisjon: Point? = null

    @Column(name = "flate")
    override var flate: MultiPolygon? = null

    @Suppress("UNCHECKED_CAST")
    fun <T : GrenseKobling> oppdaterSamling(grense: Grense, attributter: GrenseKoblingAttributter, samling: MutableSet<T>, default: GrenseKobling) {
        val kobling: T = when (val kobling = samling.find { it.grense.id == grense.id }) {
            null -> default as T
            else -> kobling
        }

        kobling.oppdaterAttributter(attributter)
        samling.add(kobling)
    }

}
