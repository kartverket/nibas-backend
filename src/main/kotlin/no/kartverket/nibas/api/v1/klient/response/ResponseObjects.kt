package no.kartverket.nibas.api.v1.klient.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.GyldighetResponse
import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.MultiPolygon
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.domain.GjeldendeMaterieltVilkaar
import no.kartverket.nibas.domain.MaterieltVilkaar
import no.kartverket.nibas.domain.utkast.EndredeInndelinger
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Coordinate
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.ZonedDateTime

@Schema(description = "Representasjon av nasjon")
data class NasjonResponse(
    @Schema(description = "ID-en til nasjonen")
    val id: ObjektIdentifikator,

    @Schema(description = "Liste over navn til nasjon")
    val navn: List<AdministrativEnhetNavn>,

    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Området til nasjonen")
    val omraade: MultiPolygon? = null,

    @Schema(description = "Representasjonspunktet for nasjonen")
    val representasjonspunkt: Feature,

    @Schema(description = "Angir når denne nasjonen ble sist oppdatert")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int
)

@Schema(description = "Representasjon av et fylke")
data class FylkeResponse(
    @Schema(description = "ID-en til fylket")
    val id: ObjektIdentifikator,

    @Schema(description = "Liste over navn til fylket")
    val navn: List<AdministrativEnhetNavn>,

    @Schema(description = "Fylkesnummeret til fylket")
    val nummer: String,

    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Angir om fylket er et samisk forvaltningsområde eller ikke")
    val samiskforvaltningsomraade: Boolean,

    @Schema(description = "Angir når dette fylket ble sist oppdatert")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Representasjonspunktet for fylket")
    val representasjonspunkt: Feature,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int
)

@Schema(description = "Representasjon av en kommune")
data class KommuneResponse(
    @Schema(description = "ID-en til kommunen")
    val id: ObjektIdentifikator,

    @Schema(description = "Lokal-id til fylket denne kommunen er i")
    val fylkeId: String,

    @Schema(description = "Liste over navn til kommunen")
    val navn: List<AdministrativEnhetNavn>,

    @Schema(description = "Kommunenummeret til kommunen")
    val nummer: String,

    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Angir om kommunen er et samisk forvaltningsområde eller ikke")
    val samiskforvaltningsomraade: Boolean,

    @Schema(description = "Angir når denne kommunen ble sist oppdatert")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Representasjonspunktet for kommunen")
    val representasjonspunkt: Feature,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int
)

@Schema(description = "Representasjon av en grunnkrets")
data class GrunnkretsResponse(
    @Schema(description = "ID-en til grunnkretsen")
    val id: ObjektIdentifikator,

    @Schema(description = "Navnet på grunnkretsen")
    val navn: String,

    @Schema(description = "Grunnkretsnummeret til grunnkretsen")
    val nummer: String,

    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Siste oppdateringstidspunkt for objektet")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Datafangsdato for objektet")
    val datafangstdato: ZonedDateTime?,

    @Schema(description = "Kommunenummeret til grunnkretsen")
    val kommunenummer: Kommunenummer,

    @Schema(description = "ID-en til kommunen til grunnkretsen")
    val kommuneIdentifikator: ObjektIdentifikator,

    @Schema(description = "Typen endring som ble gjort på objektet")
    val endringstype: String?,

    @Schema(description = "Representasjonspunktet for grunnkretsen")
    val representasjonspunkt: Feature,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int,

    @Schema(description = "Informasjon om grunnkretsen")
    val informasjon: String? = null,
)

