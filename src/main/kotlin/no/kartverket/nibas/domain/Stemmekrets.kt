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

@Table(name = "stemmekrets")
@Audited
@Entity
class Stemmekrets(override val id: String = UUID.randomUUID().toString()) : KommunalKrets(Flatetype.STEMMEKRETS), KobletTilGrense {

    override var objtype: String? = Flatetype.STEMMEKRETS.name

    @Column(name = "stemmekretsnummer", nullable = false)
    var stemmekretsnummer: String? = null

    @Column(name = "stemmekretsnavn", nullable = false)
    var stemmekretsnavn: String? = null

    @Column(name = "tellekretsnummer")
    var tellekretsnummer: String? = null

    @Column(name = "tellekretsnavn")
    var tellekretsnavn: String? = null

    @Column(name = "valgdistriktsnummer")
    var valgdistriktsnummer: String? = null

    @Column(name = "kommunenummer", nullable = false)
    var kommunenummer: String? = null

    @OneToMany(mappedBy = "stemmekrets", cascade = [CascadeType.ALL])
    var grenser: MutableSet<StemmekretsGrenseKobling> = Collections.synchronizedSet(mutableSetOf())

    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommune: Kommune? = null

    fun getGrenserAvTyper(grensetyper: Set<Grensetype>): Set<GrenseKobling> {
        return grenser
            .filter { grensetyper.contains(it.grense.grensetype) }
            .toSet()
    }

    fun getGrenserAvType(grensetype: Grensetype): Set<GrenseKobling> = getGrenserAvTyper(setOf(grensetype))


    override fun oppdaterFlate() {
        flate = SosiGeometryFunctions.beregnFlate(alleGrenser())
    }

    override fun oppdaterEllerLeggTilGrenseKobling(grenseMedAttributter: GrenseMedAttributter) {
        val grense = grenseMedAttributter.grense
        val attributter = grenseMedAttributter.attributter

        if (!tillatteGrensetyper().contains(grense.grensetype)) {
            throw RuntimeException("Kan ikke oppdatere/legge til ${grense.grensetype.name} på enhet Stemmekrets")
        }

        oppdaterSamling(
            grense = grense,
            attributter = attributter,
            samling = grenser,
            default = StemmekretsGrenseKobling(
                stemmekrets = this,
                grense = grense,
                grenseKoblingAttributter = attributter))

    }

    override fun getInfo(): String? = informasjon
    override fun setInfo(updatedInfo: String?) {
        informasjon = updatedInfo
    }

    fun addGrensekobling(grense: Grense, grenseKoblingAttributter: GrenseKoblingAttributter) {
        grenser.add(
            StemmekretsGrenseKobling(
                stemmekrets = this,
                grense = grense,
                grenseKoblingAttributter = grenseKoblingAttributter
            )
        )
    }

    override fun toString(): String {
        return "Stemmekrets(id='$id', stemmekretsnummer=$stemmekretsnummer, stemmekretsnavn=$stemmekretsnavn, tellekretsnummer=$tellekretsnummer, tellekretsnavn=$tellekretsnavn, valgdistriktsnummer=$valgdistriktsnummer, kommunenummer=$kommunenummer)"
    }

    fun alleGrenser() = getGrenserAvTyper(tillatteGrensetyper())

    private fun tillatteGrensetyper(): Set<Grensetype> = setOf(
        Grensetype.Stemmekretsgrense,
        Grensetype.Kommunegrense,
        Grensetype.Fylkesgrense,
        Grensetype.AvtaltAvgrensningslinje,
        Grensetype.Territorialgrense,
        Grensetype.Riksgrense)


}

/**
 * Koblingstabell mellom Stemmekrets - Grense
 */
@Table(name = "stemmekrets_grense")
@Audited
@Entity
@IdClass(StemmekretsGrenseId::class)
class StemmekretsGrenseKobling(
    @Id
    @ManyToOne
    @JoinColumn(name = "stemmekrets_fk")
    var stemmekrets: Stemmekrets,

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
        return stemmekrets
    }
}

data class StemmekretsGrenseId(val stemmekrets: String = "", val grense: String = "") : Serializable

