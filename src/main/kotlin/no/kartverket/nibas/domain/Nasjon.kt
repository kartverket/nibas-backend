package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.hibernate.envers.Audited
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "nasjon")
@Audited
@Entity
class Nasjon(override val id: String = UUID.randomUUID().toString()) : AdministrativEnhet(Flatetype.NASJON) {

    @OneToMany(mappedBy = "nasjonFk")
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    @OneToMany(mappedBy = "nasjon")
    var informasjonselementer: MutableSet<NasjonInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "nasjon", cascade = [CascadeType.ALL])
    var fylker: MutableSet<Fylke> = mutableSetOf()

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { NasjonInformasjon(this) }
    }

    override fun oppdaterFlate() {
        flate = SosiGeometryFunctions.beregnFlate(getGrenser())
    }

    fun getGrenser(): Set<GrenseKobling> {
        return fylker
            .flatMap { it.kommuner }
            .flatMap { it.grunnkretser }
            .flatMap { it.getGrenserAvTyper(tillatteGrensetyper()) }
            .toSet()
    }

    fun addFylke(fylke: Fylke) {
        fylke.nasjon = this
        fylker.add(fylke)
    }

    private fun tillatteGrensetyper(): Set<Grensetype> = setOf(
        Grensetype.AvtaltAvgrensningslinje,
        Grensetype.Territorialgrense,
        Grensetype.Riksgrense)
}

@Table(name = "nasjon_informasjon")
@Audited
@Entity
class NasjonInformasjon(nasjon: Nasjon) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjon: Nasjon? = nasjon

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
