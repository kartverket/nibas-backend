package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "lovvirkeomraadegrense")
@Audited
@Entity
class Lovvirkeomraadegrense : Grense() {
    override var objtype: String? = "LovVirkeområdeGrense"

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

    @Column(name = "maalemetode", nullable = false)
    var maalemetode: String = ""

    @Column(name = "noeyaktighet")
    var noeyaktighet: Int? = null


    @Column(name = "opphav")
    var opphav: String? = null

    @Column(name = "produkt")
    var produkt: String? = null


    @Column(name = "versjon")
    var versjon: String? = null

    @Column(name = "omraadeid")
    var omraadeid: Int? = null


    @Column(name = "originaldatavert")
    var originaldatavert: String? = null

    @Column(name = "kopidato")
    var kopidato: LocalDate? = null


    @Column(name = "gjelderomraade")
    var gjelderomraade: String? = null


    @Column(name = "virkeomraadenavn")
    var virkeomraadenavn: String? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    var grensestatus: Grensestatus? = null

    @ManyToOne
    @JoinColumn(name = "fastsettingstype")
    var fastsettingstype: Fastsettingstype? = null

    @Column(name = "sonenautisk")
    var sonenautisk: Int? = null

    @ManyToOne
    @JoinColumn(name = "foelgeradministrativgrense")
    var foelgeradministrativgrense: Administrativgrensekode? = null

    @OneToMany(mappedBy = "lovvirkeomraadegrenseFk", fetch = FetchType.LAZY)
    var informasjon: MutableSet<LovvirkeomraadegrenseInformasjon>? = null

    @OneToMany(mappedBy = "lovvirkeomraadegrenseFk", fetch = FetchType.LAZY)
    var dokumentasjonsreferanse: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    // Holder på liste av kommunenummer-UUIDs grensen er tilknyttet
    // Uten at vi behøver definere egen entity for dette.
    @ElementCollection
    var kommunenummer: MutableSet<String> = mutableSetOf()

}
