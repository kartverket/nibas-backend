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
@Table(name = "kommune")
@Entity
class Kommune(
    override val id: String = UUID.randomUUID().toString(),
    override val lokalid: String = UUID.randomUUID().toString()
) : AdministrativEnhet(Flatetype.KOMMUNE) {

    @Column(name = "kommunenummer", nullable = false)
    var kommunenummer: String = ""

    @Column(name = "samiskforvaltningsomraade", nullable = false)
    var samiskforvaltningsomraade: Boolean? = false

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "lokalid", referencedColumnName = "lokalid", insertable = false, updatable = false)
    @Where(clause="flatetype='KOMMUNE'")
    lateinit var kommuneLokalid : UnikLokalid

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "fylkelokalid", referencedColumnName = "lokalid")
    @Where(clause="flatetype='FYLKE'")
    lateinit var fylkeLokalid : UnikLokalid

    @OneToMany(mappedBy = "kommuneFk", cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<KommuneInformasjon> = mutableSetOf()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utledetfra")
    var utledetFra: Kommune? = null

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjonselementer)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { KommuneInformasjon(this) }
    }

    fun getGrenser(gyldighetsdato: LocalDate = LocalDate.now()): Set<GrenseKobling> {
        return getGrunnkretser(gyldighetsdato)
            .flatMap { it.getGrenserAvTyper(tillatteGrensetyper(), gyldighetsdato) }
            .toSet()
    }

    fun addGrunnkrets(grunnkrets: Grunnkrets) {
        grunnkrets.kommuneLokalid = kommuneLokalid
        kommuneLokalid.grunnkretser.add(grunnkrets)
    }

    fun addStemmekrets(stemmekrets: Stemmekrets) {
        stemmekrets.kommuneLokalid = kommuneLokalid
        kommuneLokalid.stemmekretser.add(stemmekrets)
    }

    fun getGrunnkretser(gyldighetsdato: LocalDate = LocalDate.now()): List<Grunnkrets> {
        return kommuneLokalid.grunnkretser.filter { it.gyldigPaaDato(gyldighetsdato) }
    }

    fun getStemmekretser(gyldighetsdato: LocalDate = LocalDate.now()): List<Stemmekrets> {
        return kommuneLokalid.stemmekretser.filter { it.gyldigPaaDato(gyldighetsdato) }
    }

    /**
     * Henter kommunen sitt fylke på en gitt dato. Hvis man gir en dato hvor fylket med gitt lokalid ikke er gyldig, returneres null
     */
    fun getFylke(gyldighetsdato: LocalDate = LocalDate.now()): Fylke? {
        return fylkeLokalid.fylker.filter { it.gyldigPaaDato(gyldighetsdato) }.firstOrNull()
    }

    fun addAdministrativenhetNavn(navn: Administrativenhetnavn) {
        navn.administrativenhetLokalid = kommuneLokalid
        kommuneLokalid.administrativenhetnavn.add(navn)
    }

    override fun getAdministrativenhetNavn(gyldighetsdato: LocalDate): List<Administrativenhetnavn> {
        return kommuneLokalid.administrativenhetnavn.filter { it.gyldigPaaDato(gyldighetsdato) }
    }

    /**
     * Knytt denne versjonen av kommune til dens lokalid
     */
    fun addKommuneTilUnikLokalid(unikLokalid: UnikLokalid) {
        kommuneLokalid = unikLokalid
        unikLokalid.kommuner.add(this)
    }

    fun tillatteGrensetyper(): Set<Grensetype> = setOf(
        Grensetype.Kommunegrense,
        Grensetype.Fylkesgrense,
        Grensetype.AvtaltAvgrensningslinje,
        Grensetype.Territorialgrense,
        Grensetype.Riksgrense)
}

@Table(name = "kommune_informasjon")
@Entity
class KommuneInformasjon(kommune: Kommune) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "kommune_fk")
    var kommuneFk: Kommune? = kommune

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
