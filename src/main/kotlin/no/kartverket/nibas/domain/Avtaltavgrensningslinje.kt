package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "avtaltavgrensningslinje")
@Audited
@Entity
class Avtaltavgrensningslinje : Grense() {
    override var objtype: String? = "AvtaltAvgrensningslinje"

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "datafangstdato")
    var datafangstdato: LocalDate? = null

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "gyldigfra")
    var gyldigfra: LocalDate? = null

    @Column(name = "gyldigtil")
    var gyldigtil: LocalDate? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @ManyToOne
    @JoinColumn(name = "typeendring")
    var typeendring: Typeendring? = null

    @Column(name = "tidspunktendring")
    var tidspunktendring: LocalDate? = null


    @Column(name = "opphav")
    var opphav: String? = null

    @Column(name = "omraadeid")
    var omraadeid: Int? = null


    @Column(name = "originaldatavert")
    var originaldatavert: String? = null

    @Column(name = "kopidato")
    var kopidato: LocalDate? = null


    @Column(name = "produkt")
    var produkt: String? = null


    @Column(name = "versjon")
    var versjon: String? = null

    @Column(name = "maalemetode", nullable = false)
    var maalemetode: String = ""

    @Column(name = "noeyaktighet")
    var noeyaktighet: Int? = null

    @Column(name = "gjelderomraade")
    var gjelderomraade: String? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    var grensestatus: Grensestatus? = null


    @Column(name = "virkeomraadenavn")
    var virkeomraadenavn: String? = null

    @Column(name = "transformasjonsdato")
    var transformasjonsdato: LocalDate? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "fastsettingstype", nullable = false)
    var fastsettingstype: Fastsettingstype? = null

    @ManyToMany
    @JoinTable(
        schema = "nibas",
        name = "avtaltavgrensningslinje_land",
        joinColumns = arrayOf(JoinColumn(name = "avtaltavgrensningslinje_fk")),
        inverseJoinColumns = arrayOf(JoinColumn(name = "landkode_fk"))
    )
    var landkode: MutableSet<Landkode> = mutableSetOf()

    @OneToMany(mappedBy = "avtaltavgrensningslinjeFk", fetch = FetchType.LAZY)
    var dokumentasjonsreferanse: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()
}
