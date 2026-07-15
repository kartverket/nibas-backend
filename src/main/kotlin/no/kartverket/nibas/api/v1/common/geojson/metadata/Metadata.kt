package no.kartverket.nibas.api.v1.common.geojson.metadata

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.domain.ContainsSosiIdentifikasjon
import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import java.time.LocalDate
import java.time.ZonedDateTime
import jakarta.validation.constraints.NotNull

const val FLATE_METADATA_SCHEMA: String = "FlateMetadata"
const val ADMINISTRATIV_GRENSE_METADATA_SCHEMA: String = "AdministrativGrenseMetadata"
const val RIKSGRENSE_METADATA_SCHEMA: String = "RiksgrenseMetadata"
const val TERRITORIALGRENSE_METADATA_SCHEMA: String = "TerritorialgrenseMetadata"
const val AVTALT_AVGRENSNINGSLINJE_METADATA_SCHEMA: String = "AvtaltAvgrensningslinjeMetadata"
const val GRUNNLINJE_METADATA_SCHEMA: String = "GrunnlinjeMetadata"
const val STATISTISKGRENSE_METADATA_SCHEMA: String = "StatistiskgrenseMetadata"
const val KOMMUNAL_KRETSGRENSE_METADATA_SCHEMA: String = "KommunalKretsgrenseMetadata"

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
        FlateMetadata::class,
        AdministrativGrenseMetadata::class,
        RiksgrenseMetadata::class,
        TerritorialgrenseMetadata::class,
        AvtaltAvgrensningslinjeMetadata::class,
        GrunnlinjeMetadata::class,
        StatistiskgrenseMetadata::class
    ],
    discriminatorProperty = DISCRIMINATOR_FIELD,
    discriminatorMapping = [
        DiscriminatorMapping(value = FLATE_METADATA_SCHEMA, schema = FlateMetadata::class),
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
    JsonSubTypes.Type(value = FlateMetadata::class, name = FLATE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = AdministrativGrenseMetadata::class, name = ADMINISTRATIV_GRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = RiksgrenseMetadata::class, name = RIKSGRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = TerritorialgrenseMetadata::class, name = TERRITORIALGRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = AvtaltAvgrensningslinjeMetadata::class, name = AVTALT_AVGRENSNINGSLINJE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = GrunnlinjeMetadata::class, name = GRUNNLINJE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = StatistiskgrenseMetadata::class, name = STATISTISKGRENSE_METADATA_SCHEMA),
    JsonSubTypes.Type(value = KommunalKretsgrenseMetadata::class, name = KOMMUNAL_KRETSGRENSE_METADATA_SCHEMA)
)
interface Metadata

interface HarCommonMetadata {
    val common: CommonMetadata
}

interface HarCommonGrense {
    val commonGrense: CommonGrenseMetadata
}

interface HarDokumentasjonsreferanse {
    var dokumentasjonsreferanser: List<DokumentasjonsreferanseDTO>
}

@Schema(
    name = FLATE_METADATA_SCHEMA,
    description = "Metadata for en flate (felles for nasjon, fylke, kommune og grunnkrets)",
)
data class FlateMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    override val common: CommonMetadata,
) : Metadata, HarCommonMetadata

@Schema(
    name = ADMINISTRATIV_GRENSE_METADATA_SCHEMA,
    description = "Spesfikk metadata for en administrativ grense (fylkes/kommunegrense). Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Objekttype/Index/EAID_5142EDBA_4226_4ca3_924D_EECEFD216D1A",
)
data class AdministrativGrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    override val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    override val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    override var dokumentasjonsreferanser: List<DokumentasjonsreferanseDTO> = emptyList(),
) : Metadata, HarCommonMetadata, HarDokumentasjonsreferanse, HarCommonGrense

@Schema(
    name = RIKSGRENSE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en riksgrense. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Objekttype/Index/EAID_5C4B9E4D_05D7_4b2e_B467_9FCA428994F9",
)
data class RiksgrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    override val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    override val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    override var dokumentasjonsreferanser: List<DokumentasjonsreferanseDTO> = emptyList(),
) : Metadata, HarCommonMetadata, HarCommonGrense, HarDokumentasjonsreferanse

@Schema(
    name = TERRITORIALGRENSE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en territorialgrense. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class TerritorialgrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    override val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    override val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    override var dokumentasjonsreferanser: List<DokumentasjonsreferanseDTO> = emptyList(),
) : Metadata, HarCommonMetadata, HarCommonGrense, HarDokumentasjonsreferanse

@Schema(
    name = AVTALT_AVGRENSNINGSLINJE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en AvtaltAvgrensningslinje. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class AvtaltAvgrensningslinjeMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    override val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    override val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    override var dokumentasjonsreferanser: List<DokumentasjonsreferanseDTO> = emptyList(),
) : Metadata, HarCommonMetadata, HarCommonGrense, HarDokumentasjonsreferanse

