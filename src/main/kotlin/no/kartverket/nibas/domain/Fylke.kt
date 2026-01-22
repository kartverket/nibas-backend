package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDate
import java.util.*
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "fylke")
@Entity
class Fylke(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString()
) : AdministrativEnhet(Flatetype.FYLKE) {

    @Column(name = "fylkesnummeruuid", nullable = false)
    var fylkesnummerUuid: String = ""

    @Column(name = "fylkesnummer", nullable = false)
    var fylkesnummer: String = ""

    @Column(name = "samiskforvaltningsomraade")
    var samiskforvaltningsomraade: Boolean? = null

    @Column(name = "informasjon")
    override var informasjon: String? = null

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "nasjonlokalid", referencedColumnName = "lokalid")
    @SQLRestriction("flatetype='NASJON'")
    lateinit var nasjonLokalid : UnikLokalid

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utledetfra")
    var utledetFra: Fylke? = null

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "lokalid", referencedColumnName = "lokalid", insertable = false, updatable = false)
    @SQLRestriction("flatetype='FYLKE'")
    lateinit var fylkeLokalid : UnikLokalid

    override fun getGrensekoblinger(gyldighetsdato: LocalDate): Set<GrunnkretsGrenseKobling> {
        return getKommuner(gyldighetsdato)
            .flatMap { it.getGrunnkretser(gyldighetsdato) }
            .flatMap { it.getGrensekoblingerAvTyper(tillatteGrensetyper(), gyldighetsdato) }
            .toSet()
    }

    override fun getFlateInformasjon(): FlateInformasjon {
        val navn = getAdministrativenhetNavn(gyldighet.gyldigFra).getInSpraak("nor") ?: "Ukjent navn"
        return FlateInformasjon(type, "$fylkesnummer $navn")
    }

    fun addKommune(kommune: Kommune) {
        kommune.fylkeLokalid = fylkeLokalid
        fylkeLokalid.kommunerForFylke.add(kommune)
    }

    fun getKommuner(gyldighetsdato: LocalDate = LocalDate.now()): List<Kommune> {
        return fylkeLokalid.kommunerForFylke.filter { it.gyldigPaaDato(gyldighetsdato) }
    }

    /**
     * Henter fylket sin nasjon på en gitt dato. Hvis man gir en dato hvor nasjonen med gitt lokalid ikke er gyldig, returneres null
     */
    fun getNasjon(gyldighetsdato: LocalDate = LocalDate.now()): Nasjon? {
        return nasjonLokalid.nasjoner.firstOrNull { it.gyldigPaaDato(gyldighetsdato) }
    }

    override fun addAdministrativenhetNavn(navn: Administrativenhetnavn) {
        navn.administrativenhetLokalid = fylkeLokalid
        fylkeLokalid.administrativenhetnavn.add(navn)
    }

    override fun getAdministrativenhetNavn(gyldighetsdato: LocalDate): List<Administrativenhetnavn> {
        return fylkeLokalid.administrativenhetnavn.filter { it.gyldigPaaDato(gyldighetsdato) }
    }

    /**
     * Knytt denne versjonen av fylket til dets lokalid
     */
    fun addFylkeTilUnikLokalid(unikLokalid: UnikLokalid) {
        fylkeLokalid = unikLokalid
        unikLokalid.fylker.add(this)
    }

    fun applyMappingFromExistingFylke(gammeltFylke: Fylke): Fylke {
        return this.apply {
            fylkesnummerUuid = gammeltFylke.fylkesnummerUuid
            fylkesnummer = gammeltFylke.fylkesnummer
            samiskforvaltningsomraade = gammeltFylke.samiskforvaltningsomraade
            datafangstdato = gammeltFylke.datafangstdato
            foerstedigitaliseringsdato = gammeltFylke.foerstedigitaliseringsdato
            opphav = gammeltFylke.opphav
            posisjon = gammeltFylke.posisjon
            omraade = gammeltFylke.omraade
            utledetFra = gammeltFylke.utledetFra
            informasjon = gammeltFylke.informasjon
        }
    }

    fun applyCascadingOperationsBasedOnExistingFylke(gammeltFylke: Fylke, gyldigFra: LocalDate = LocalDate.now()): Fylke {
        return this.apply {
            addFylkeTilUnikLokalid(gammeltFylke.fylkeLokalid)

            // Må legge til dette fylket på eksisterende fylke sin nasjon
            gammeltFylke.getNasjon(gyldighetsdato = gyldigFra)!!.addFylke(this)
        }
    }

    override fun toEventRad(eventType: EventType): EventRad =
        EventRad(
            uuid = this.id,
            eventType = eventType,
            flateType = Flatetype.FYLKE,
            lokalid = this.lokalid
        )

    companion object {
        fun tillatteGrensetyper(): Set<Grensetype> = setOf(
            Grensetype.Fylkesgrense,
            Grensetype.AvtaltAvgrensningslinje,
            Grensetype.Territorialgrense,
            Grensetype.Riksgrense
        )
    }
}
