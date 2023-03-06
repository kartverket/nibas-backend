package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import java.io.Serializable
import java.time.LocalDate
import java.util.*
import jakarta.persistence.*

@Table(name = "stemmekrets")
@Entity
class Stemmekrets(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString(),
) : KommunalKrets(Flatetype.STEMMEKRETS), KobletTilGrense {

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

    @OneToMany(mappedBy = "stemmekretsFk", cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<StemmekretsInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "stemmekrets", cascade = [CascadeType.ALL])
    var grenser: MutableSet<StemmekretsGrenseKobling> = Collections.synchronizedSet(mutableSetOf())

    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    lateinit var kommune: Kommune

    @ManyToOne
    @JoinColumn(name = "utledetfra")
    var utledetFra: Stemmekrets? = null

    fun getGrenserAvTyper(grensetyper: Set<Grensetype>, gyldighetsdato: LocalDate = LocalDate.now()): List<GrenseKobling> {
        return grenser
            .filter { it.grense.erRelevantGrenseForTyperOgDato(grensetyper, gyldighetsdato) }
    }

    fun getGrenser(gyldighetsdato: LocalDate = LocalDate.now()): List<GrenseKobling> = getGrenserAvTyper(tillatteGrensetyper(), gyldighetsdato)

    fun oppdaterOmraade() {
        omraade = SosiGeometryFunctions.beregnFlate(getGrenser().toSet())
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

    override fun antallGrenser(): Int = grenser.size

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)
    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { StemmekretsInformasjon(this) }
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
        return "Stemmekrets(id='$id', stemmekretsnummer=$stemmekretsnummer, stemmekretsnavn=$stemmekretsnavn, tellekretsnummer=$tellekretsnummer, " +
            "tellekretsnavn=$tellekretsnavn, valgdistriktsnummer=$valgdistriktsnummer)"
    }

    fun applyMappingFromExistingStemmekrets(gammelStemmekrets: Stemmekrets): Stemmekrets =
        this.apply {
            gammelStemmekrets.gyldighet.gyldigTil?.let { setGyldigTil(it) }
            stemmekretsnummer = gammelStemmekrets.stemmekretsnummer
            stemmekretsnavn = gammelStemmekrets.stemmekretsnavn
            tellekretsnummer = gammelStemmekrets.tellekretsnummer
            tellekretsnavn = gammelStemmekrets.tellekretsnavn
            valgdistriktsnummer = gammelStemmekrets.valgdistriktsnummer
            utledetFra = gammelStemmekrets.utledetFra
            omraade = gammelStemmekrets.omraade
            datafangstdato = gammelStemmekrets.datafangstdato
            foerstedigitaliseringsdato = gammelStemmekrets.foerstedigitaliseringsdato
            posisjon = gammelStemmekrets.posisjon
            opphav = gammelStemmekrets.opphav
        }

    fun applyCascadingOperationsBasedOnExistingStemmekrets (gammelStemmekrets: Stemmekrets) : Stemmekrets {
        gammelStemmekrets.kommune.addStemmekrets(this)

        grenser = gammelStemmekrets.grenser.map { grensekobling ->
            StemmekretsGrenseKobling(
                stemmekrets = this,
                grense = grensekobling.grense,
                grenseKoblingAttributter = grensekobling.grenseKoblingAttributter
            )
        }.toMutableSet()

        oppdaterOmraade()

        informasjonselementer = gammelStemmekrets.informasjonselementer.map { stemmekretsInformasjon ->
            StemmekretsInformasjon(stemmekrets = this).also { it.informasjon = stemmekretsInformasjon.informasjon }
        }.toMutableSet()

        return this
    }

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

@Table(name = "stemmekrets_informasjon")
@Entity
class StemmekretsInformasjon(stemmekrets: Stemmekrets) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "stemmekrets_fk")
    var stemmekretsFk: Stemmekrets? = stemmekrets

    @Column(name = "informasjon")
    override var informasjon: String? = null
}

data class StemmekretsGrenseId(val stemmekrets: String = "", val grense: String = "") : Serializable

