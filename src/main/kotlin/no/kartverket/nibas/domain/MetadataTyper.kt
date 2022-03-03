package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.AdmGrenserMetadata
import no.kartverket.nibas.api.v1.common.geojson.Dokref
import no.kartverket.nibas.api.v1.common.geojson.Identifikasjon
import no.kartverket.nibas.api.v1.common.geojson.Posisjonskvalitet
import java.time.LocalDate
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
    @Column(name = "maalemetode")
    var maalemetode: String? = null,
    @Column(name = "noeyaktighet")
    var noeyaktighet: Int? = null
)

fun PosisjonskvalitetEmbeddable.toKvalitet(): Posisjonskvalitet =
    Posisjonskvalitet(maalemetode, noeyaktighet)

@Embeddable
data class Registreringsversjon(
    @Column(name = "produkt")
    var produkt: String? = null,

    @Column(name = "versjon")
    var versjon: String? = null
)

interface LovvirkeomraadeMetadata {
    var oppdateringsdato: LocalDate?
}

interface RiksgrenseMetadata {
    var oppdateringsdato: LocalDate?
}

interface MaritimeGrenserMetadata {
    var oppdateringsdato: LocalDate?
}

fun AdministrativGrense.toMetadata(): AdmGrenserMetadata =
    AdmGrenserMetadata(
        datafangstdato = this.datafangstdato,
        gyldigFra = this.gyldigFra,
        gyldigTil = this.gyldigTil,
        informasjonselementer = this.getInformasjonselementer(),
        posisjonskvalitet = this.kvalitet?.toKvalitet(),
        oppdateringsdato = this.oppdateringsdato,
        opphav = this.opphav,
        identifikasjon = Identifikasjon(this.lokalid, this.navnerom, this.versjonid),
        dokumentasjonsreferanser = this.getListeAvDokumentasjonsreferanser(),
        fastsettingstype = this.fastsettingstype?.id,
        foelgerTerrengdetalj = this.foelgerterrengdetalj?.id,
        grensestatus = this.grensestatus?.id,
        noeyaktighetsklasse = this.noeyaktighetsklasse?.id,
        omtvistet = this.omtvistet
    )

fun Dokumentasjonsreferanse.toDokRef() = Dokref(
    dokumentlenker = this.dokumentlink
        .mapNotNull { it.dokumentlink }
        .sorted(),
    fastsettingsdato = this.fastsettingsdato!!,
    fastsettingsmyndighet = this.fastsettingsmyndighet,
    hjemmel = this.hjemmel,
    internReferanserKartverket = this.internReferanserKartverket
        .mapNotNull { it.internReferanseKartverket }
        .sorted(),
    rettskildeId = this.rettskildeid,
    rettskildeTittel = this.rettskildetittel!!
)
