package no.kartverket.nibas.domain

import org.hibernate.envers.Audited
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "grunnlinje")
@Audited
@Entity
class Grunnlinje : Grense(), MaritimGrense, CommonMetadataProperties {
    override var objtype: String? = "Grunnlinje"

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

    @Column(name = "oppdateringsdato")
    override var oppdateringsdato: ZonedDateTime = ZonedDateTime.now()

    @ManyToOne
    @JoinColumn(name = "typeendring")
    var typeendring: Typeendring? = null

    @Column(name = "tidspunktendring")
    var tidspunktendring: LocalDate? = null

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

    @Embedded
    override var kvalitet: PosisjonskvalitetEmbeddable? = null

    @Column(name = "gjelderomraade")
    override var gjelderomraade: String? = null

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    override var grensestatus: Grensestatus? = null

    @Column(name = "virkeomraadenavn")
    override var virkeomraadenavn: String? = null

    @Column(name = "transformasjonsdato")
    override var transformasjonsdato: LocalDate? = null

    @ManyToOne
    @JoinColumn(name = "land")
    var land: Landkode? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "fastsettingstype", nullable = false)
    override var fastsettingstype: Fastsettingstype? = null

    // UUID
    @Column(name = "kommunenummer")
    var kommunenummerId: String? = null

    @OneToMany(mappedBy = "grunnlinjeFk", fetch = FetchType.LAZY)
    override var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "grunnlinjeFk", fetch = FetchType.LAZY)
    var informasjon: MutableSet<GrunnlinjeInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "grunnlinjeFk", fetch = FetchType.LAZY)
    var grunnlinjeAnsvarligeMyndigheter: MutableSet<GrunnlinjeAnsvarligMyndighet> = mutableSetOf()

    override fun getAnsvarligeMyndigheter(): Collection<AnsvarligMyndighet> {
        return this.grunnlinjeAnsvarligeMyndigheter
    }

    override fun getLand(): Collection<Landkode> {
        return if (land != null) listOf(land!!) else emptyList()
    }

    override fun getGrenseInfoElementer(): Collection<GrenseInfo> {
        return this.informasjon
    }
}

@Table(name = "grunnlinje_informasjon")
@Audited
@Entity
class GrunnlinjeInformasjon : GrenseInfo {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "grunnlinje_fk")
    var grunnlinjeFk: Grunnlinje? = null

    @Column(name = "informasjon")
    override var informasjon: String? = null
}

@Table(name = "grunnlinje_ansvarligmyndighet")
@Audited
@Entity
class GrunnlinjeAnsvarligMyndighet : AnsvarligMyndighet {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "grunnlinje_fk")
    var grunnlinjeFk: Grunnlinje? = null

    @Column(name = "ansvarligmyndighet")
    override var ansvarligmyndighet: String? = null
}
