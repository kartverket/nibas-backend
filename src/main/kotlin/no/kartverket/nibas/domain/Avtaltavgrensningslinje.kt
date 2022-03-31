package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "avtaltavgrensningslinje")
@Audited
@Entity
class Avtaltavgrensningslinje : Grense(), MaritimGrense {
    override var objtype: String? = "AvtaltAvgrensningslinje"

    @Column(name = "versjonid")
    override var versjonid: String? = null

    @Column(name = "datafangstdato")
    override var datafangstdato: ZonedDateTime? = null

    @Column(name = "sluttdato")
    var sluttdato: LocalDate? = null

    @Column(name = "gyldigfra")
    override var gyldigFra: ZonedDateTime? = null

    @Column(name = "gyldigtil")
    override var gyldigTil: ZonedDateTime? = null

    @Column(name = "datauttaksdato")
    var datauttaksdato: ZonedDateTime? = null

    @ManyToOne
    @JoinColumn(name = "typeendring")
    var typeendring: Typeendring? = null

    @Column(name = "tidspunktendring")
    var tidspunktendring: LocalDate? = null

    @Column(name = "oppdateringsdato")
    override var oppdateringsdato: ZonedDateTime = ZonedDateTime.now()

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

    // Overstyres fordi den er non-nullable
    @ManyToOne(optional = false)
    @JoinColumn(name = "fastsettingstype", nullable = false)
    override var fastsettingstype: Fastsettingstype? = null

    @ManyToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        schema = "nibas",
        name = "avtaltavgrensningslinje_land",
        joinColumns = [JoinColumn(name = "avtaltavgrensningslinje_fk")],
        inverseJoinColumns = [JoinColumn(name = "landkode_fk")]
    )
    var landkode: MutableSet<Landkode> = mutableSetOf()

    @OneToMany(mappedBy = "avtaltavgrensningslinjeFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    override var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "avtaltavgrensningslinjeFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var informasjonselementer: MutableSet<AvtaltavgrensningslinjeInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "avtaltavgrensningslinjeFk", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var ansvarligeMyndigheter: MutableSet<AvtaltavgrensningslinjeAnsvarligMyndighet> =
        mutableSetOf()

    override fun getInfo(): String? {
        return doGetInfoFromSingletonCollection(informasjonselementer)
    }

    override fun setInfo(updatedInfo: String?) {
        doSetInfoInSingletonCollection(updatedInfo, informasjonselementer) { AvtaltavgrensningslinjeInformasjon(this) }
    }

    override fun getAnsvarligeMyndigheter(): Collection<AnsvarligMyndighet> {
        return this.ansvarligeMyndigheter
    }

    override fun setAnsvarligeMyndigheter(ansvarligeMyndigheter: List<AnsvarligMyndighetHolder>) {
        doSetAnsvarligeMyndigheter(ansvarligeMyndigheter, this.ansvarligeMyndigheter) { AvtaltavgrensningslinjeAnsvarligMyndighet() }
    }

    override fun getLand(): Collection<Landkode> {
        return this.landkode
    }

    override fun setLand(landkoder: List<Landkode>) {
        this.landkode.clear()
        this.landkode.addAll(landkoder)
    }
}

@Table(name = "avtaltavgrensningslinje_ansvarligmyndighet")
@Audited
@Entity
class AvtaltavgrensningslinjeAnsvarligMyndighet : AnsvarligMyndighet {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "avtaltavgrensningslinje_fk")
    var avtaltavgrensningslinjeFk: Avtaltavgrensningslinje? = null

    @Column(name = "ansvarligmyndighet")
    override var ansvarligmyndighet: String? = null
}

@Table(name = "avtaltavgrensningslinje_informasjon")
@Audited
@Entity
class AvtaltavgrensningslinjeInformasjon(avtaltavgrensningslinje: Avtaltavgrensningslinje) : Info {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    override val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "avtaltavgrensningslinje_fk")
    var avtaltavgrensningslinjeFk: Avtaltavgrensningslinje? = avtaltavgrensningslinje

    @Column(name = "informasjon")
    override var informasjon: String? = null
}
