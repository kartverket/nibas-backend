package no.kartverket.nibas.api.v1.request

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
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

    @Schema(description = "UUID til fylkesnummeret")
    @NotBlank
    val fylkesnummerId: String,

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

    @Schema(description = "UUID til kommunenummeret")
    @NotBlank
    val kommunenummerId: String,

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
    val version: Int
)

@Schema(description = "Representasjon av en stemmekrets")
data class StemmekretsRequest(

    @Schema(description = "Stemmekretsnavnet til stemmekretsen")
    var navn: String? = null,

    @Schema(description = "Stemmekretsnummeret til stemmekretsen")
    var nummer: String? = null,

    @Schema(description = "Identifikasjon av stemmekretsen")
    val identifikasjon: Identifikasjon,

    @Schema(description = "Tellekretsnummeret til stemmekretsen")
    var tellekretsnummer: String? = null,

    @Schema(description = "Tellekretsnavnet til stemmekretsen")
    var tellekretsnavn: String? = null,

    @Schema(description = "Valgdistriktsnummeret til stemmekretsen")
    var valgdistriktsnummer: String? = null,

    @Schema(description = "Kommunenummeret stemmekretsen tilhører til")
    var kommunenummer: String? = null,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int
)

@Schema(description = "Representasjon for opprettelse av utkast")
data class OpprettUtkastRequest(
    @Schema(description = "Arbeidsnavnet til utkastet.")
    val navn: String,
    @Schema(description = "Typen endring utkastet representerer.")
    val endringstype: String,
)

@Schema(description = "Representasjon for oppdatering av utkast")
data class OppdaterUtkastRequest(
    @Schema(description = "Arbeidsnavnet til utkastet.")
    val navn: String,
    @Schema(description = "Typen endring utkastet representerer.")
    val endringstype: String,
    @Schema(description = "Tidspunktet utkastet skal være gyldig fra.")
    val gyldigFra: LocalDate? = LocalDate.now().plusYears(10),
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
    val nummer: String
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
    val nummer: String
)

@Schema(description = "Requestbody for publisering av utkast.")
data class PubliserUtkastRequest (
    @Schema (description = "Datoen utkastet skal publiseres fra. Settes default til dagens dato om ikke satt.")
    val publiseringsdato: LocalDate = LocalDate.now(),
)
