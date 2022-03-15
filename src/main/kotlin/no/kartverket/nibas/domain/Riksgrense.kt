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

/**
 * id overrides i konstruktør for å kunne overstyre verdi for test.
 */
@Table(name = "riksgrense")
@Audited
@Entity
class Riksgrense(override val id: String = UUID.randomUUID().toString()) : Grense(), CommonMetadataProperties {

    override var objtype: String? = "Riksgrense"

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

    @Column(name = "oppdateringsdato", nullable = false)
    override var oppdateringsdato: ZonedDateTime = ZonedDateTime.now()

    @ManyToOne
    @JoinColumn(name = "typeendring")
    var typeendring: Typeendring? = null

    @Column(name = "tidspunktendring")
    var tidspunktendring: LocalDate? = null

    @Embedded
    override var kvalitet: PosisjonskvalitetEmbeddable? = null

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

    @ManyToOne
    @JoinColumn(name = "grensestatus")
    override var grensestatus: Grensestatus? = null

    @ManyToOne
    @JoinColumn(name = "fastsettingstype")
    override var fastsettingstype: Fastsettingstype? = null

    @OneToMany(mappedBy = "riksgrenseFk", fetch = FetchType.LAZY)
    override var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse> = mutableSetOf()

    @OneToMany(mappedBy = "riksgrenseFk", fetch = FetchType.LAZY)
    var riksgrenseInformasjonselementer: MutableSet<RiksgrenseInformasjon> = mutableSetOf()

    @OneToMany(mappedBy = "riksgrenseFk", fetch = FetchType.LAZY)
    var riksgrenseAnsvarligeMyndigheter: MutableSet<RiksgrenseAnsvarligMyndighet> = mutableSetOf()

    override fun getGrenseInfoElementer(): Collection<GrenseInfo> {
        return this.riksgrenseInformasjonselementer
    }

    fun getAnsvarligeMyndigheter(): List<String> {
        return this.riksgrenseAnsvarligeMyndigheter.mapNotNull { it.ansvarligmyndighet }
    }
}

@Table(name = "riksgrense_informasjon")
@Audited
@Entity
class RiksgrenseInformasjon : GrenseInfo {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "riksgrense_fk")
    var riksgrenseFk: Riksgrense? = null

    @Column(name = "informasjon")
    override var informasjon: String? = null
}

@Table(name = "riksgrense_ansvarligmyndighet")
@Audited
@Entity
class RiksgrenseAnsvarligMyndighet {
    @Id
    @Column(name = "objid", nullable = false, updatable = false)
    val id: String = UUID.randomUUID().toString()

    @ManyToOne
    @JoinColumn(name = "riksgrense_fk")
    var riksgrenseFk: Riksgrense? = null

    @Column(name = "ansvarligmyndighet")
    var ansvarligmyndighet: String? = null
}
