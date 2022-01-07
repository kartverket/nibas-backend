package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import java.time.LocalDate
import org.hibernate.envers.Audited
import javax.persistence.*
import kotlin.jvm.Transient

@MappedSuperclass
@Audited
abstract class AdministrativEnhet(
    @Transient val type: AdministrativEnhetType,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "objid", nullable = false)
    val id: Int
) {
    @Column(name = "objtype")
    var objtype: String? = null

    @Column(name = "lokalid", nullable = false)
    var lokalid: String? = null


    @Column(name = "navnerom", nullable = false)
    var navnerom: String? = null


    @Column(name = "versjonid")
    var versjonid: String? = null

    @Column(name = "oppdateringsdato")
    var oppdateringsdato: LocalDate? = null

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @Column(name = "opphav")
    var opphav: String? = null

    @Column(name = "produkt")
    var produkt: String? = null

    @Column(name = "versjon")
    var versjon: String? = null

    @Column(name = "gyldigfra")
    var gyldigfra: LocalDate? = null

    @Column(name = "gyldigtil")
    var gyldigtil: LocalDate? = null

    @Column(name = "omraadeid")
    var omraadeid: Int? = null

    @Column(name = "originaldatavert")
    var originaldatavert: String? = null

    @Column(name = "kopidato")
    var kopidato: LocalDate? = null

    @Column(name = "datafangstdato")
    var datafangstdato: LocalDate? = null

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
    var flate: Polygon? = null

    @javax.persistence.Transient
    var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()
}