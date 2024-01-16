package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.hibernate.annotations.Where
import java.io.Serializable
import java.time.LocalDate
import java.util.*
import jakarta.persistence.*

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */

@Table(name = "grunnkrets")
@Entity
class Grunnkrets(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString()
) : StatistiskEnhet(Flatetype.GRUNNKRETS), KobletTilGrense {

    @Column(name = "grunnkretsnummer", nullable = false)
    var grunnkretsnummer: String? = null

    @Column(name = "grunnkretsnavn")
    var grunnkretsnavn: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delomraadeinndeling_fk")
    var delomraadeinndelingFk: Delomraade? = null

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "kommunelokalid", referencedColumnName = "lokalid")
    @Where(clause="flatetype='KOMMUNE'")
    lateinit var kommuneLokalid : UnikLokalid

    @ManyToOne(fetch = FetchType.LAZY)
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

    /**
     * Henter kretsen sin kommune på en gitt dato. Hvis man gir en dato hvor kommunen med gitt lokalid ikke er gyldig, returneres null
     */
    fun getKommune(gyldighetsdato: LocalDate = LocalDate.now()): Kommune? {
        return kommuneLokalid.kommuner.firstOrNull { it.gyldigPaaDato(gyldighetsdato) }
    }

    override fun oppdaterOmraadeOgPosisjon() {
        omraade = SosiGeometryFunctions.beregnFlate(getGrensekoblinger(gyldighetsdato = gyldighet.gyldigFra).toSet())
        if (omraade != null && (posisjon == null || !omraade!!.contains(posisjon))) {
            posisjon = omraade?.interiorPoint
        }
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

    fun getGrensekoblingerAvTyper(grensetyper: Set<Grensetype>, gyldighetsdato: LocalDate = LocalDate.now()): List<GrunnkretsGrenseKobling> {
        return grenser
            .filter { it.grense.harGrenseTypeContainedInSet(grensetyper) }
            .filter { it.grense.gyldigPaaDato(gyldighetsdato) }
    }

    fun getGrensekoblinger(gyldighetsdato: LocalDate = LocalDate.now()): List<GrunnkretsGrenseKobling> {
        return grenser.filter { it.grense.gyldigPaaDato(gyldighetsdato) }
    }

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

    override fun addGrensekobling(grense: Grense, grenseKoblingAttributter: GrenseKoblingAttributter) {
        val grunnkretsGrenseKobling = GrunnkretsGrenseKobling(
            grunnkrets = this,
            grense = grense,
            grenseKoblingAttributter = grenseKoblingAttributter
        )
        //Legger grensekoblingen til i begge steder den hører til, ellers feiler tester
        grenser.add(grunnkretsGrenseKobling)
        grense.grunnkretser.add(grunnkretsGrenseKobling)
    }

    override fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?, grenseKoblingAttributter: GrenseKoblingAttributter) {
        if (!tillatteGrensetyper().contains(grense?.grensetype) && grense != null) {
            throw RuntimeException("Kan ikke oppdatere/legge til ${grense?.grensetype?.name} på enhet Grunnkrets")
        }
        if (grenser.removeIf { it.grense.id == gammelGrense.id }) {
            //Grensa ble tatt vekk fra grunnkretsen. Fjerner grunnkretsen også fra grensa (koblingen fra den andre veien), ellers feiler tester
            gammelGrense.grunnkretser.removeIf { it.grunnkrets.id == this.id }
            if(grense != null) {
                addGrensekobling(grense, grenseKoblingAttributter)
            }
        }
    }

    override fun removeGrensekobling(gammelGrense: Grense) {
        if (grenser.removeIf { it.grense.id == gammelGrense.id }) {
            //Grensa ble tatt vekk fra grunnkretsen. Fjerner grunnkretsen også fra grensa (koblingen fra den andre veien), ellers feiler tester
            gammelGrense.grunnkretser.removeIf { it.grunnkrets.id == this.id }
        }
    }

    fun addInformasjonselementerFrom(informasjonselementer: Set<GrunnkretsInformasjon>) {
        informasjonselementer.forEach { grunnkretsInformasjon ->
            setInfo(grunnkretsInformasjon.informasjon)
        }
    }

    fun applyCascadingOperationsBasedOnExistingGrunnkrets(gammelGrunnkrets: Grunnkrets, gyldigFra: LocalDate = LocalDate.now()): Grunnkrets {
        return this.apply {
            // For eksisterende Grunnkrets sine informasjonselementer (1 i tallet), lag nye koblinger med denne Grunnkrets
            addInformasjonselementerFrom(gammelGrunnkrets.informasjonselementer)

            gammelGrunnkrets.grenser.forEach { gammelGrenseKobling ->
                addGrensekobling(grense = gammelGrenseKobling.grense, grenseKoblingAttributter = gammelGrenseKobling.grenseKoblingAttributter)
            }
        }
    }

    fun applyMappingFromExistingGrunnkrets(gammelGrunnkrets: Grunnkrets): Grunnkrets {
        return this.apply {
            grunnkretsnummer = gammelGrunnkrets.grunnkretsnummer
            grunnkretsnavn = gammelGrunnkrets.grunnkretsnavn
            datafangstdato = gammelGrunnkrets.datafangstdato
            foerstedigitaliseringsdato = gammelGrunnkrets.foerstedigitaliseringsdato
            opphav = gammelGrunnkrets.opphav
            posisjon = gammelGrunnkrets.posisjon
            omraade = gammelGrunnkrets.omraade
            utledetFra = gammelGrunnkrets.utledetFra
        }
    }
}

@Table(name = "grunnkrets_informasjon")
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