@Schema(description = "Representasjon av en stemmekrets")
data class StemmekretsResponse(
    @Schema(description = "ID-en til stemmekretsen")
    val id: ObjektIdentifikator,

    @Schema(description = "Navnet på stemmekretsen")
    val navn: String,

    @Schema(description = "Stemmekretsnummeret til stemmekretsen")
    val nummer: String,

    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Typen endring som ble gjort på objektet")
    val endringstype: String?,

    @Schema(description = "Siste oppdateringstidspunkt for objektet")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Datafangsdato for objektet")
    val datafangstdato: ZonedDateTime?,

    @Schema(description = "Kommunenummeret til stemmekretsen")
    val kommunenummer: Kommunenummer,

    @Schema(description = "ID-en til kommunen til stemmekretsen")
    val kommuneIdentifikator: ObjektIdentifikator,

    @Schema(description = "Tellekretsnummer til stemmekretsen")
    var tellekretsnummer: String? = null,

    @Schema(description = "Tellekretsnavn til stemmekretsen")
    var tellekretsnavn: String? = null,

    @Schema(description = "Valgdistriktsnummer til stemmekretsen")
    var valgdistriktsnummer: String? = null,

    @Schema(description = "Representasjonspunktet for stemmekretsen")
    val representasjonspunkt: Feature,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int,

    @Schema(description = "Informasjon om stemmekretsen")
    val informasjon: String? = null,
)
@Schema(description = "Representasjon av et bopliktområde")
data class BopliktomraadeResponse(

    @Schema(description = "ID-en til bopliktområdet")
    val id: ObjektIdentifikator,

    @Schema(description = "Navnet til bopliktområdet")
    val navn: String,

    @Schema(description = "Nummeret til bopliktområdet")
    val nummer: String,

    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,

    @Schema(description = "Siste oppdateringstidspunkt for objektet")
    val oppdateringsdato: ZonedDateTime,

    @Schema(description = "Datafangstdato for objektet")
    val datafangstdato: ZonedDateTime?,

    @Schema(description = "Kommunenummeret til bopliktområdet")
    val kommunenummer: Kommunenummer,

    @Schema(description = "ID-en til kommunen bopliktområdet tilhører")
    val kommuneIdentifikator: ObjektIdentifikator,

    @Schema(description = "Typen endring som ble gjort på objektet")
    val endringstype: String?,

    @Schema(description = "Representasjonspunktet for bopliktområdet")
    val representasjonspunkt: Feature,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int,

    @Schema(description = "Tilleggsinformasjon om bopliktområdet")
    val informasjon: String? = null,

    @Schema(description = "Om boplikten gjelder delvis innenfor området")
    val delvisBoplikt: Boolean? = null,

    @Schema(description = "Indikerer om bopliktomraadet dekker deler av kommunen eller ikke")
    val gjelderKunDelAvKommunen: Boolean,

    @Schema(description = "Usikker avgrensning")
    @field:JsonProperty("usikkerAvgrensning")
    val usikkerAvgrensning: Boolean = false,

    @Schema(description = "Har usikker avgrensning (nytt navn for usikkerAvgrensning)")
    val harUsikkerAvgrensning: Boolean,

    @Schema(description = "Forskriftsreferansen for bopliktområdet")
    val forskriftsreferanse: String? = null,

    @ArraySchema(
        arraySchema = Schema(description = "Materielle vilkår for bopliktområdet"),
        schema = Schema(
            description = "Materielt vilkår",
            implementation = MaterieltVilkaar::class
        )
    )
    val materielleVilkaar: Set<MaterieltVilkaar> = emptySet(),

    @ArraySchema(
        arraySchema = Schema(description = "Materielle vilkår for bopliktområdet (nye enum-verdier, erstatter materielleVilkaar)"),
        schema = Schema(
            description = "Gjeldende materielt vilkår",
            implementation = GjeldendeMaterieltVilkaar::class
        )
    )
    val gjeldendeMaterielleVilkaar: Set<GjeldendeMaterieltVilkaar> = emptySet(),

    @Schema(description = "Andre avgrensninger")
    val andreAvgrensninger: String? = null,

    @Schema(description = "Andre lokale avgrensninger (nytt navn for andreAvgrensninger)")
    val andreLokaleAvgrensninger: String? = null,

    @Schema(description = "Lenke til mer informasjon")
    val url: String? = null,
)

@Schema(description = "Returtype for opprettelse av utkast")
data class UtkastOpprettetResponse(
    @Schema(description = "Unik uuid for utkastet")
    val id: String
)

