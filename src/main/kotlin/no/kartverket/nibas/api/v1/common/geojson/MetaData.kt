package no.kartverket.nibas.api.v1.common.geojson

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.domain.ContainsSosiIdentifikasjon
import no.kartverket.nibas.domain.Posisjonskvalitet
import java.time.LocalDate

/**
 * Representerer metadata som skal slippe ut av API-et for en adm. grense.
 */
@Schema(description = "Metadata om en administrativ grense")
data class AdmGrenserMetadata(
    @Schema(description = "Dato når objektet siste gang ble registrert/observert/målt i terrenget")
    var datafangstdato: LocalDate?,
    @Schema(description = "Tidspunktet når objektet oppstod i den virkelige verden")
    var gyldigFra: LocalDate?,
    @Schema(description = "Tidspunktet når objektet opphørte å eksistere i den virkelige verden")
    var gyldigTil: LocalDate?,
    @Schema(description = "Generelle opplysninger/merknad")
    var informasjon: List<String> = emptyList(),
    @Schema(description = "Unik identifikasjon av et objekt")
    var identifikasjon: Identifikasjon,
    @Schema(description = "Beskrivelse av kvaliteten på stedfestingen")
    var kvalitet: Posisjonskvalitet?,
    @Schema(description = "Dato for siste endring på objektdataene")
    var oppdateringsdato: LocalDate,
    @Schema(description = "Referanse til opphavsmaterialet, kildematerialet, organisasjons/publiseringskilde.")
    var opphav: String?,
)

data class Identifikasjon(
    @Schema(
        description = "Lokal id som identifiserer geometrien. Dette er en globalt unik id."
    )
    override var lokalid: String,
    @Schema(
        description = "Navnerom som unikt identifiserer datakilden til objektet, starter med to bokstavs kode jfr " +
                "ISO 3166."
    )
    override var navnerom: String,
    @Schema(
        description = "identifikasjon av en spesiell versjon av et geografisk objekt (instans), maksimum lengde på " +
                "25 karakterer."
    )
    override var versjonid: String? = null
) : ContainsSosiIdentifikasjon

