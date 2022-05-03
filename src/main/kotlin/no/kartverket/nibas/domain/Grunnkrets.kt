package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.StatistiskEnhetType
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.hibernate.envers.Audited
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */

@Table(name = "grunnkrets")
@Audited
@Entity
class Grunnkrets(override val id: String = UUID.randomUUID().toString()) :
    StatistiskEnhet(StatistiskEnhetType.GRUNNKRETS) {

    @Column(name = "posisjon")
    var posisjon: Point? = null

    @Column(name = "flate")
    var flate: MultiPolygon? = null

    @Column(name = "grunnkretsnummer", nullable = false)
    var grunnkretsnummer: String? = null

    @Column(name = "grunnkretsnavn")
    var grunnkretsnavn: String? = null

    @Column(name = "kommunenummer", nullable = false)
    var kommunenummer: String? = null

    @ManyToOne
    @JoinColumn(name = "delomraadeinndeling_fk")
    var delomraadeinndelingFk: Delomraade? = null

    @OneToMany(mappedBy = "grunnkrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var grunnkretsgrenser: MutableSet<GrunnkretsGrunnkretsgrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "grunnkrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var delomraadegrenser: MutableSet<GrunnkretsDelomraadegrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "grunnkrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var kommunegrenser: MutableSet<GrunnkretsKommunegrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "grunnkrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var fylkesgrenser: MutableSet<GrunnkretsFylkesgrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "grunnkrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var avtaltavgrensningslinjer: MutableSet<GrunnkretsAvtaltavgrensningslinje> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "grunnkrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var territorialgrenser: MutableSet<GrunnkretsTerritorialgrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "grunnkrets", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var riksgrenser: MutableSet<GrunnkretsRiksgrense> = Collections.synchronizedSet(mutableSetOf())

    @OneToMany(mappedBy = "grunnkretsFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<GrunnkretsInformasjon> = mutableSetOf()

    override fun getInfo(): String? {
        return doGetInfoFromSingletonCollection(informasjonselementer)
    }

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { GrunnkretsInformasjon(this) }
    }

    override fun oppdaterFlate() {
        flate = SosiGeometryFunctions.beregnFlate(grunnkretsgrenser
                + delomraadegrenser
                + kommunegrenser
                + fylkesgrenser
                + avtaltavgrensningslinjer
                + territorialgrenser
                + riksgrenser)
    }

    override fun toString(): String {
        return "Grunnkrets(id='$id', grunnkretsnummer=$grunnkretsnummer, grunnkretsnavn=$grunnkretsnavn, kommunenummer=$kommunenummer)"
    }

}

@Table(name = "grunnkrets_informasjon")
@Audited
@Entity
class GrunnkretsInformasjon(grunnkrets: Grunnkrets) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "grunnkrets_fk")
    var grunnkretsFk: Grunnkrets? = grunnkrets

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
