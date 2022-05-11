package no.kartverket.nibas.api.v1.common.geojson.metadata

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.domain.ContainsSosiIdentifikasjon
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import java.time.LocalDate

const val ADMINISTRATIV_GRENSE_METADATA_SCHEMA: String = "AdministrativGrenseMetadata"
const val RIKSGRENSE_METADATA_SCHEMA: String = "RiksgrenseMetadata"
const val TERRITORIALGRENSE_METADATA_SCHEMA: String = "TerritorialgrenseMetadata"
const val AVTALT_AVGRENSNINGSLINJE_METADATA_SCHEMA: String = "AvtaltAvgrensningslinjeMetadata"
const val GRUNNLINJE_METADATA_SCHEMA: String = "GrunnlinjeMetadata"
const val STATISTISKGRENSE_METADATA_SCHEMA: String = "StatistiskgrenseMetadata"

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
        GrunnlinjeMetadata::class,
        StatistiskgrenseMetadata::class
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
        DiscriminatorMapping(value = STATISTISKGRENSE_METADATA_SCHEMA, schema = StatistiskgrenseMetadata::class)
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = DISCRIMINATOR_FIELD)
@JsonSubTypes(
    JsonSubTypes.Type(value = AdministrativGrenseMetadata::class, name = ADMINISTRATIV_GRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = RiksgrenseMetadata::class, name = RIKSGRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = TerritorialgrenseMetadata::class, name = TERRITORIALGRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = AvtaltAvgrensningslinjeMetadata::class, name = AVTALT_AVGRENSNINGSLINJE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = GrunnlinjeMetadata::class, name = GRUNNLINJE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = StatistiskgrenseMetadata::class, name = STATISTISKGRENSE_METADATA_SCHEMA)
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
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    var dokumentasjonsreferanser: List<Dokref> = emptyList(),
    @Schema(description = "Opplysning om at grense følger naturlige skillelinjer i terrenget.")
    var foelgerTerrengdetalj: KodelisteEntry = KodelisteEntry(),
    @Schema(description = "Klassifikasjon av stedfestingsnøyaktighet på grenser, og er basert på dårligste nøyaktighet til grensepunktene til grensen.")
    var noeyaktighetsklasse: KodelisteEntry = KodelisteEntry(),
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
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    var dokumentasjonsreferanser: List<Dokref> = emptyList(),
    @Schema(description = "Opplysning om at grense følger naturlige skillelinjer i terrenget.")
    var foelgerTerrengdetalj: KodelisteEntry = KodelisteEntry(),
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
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    var dokumentasjonsreferanser: List<Dokref> = emptyList(),
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
    @Schema(description = "Felles metadata-egenskaper.")
    val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    var dokumentasjonsreferanser: List<Dokref> = emptyList(),
    @Schema(description = "Metadata-egenskaper for maritim grense.")
    var maritimeGrenser: MaritimeGrenserMetadata = MaritimeGrenserMetadata(),
) : Metadata

@Schema(
    name = GRUNNLINJE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en Grunnlinje. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class GrunnlinjeMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    var dokumentasjonsreferanser: List<Dokref> = emptyList(),
    @Schema(description = "Metadata-egenskaper for maritim grense.")
    var maritimeGrenser: MaritimeGrenserMetadata = MaritimeGrenserMetadata(),
    @Schema(description = "Nummerering av kommunen i henhold til Statistisk sentralbyrå sin offisielle liste.")
    var kommunenummer: Kommunenummer? = null
) : Metadata

@Schema(
    name = STATISTISKGRENSE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en statistisk grense (grunnkrets/delområde). Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_A3C68E12_E409_42c0_937D_6C1B62351E42",
)
data class StatistiskgrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Opplysning om at grense følger naturlige skillelinjer i terrenget.")
    var foelgerTerrengdetalj: KodelisteEntry = KodelisteEntry(),
    @Schema(description = "Klassifikasjon av stedfestingsnøyaktighet på grenser, og er basert på dårligste nøyaktighet til grensepunktene til grensen.")
    var noeyaktighetsklasse: KodelisteEntry = KodelisteEntry(),
) : Metadata

data class CommonMetadata(
    @Schema(description = "Unik identifikasjon av et objekt")
    var identifikasjon: Identifikasjon,
    @Schema(description = "Dato når objektet siste gang ble registrert/observert/målt i terrenget")
    var datafangstdato: LocalDate? = null,
    @Schema(description = "Tidspunktet når objektet oppstod i den virkelige verden")
    var gyldigFra: LocalDate? = null,
    @Schema(description = "Tidspunktet når objektet opphørte å eksistere i den virkelige verden")
    var gyldigTil: LocalDate? = null,
    @Schema(description = "Generelle opplysninger/merknad")
    var informasjon: String? = null,
    @Schema(description = "Holder på informasjon om endring av objektet.")
    var sporingsinformasjon: Sporingsinformasjon = Sporingsinformasjon(),
    @Schema(description = "Referanse til opphavsmaterialet, kildematerialet, organisasjons/publiseringskilde.")
    var opphav: String? = null,
)

data class CommonGrenseMetadata(
    @Schema(description = "Beskrivelse av kvaliteten på stedfestingen")
    var posisjonskvalitet: Posisjonskvalitet? = null,
    @Schema(description = "Juridisk status på grensa.")
    var grensestatus: KodelisteEntry = KodelisteEntry(),
    @Schema(description = "Måte en grense er fastsatt på.")
    var fastsettingstype: KodelisteEntry = KodelisteEntry()
)

data class KodelisteEntry(
    @Schema(description = "Id for kodeliste-innslaget.")
    val id: String? = null,
    @Schema(description = "Lenke til kodelista.")
    val href: String = "")

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
    var land: Landkoder = Landkoder(),
)

data class Landkoder(
    @Schema(description = "Liste av valgte landkode-ids.")
    var ids: List<String> = emptyList(),
    @Schema(description = "Peker til landkoder-kodeliste.")
    var href: String = "")

data class Posisjonskvalitet(
    @Schema(description = "Metode for måling i grunnriss (x,y), og høyde (z) når metoden er den samme som ved måling i grunnriss.")
    var maalemetode: KodelisteEntry = KodelisteEntry(),
    @Schema(description = "Punktstandardavviket i grunnriss for punkter samt tverravvik for linjer. Merknad: Oppgitt i cm.")
    var noeyaktighet: Int? = null
)

fun Posisjonskvalitet.toEmbeddable(): PosisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(
    maalemetode = this.maalemetode.id!!,
    noeyaktighet = noeyaktighet)

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

data class Sporingsinformasjon(
    @Schema(description = "Dato for siste endring på objektdataene")
    var oppdateringsdato: LocalDate = LocalDate.now(),
    @Schema(description = "Angir hvem som endret data på objektet sist.")
    var endretAv: String? = null
)