@Schema(description = "Representasjon av utkast")
data class UtkastResponse(
    @Schema(description = "Unik uuid for utkastet")
    val id: String,
    @Schema(description = "Arbeidsnavnet til utkastet.")
    val navn: String,
    @Schema(description = "Typen endring utkastet representerer.")
    val endringstype: String,
    @Schema(description = "Status for utkastet.")
    val status: String,
    @Schema(description = "Da utkastet ble opprettet.")
    val opprettetDato: ZonedDateTime,
    @Schema(description = "Info om endringer på utkast.")
    val auditInfoResponse: AuditInfoResponse,
    @Schema(description = "Representasjon av endringene som er utført i utkastet.")
    val operasjoner: Operasjoner = Operasjoner(),
    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    val version: Int,
    @Schema(description = "Gyldig fra-datoen til utkastet")
    val gyldigFra: LocalDate,
    @Schema(description = "Liste med lokalider for inndelinger som har blitt endret i utkastet. Kun satt hvis status er PUBLISERT")
    val endredeInndelinger: EndredeInndelinger
)

data class InndelingSearchResponse(
    @Schema(description = "Lokalid til inndelingen")
    val id: String,
    @Schema(description = "Flatetypen til inndelingen")
    val type: Flatetype,
    @Schema(description = "Navnet til inndelingen")
    val navn: String,
    @Schema(description = "Nummeret til inndelingen")
    val nummer: String,
    @Schema(description = "Koordinatet til representasjonspunktet til inndelingen.")
    val representasjonspunkt: Coordinate,
)

data class InndelingResponse(
    @Schema(description = "ID til inndelingen")
    val id: ObjektIdentifikator,
    @Schema(description = "Flatetypen til inndelingen")
    val type: Flatetype,
    @Schema(description = "Navnet til inndelingen")
    val navn: String,
    @Schema(description = "Nummeret til inndelingen")
    val nummer: String,
    @Schema(description = "Feature til representasjonspunktet til inndelingen.")
    val representasjonspunkt: Feature,
    @Schema(description = "Gyldighetsintervall for objektet")
    val gyldighet: GyldighetResponse,
)

@Schema(description = "Representasjon av audit info for et objekt.")
data class AuditInfoResponse(
    @Schema(description = "Da objektet sist ble oppdatert.")
    val oppdateringsdato: ZonedDateTime
)

data class OptimistiskLaasWrapper(
    @Schema(description = "HttpStatus for responsen.")
    val httpStatus: HttpStatus = HttpStatus.CONFLICT,
    @Schema(description = "Feil som har oppstått pga optimistisk lås.")
    val optimisticLockExceptions: List<OptimistiskLaasResponse> = emptyList(),
)

data class OptimistiskLaasResponse(
    // TODO: Bruke ObjektIdentifikator også her
    @Schema(description = "Identifikatoren til objektet som er utdatert.")
    val id: String,

    @Schema(description = "Typen til objektet som er utdatert.")
    val type: String,

    @Schema(description = "Versjon som prøves å oppdateres.")
    val versjon: Int,

    @Schema(description = "Versjon som er gjeldende for objektet nå.")
    val gjeldendeVersjon: Int,

    @Schema(description = "Beskrivelse av hva som har gått galt.")
    val melding: String?
)

data class ErrorDescription(
    @Schema(description = "Tittelen på feilmeldingen som skal vises.")
    val title: String = "Noe gikk galt",

    @Schema(description = "En beskrivende forklaring av feilen som oppstod.")
    val description: String,

    @Schema(description = "Litt tilleggsinfo relatert til feilen om man vil gi noe mer forklaring av konteksten. Ikke obligatorisk.")
    val additionalInfo: String? = null,
)

data class ApiErrorResponse(
    @Schema(description = "En unik feilkode for denne feilen som kan vises til bruker. Denne feilkoden burde også være med i loggene så man finner igjen feilen som oppstod.")
    val errorCode: String,

    @Schema(description = "En forklaring av feilen som oppstod. Denne er ment til å kunne vises direkte til brukeren.")
    val errorDescription: ErrorDescription,
)

data class User(
    @Schema(description = "Fullt navn til innlogget bruker.")
    val username: String?,
)

fun Identifikator.toObjektIdentifikator(): ObjektIdentifikator = ObjektIdentifikator(this.lokalid, this.gyldighetsdato)

fun Gyldighet.toGyldighetResponse(): GyldighetResponse = GyldighetResponse(this.gyldigFra, this.gyldigTil)

