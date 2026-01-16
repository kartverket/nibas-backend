package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.geometry.toMultiPolygon
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
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

@Table(name = "bopliktomraade")
@Entity
class Bopliktomraade(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString()
) : InndelingMedDeltGeometri(Flatetype.BOPLIKTOMRAADE), KobletTilGrense {

    @Column(name = "navn", nullable = false)
    lateinit var navn: String

    @Column(name = "nummer", nullable = false)
    lateinit var nummer: String

    @Column(name = "informasjon")
    var informasjon: String? = null

    @Column(name = "delvis_boplikt")
    var delvisBoplikt: Boolean = false

    @Column(name = "forskriftsreferanse")
    var forskriftsreferanse: String? = null

    @Column(name = "url")
    var url: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kommunelokalid", referencedColumnName = "lokalid")
    @SQLRestriction("flatetype='KOMMUNE'")
    lateinit var kommuneLokalid: UnikLokalid

    override fun getInfo(): String? = informasjon
    override fun setInfo(updatedInfo: String?) { informasjon = updatedInfo }

    fun getKommune(gyldighetsdato: LocalDate = LocalDate.now()): Kommune? {
        return kommuneLokalid.kommuner.firstOrNull { it.gyldigPaaDato(gyldighetsdato) }
    }

    @OneToMany(mappedBy = "bopliktomraade", cascade = [CascadeType.ALL])
    var grenser: MutableSet<BopliktomraadeGrenseKobling> = Collections.synchronizedSet(mutableSetOf())

    override fun getGrensekoblinger(gyldighetsdato: LocalDate): Set<BopliktomraadeGrenseKobling> {
        return grenser.filter { it.grense.gyldigPaaDato(gyldighetsdato) }.toSet()
    }

    override fun addGrensekobling(grense: Grense) {
        val bopliktomraadeGrenseKobling = BopliktomraadeGrenseKobling(
            bopliktomraade = this,
            grense = grense
        )
        grenser.add(bopliktomraadeGrenseKobling)
        grense.bopliktomraader.add(bopliktomraadeGrenseKobling)
    }

    override fun removeGrensekobling(gammelGrense: Grense) {
        if (grenser.removeIf { it.grense.id == gammelGrense.id }) {
            gammelGrense.bopliktomraader.removeIf { it.bopliktomraade.id == this.id }
        }
    }

    override fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?) {
        if (!tillatteGrensetyper().contains(grense?.grensetype) && grense != null) {
            throw RuntimeException("Kan ikke oppdatere/legge til ${grense.grensetype.name} på enhet Bopliktomraade")
        }
        if (grenser.removeIf { it.grense.id == gammelGrense.id }) {
            gammelGrense.bopliktomraader.removeIf { it.bopliktomraade.id == this.id }
            //skal bare adde om det grense != null
            if (grense != null) {
                addGrensekobling(grense)
            }
        }
    }

    override fun oppdaterOmraadeOgPosisjon() {
        val grenserForFlate = getGrensekoblinger(gyldighetsdato = gyldighet.gyldigFra)
        omraade = getGrenserIRiktigRekkefoelge(grenserForFlate.map { it.grense.grense }).toMultiPolygon()
        posisjon = omraade?.interiorPoint
    }

    override fun getFlateInformasjon(): FlateInformasjon =
        FlateInformasjon(type, "$id $lokalid")

    override fun toString(): String =
        "Bopliktomraade(id='$id', gyldighet: $gyldighet)"

    override fun toEventRad(eventType: EventType): EventRad =
        EventRad(
            uuid = this.id,
            eventType = eventType,
            flateType = Flatetype.BOPLIKTOMRAADE,
            lokalid = this.lokalid
        )

    fun applyMappingFromExistingBopliktomraade(gammeltBopliktomraade: Bopliktomraade): Bopliktomraade =
        this.apply {
            nummer = gammeltBopliktomraade.nummer
            navn = gammeltBopliktomraade.navn
            delvisBoplikt = gammeltBopliktomraade.delvisBoplikt
            forskriftsreferanse = gammeltBopliktomraade.forskriftsreferanse
            url = gammeltBopliktomraade.url
            omraade = gammeltBopliktomraade.omraade
            datafangstdato = gammeltBopliktomraade.datafangstdato
            foerstedigitaliseringsdato = gammeltBopliktomraade.foerstedigitaliseringsdato
            posisjon = gammeltBopliktomraade.posisjon
            opphav = gammeltBopliktomraade.opphav
            informasjon = gammeltBopliktomraade.informasjon
        }

    fun copyGrenserFromExistingBopliktomraade(gammeltBopliktomraade: Bopliktomraade, gyldigFra: LocalDate = LocalDate.now()): Bopliktomraade {
        gammeltBopliktomraade.getGrensekoblinger(gyldigFra).forEach { gammelGrenseKobling ->
            addGrensekobling(grense = gammelGrenseKobling.grense)
        }
        return this
    }

    companion object {
        fun tillatteGrensetyper(): Set<Grensetype> = setOf(
            Grensetype.Bopliktgrense
        )
    }

}

/**
 * Koblingstabell mellom Bopliktomraade - Grense
 */
@Table(name = "bopliktomraade_grense")
@Entity
@IdClass(BopliktomraadeGrenseId::class)
class BopliktomraadeGrenseKobling(
    @Id
    @ManyToOne
    @JoinColumn(name = "bopliktomraade_fk")
    var bopliktomraade: Bopliktomraade,

    @Id
    @ManyToOne
    @JoinColumn(name = "grense_fk")
    override var grense: Grense,
) : GrenseKobling {
    override fun getEnhetKobletTilGrense(): KobletTilGrense {
        return bopliktomraade
    }

    override fun getLokalIdForKrets(): String {
        return bopliktomraade.lokalid
    }
}

data class BopliktomraadeGrenseId(val bopliktomraade: String = "", val grense: String = "") : Serializable
