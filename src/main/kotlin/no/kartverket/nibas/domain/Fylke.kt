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
@Table(name = "fylke")
@Entity
class Fylke(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString()
) : AdministrativEnhet(Flatetype.FYLKE) {

    @Column(name = "fylkesnummer", nullable = false)
    var fylkesnummer: String = ""

    @Column(name = "samiskforvaltningsomraade")
    var samiskforvaltningsomraade: Boolean? = null

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "nasjonlokalid", referencedColumnName = "lokalid")
    @Where(clause="flatetype='NASJON'")
    lateinit var nasjonLokalid : UnikLokalid

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utledetfra")
    var utledetFra: Fylke? = null

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "lokalid", referencedColumnName = "lokalid", insertable = false, updatable = false)
    @Where(clause="flatetype='FYLKE'")
    lateinit var fylkeLokalid : UnikLokalid

    @OneToMany(mappedBy = "fylkeFk", cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<FylkeInformasjon> = mutableSetOf()

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { FylkeInformasjon(this) }
    }

    fun getGrenser(gyldighetsdato: LocalDate = LocalDate.now()): Set<GrenseKobling> {
        return getKommuner(gyldighetsdato)
            .flatMap { it.getGrunnkretser(gyldighetsdato) }
            .flatMap { it.getGrenserAvTyper(tillatteGrensetyper(), gyldighetsdato) }
            .toSet()
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
        return nasjonLokalid.nasjoner.filter { it.gyldigPaaDato(gyldighetsdato) }.firstOrNull()
    }

    fun addAdministrativenhetNavn(navn: Administrativenhetnavn) {
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

    fun tillatteGrensetyper(): Set<Grensetype> = setOf(
        Grensetype.Fylkesgrense,
        Grensetype.AvtaltAvgrensningslinje,
        Grensetype.Territorialgrense,
        Grensetype.Riksgrense)
}

@Table(name = "fylke_informasjon")
@Entity
class FylkeInformasjon(fylke: Fylke) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    var fylkeFk: Fylke? = fylke

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
