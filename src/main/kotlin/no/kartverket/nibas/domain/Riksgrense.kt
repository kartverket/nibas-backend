package no.kartverket.nibas.domain

import java.time.LocalDate
import org.hibernate.envers.Audited
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "riksgrense")
@Audited
@Entity
class Riksgrense : Grense() {

    override var objtype: String? = "Riksgrense"

    @Column(name = "lokalid", nullable = false)
    var lokalid: String? = null

    @Column(name = "navnerom", nullable = false)
    var navnerom: String? = null

    @Column(name = "versjonid")
    var versjonid: String? = null

    @Column(name = "datafangstdato")
    var datafangstdato: LocalDate? = null

    @Column(name = "oppdateringsdato")
    var oppdateringsdato: LocalDate? = null

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

    @Column(name = "stedfestingverifisert")
    var stedfestingverifisert: Boolean? = null

    @Column(name = "produkt")
    var produkt: String? = null

    @Column(name = "versjon")
    var versjon: String? = null

    @ManyToOne
    @JoinColumn(name = "foelgerterrengdetalj")
    var foelgerterrengdetalj: Terrengdetaljkode? = null

    @Column(name = "grensegatebredde")
    var grensegatebredde: Double? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    var grensestatus: Grensestatus? = null

    @ManyToOne
    @JoinColumn(name = "fastsettingstype")
    var fastsettingstype: Fastsettingstype? = null

    @OneToMany(mappedBy = "riksgrenseFk", fetch = FetchType.LAZY)
    var dokumentasjonsreferanse: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()
}
