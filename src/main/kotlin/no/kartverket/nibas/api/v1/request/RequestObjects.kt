package no.kartverket.nibas.api.v1.request

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.NonExhaustiveFlatetype
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.geojson.metadata.DISCRIMINATOR_FIELD
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GjeldendeMaterieltVilkaar
import java.time.LocalDate
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class NasjonRequest(
    @Schema(description = "Navnene til nasjonen")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "LokalId for angitt nasjon")
    @NotBlank
    val lokalid: String,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int
)

data class FylkeRequest(
    @Schema(description = "Navnene til fylket")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "LokalId for angitt fylke")
    @NotBlank
    val lokalid: String,

    @Schema(description = "Angir om fylket er et samisk forvaltningsområde")
    @NotNull
    val samiskforvaltningsomraade: Boolean,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int
)

@Schema(description = "Representasjon av en kommune")
data class KommuneRequest(

    @Schema(description = "Navnene til kommunen")
    val administrativenhetnavn: List<AdministrativEnhetNavn>,

    @Schema(description = "LokalId for angitt kommune")
    @NotBlank
    val lokalid: String,

    @Schema(description = "Angir om kommunen er et samisk forvaltningsområde")
    @NotNull
    val samiskforvaltningsomraade: Boolean,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int
)

@Schema(description = "Representasjon av en grunnkrets")
data class GrunnkretsRequest(

    @Schema(description = "Navnet på grunnkretsen")
    val navn: String,

    @Schema(description = "Grunnkretsnummeret til grunnkretsen")
    val nummer: String,

    @Schema(description = "Identifikasjon av grunnkretsen")
    val identifikasjon: Identifikasjon,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int,

    @Schema(description = "informasjon om grunnkretsen")
    val informasjon: String? = null
)

@Schema(description = "Representasjon av en stemmekrets")
data class StemmekretsRequest(

    @Schema(description = "Stemmekretsnavnet til stemmekretsen")
    var navn: String,

    @Schema(description = "Stemmekretsnummeret til stemmekretsen")
    var nummer: String,

    @Schema(description = "Identifikasjon av stemmekretsen")
    val identifikasjon: Identifikasjon,

    @Schema(description = "Tellekretsnummeret til stemmekretsen")
    var tellekretsnummer: String? = null,

    @Schema(description = "Tellekretsnavnet til stemmekretsen")
    var tellekretsnavn: String? = null,

    @Schema(description = "Valgdistriktsnummeret til stemmekretsen")
    var valgdistriktsnummer: String? = null,

    @Schema(description = "Kommunenummeret stemmekretsen tilhører til")
    var kommunenummer: Kommunenummer? = null,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int,

    @Schema(description = "informasjon om stemmekretsen")
    val informasjon: String? = null
)

@Schema(description = "Representasjon av et bopliktomraade")
data class BopliktomraadeRequest(
    @Schema(description = "Identifikasjon av bopliktomraade")
    override val identifikasjon: Identifikasjon,

    @Schema(description = "Navnet til bopliktomraadet")
    override val navn: String,

    @Schema(description = "Nummeret til bopliktomraadet")
    override val nummer: String,

    @Schema(description = "Kommunenummeret bopliktomraadet tilhører til")
    override val kommuneIdentifikasjon: Identifikasjon,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    override var version: Int,

    @Schema(description = "Indikerer om bopliktomraadet dekker deler av kommunen eller ikke")
    override val gjelderKunDelAvKommunen: Boolean,

    @Schema(description = "Har usikker avgrensning (nytt navn for usikkerAvgrensning)")
    override val harUsikkerAvgrensning: Boolean,

    @ArraySchema(
        arraySchema = Schema(description = "Materielle vilkår for bopliktområdet (nye enum-verdier, erstatter materielleVilkaar)"),
        schema = Schema(
            description = "Gjeldende materielt vilkår",
            implementation = GjeldendeMaterieltVilkaar::class
        )
    )
    override val gjeldendeMaterielleVilkaar: Set<GjeldendeMaterieltVilkaar> = emptySet(),

    @Schema(description = "Andre lokale avgrensninger (nytt navn for andreAvgrensninger)")
    override val andreLokaleAvgrensninger: String? = null,

    @Schema(description = "Referanse til forskriften for bopliktomraadet")
    override var forskriftsreferanse: String? = null,

    @Schema(description = "URL til kommunen sin informasjonside")
    override var url: String? = null,
): BopliktomraadeRequestFields

@Schema(description = "Representasjon for opprettelse av utkast")
data class OpprettUtkastRequest(
    @Schema(description = "Arbeidsnavnet til utkastet.")
    val navn: String,
    @Schema(description = "Typen endring utkastet representerer.")
    val endringstype: Endringstype,
    @Schema(description = "Gyldig fra-datoen til utkastet.")
    val gyldigFra: LocalDate,
)

