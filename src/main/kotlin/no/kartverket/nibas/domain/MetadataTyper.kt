package no.kartverket.nibas.domain

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.geojson.AdmGrenserMetadata
import no.kartverket.nibas.api.v1.common.geojson.Identifikasjon
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
data class Posisjonskvalitet(
    @Schema(
        description = "Metode for måling i grunnriss (x,y), og høyde (z) når metoden er den samme som ved måling i " +
                "grunnriss"
    )
    @Column(name = "maalemetode")
    var maalemetode: String? = null,
    @Schema(
        description = "Punktstandardavviket i grunnriss for punkter samt tverravvik for linjer Merknad: Oppgitt i " +
                "cm"
    )
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

interface LovvirkeomraadeMetadata {
    var oppdateringsdato: LocalDate?
}

interface RiksgrenseMetadata {
    var oppdateringsdato: LocalDate?
}

interface MaritimeGrenserMetadata {
    var oppdateringsdato: LocalDate?
}

/**
 * TODO: Mange felles metadata-felter for adm_grenser/adm_enheter/adm_punkter. Slå sammen på sikt?
 */
interface ContainingMetadataForAdmGrenser : ContainsSosiIdentifikasjon {
    var datafangstdato: LocalDate?
    var gyldigFra: LocalDate?
    var gyldigTil: LocalDate?
    fun informasjon(): List<String>
    var kvalitet: Posisjonskvalitet?
    var oppdateringsdato: LocalDate
    var opphav: String?
}

fun ContainingMetadataForAdmGrenser.toMetadata(): AdmGrenserMetadata =
    AdmGrenserMetadata(
        datafangstdato = this.datafangstdato,
        gyldigFra = this.gyldigFra,
        gyldigTil = this.gyldigTil,
        informasjon = this.informasjon(),
        kvalitet = this.kvalitet,
        oppdateringsdato = this.oppdateringsdato,
        opphav = this.opphav,
        identifikasjon = Identifikasjon(this.lokalid, this.navnerom, this.versjonid)
    )
