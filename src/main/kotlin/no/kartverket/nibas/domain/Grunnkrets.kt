package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.hibernate.envers.Audited
import java.io.Serializable
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
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
class Grunnkrets(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString(),
) : StatistiskEnhet(Flatetype.GRUNNKRETS), KobletTilGrense {

    @Column(name = "grunnkretsnummer", nullable = false)
    var grunnkretsnummer: String? = null

    @Column(name = "grunnkretsnavn")
    var grunnkretsnavn: String? = null

    @ManyToOne
    @JoinColumn(name = "delomraadeinndeling_fk")
    var delomraadeinndelingFk: Delomraade? = null

    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    lateinit var kommune: Kommune

    @ManyToOne
    @JoinColumn(name = "utledetfra")
    var utledetFra: Grunnkrets? = null

    @OneToMany(mappedBy = "grunnkrets", cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<GrunnkretsInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "grunnkrets", cascade = [CascadeType.ALL])
    var grenser: MutableSet<GrunnkretsGrenseKobling> = Collections.synchronizedSet(mutableSetOf())

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { GrunnkretsInformasjon(this) }
    }

    override fun oppdaterFlate() {
        flate = SosiGeometryFunctions.beregnFlate(getGrenserAvTyper(tillatteGrensetyper()))
    }

    override fun oppdaterEllerLeggTilGrenseKobling(grenseMedAttributter: GrenseMedAttributter) {
        val grense = grenseMedAttributter.grense
        val attributter = grenseMedAttributter.attributter
        if (!tillatteGrensetyper().contains(grense.grensetype)) {
            throw RuntimeException("Kan ikke oppdatere/legge til ${grense.grensetype.name} på enhet Grunnkrets")
        }
        oppdaterSamling(
            grense = grense,
            attributter = attributter,
            samling = grenser,
            default = GrunnkretsGrenseKobling(grunnkrets = this, grense = grense, grenseKoblingAttributter = attributter))
    }

    override fun antallGrenser(): Int = grenser.size

    fun getGrenserAvTyper(grensetyper: Set<Grensetype>): Set<GrenseKobling> {
        return grenser
            .filter { grensetyper.contains(it.grense.grensetype) }
            .toSet()
    }

    fun getGrenserAvType(grensetype: Grensetype): Set<GrenseKobling> = getGrenserAvTyper(setOf(grensetype))


    override fun toString(): String {
        return "Grunnkrets(id='$id', grunnkretsnummer=$grunnkretsnummer, grunnkretsnavn=$grunnkretsnavn, gyldighet: $gyldighet)"
    }

    private fun tillatteGrensetyper(): Set<Grensetype> = setOf(
        Grensetype.Grunnkretsgrense,
        Grensetype.Delomraadegrense,
        Grensetype.Kommunegrense,
        Grensetype.Fylkesgrense,
        Grensetype.AvtaltAvgrensningslinje,
        Grensetype.Territorialgrense,
        Grensetype.Riksgrense)

    fun addGrensekobling(grense: Grense, grenseKoblingAttributter: GrenseKoblingAttributter) {
        grenser.add(
            GrunnkretsGrenseKobling(
                grunnkrets = this,
                grense = grense,
                grenseKoblingAttributter = grenseKoblingAttributter
            )
        )
    }

    fun addGrenseKoblingerFrom(grensekoblinger: Set<GrunnkretsGrenseKobling>) {
        grensekoblinger.forEach {
            addGrensekobling(it.grense, it.grenseKoblingAttributter)
        }
    }

    fun addInformasjonselementerFrom(informasjonselementer: Set<GrunnkretsInformasjon>) {
        informasjonselementer.forEach { grunnkretsInformasjon ->
            setInfo(grunnkretsInformasjon.informasjon)
        }
    }

    fun kommunegrenser(): Set<GrenseKobling> = getGrenserAvType(Grensetype.Kommunegrense)

    fun fylkesgrenser(): Set<GrenseKobling> = getGrenserAvType(Grensetype.Fylkesgrense)

    fun riksgrenser(): Set<GrenseKobling> = getGrenserAvType(Grensetype.Riksgrense)

    fun grunnkretsgrenser(): Set<GrenseKobling> = getGrenserAvType(Grensetype.Grunnkretsgrense)

    fun delomraadegrenser(): Set<GrenseKobling> = getGrenserAvType(Grensetype.Delomraadegrense)

    fun territorialgrenser(): Set<GrenseKobling> = getGrenserAvType(Grensetype.Territorialgrense)

    fun avtaltavgrensningslinjer(): Set<GrenseKobling> = getGrenserAvType(Grensetype.AvtaltAvgrensningslinje)

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
    var grunnkrets: Grunnkrets? = grunnkrets

    @Column(name = "informasjon")
    override var informasjon: String? = null
}

/**
 * Koblingstabell mellom Grunnkrets - Grense.
 */
@Table(name = "grunnkrets_grense")
@Audited
@Entity
@IdClass(GrunnkretsGrenseId::class)
class GrunnkretsGrenseKobling(
    @Id
    @ManyToOne
    @JoinColumn(name = "grunnkrets_fk")
    var grunnkrets: Grunnkrets,

    @Id
    @ManyToOne
    @JoinColumn(name = "grense_fk")
    override var grense: Grense,

    @Embedded
    override var grenseKoblingAttributter: GrenseKoblingAttributter
) : GrenseKobling {
    override fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter) {
        grenseKoblingAttributter = grenseKoblingAttributter.oppdater(nyeAttributter)
    }

    override fun getEnhetKobletTilGrense(): KobletTilGrense {
        return grunnkrets
    }
}

data class GrunnkretsGrenseId(val grunnkrets: String = "", val grense: String = "") : Serializable
