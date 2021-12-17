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

@Table(name = "grunnlinje")
@Audited
@Entity
class Grunnlinje : Grense() {
    override var objtype: String? = "Grunnlinje"
    
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

    @ManyToOne
    @JoinColumn(name = "land")
    var land: Landkode? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "fastsettingstype", nullable = false)
    var fastsettingstype: Fastsettingstype? = null

    @Column(name = "kommunenummer")
    var kommunenummer: String? = null

    @OneToMany(mappedBy = "grunnlinjeFk", fetch = FetchType.LAZY)
    var dokumentasjonsreferanse: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "grunnlinjeFk", fetch = FetchType.LAZY)
    var informasjon: MutableSet<GrunnlinjeInformasjon> = mutableSetOf()
}