@Schema(
    name = GRUNNLINJE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en Grunnlinje. Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Diagram/Index/EAID_EEECEE48_B3FA_4807_AAE4_B30B63BC28E1",
)
data class GrunnlinjeMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    override val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    override val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata(),
    @Schema(description = "Henviser til fastsettings- eller lovinformasjon.")
    override var dokumentasjonsreferanser: List<DokumentasjonsreferanseDTO> = emptyList(),
) : Metadata, HarCommonMetadata, HarCommonGrense, HarDokumentasjonsreferanse

@Schema(
    name = STATISTISKGRENSE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en statistisk grense (grunnkrets/delområde). Beskrevet i SOSI-modellen her: https://objektkatalog.geonorge.no/Objekttype/Index/EAID_374BE5B3_5F8B_498a_8F53_9CCA72619C36",
)
data class StatistiskgrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    override val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    override val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata()
) : Metadata, HarCommonMetadata, HarCommonGrense

@Schema(
    name = KOMMUNAL_KRETSGRENSE_METADATA_SCHEMA,
    description = "Spesifikk metadata for en kommunal kretsgrense (stemmekretsgrense/valgdistriktgrense/skolekretsgrense)"
)
data class KommunalKretsgrenseMetadata(
    @Schema(description = "Felles metadata-egenskaper.")
    override val common: CommonMetadata,
    @Schema(description = "Felles metadata-egenskaper for grenser.")
    override val commonGrense: CommonGrenseMetadata = CommonGrenseMetadata()
) : Metadata, HarCommonMetadata, HarCommonGrense

data class CommonMetadata(
    @Schema(description = "Unik identifikasjon av et objekt")
    var identifikasjon: Identifikasjon,
    @Schema(description = "Dato når objektet siste gang ble registrert/observert/målt i terrenget")
    var datafangstdato: ZonedDateTime? = null,
    @Schema(description = "Tidspunktet når objektet oppstod i den virkelige verden")
    var gyldigFra: LocalDate,
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
    var posisjonskvalitet: Posisjonskvalitet = Posisjonskvalitet(),
)

data class KodelisteEntry(
    @Schema(
        description = "Id for kodeliste-innslaget.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    val id: String? = null,
    @Schema(description = "Lenke til kodelista.")
    val href: String = "")

data class TekstHolder(
    @Schema(description = "ID for elementet.")
    val id: String? = null,
    @Schema(description = "Tekstfelt.")
    val beskrivelse: String
)

data class Landkoder(
    @Schema(description = "Liste av valgte landkode-ids.")
    var ids: List<String> = emptyList(),
    @Schema(description = "Peker til landkoder-kodeliste.")
    var href: String = "")

data class Posisjonskvalitet(
    @Schema(description = "Metode for måling i grunnriss (x,y), og høyde (z) når metoden er den samme som ved måling i grunnriss.")
    var maalemetode: KodelisteEntry = KodelisteEntry(),
    @Schema(
        description = "Punktstandardavviket i grunnriss for punkter samt tverravvik for linjer. Merknad: Oppgitt i cm.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    var noeyaktighet: Int? = null,
)

fun Posisjonskvalitet.toEmbeddable(): PosisjonskvalitetEmbeddable = PosisjonskvalitetEmbeddable(
    maalemetode = this.maalemetode.id ?: "",
    noeyaktighet = noeyaktighet
)

data class Identifikasjon(
    @Schema(description = "Lokal id som identifiserer geometrien. Dette er en globalt unik id.")
    override val lokalid: String
) : ContainsSosiIdentifikasjon

@Schema(description = "Identifikator av en stemmekrets eller grunnkrets")
data class IdentifikatorMedVersjon(
    @Schema(description = "LokalId for stemmekrets/grunnkretsen")
    val lokalId: String,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int
)

data class DokumentasjonsreferanseDTO(
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
    var rettskildeTittel: String,
    @Schema(description = "Tidspunktet når objektet oppstod i den virkelige verden (Vedtakets gyldighet). Påkrevd for alle nye dokumentasjonsreferanser.")
    var vedtakGyldigFra: LocalDate?,
    @Schema(description = "Tidspunktet når objektet opphørte å eksistere i den virkelige verden (Vedtakets gyldighet)")
    var vedtakGyldigTil: LocalDate? = null,
    @Schema(description = "Egenskap som sier om en dokumentasjonsreferanse skal arkiveres")
    val shouldArchive: Boolean = false
)

data class Sporingsinformasjon(
    @Schema(description = "Dato for siste endring på objektdataene")
    var oppdateringsdato: ZonedDateTime = ZonedDateTime.now()
)
