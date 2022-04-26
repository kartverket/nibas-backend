package no.kartverket.nibas.domain

import no.kartverket.nibas.inndelinger.GrenseType
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "riksgrense")
@Audited
@Entity
class Riksgrense(override val id: String = UUID.randomUUID().toString()) : Grense() {

    override var objtype: String? = GrenseType.Riksgrense.value

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

    @Column(name = "oppdateringsdato", nullable = false)
    override var oppdateringsdato: LocalDate = LocalDate.now()

    @ManyToOne
    @JoinColumn(name = "typeendring")
    var typeendring: Typeendring? = null

    @Column(name = "tidspunktendring")
    var tidspunktendring: LocalDate? = null

    @Column(name = "opphav")
    override var opphav: String? = null

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

    @OneToMany(mappedBy = "riksgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    override var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "riksgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<RiksgrenseInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "riksgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var riksgrenseAnsvarligeMyndigheter: MutableSet<RiksgrenseAnsvarligMyndighet> = mutableSetOf()

    override fun getInfo(): String? {
        return doGetInfoFromSingletonCollection(informasjonselementer)
    }

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { RiksgrenseInformasjon(this) }
    }

    fun setAnsvarligeMyndigheter(listToUpdate: List<AnsvarligMyndighetHolder>) {
        doSetAnsvarligeMyndigheter(listToUpdate, riksgrenseAnsvarligeMyndigheter) { RiksgrenseAnsvarligMyndighet(this) }
    }
}

@Table(name = "riksgrense_informasjon")
@Audited
@Entity
class RiksgrenseInformasjon(riksgrense: Riksgrense) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "riksgrense_fk")
    var riksgrenseFk: Riksgrense? = riksgrense

    @Column(name = "informasjon")
    override var informasjon: String? = null
}

@Table(name = "riksgrense_ansvarligmyndighet")
@Audited
@Entity
class RiksgrenseAnsvarligMyndighet(riksgrense: Riksgrense) : AnsvarligMyndighet {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "riksgrense_fk")
    var riksgrenseFk: Riksgrense? = riksgrense

    @Column(name = "ansvarligmyndighet")
    override var ansvarligmyndighet: String? = null
}
