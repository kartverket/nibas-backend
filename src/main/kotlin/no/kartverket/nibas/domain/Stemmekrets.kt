package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.hibernate.annotations.Where
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "kommunelokalid", referencedColumnName = "lokalid")
    @Where(clause="flatetype='KOMMUNE'")
    lateinit var kommuneLokalid : UnikLokalid

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utledetfra")
    var utledetFra: Stemmekrets? = null

    fun getGrenserAvTyper(grensetyper: Set<Grensetype>, gyldighetsdato: LocalDate = LocalDate.now()): List<StemmekretsGrenseKobling> {
        //Denne kan returnere flere grensekoblinger som ender opp med å peke på samme grense (via neste versjon)
        return grenser
            .filter { it.grense.erRelevantGrenseForTyperOgDato(grensetyper, gyldighetsdato) }
    }

    fun getGrenser(gyldighetsdato: LocalDate = LocalDate.now()): List<StemmekretsGrenseKobling> = getGrenserAvTyper(tillatteGrensetyper(), gyldighetsdato)

    /**
     * Henter kretsen sin kommune på en gitt dato. Hvis man gir en dato hvor kommunen med gitt lokalid ikke er gyldig, returneres null
     */
    fun getKommune(gyldighetsdato: LocalDate = LocalDate.now()): Kommune? {
        return kommuneLokalid.kommuner.firstOrNull { it.gyldigPaaDato(gyldighetsdato) }
    }

    override fun oppdaterOmraadeOgPosisjon() {
        omraade = SosiGeometryFunctions.beregnFlate(getGrenser(gyldighetsdato = gyldighet.gyldigFra).toSet(), gyldighet.gyldigFra)
        if (omraade != null && (posisjon == null || !omraade!!.contains(posisjon))) {
            posisjon = omraade?.interiorPoint
        }
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
        val stemmekretsGrenseKobling = StemmekretsGrenseKobling(
            stemmekrets = this,
            grense = grense,
            grenseKoblingAttributter = grenseKoblingAttributter
        )
        //Legger grensekoblingen til i begge steder den hører til, ellers feiler tester
        grenser.add(stemmekretsGrenseKobling)
        grense.stemmekretser.add(stemmekretsGrenseKobling)
    }

    override fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?, grenseKoblingAttributter: GrenseKoblingAttributter) {
        if (!tillatteGrensetyper().contains(grense?.grensetype) && grense != null) {
            throw RuntimeException("Kan ikke oppdatere/legge til ${grense?.grensetype?.name} på enhet Stemmekrets")
        }
        if (grenser.removeIf { it.grense.id == gammelGrense.id }) {
            //Grensa ble tatt vekk fra stemmekretsen. Fjerner stemmekretsen også fra grensa (koblingen fra den andre veien), ellers feiler tester
            gammelGrense.stemmekretser.removeIf { it.stemmekrets.id == this.id }
            //skal bare adde om det grense != null
            if(grense != null) {
                addGrensekobling(grense, grenseKoblingAttributter)
            }
        }
    }

    override fun toString(): String {
        return "Stemmekrets(id='$id', stemmekretsnummer=$stemmekretsnummer, stemmekretsnavn=$stemmekretsnavn, tellekretsnummer=$tellekretsnummer, " +
            "tellekretsnavn=$tellekretsnavn, valgdistriktsnummer=$valgdistriktsnummer)"
    }

    fun applyMappingFromExistingStemmekrets(gammelStemmekrets: Stemmekrets): Stemmekrets =
        this.apply {
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

    fun applyCascadingOperationsBasedOnExistingStemmekrets (gammelStemmekrets: Stemmekrets, gyldigFra: LocalDate = LocalDate.now()) : Stemmekrets {
        gammelStemmekrets.grenser.forEach { gammelGrenseKobling ->
            addGrensekobling(grense = gammelGrenseKobling.grense, grenseKoblingAttributter = gammelGrenseKobling.grenseKoblingAttributter)
        }

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

