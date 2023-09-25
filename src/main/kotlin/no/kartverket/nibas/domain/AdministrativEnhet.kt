package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.sporing.Laasbar
import no.kartverket.nibas.sporing.Sporbar
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version

@MappedSuperclass
abstract class AdministrativEnhet(
    @Transient override val type: Flatetype,
) : Flate, Sporbar, Laasbar {

    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @Column(name = "lokalid", nullable = false, updatable = false)
    override val lokalid: String = UUID.randomUUID().toString()

    @Embedded
    override var auditInfo: AuditInfo = AuditInfo()

    @Embedded
    override var gyldighet: Gyldighet = Gyldighet()

    @Version
    override var version: Int = 1

    @Column(name = "objtype")
    var objtype: String? = null

    @Column(name = "opphav")
    override var opphav: String? = null

    @Column(name = "datafangstdato")
    override var datafangstdato: ZonedDateTime? = null

    @Column(name = "posisjon")
    var posisjon: Point? = null

    @Column(name = "omraade")
    override var omraade: MultiPolygon? = null

    @Column(name = "foerstedigitaliseringsdato")
    override var foerstedigitaliseringsdato: ZonedDateTime = ZonedDateTime.now()

    fun getAdministrativenhetNavn(gyldighetsdato: LocalDate = LocalDate.now()): List<Administrativenhetnavn> = emptyList()
}
