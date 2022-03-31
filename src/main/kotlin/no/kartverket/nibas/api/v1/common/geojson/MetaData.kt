package no.kartverket.nibas.api.v1.common.geojson

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.domain.AdministrativGrense
import no.kartverket.nibas.domain.AnsvarligMyndighetHolder
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.CommonMetadataProperties
import no.kartverket.nibas.domain.ContainsSosiIdentifikasjon
import no.kartverket.nibas.domain.Grunnlinje
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.domain.MaritimGrense
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.inndelinger.createKommunenummerFromUUID
import no.kartverket.nibas.kodelister.KodelisteService
import java.time.LocalDate
import java.time.ZonedDateTime

const val ADMINISTRATIV_GRENSE_METADATA_SCHEMA: String = "AdministrativGrenseMetadata"
const val RIKSGRENSE_METADATA_SCHEMA: String = "RiksgrenseMetadata"
const val TERRITORIALGRENSE_METADATA_SCHEMA: String = "TerritorialgrenseMetadata"
const val AVTALT_AVGRENSNINGSLINJE_METADATA_SCHEMA: String = "AvtaltAvgrensningslinjeMetadata"
const val GRUNNLINJE_METADATA_SCHEMA: String = "GrunnlinjeMetadata"

const val DISCRIMINATOR_FIELD: String = "discriminator"

/**
 * Marker interface for typer med metadata som skal slippe ut av API-et for en grense/feature.
 *
 * Json-annotasjonene behøves for Jackson-deserialisering.
 * Schema-annotasjonen med mappinger er tilstede for generert OpenAPI (tidligere kalt Swagger)-dokumentasjon og koblinger
 * mellom skjemaene. Dette kan også brukes som grunnlag for generering av Typescript-kode.
 *
 * Property i JsonTypeInfo legger til et felt i responsen. Dette har 2 funksjoner:
 * 1. Diskriminator for Jackson-deserialisering
 * 2. Fungerer også som diskriminator for OpenAPI for å hjelpe bruker til å navigere i Schemas for den
 * polymorfiske typen.
 *
 */
@Schema(
    description = "Diverse metadata-felter fra sosi-modellen.",
    subTypes = [
        AdministrativGrenseMetadata::class,
        RiksgrenseMetadata::class,
        TerritorialgrenseMetadata::class,
        AvtaltAvgrensningslinjeMetadata::class,
        GrunnlinjeMetadata::class
    ],
    discriminatorProperty = DISCRIMINATOR_FIELD,
    discriminatorMapping = [
        DiscriminatorMapping(value = ADMINISTRATIV_GRENSE_METADATA_SCHEMA, schema = AdministrativGrenseMetadata::class),
        DiscriminatorMapping(value = RIKSGRENSE_METADATA_SCHEMA, schema = RiksgrenseMetadata::class),
        DiscriminatorMapping(value = TERRITORIALGRENSE_METADATA_SCHEMA, schema = TerritorialgrenseMetadata::class),
        DiscriminatorMapping(
            value = AVTALT_AVGRENSNINGSLINJE_METADATA_SCHEMA,
            schema = AvtaltAvgrensningslinjeMetadata::class
        ),
        DiscriminatorMapping(value = GRUNNLINJE_METADATA_SCHEMA, schema = GrunnlinjeMetadata::class),
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = DISCRIMINATOR_FIELD)
@JsonSubTypes(
    JsonSubTypes.Type(value = AdministrativGrenseMetadata::class, name = ADMINISTRATIV_GRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = RiksgrenseMetadata::class, name = RIKSGRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = TerritorialgrenseMetadata::class, name = TERRITORIALGRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = AvtaltAvgrensningslinjeMetadata::class, name = AVTALT_AVGRENSNINGSLINJE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = GrunnlinjeMetadata::class, name = GRUNNLINJE_METADATA_SCHEMA)
)
interface Metadata

@Schema(
    name = ADMINISTRATIV_GRENSE_METADATA_SCHEMA,
    description = "Spesfikk metadata for en administrativ grense (fylkes/kommunegrense). Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Objekttype/Index/EAID_5142EDBA_4226_4ca3_924D_EECEFD216D1A",
)
data class AdministrativGrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Opplysning om at grense følger naturlige skillelinjer i terrenget.")
    var foelgerTerrengdetalj: String? = null,
    @Schema(description = "Klassifikasjon av stedfestingsnøyaktighet på grenser, og er basert på dårligste nøyaktighet til grensepunktene til grensen.")
    var noeyaktighetsklasse: String? = null,
    @Schema(description = "Angir om grensen er omtvistet, eller det er tvil om forløpet.")
    var omtvistet: Boolean? = null,
) : Metadata

