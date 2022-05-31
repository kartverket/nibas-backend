package no.kartverket.nibas.domain

import no.kartverket.nibas.inndelinger.GrenseType
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.util.*
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "lovvirkeomraadegrense")
@Audited
@Entity
class Lovvirkeomraadegrense : GrenseMedDokumentasjonsreferanser() {
    override var objtype: String? = GrenseType.Lovvirkeomraadegrense.value

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "datafangstdato")
    override var datafangstdato: LocalDate? = null

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "gyldigfra")
    override var gyldigFra: LocalDate? = null

    @Column(name = "gyldigtil")
    override var gyldigTil: LocalDate? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: LocalDate? = null

    @ManyToOne
    @JoinColumn(name = "typeendring")
    var typeendring: Typeendring? = null

    @Column(name = "tidspunktendring")
    var tidspunktendring: LocalDate? = null

    @Column(name = "opphav")
    override var opphav: String? = null

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

    @Column(name = "sonenautisk")
    var sonenautisk: Int? = null

    @ManyToOne
    @JoinColumn(name = "foelgeradministrativgrense")
    var foelgeradministrativgrense: Administrativgrensekode? = null

    @OneToMany(mappedBy = "lovvirkeomraadegrenseFk", fetch = FetchType.LAZY)
    var informasjon: MutableSet<LovvirkeomraadegrenseInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "lovvirkeomraadegrenseFk", fetch = FetchType.LAZY)
    override var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    // Holder på liste av kommunenummer-UUIDs grensen er tilknyttet
    // Uten at vi behøver definere egen entity for dette.
    @ElementCollection
    var kommunenummer: MutableSet<String> = mutableSetOf()

    override fun getInfo(): String? = doGetInfoFromSingletonCollection(informasjon)

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjon) { LovvirkeomraadegrenseInformasjon(this) }
    }

    override fun setDokumentasjonsreferanseTilbakePeker(dokumentasjonsreferanse: Dokumentasjonsreferanse) {
        dokumentasjonsreferanse.lovvirkeomraadegrenseFk = this
    }
}

@Table(name = "lovvirkeomraadegrense_informasjon")
@Audited
@Entity
class LovvirkeomraadegrenseInformasjon(lovvirkeomraadegrense: Lovvirkeomraadegrense) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "lovvirkeomraadegrense_fk")
    var lovvirkeomraadegrenseFk: Lovvirkeomraadegrense? = lovvirkeomraadegrense

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
