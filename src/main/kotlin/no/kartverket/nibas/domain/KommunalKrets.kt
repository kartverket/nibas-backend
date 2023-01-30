package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import no.kartverket.nibas.sporing.Sporbar
import org.hibernate.envers.Audited
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import javax.persistence.Version

@MappedSuperclass
@Audited
abstract class KommunalKrets(
    @Transient override val type: Flatetype
) : Flate, Sporbar, Laasbar {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @Column(name = "lokalid", nullable = false, updatable = false)
    override val lokalid: String = UUID.randomUUID().toString()

    @Column(name = "objtype")
    var objtype: String? = type.name

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Embedded
    override var gyldighet: Gyldighet = Gyldighet()

    override var version: Int = 1

    @Column(name = "datafangstdato")
    override var datafangstdato: ZonedDateTime? = null

    @Column(name = "opphav")
    override var opphav: String? = null

    @Column(name = "posisjon")
    var posisjon: Point? = null

    @Column(name = "omraade")
    override var omraade: MultiPolygon? = null

    @Column(name = "foerstedigitaliseringsdato")
    override var foerstedigitaliseringsdato: ZonedDateTime = ZonedDateTime.now()

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
