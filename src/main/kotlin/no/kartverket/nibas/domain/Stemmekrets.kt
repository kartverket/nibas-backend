package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.geometry.toMultiPolygon
import org.hibernate.annotations.SQLRestriction
import java.io.Serializable
import java.time.LocalDate
import java.util.*
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Table(name = "stemmekrets")
@Entity
class Stemmekrets(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString(),
) : KommunalKrets(Flatetype.STEMMEKRETS), KobletTilGrense {

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
    @SQLRestriction("flatetype='KOMMUNE'")
    lateinit var kommuneLokalid : UnikLokalid

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utledetfra")
    var utledetFra: Stemmekrets? = null

    override fun getGrensekoblinger(gyldighetsdato: LocalDate): Set<StemmekretsGrenseKobling> {
        return grenser.filter { it.grense.gyldigPaaDato(gyldighetsdato) }.toSet()
    }

    override fun getFlateInformasjon(): FlateInformasjon {
        return FlateInformasjon(type, "$stemmekretsnummer $stemmekretsnavn")
    }

    /**
     * Henter kretsen sin kommune på en gitt dato. Hvis man gir en dato hvor kommunen med gitt lokalid ikke er gyldig, returneres null
     */
    fun getKommune(gyldighetsdato: LocalDate = LocalDate.now()): Kommune? {
        return kommuneLokalid.kommuner.firstOrNull { it.gyldigPaaDato(gyldighetsdato) }
    }

    override fun oppdaterOmraadeOgPosisjon() {
        val grenserForFlate = getGrensekoblinger(gyldighetsdato = gyldighet.gyldigFra)
        omraade = getGrenserIRiktigRekkefoelge(grenserForFlate.map { it.grense.grense }).toMultiPolygon()
        if (omraade != null && (posisjon == null || !omraade!!.contains(posisjon))) {
            posisjon = omraade?.interiorPoint
        }
    }

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)
    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { StemmekretsInformasjon(this) }
    }

    override fun addGrensekobling(grense: Grense) {
        val stemmekretsGrenseKobling = StemmekretsGrenseKobling(
            stemmekrets = this,
            grense = grense
        )
        //Legger grensekoblingen til i begge steder den hører til, ellers feiler tester
        grenser.add(stemmekretsGrenseKobling)
        grense.stemmekretser.add(stemmekretsGrenseKobling)
    }

    override fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?) {
        if (!tillatteGrensetyper().contains(grense?.grensetype) && grense != null) {
            throw RuntimeException("Kan ikke oppdatere/legge til ${grense.grensetype.name} på enhet Stemmekrets")
        }
        if (grenser.removeIf { it.grense.id == gammelGrense.id }) {
            //Grensa ble tatt vekk fra stemmekretsen. Fjerner stemmekretsen også fra grensa (koblingen fra den andre veien), ellers feiler tester
            gammelGrense.stemmekretser.removeIf { it.stemmekrets.id == this.id }
            //skal bare adde om det grense != null
            if(grense != null) {
                addGrensekobling(grense)
            }
        }
    }

    override fun removeGrensekobling(gammelGrense: Grense) {
        if (grenser.removeIf { it.grense.id == gammelGrense.id }) {
            //Grensa ble tatt vekk fra stemmekretsen. Fjerner stemmekretsen også fra grensa (koblingen fra den andre veien), ellers feiler tester
            gammelGrense.stemmekretser.removeIf { it.stemmekrets.id == this.id }
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

    fun copyInformasjonselementerFromExistingStemmekrets(gammelStemmekrets: Stemmekrets, gyldigFra: LocalDate = LocalDate.now()): Stemmekrets {
        informasjonselementer = gammelStemmekrets.informasjonselementer.map { stemmekretsInformasjon ->
            StemmekretsInformasjon(stemmekrets = this).also { it.informasjon = stemmekretsInformasjon.informasjon }
        }.toMutableSet()

        return this
    }

    fun copyGrenserFromExistingStemmekrets(gammelStemmekrets: Stemmekrets, gyldigFra: LocalDate = LocalDate.now()): Stemmekrets {
        gammelStemmekrets.getGrensekoblinger(gyldigFra).forEach { gammelGrenseKobling ->
            addGrensekobling(grense = gammelGrenseKobling.grense)
        }
        return this
    }

    companion object {
         fun tillatteGrensetyper(): Set<Grensetype> = setOf(
            Grensetype.Stemmekretsgrense,
            Grensetype.Kommunegrense,
            Grensetype.Fylkesgrense,
            Grensetype.AvtaltAvgrensningslinje,
            Grensetype.Territorialgrense,
            Grensetype.Riksgrense
        )
    }
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
) : GrenseKobling {
    override fun getEnhetKobletTilGrense(): KobletTilGrense {
        return stemmekrets
    }

    override fun getLokalIdForKrets(): String {
        return stemmekrets.lokalid
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

