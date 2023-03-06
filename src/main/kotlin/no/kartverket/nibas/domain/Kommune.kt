package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import java.time.LocalDate
import java.util.*
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
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

    @ManyToOne
    @JoinColumn(name = "fylke_fk")
    lateinit var fylke: Fylke

    @OneToMany(mappedBy = "kommuneFk", cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<KommuneInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "kommuneFk")
    override var administrativenhetnavn: MutableSet<Administrativenhetnavn> = mutableSetOf()

    @OneToMany(mappedBy = "kommune", cascade = [CascadeType.ALL])
    var grunnkretser: MutableSet<Grunnkrets> = mutableSetOf()

    @OneToMany(mappedBy = "kommune", cascade = [CascadeType.ALL])
    var stemmekretser: MutableSet<Stemmekrets> = mutableSetOf()

    @ManyToOne
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
        grunnkrets.kommune = this
        grunnkretser.add(grunnkrets)
    }

    fun addStemmekrets(stemmekrets: Stemmekrets) {
        stemmekrets.kommune = this
        stemmekretser.add(stemmekrets)
    }

    fun getGrunnkretser(gyldighetsdato: LocalDate = LocalDate.now()): List<Grunnkrets> {
        return grunnkretser.filter { it.gyldigPaaDato(gyldighetsdato) }
    }

    fun getStemmekretser(gyldighetsdato: LocalDate = LocalDate.now()): List<Stemmekrets> {
        return stemmekretser.filter { it.gyldigPaaDato(gyldighetsdato) }
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
