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
@Table(name = "nasjon")
@Entity
class Nasjon(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString()
) : AdministrativEnhet(Flatetype.NASJON) {

    @Column(name = "informasjon")
    var informasjon: String? = null

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "lokalid", referencedColumnName = "lokalid", insertable = false, updatable = false)
    @SQLRestriction("flatetype='NASJON'")
    lateinit var nasjonLokalid : UnikLokalid

    override fun getInfo(): String? = informasjon

    override fun setInfo(updatedInfo: String?) {
        informasjon = updatedInfo
    }

    override fun getGrensekoblinger(gyldighetsdato: LocalDate): Set<GrunnkretsGrenseKobling> {
        return getFylker(gyldighetsdato)
            .flatMap { it.getKommuner(gyldighetsdato) }
            .flatMap { it.getGrunnkretser(gyldighetsdato) }
            .flatMap { it.getGrensekoblingerAvTyper(tillatteGrensetyper(), gyldighetsdato) }
            .toSet()
    }

    override fun getFlateInformasjon(): FlateInformasjon {
        val navn = getAdministrativenhetNavn(gyldighet.gyldigFra).getInSpraak("nor") ?: "Ukjent navn"
        return FlateInformasjon(type, navn)
    }

    override fun toEventRad(eventType: EventType): EventRad =
        EventRad(
            uuid = this.id,
            eventType = eventType,
            flateType = Flatetype.NASJON,
            lokalid = this.lokalid
        )

    fun getFylker(gyldighetsdato: LocalDate = LocalDate.now()): List<Fylke> {
        return nasjonLokalid.fylkerForNasjon.filter { it.gyldigPaaDato(gyldighetsdato) }
    }

    fun addFylke(fylke: Fylke) {
        fylke.nasjonLokalid = nasjonLokalid
        nasjonLokalid.fylkerForNasjon.add(fylke)
    }

    override fun addAdministrativenhetNavn(navn: Administrativenhetnavn) {
        navn.administrativenhetLokalid = nasjonLokalid
        nasjonLokalid.administrativenhetnavn.add(navn)
    }

    override fun getAdministrativenhetNavn(gyldighetsdato: LocalDate): List<Administrativenhetnavn> {
        return nasjonLokalid.administrativenhetnavn.filter { it.gyldigPaaDato(gyldighetsdato) }
    }

    /**
     * Knytt denne versjonen av nasjon til dens lokalid
     */
    fun addNasjonTilUnikLokalid(unikLokalid: UnikLokalid) {
        nasjonLokalid = unikLokalid
        unikLokalid.nasjoner.add(this)
    }

    fun applyMappingFromExistingNasjon(gammelNasjon: Nasjon): Nasjon {
        return this.apply {
            gammelNasjon.gyldighet.gyldigTil?.let { setGyldigTil(it) }
            datafangstdato = gammelNasjon.datafangstdato
            foerstedigitaliseringsdato = gammelNasjon.foerstedigitaliseringsdato
            opphav = gammelNasjon.opphav
            posisjon = gammelNasjon.posisjon
            omraade = gammelNasjon.omraade
            informasjon = gammelNasjon.informasjon
        }
    }

    fun applyCascadingOperationsBasedOnExistingNasjon(gammelNasjon: Nasjon): Nasjon {
        return this.apply {
            addNasjonTilUnikLokalid(gammelNasjon.nasjonLokalid)
        }
    }

    companion object {
        fun tillatteGrensetyper(): Set<Grensetype> = setOf(
            Grensetype.AvtaltAvgrensningslinje,
            Grensetype.Territorialgrense,
            Grensetype.Riksgrense
        )
    }
}
