package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import java.time.LocalDate
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

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

    @ManyToOne
    @JoinColumn(name = "nasjon_fk")
    lateinit var nasjon: Nasjon

    @ManyToOne
    @JoinColumn(name = "utledetfra")
    var utledetFra: Fylke? = null

    @OneToMany(mappedBy = "fylkeFk", cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<FylkeInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "fylkeFk")
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    @OneToMany(mappedBy = "fylke", cascade = [CascadeType.ALL])
    var kommuner: MutableSet<Kommune> = mutableSetOf()

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
        kommune.fylke = this
        kommuner.add(kommune)
    }

    fun getKommuner(gyldighetsdato: LocalDate = LocalDate.now()): List<Kommune> {
        return kommuner.filter { it.gyldigPaaDato(gyldighetsdato) }
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
