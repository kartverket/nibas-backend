package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import org.hibernate.annotations.Where
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

    @OneToMany(mappedBy = "nasjon", cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<NasjonInformasjon> = mutableSetOf()

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "lokalid", referencedColumnName = "lokalid", insertable = false, updatable = false)
    @Where(clause="flatetype='NASJON'")
    lateinit var nasjonLokalid : UnikLokalid

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { NasjonInformasjon(this) }
    }

    fun getGrenser(gyldighetsdato: LocalDate = LocalDate.now()): Set<GrenseKobling> {
        return getFylker(gyldighetsdato)
            .flatMap { it.getKommuner(gyldighetsdato) }
            .flatMap { it.getGrunnkretser(gyldighetsdato) }
            .flatMap { it.getGrenserAvTyper(tillatteGrensetyper(), gyldighetsdato) }
            .toSet()
    }

    fun getFylker(gyldighetsdato: LocalDate = LocalDate.now()): List<Fylke> {
        return nasjonLokalid.fylkerForNasjon.filter { it.gyldigPaaDato(gyldighetsdato) }
    }

    fun addFylke(fylke: Fylke) {
        fylke.nasjonLokalid = nasjonLokalid
        nasjonLokalid.fylkerForNasjon.add(fylke)
    }

    fun addAdministrativenhetNavn(navn: Administrativenhetnavn) {
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

    fun tillatteGrensetyper(): Set<Grensetype> = setOf(
        Grensetype.AvtaltAvgrensningslinje,
        Grensetype.Territorialgrense,
        Grensetype.Riksgrense)
}

@Table(name = "nasjon_informasjon")
@Entity
class NasjonInformasjon(nasjon: Nasjon) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    var nasjon: Nasjon? = nasjon

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
