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

@Table(name = "territorialgrense")
@Audited
@Entity
class Territorialgrense : Grense(), MaritimGrense {
    override var objtype: String? = GrenseType.Territorialgrense.value

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

    @Column(name = "oppdateringsdato")
    override var oppdateringsdato: LocalDate = LocalDate.now()

    @Column(name = "opphav")
    override var opphav: String? = null

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

    @Column(name = "gjelderomraade")
    override var gjelderomraade: String? = null

    @Column(name = "virkeomraadenavn")
    override var virkeomraadenavn: String? = null

    @Column(name = "transformasjonsdato")
    override var transformasjonsdato: LocalDate? = null

    @ManyToOne
    @JoinColumn(name = "land")
    var land: Landkode? = null

    // Overstyres fordi den er non-nullable
    @ManyToOne(optional = false)
    @JoinColumn(name = "fastsettingstype", nullable = false)
    override var fastsettingstype: Fastsettingstype? = null

    @Column(name = "beregningsdato")
    var beregningsdato: LocalDate? = null

    @OneToMany(mappedBy = "territorialgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    override var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "territorialgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var ansvarligeMyndigheter: MutableSet<TerritorialgrenseAnsvarligMyndighet> = mutableSetOf()

    @OneToMany(mappedBy = "territorialgrenseFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<TerritorialgrenseInformasjon> = mutableSetOf()

    override fun getLandIds(): List<String> {
        return if (land != null) listOf(land!!.id) else emptyList()
    }

    override fun setLand(landkoder: List<Landkode>) {
        land = if (landkoder.size > 1) {
            throw RuntimeException("Invalid number of land (${landkoder.size}). Max 1 allowed for territorialgrense.")
        } else if (landkoder.isEmpty()) {
            null
        } else {
            landkoder[0]
        }
    }

    override fun getAnsvarligeMyndigheter(): Collection<AnsvarligMyndighet> {
        return this.ansvarligeMyndigheter
    }

    override fun setAnsvarligeMyndigheter(ansvarligeMyndigheter: List<AnsvarligMyndighetHolder>) {
        doSetAnsvarligeMyndigheter(ansvarligeMyndigheter, this.ansvarligeMyndigheter) { TerritorialgrenseAnsvarligMyndighet(this) }
    }

    override fun getInfo(): String? {
        return doGetInfoFromSingletonCollection(informasjonselementer)
    }

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { TerritorialgrenseInformasjon(this) }
    }

}

@Table(name = "territorialgrense_ansvarligmyndighet")
@Audited
@Entity
class TerritorialgrenseAnsvarligMyndighet(territorialgrense: Territorialgrense) : AnsvarligMyndighet {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "territorialgrense_fk")
    var territorialgrenseFk: Territorialgrense? = territorialgrense

    @Column(name = "ansvarligmyndighet")
    override var ansvarligmyndighet: String? = null
}

@Table(name = "territorialgrense_informasjon")
@Audited
@Entity
class TerritorialgrenseInformasjon(territorialgrense: Territorialgrense) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "territorialgrense_fk")
    var territorialgrenseFk: Territorialgrense? = territorialgrense

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
