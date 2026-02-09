package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.geometry.toMultiPolygon
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import org.hibernate.annotations.SQLRestriction
import org.locationtech.jts.io.WKTWriter
import org.slf4j.LoggerFactory
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
import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
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
    override var informasjon: String? = null

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

    fun getKommune(gyldighetsdato: LocalDate = LocalDate.now()): Kommune? {
        return kommuneLokalid.kommuner.firstOrNull { it.gyldigPaaDato(gyldighetsdato) }
    }

    @OneToMany(mappedBy = "bopliktomraade", cascade = [CascadeType.ALL])
    var grenser: MutableSet<BopliktomraadeGrenseKobling> = Collections.synchronizedSet(mutableSetOf())

    override fun getGrensekoblinger(gyldighetsdato: LocalDate): Set<BopliktomraadeGrenseKobling> {
        return grenser.filter { it.grense.gyldigPaaDato(gyldighetsdato) }.toSet()
    }

    override fun addGrensekobling(grense: Grense) {
        require(tillatteGrensetyper().contains(grense.grensetype)) {
            "Kan ikke legge til ${grense.grensetype.name} på Bopliktomraade"
        }
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
        require(grense == null || tillatteGrensetyper().contains(grense.grensetype)) {
            "Kan ikke oppdatere/legge til ${grense?.grensetype?.name} på Bopliktomraade"
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
        FlateInformasjon(type, "$nummer $navn")

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


    /**
    * Sjekker om bopliktområdet ligger innenfor kommunen, eller om de ikke overlapper i tid.
    *
    * Returnerer `true` hvis:
    * - Bopliktområdet og kommunen har ulike gyldighetsperioder som ikke overlapper
    * - Bopliktområdet ligger innenfor kommunens grenser (med en liten buffer på [KOMMUNE_BOUNDARY_BUFFER_METER])
    * - Enten bopliktområdet eller kommunen mangler geometri
    *
    * @param kommune kommunen som skal sjekkes mot
    * @return `true` hvis området ligger innenfor kommunen eller ikke overlapper i tid, `false` ellers
    */
    fun isCoveredByOrTemporallyDisjoint(kommune: Kommune): Boolean {
        if (!gyldighet.overlapperMed(kommune.gyldighet)) {
            return true
        }
        val kommuneOmraade = kommune.omraade?.takeUnless { it.isEmpty } ?: return true
        val thisOmraade = omraade?.takeUnless { it.isEmpty } ?: return true
        val coveredByKommune = kommuneOmraade
            .buffer(KOMMUNE_BOUNDARY_BUFFER_METER)
            .covers(thisOmraade)
        if (!coveredByKommune) {
            val differenceText = WKTWriter()
                .writeFormatted(kommuneOmraade.difference(thisOmraade))
                .let {
                    if (it.length > 500) {
                        it.substring(0, 500) + "..."
                    } else {
                        it
                    }
                }
            log.error(
                "Bopliktomraade({}) er ikke dekket av kommune {}({})@{}:\n{}",
                id, kommune.kommunenummer, kommune.id, kommune.gyldighet, differenceText
            )
            return false
        }
        return true
    }

    @PostUpdate
    @PostPersist
    fun validateCoveredByKommune() {
        if (!this::kommuneLokalid.isInitialized) return
        val valid = kommuneLokalid.kommuner.asSequence()
            .map(this::isCoveredByOrTemporallyDisjoint)
            .fold(true) { a, b -> a && b }
        require(valid) {
            "Bopliktomraade($id) er ikke dekket av kommunen(i hele sin gyldighetsperiode: $gyldighet)"
        }
    }

    companion object {
        const val KOMMUNE_BOUNDARY_BUFFER_METER = 1.0 / 1_000_000.0
        private val log = LoggerFactory.getLogger(Bopliktomraade::class.java)
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
