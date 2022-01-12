package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass

@MappedSuperclass
@Audited
abstract class AdministrativGrense : Grense() {

    @Column(name = "versjonid")
    var versjonid: String? = null

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @ManyToOne
    @JoinColumn(name = "typeendring")
    var typeendring: Typeendring? = null

    @Column(name = "tidspunktendring")
    var tidspunktendring: LocalDate? = null

    @Column(name = "maalemetode")
    var maalemetode: String? = null

    @Column(name = "noeyaktighet")
    var noeyaktighet: Int? = null


    @Column(name = "opphav")
    var opphav: String? = null


    @Column(name = "produkt")
    var produkt: String? = null


    @Column(name = "versjon")
    var versjon: String? = null

    @Column(name = "gyldigfra")
    var gyldigfra: LocalDate? = null

    @Column(name = "gyldigtil")
    var gyldigtil: LocalDate? = null

    @Column(name = "omraadeid")
    var omraadeid: Int? = null


    @Column(name = "originaldatavert")
    var originaldatavert: String? = null

    @Column(name = "kopidato")
    var kopidato: LocalDate? = null

    @Column(name = "datafangstdato")
    var datafangstdato: LocalDate? = null

    @Column(name = "omtvistet")
    var omtvistet: Boolean? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    var grensestatus: Grensestatus? = null

    @ManyToOne
    @JoinColumn(name = "fastsettingstype")
    var fastsettingstype: Fastsettingstype? = null

    @ManyToOne
    @JoinColumn(name = "foelgerterrengdetalj")
    var foelgerterrengdetalj: Terrengdetaljkode? = null

    @ManyToOne
    @JoinColumn(name = "noeyaktighetsklasse")
    var noeyaktighetsklasse: Noeyaktighetsklassekode? = null
}
