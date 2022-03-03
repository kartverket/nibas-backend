package no.kartverket.nibas.api.v1.common.geojson

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.domain.ContainsSosiIdentifikasjon
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Representerer metadata som skal slippe ut av API-et for en adm. grense.
 */
@Schema(description = "Metadata om en administrativ grense. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Objekttype/Index/EAID_5142EDBA_4226_4ca3_924D_EECEFD216D1A")
data class AdmGrenserMetadata(
    @Schema(description = "Dato når objektet siste gang ble registrert/observert/målt i terrenget")
    var datafangstdato: ZonedDateTime?,
    @Schema(description = "Tidspunktet når objektet oppstod i den virkelige verden")
    var gyldigFra: ZonedDateTime?,
    @Schema(description = "Tidspunktet når objektet opphørte å eksistere i den virkelige verden")
    var gyldigTil: ZonedDateTime?,
    @Schema(description = "Generelle opplysninger/merknad")
    var informasjonselementer: List<String> = emptyList(),
    @Schema(description = "Unik identifikasjon av et objekt")
    var identifikasjon: Identifikasjon,
    @Schema(description = "Beskrivelse av kvaliteten på stedfestingen")
    var posisjonskvalitet: Posisjonskvalitet?,
    @Schema(description = "Dato for siste endring på objektdataene")
    var oppdateringsdato: ZonedDateTime,
    @Schema(description = "Referanse til opphavsmaterialet, kildematerialet, organisasjons/publiseringskilde.")
    var opphav: String?,
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    var dokumentasjonsreferanser: List<Dokref> = emptyList(),
    @Schema(description = "Måte en administrativ grense er fastsatt på.")
    var fastsettingstype: String?,
    @Schema(description = "Opplysning om at grense følger naturlige skillelinjer i terrenget.")
    var foelgerTerrengdetalj: String?,
    @Schema(description = "Juridisk status på grensa.")
    var grensestatus: String?,
    @Schema(description = "Klassifikasjon av stedfestingsnøyaktighet på grenser, og er basert på dårligste nøyaktighet til grensepunktene til grensen.")
    var noeyaktighetsklasse: String?,
    @Schema(description = "Angir om grensen er omtvistet, eller det er tvil om forløpet.")
    var omtvistet: Boolean?
)

data class Posisjonskvalitet(
    @Schema(description = "Metode for måling i grunnriss (x,y), og høyde (z) når metoden er den samme som ved måling i grunnriss.")
    var maalemetode: String? = null,
    @Schema(description = "Punktstandardavviket i grunnriss for punkter samt tverravvik for linjer. Merknad: Oppgitt i cm.")
    var noeyaktighet: Int? = null
)

data class Identifikasjon(
    @Schema(description = "Lokal id som identifiserer geometrien. Dette er en globalt unik id.")
    override var lokalid: String,
    @Schema(description = "Navnerom som unikt identifiserer datakilden til objektet, starter med to bokstavs kode jfr ISO 3166.")
    override var navnerom: String,
    @Schema(description = "Identifikasjon av en spesiell versjon av et geografisk objekt (instans), maksimum lengde på 25 karakterer.")
    override var versjonid: String? = null
) : ContainsSosiIdentifikasjon

data class Dokref(
    @Schema(description = "URL til saksdokument.")
    var dokumentlenker: List<String> = emptyList(),
    @Schema(description = "Dato for når dokumentet ble skrevet, publisert eller revidert.")
    var fastsettingsdato: LocalDate,
    @Schema(description = "Offentlig instans som har fastsatt en grense.")
    var fastsettingsmyndighet: String? = null,
    @Schema(description = "Lov som rettskilden er begrunnet i.")
    var hjemmel: String? = null,
    @Schema(description = "Henvisning til saksdokument i Kartverkets eget arkiv.")
    var internReferanserKartverket: List<String> = emptyList(),
    @Schema(description = "Referanse til lov, forskrift, vedtak, dom eller traktat i form av kode som angir type dokument, dato og nummer For eksempel: LOV-2012-09-07-65.")
    var rettskildeId: String? = null,
    @Schema(description = "Navn på lov, forskrift, vedtak, dom eller traktat.")
    var rettskildeTittel: String
)
