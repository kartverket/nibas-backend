package no.kartverket.nibas.api.v1.common.geojson

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.domain.AdministrativGrense
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.CommonMetadataProperties
import no.kartverket.nibas.domain.ContainsSosiIdentifikasjon
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Grunnlinje
import no.kartverket.nibas.domain.MaritimGrense
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.inndelinger.createKommunenummerFromUUID
import no.kartverket.nibas.kodelister.KodelisteService
import java.time.LocalDate
import java.time.ZonedDateTime

const val administrativGrenseMetadataSchema: String = "AdministrativGrenseMetadata"
const val riksgrenseMetadataSchema: String = "RiksgrenseMetadata"
const val territorialgrenseMetadataSchema: String = "TerritorialgrenseMetadata"
const val avtaltAvgrensningslinjeMetadataSchema: String = "AvtaltAvgrensningslinjeMetadata"
const val grunnlinjeMetadataSchema: String = "GrunnlinjeMetadata"

const val discriminatorField: String = "discriminator"

/**
 * Marker interface for typer med metadata som skal slippe ut av API-et for en grense/feature. JsonTypeInfo-property
 * legger til et felt i responsen. Dette har 2 funksjoner:
 * 1. Diskriminator for Jackson-deserialisering
 * 2. Fungerer også som diskriminator for Swagger for å hjelpe bruker til å navigere i Swagger-Schemas for den
 * polymorfiske typen.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = discriminatorField)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = AdministrativGrenseMetadata::class, name = administrativGrenseMetadataSchema),
        JsonSubTypes.Type(value = RiksgrenseMetadata::class, name = riksgrenseMetadataSchema),
        JsonSubTypes.Type(value = TerritorialgrenseMetadata::class, name = territorialgrenseMetadataSchema),
        JsonSubTypes.Type(value = AvtaltAvgrensningslinjeMetadata::class, name = avtaltAvgrensningslinjeMetadataSchema),
        JsonSubTypes.Type(value = GrunnlinjeMetadata::class, name = grunnlinjeMetadataSchema)
    ]
)
interface Metadata

@Schema(
    name = administrativGrenseMetadataSchema,
    description = "Spesfikk metadata for en administrativ grense (fylkes/kommunegrense). Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Objekttype/Index/EAID_5142EDBA_4226_4ca3_924D_EECEFD216D1A",
)
data class AdministrativGrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    val common: CommonMetadata,
    @Schema(description = "Opplysning om at grense følger naturlige skillelinjer i terrenget.")
    var foelgerTerrengdetalj: String? = null,
    @Schema(description = "Klassifikasjon av stedfestingsnøyaktighet på grenser, og er basert på dårligste nøyaktighet til grensepunktene til grensen.")
    var noeyaktighetsklasse: String? = null,
    @Schema(description = "Angir om grensen er omtvistet, eller det er tvil om forløpet.")
    var omtvistet: Boolean? = null,
) : Metadata

@Schema(
    name = riksgrenseMetadataSchema,
    description = "Spesifikk metadata for en riksgrense. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Objekttype/Index/EAID_5C4B9E4D_05D7_4b2e_B467_9FCA428994F9",
)
data class RiksgrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    val common: CommonMetadata,
    @Schema(description = "Opplysning om at grense følger naturlige skillelinjer i terrenget.")
    var foelgerTerrengdetalj: String? = null,
    @Schema(description = "Angivelse om stedfestingen (koordinatene) er kontrollert og funnet i orden (verifisert).")
    var stedfestingVerifisert: Boolean? = null,
    @Schema(description = "Organisasjon(er) som er ansvarlig for opprettholdelse av grensa.")
    var ansvarligeMyndigheter: List<String> = emptyList(),
    @Schema(description = "Bredde av området på begge sider av riksgrensen som er ryddet for vegetasjon høyere enn 0,5 m.")
    var grensegateBredde: Double? = null,
) : Metadata

@Schema(
    name = territorialgrenseMetadataSchema,
    description = "Spesifikk metadata for en territorialgrense. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class TerritorialgrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val common: CommonMetadata,
    @Schema(description = "Metadata-egenskaper for maritim grense.")
    var maritimeGrenser: MaritimeGrenserMetadata = MaritimeGrenserMetadata(),
    @Schema(description = "Tidspunktet for når koordinatene er utregnet.")
    var beregningsdato: LocalDate? = null
) : Metadata

@Schema(
    name = avtaltAvgrensningslinjeMetadataSchema,
    description = "Spesifikk metadata for en AvtaltAvgrensningslinje. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class AvtaltAvgrensningslinjeMetadata(
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val common: CommonMetadata,
    @Schema(description = "Metadata-egenskaper for maritim grense.")
    var maritimeGrenser: MaritimeGrenserMetadata = MaritimeGrenserMetadata(),
) : Metadata

@Schema(
    name = grunnlinjeMetadataSchema,
    description = "Spesifikk metadata for en Grunnlinje. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class GrunnlinjeMetadata(
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val common: CommonMetadata,
    @Schema(description = "Metadata-egenskaper for maritim grense.")
    var maritimeGrenser: MaritimeGrenserMetadata = MaritimeGrenserMetadata(),
    @Schema(description = "Nummerering av kommunen i henhold til Statistisk sentralbyrå sin offisielle liste.")
    var kommunenummer: Kommunenummer? = null
) : Metadata

data class CommonMetadata(
    @Schema(description = "Unik identifikasjon av et objekt")
    var identifikasjon: Identifikasjon,
    @Schema(description = "Dato når objektet siste gang ble registrert/observert/målt i terrenget")
    var datafangstdato: ZonedDateTime? = null,
    @Schema(description = "Tidspunktet når objektet oppstod i den virkelige verden")
    var gyldigFra: ZonedDateTime? = null,
    @Schema(description = "Tidspunktet når objektet opphørte å eksistere i den virkelige verden")
    var gyldigTil: ZonedDateTime? = null,
    @Schema(description = "Generelle opplysninger/merknad")
    var informasjonselementer: List<String> = emptyList(),
    @Schema(description = "Beskrivelse av kvaliteten på stedfestingen")
    var posisjonskvalitet: Posisjonskvalitet? = null,
    @Schema(description = "Dato for siste endring på objektdataene")
    var oppdateringsdato: ZonedDateTime = ZonedDateTime.now(),
    @Schema(description = "Referanse til opphavsmaterialet, kildematerialet, organisasjons/publiseringskilde.")
    var opphav: String? = null,
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    var dokumentasjonsreferanser: List<Dokref> = emptyList(),
    @Schema(description = "Måte en grense er fastsatt på.")
    var fastsettingstype: String? = null,
    @Schema(description = "Juridisk status på grensa.")
    var grensestatus: String? = null
)

data class MaritimeGrenserMetadata(
    @Schema(description = "Organisasjon som er ansvarlig for opprettholdelse av grensa.")
    var ansvarligeMyndigheter: List<String> = emptyList(),
    @Schema(description = "Navn på område som loven gjelder for, eller område som grensa er beregnet fra.")
    var gjelderOmraade: String? = null,
    @Schema(description = "Tidspunktet for når koordinatene er transformert fra originalt koordinatsystem.")
    var transformasjonsdato: LocalDate? = null,
    @Schema(description = "Begrep knyttet til yttergrensa for virkeområdet, som oftest angitt i lov eller forskrift.")
    var virkeomraadenavn: String? = null,
    @Schema(description = "Angivelse av hvilke land grensa er knyttet til Merknad: Alfanumerisk kode (ISO 3166-1 alpha-2) for land som definert i ISO 3166.")
    var land: List<Land> = emptyList(),
)

data class Land(
    @Schema(description = "ID for landet.")
    var id: String,
    @Schema(description = "Beskrivelse av landet.")
    var description: String
)

data class Posisjonskvalitet(
    @Schema(description = "Metode for måling i grunnriss (x,y), og høyde (z) når metoden er den samme som ved måling i grunnriss.")
    var maalemetode: String = "",
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

fun PosisjonskvalitetEmbeddable.toKvalitet(): Posisjonskvalitet =
    Posisjonskvalitet(maalemetode, noeyaktighet)

fun CommonMetadataProperties.getCommonMetadata(): CommonMetadata = CommonMetadata(
    datafangstdato = this.datafangstdato,
    gyldigFra = this.gyldigFra,
    gyldigTil = this.gyldigTil,
    informasjonselementer = this.getGrenseInfoElementer().mapNotNull { it.informasjon },
    posisjonskvalitet = this.kvalitet?.toKvalitet(),
    oppdateringsdato = this.oppdateringsdato,
    opphav = this.opphav,
    identifikasjon = Identifikasjon(this.lokalid, this.navnerom, this.versjonid),
    dokumentasjonsreferanser = this.dokumentasjonsreferanser.map { it.toDokRef() }, //TODO: Kanskje sortere?
    fastsettingstype = this.fastsettingstype?.id,
    grensestatus = this.grensestatus?.id,
)

fun MaritimGrense.getMaritimeMetadata(): MaritimeGrenserMetadata = MaritimeGrenserMetadata(
    ansvarligeMyndigheter = this.getAnsvarligeMyndigheter().mapNotNull { it.ansvarligmyndighet },
    gjelderOmraade = this.gjelderomraade,
    transformasjonsdato = this.transformasjonsdato,
    virkeomraadenavn = this.virkeomraadenavn,
    land = this.getLand().map { Land(it.id!!, it.description!!) }.sortedBy { it.id }
)

fun AdministrativGrense.toMetadata(): Metadata = AdministrativGrenseMetadata(
    common = this.getCommonMetadata(),
    foelgerTerrengdetalj = this.foelgerterrengdetalj?.id,
    noeyaktighetsklasse = this.noeyaktighetsklasse?.id,
    omtvistet = this.omtvistet
)

fun Riksgrense.toMetadata(): Metadata = RiksgrenseMetadata(
    common = this.getCommonMetadata(),
    foelgerTerrengdetalj = this.foelgerterrengdetalj?.id,
    stedfestingVerifisert = this.stedfestingverifisert,
    ansvarligeMyndigheter = this.getAnsvarligeMyndigheter(),
    grensegateBredde = this.grensegatebredde
)

fun Avtaltavgrensningslinje.toMetadata(): Metadata = AvtaltAvgrensningslinjeMetadata(
    common = this.getCommonMetadata(),
    maritimeGrenser = this.getMaritimeMetadata()
)

fun Territorialgrense.toMetadata(): Metadata = TerritorialgrenseMetadata(
    common = this.getCommonMetadata(),
    maritimeGrenser = this.getMaritimeMetadata(),
    beregningsdato = this.beregningsdato
)

fun Grunnlinje.toMetadata(kodelisteService: KodelisteService): Metadata {
    var grunnlinjeMetadata = GrunnlinjeMetadata(
        common = this.getCommonMetadata(),
        maritimeGrenser = this.getMaritimeMetadata()
    )
    if (this.kommunenummerId != null) {
        grunnlinjeMetadata =
            grunnlinjeMetadata.copy(
                kommunenummer = createKommunenummerFromUUID(
                    this.kommunenummerId!!,
                    kodelisteService
                )
            )
    }
    return grunnlinjeMetadata
}

fun Dokumentasjonsreferanse.toDokRef(): Dokref = Dokref(
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
