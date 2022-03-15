package no.kartverket.nibas.domain

import java.time.LocalDate
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Embeddable
data class Endringsflagg(
    @ManyToOne
    @JoinColumn(name = "typeendring")
    var typeendring: Typeendring? = null,

    @Column(name = "tidspunktendring")
    var tidspunktendring: LocalDate? = null
)

@Embeddable
data class Kopidata(
    @Column(name = "omraadeid")
    var omraadeid: Int? = null,

    @Column(name = "originaldatavert")
    var originaldatavert: String? = null,

    @Column(name = "kopidato")
    var kopidato: LocalDate? = null
)

@Embeddable
data class PosisjonskvalitetEmbeddable(
    @Column(name = "maalemetode", nullable = false)
    var maalemetode: String = "",
    @Column(name = "noeyaktighet")
    var noeyaktighet: Int? = null
)

@Embeddable
data class Registreringsversjon(
    @Column(name = "produkt")
    var produkt: String? = null,

    @Column(name = "versjon")
    var versjon: String? = null
)

interface MaritimGrense {
    fun getAnsvarligeMyndigheter(): Collection<AnsvarligMyndighet>
    var gjelderomraade: String?
    var transformasjonsdato: LocalDate?
    var virkeomraadenavn: String?
    fun getLand(): Collection<Landkode>
}

interface CommonMetadataProperties : ContainsSosiIdentifikasjon {
    var datafangstdato: ZonedDateTime?
    var gyldigFra: ZonedDateTime?
    var gyldigTil: ZonedDateTime?
    fun getGrenseInfoElementer(): Collection<GrenseInfo>
    var kvalitet: PosisjonskvalitetEmbeddable?
    var oppdateringsdato: ZonedDateTime
    var opphav: String?
    var dokumentasjonsreferanser: MutableSet<Dokumentasjonsreferanse>
    var fastsettingstype: Fastsettingstype?
    var grensestatus: Grensestatus?
}

interface AnsvarligMyndighet {
    var ansvarligmyndighet: String?
}

interface GrenseInfo {
    var informasjon: String?
}