@Schema(
    name = RIKSGRENSE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en riksgrense. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Objekttype/Index/EAID_5C4B9E4D_05D7_4b2e_B467_9FCA428994F9",
)
data class RiksgrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Opplysning om at grense følger naturlige skillelinjer i terrenget.")
    var foelgerTerrengdetalj: String? = null,
    @Schema(description = "Angivelse om stedfestingen (koordinatene) er kontrollert og funnet i orden (verifisert).")
    var stedfestingVerifisert: Boolean? = null,
    @Schema(description = "Organisasjon(er) som er ansvarlig for opprettholdelse av grensa.")
    var ansvarligeMyndigheter: List<TekstHolder> = emptyList(),
    @Schema(description = "Bredde av området på begge sider av riksgrensen som er ryddet for vegetasjon høyere enn 0,5 m.")
    var grensegateBredde: Double? = null,
) : Metadata

@Schema(
    name = TERRITORIALGRENSE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en territorialgrense. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class TerritorialgrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Metadata-egenskaper for maritim grense.")
    var maritimeGrenser: MaritimeGrenserMetadata = MaritimeGrenserMetadata(),
    @Schema(description = "Tidspunktet for når koordinatene er utregnet.")
    var beregningsdato: LocalDate? = null
) : Metadata

@Schema(
    name = AVTALT_AVGRENSNINGSLINJE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en AvtaltAvgrensningslinje. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class AvtaltAvgrensningslinjeMetadata(
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Metadata-egenskaper for maritim grense.")
    var maritimeGrenser: MaritimeGrenserMetadata = MaritimeGrenserMetadata(),
) : Metadata

@Schema(
    name = GRUNNLINJE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en Grunnlinje. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class GrunnlinjeMetadata(
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
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
    var informasjon: String? = null,
    @Schema(description = "Dato for siste endring på objektdataene")
    var oppdateringsdato: ZonedDateTime = ZonedDateTime.now(),
    @Schema(description = "Referanse til opphavsmaterialet, kildematerialet, organisasjons/publiseringskilde.")
    var opphav: String? = null,
)

data class CommonGrenseMetadata(
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    var dokumentasjonsreferanser: List<Dokref> = emptyList(),
    @Schema(description = "Beskrivelse av kvaliteten på stedfestingen")
    var posisjonskvalitet: Posisjonskvalitet? = null,
    @Schema(description = "Juridisk status på grensa.")
    var grensestatus: String? = null,
    @Schema(description = "Måte en grense er fastsatt på.")
    var fastsettingstype: String? = null
)

data class TekstHolder(
    @Schema(description = "ID for elementet.")
    val id: String? = null,
    @Schema(description = "Tekstfelt.")
    val beskrivelse: String
)

data class MaritimeGrenserMetadata(
    @Schema(description = "Organisasjon som er ansvarlig for opprettholdelse av grensa.")
    var ansvarligeMyndigheter: List<TekstHolder> = emptyList(),
    @Schema(description = "Navn på område som loven gjelder for, eller område som grensa er beregnet fra.")
    var gjelderOmraade: String? = null,
    @Schema(description = "Tidspunktet for når koordinatene er transformert fra originalt koordinatsystem.")
    var transformasjonsdato: LocalDate? = null,
    @Schema(description = "Begrep knyttet til yttergrensa for virkeområdet, som oftest angitt i lov eller forskrift.")
    var virkeomraadenavn: String? = null,
    @Schema(description = "Angivelse av hvilke land grensa er knyttet til Merknad: Alfanumerisk kode (ISO 3166-1 alpha-2) for land som definert i ISO 3166.")
    var land: List<TekstHolder> = emptyList(),
)

data class Posisjonskvalitet(
    @Schema(description = "Metode for måling i grunnriss (x,y), og høyde (z) når metoden er den samme som ved måling i grunnriss.")
    var maalemetode: String = "",
    @Schema(description = "Punktstandardavviket i grunnriss for punkter samt tverravvik for linjer. Merknad: Oppgitt i cm.")
    var noeyaktighet: Int? = null
)

fun Posisjonskvalitet.toEmbeddable(): PosisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(maalemetode, noeyaktighet)

data class Identifikasjon(
    @Schema(description = "Lokal id som identifiserer geometrien. Dette er en globalt unik id.")
    override var lokalid: String,
    @Schema(description = "Navnerom som unikt identifiserer datakilden til objektet, starter med to bokstavs kode jfr ISO 3166.")
    override var navnerom: String,
    @Schema(description = "Identifikasjon av en spesiell versjon av et geografisk objekt (instans), maksimum lengde på 25 karakterer.")
    override var versjonid: String? = null
) : ContainsSosiIdentifikasjon

data class Dokref(
    @Schema(description = "ID for dokumentasjonsreferansen.")
    var id: String? = null,
    @Schema(description = "URL til saksdokument.")
    var dokumentlenker: List<TekstHolder> = emptyList(),
    @Schema(description = "Dato for når dokumentet ble skrevet, publisert eller revidert.")
    var fastsettingsdato: LocalDate = LocalDate.now(),
    @Schema(description = "Offentlig instans som har fastsatt en grense.")
    var fastsettingsmyndighet: String? = null,
    @Schema(description = "Lov som rettskilden er begrunnet i.")
    var hjemmel: String? = null,
    @Schema(description = "Henvisning til saksdokument i Kartverkets eget arkiv.")
    var internReferanserKartverket: List<TekstHolder> = emptyList(),
    @Schema(description = "Referanse til lov, forskrift, vedtak, dom eller traktat i form av kode som angir type dokument, dato og nummer For eksempel: LOV-2012-09-07-65.")
    var rettskildeId: String? = null,
    @Schema(description = "Navn på lov, forskrift, vedtak, dom eller traktat.")
    var rettskildeTittel: String
)

fun AdministrativGrense.toMetadata(): AdministrativGrenseMetadata = AdministrativGrenseMetadata(
    common = this.getCommonMetadata(),
    commonGrense = this.getGrenseMetadata(),
    foelgerTerrengdetalj = this.foelgerterrengdetalj?.id,
    noeyaktighetsklasse = this.noeyaktighetsklasse?.id,
    omtvistet = this.omtvistet
)

fun AdministrativGrense.updateMetadata(metadata: AdministrativGrenseMetadata) {
    updateWithCommonMetadata(metadata.common)
    updateWithGrenseMetadata(metadata.commonGrense)
    foelgerterrengdetalj?.id = metadata.foelgerTerrengdetalj
    noeyaktighetsklasse?.id = metadata.noeyaktighetsklasse
    omtvistet = metadata.omtvistet
}

fun Riksgrense.toMetadata(): RiksgrenseMetadata = RiksgrenseMetadata(
    common = this.getCommonMetadata(),
    commonGrense = this.getGrenseMetadata(),
    foelgerTerrengdetalj = this.foelgerterrengdetalj?.id,
    stedfestingVerifisert = this.stedfestingverifisert,
    ansvarligeMyndigheter = this.riksgrenseAnsvarligeMyndigheter
        .filter { it.ansvarligmyndighet != null }
        .map { TekstHolder(it.id, it.ansvarligmyndighet!!) },
    grensegateBredde = this.grensegatebredde
)

fun Riksgrense.updateMetadata(metadata: RiksgrenseMetadata) {
    updateWithCommonMetadata(metadata.common)
    updateWithGrenseMetadata(metadata.commonGrense)
    foelgerterrengdetalj?.id = metadata.foelgerTerrengdetalj
    stedfestingverifisert = metadata.stedfestingVerifisert
    setAnsvarligeMyndigheter(toAnsvarligeMyndigheter(metadata.ansvarligeMyndigheter))
    grensegatebredde = metadata.grensegateBredde
}

fun Avtaltavgrensningslinje.toMetadata(): AvtaltAvgrensningslinjeMetadata = AvtaltAvgrensningslinjeMetadata(
    common = this.getCommonMetadata(),
    commonGrense = this.getGrenseMetadata(),
    maritimeGrenser = this.getMaritimeMetadata()
)

fun Avtaltavgrensningslinje.updateMetadata(metadata: AvtaltAvgrensningslinjeMetadata) {
    updateWithCommonMetadata(metadata.common)
    updateWithGrenseMetadata(metadata.commonGrense)
    updateWithMaritimeMetadata(metadata.maritimeGrenser)
}

fun Territorialgrense.toMetadata(): TerritorialgrenseMetadata = TerritorialgrenseMetadata(
    common = this.getCommonMetadata(),
    commonGrense = this.getGrenseMetadata(),
    maritimeGrenser = this.getMaritimeMetadata(),
    beregningsdato = this.beregningsdato
)

fun Territorialgrense.updateMetadata(metadata: TerritorialgrenseMetadata) {
    updateWithCommonMetadata(metadata.common)
    updateWithGrenseMetadata(metadata.commonGrense)
    updateWithMaritimeMetadata(metadata.maritimeGrenser)
    beregningsdato = metadata.beregningsdato
}

fun Grunnlinje.toMetadata(kodelisteService: KodelisteService): GrunnlinjeMetadata {
    val grunnlinjeMetadata = GrunnlinjeMetadata(
        common = this.getCommonMetadata(),
        commonGrense = this.getGrenseMetadata(),
        maritimeGrenser = this.getMaritimeMetadata()
    )
    if (this.kommunenummerId != null) {
        grunnlinjeMetadata.kommunenummer = createKommunenummerFromUUID(this.kommunenummerId!!, kodelisteService)
    }
    return grunnlinjeMetadata
}

private fun CommonMetadataProperties.getCommonMetadata(): CommonMetadata = CommonMetadata(
    identifikasjon = Identifikasjon(this.lokalid, this.navnerom, this.versjonid),
    datafangstdato = this.datafangstdato,
    gyldigFra = this.gyldigFra,
    gyldigTil = this.gyldigTil,
    informasjon = this.getInfo(),
    oppdateringsdato = this.oppdateringsdato,
    opphav = this.opphav,
)

// Identifikasjon ignoreres, dette skal ikke kunne oppdateres, burde egentlig ikke være med på tur inn
private fun CommonMetadataProperties.updateWithCommonMetadata(common: CommonMetadata) {
    this.datafangstdato = common.datafangstdato
    this.gyldigFra = common.gyldigFra
    this.gyldigTil = common.gyldigTil
    this.setInfo(common.informasjon)
    this.oppdateringsdato = ZonedDateTime.now()
    this.opphav = common.opphav
}

private fun MaritimGrense.getMaritimeMetadata(): MaritimeGrenserMetadata = MaritimeGrenserMetadata(
    ansvarligeMyndigheter = this.getAnsvarligeMyndigheter()
        .filter { it.ansvarligmyndighet != null }
        .map { TekstHolder(it.id, it.ansvarligmyndighet!!) },
    gjelderOmraade = this.gjelderomraade,
    transformasjonsdato = this.transformasjonsdato,
    virkeomraadenavn = this.virkeomraadenavn,
    land = this.getLand()
        .map { TekstHolder(it.id!!, it.description!!) }
        .sortedBy { it.id }
)

private fun MaritimGrense.updateWithMaritimeMetadata(maritimeGrenserMetadata: MaritimeGrenserMetadata) {
    this.setAnsvarligeMyndigheter(toAnsvarligeMyndigheter(maritimeGrenserMetadata.ansvarligeMyndigheter))
    this.gjelderomraade = maritimeGrenserMetadata.gjelderOmraade
    this.transformasjonsdato = maritimeGrenserMetadata.transformasjonsdato
    this.virkeomraadenavn = maritimeGrenserMetadata.virkeomraadenavn
    this.setLand(maritimeGrenserMetadata.toListeAvLand())
}

private fun toAnsvarligeMyndigheter(tekstholdere: List<TekstHolder>): List<AnsvarligMyndighetHolder> =
    tekstholdere.map { AnsvarligMyndighetHolder(id = it.id, ansvarligmyndighet = it.beskrivelse) }

private fun MaritimeGrenserMetadata.toListeAvLand(): List<Landkode> = this.land.map {
    val landkode = Landkode()
    landkode.id = it.id
    landkode.description = it.beskrivelse
    landkode
}
