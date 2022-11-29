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
@Table(name = "kommune")
@Audited
@Entity
class Kommune(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString()
) : AdministrativEnhet(Flatetype.KOMMUNE) {

    @Column(name = "kommunenummer", nullable = false)
    var kommunenummer: String = ""

    @Column(name = "samiskforvaltningsomraade", nullable = false)
    var samiskforvaltningsomraade: Boolean? = false

    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    lateinit var fylke: Fylke

    @OneToMany(mappedBy = "kommuneFk", cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<KommuneInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "kommuneFk")
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    @OneToMany(mappedBy = "kommune", cascade = [CascadeType.ALL])
    var grunnkretser: MutableSet<Grunnkrets> = mutableSetOf()

    @OneToMany(mappedBy = "kommune", cascade = [CascadeType.ALL])
    var stemmekretser: MutableSet<Stemmekrets> = mutableSetOf()

    @ManyToOne
    @JoinColumn(name = "utledetfra")
    var utledetFra: Kommune? = null

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { KommuneInformasjon(this) }
    }

    override fun oppdaterOmraade() {
        omraade = SosiGeometryFunctions.beregnFlate(getGrenser())
    }

    fun getGrenser(): Set<GrenseKobling> {
        return grunnkretser
            .flatMap { it.getGrenserAvTyper(tillatteGrensetyper()) }
            .toSet()
    }

    fun kommunegrenser(): Set<GrenseKobling> {
        return getGrenser().filter { it.grense.grensetype == Grensetype.Kommunegrense }.toSet()
    }

    fun addGrunnkrets(grunnkrets: Grunnkrets) {
        grunnkrets.kommune = this
        grunnkretser.add(grunnkrets)
    }

    fun addStemmekrets(stemmekrets: Stemmekrets) {
        stemmekrets.kommune = this
        stemmekretser.add(stemmekrets)
    }

    private fun tillatteGrensetyper(): Set<Grensetype> = setOf(
        Grensetype.Kommunegrense,
        Grensetype.Fylkesgrense,
        Grensetype.AvtaltAvgrensningslinje,
        Grensetype.Territorialgrense,
        Grensetype.Riksgrense)
}

@Table(name = "kommune_informasjon")
@Audited
@Entity
class KommuneInformasjon(kommune: Kommune) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommuneFk: Kommune? = kommune

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