@Schema(description = "Representasjon for oppdatering av utkast")
data class OppdaterUtkastRequest(
    @Schema(description = "Arbeidsnavnet til utkastet.")
    val navn: String,
    @Schema(description = "Typen endring utkastet representerer.")
    val endringstype: Endringstype,
    @Schema(description = "Representasjon av endringene som er utført i utkastet.")
    val operasjoner: Operasjoner = Operasjoner(),
    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int
)

@Schema(description = "Representasjon av sammenslåing av en stemmekrets med 1 eller flere andre stemmekretser i samme kommune")
data class StemmekretsSammenslaaingsendringRequest (
    @Schema (description = "Identifikator til den stemmekretsen som skal videreføres")
    val viderefoertStemmekrets: IdentifikatorMedVersjon,
    @Schema(description = "Liste av identifikatorer som skal bli del av den videreførte stemmekretsen")
    val stemmekretserTilSammenslaaing: List<IdentifikatorMedVersjon>,
    @Schema (description = "Navnet til den sammenslåtte stemmekretsen")
    val navn: String,
    @Schema (description = "Stemmekretsnummeret til den sammenslåtte stemmekretsen")
    val nummer: String,
    @Schema (description = "Informasjon om sammenslåingen som videreføres til den nye stemmekretsen og de gamle")
    val informasjon: String?
)

@Schema(description = "Representasjon av deling av en krets som ender i to kretser der én er videreført og én er ny")
data class KretsDelingEndringRequest(
    @Schema(description = "Identifikator til den kretsen som skal deles")
    val opprinneligKrets: IdentifikatorMedVersjon,
    @Schema(description = "Lokalid til kommune som deling foregår i")
    val kommuneId: ObjektIdentifikator,
    @Schema(description = "Flatetypen som skal deles")
    val flatetype: Flatetype,
    @Schema(description = "Navn og nummer for de nye kretsene som skal utledes fra opprinnelig krets")
    val nyeKretser: List<KretsNavnOgNummer> = listOf()
)

@Schema(description = "Representasjon av navn og nummer for en ny krets")
data class KretsNavnOgNummer(
    @Schema(description = "Navne til den nye kretsen som skal dannes fra opprinneligKrets")
    val kretsNavn: String,
    @Schema(description = "Grunnkrets/stemmekrets-nummmer til den nye kretsen")
    val kretsNummer: String
)

@Schema(description = "Representasjon av sammenslåing av en grunnkrets med 1 eller flere andre grunnkretser i samme kommune")
data class GrunnkretsSammenslaaingsendringRequest (
    @Schema (description = "Identifikator til den grunnkretsen som skal videreføres")
    val viderefoertGrunnkrets: IdentifikatorMedVersjon,
    @Schema(description = "Liste av identifikatorer som skal bli del av den videreførte grunnkretsen")
    val grunnkretserTilSammenslaaing: List<IdentifikatorMedVersjon>,
    @Schema (description = "Navnet til den sammenslåtte grunnkretsen")
    val navn: String,
    @Schema (description = "Grunnkretsnummeret til den sammenslåtte grunnkretsen")
    val nummer: String,
    @Schema (description = "Informasjon om sammenslåingen som videreføres til den nye grunnkretsen og de gamle")
    val informasjon: String?
)

const val CREATE_BOPLIKTOMRAADE_SCHEMA: String = "CreateBopliktomraadeRequest"
@Schema(
    description = "Representasjon av en opprettelse av en ny inndeling for en ikke-heldekkende inndeling.",
    subTypes = [
        CreateBopliktomraadeRequest::class,
    ],
    discriminatorProperty = DISCRIMINATOR_FIELD,
    discriminatorMapping = [
        DiscriminatorMapping(value = CREATE_BOPLIKTOMRAADE_SCHEMA, schema = CreateBopliktomraadeRequest::class),
    ]
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = DISCRIMINATOR_FIELD)
@JsonSubTypes(
    JsonSubTypes.Type(value = CreateBopliktomraadeRequest::class, name = CREATE_BOPLIKTOMRAADE_SCHEMA),
)
sealed interface CreateInndelingRequest

@Schema(
    name = CREATE_BOPLIKTOMRAADE_SCHEMA,
    description = "Representasjon av opprettelsen av nytt bopliktområde"
)
data class CreateBopliktomraadeRequest @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @Schema(hidden = true)
    private val request: BopliktomraadeRequest
) : CreateInndelingRequest, BopliktomraadeRequestFields by request

@Schema(description = "Representasjon av arkivering av en ikke-heldekkende inndeling.")
data class ArchiveInndelingRequest(
    @Schema(description = "Type inndeling som skal arkiveres")
    val flatetype: NonExhaustiveFlatetype,

    @Schema(description = "Identifikator og teknisk versjon for inndelingen som skal arkiveres")
    val identifikator: IdentifikatorMedVersjon,
)

