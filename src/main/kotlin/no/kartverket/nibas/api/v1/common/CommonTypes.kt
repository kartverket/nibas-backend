package no.kartverket.nibas.api.v1.common

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.request.BopliktomraadeRequest
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsSammenslaaingsendringRequest
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Bopliktomraade
import java.time.LocalDate
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import jakarta.validation.constraints.NotNull

/**
 * Definerer flatene/enhetene i domenet.
 */
enum class Flatetype {
    FYLKE, KOMMUNE, NASJON, GRUNNKRETS, STEMMEKRETS, SKOLEKRETS, BOPLIKTOMRAADE
}

/**
 * Definerer hvilken kontekst en skal se grense/feature i.
 */
enum class KontekstEnhetType {
    GRUNNKRETS, STEMMEKRETS, BOPLIKTOMRAADE
}

/**
 * Introduserte en value-attributt for å kunne ha norske tegn i verdi (ut i klienten), men ikke i enum-konstant-navn.
 * NB: dbKey brukes som konstant i database-tabellen for grenser (disse må da endres med omhu).
 */
enum class Grensetype(val value: String, val dbKey: String, val editerbar: Boolean) {
    Kommunegrense(value = "Kommunegrense", dbKey =  "Kommunegrense", editerbar = true),
    Fylkesgrense(value = "Fylkesgrense", dbKey = "Fylkesgrense", editerbar = true),
    Riksgrense(value = "Riksgrense", dbKey = "Riksgrense", editerbar = true),
    AvtaltAvgrensningslinje(value = "AvtaltAvgrensningslinje", dbKey =  "AvtaltAvgrensningslinje", editerbar = true),
    Territorialgrense(value = "Territorialgrense", dbKey = "Territorialgrense", editerbar = true),
    Grunnlinje(value = "Grunnlinje", dbKey = "Grunnlinje", editerbar = false),
    Lovvirkeomraadegrense(value = "LovVirkeområdeGrense", dbKey =  "LovVirkeområdeGrense", editerbar = false),
    Grunnkretsgrense(value = "Grunnkretsgrense", dbKey = "Grunnkretsgrense", editerbar = true),
    Delomraadegrense(value = "Delområdegrense", dbKey = "Delområdegrense", editerbar = true),
    Stemmekretsgrense(value = "Stemmekretsgrense", dbKey = "Stemmekretsgrense", editerbar = true),
    Bopliktgrense(value = "Bopliktgrense", dbKey = "Bopliktgrense", editerbar = true),
    Soknegrense(value = "Soknegrense", dbKey = "Soknegrense", editerbar = false),
    Skolekretsgrense(value = "Skolekretsgrense", dbKey = "Skolekretsgrense", editerbar = false);

    companion object {
        fun fromValue(valueIn: String): Grensetype {
            try {
                return entries.first { it.value == valueIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid value: $valueIn")
            }
        }

        fun fromDbKey(dbKeyIn: String): Grensetype {
            try {
                return entries.first { it.dbKey == dbKeyIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid dbKey: $dbKeyIn")
            }
        }

        fun erAdministrativGrense(grensetype: Grensetype): Boolean {
            val adminstrativeGrensetyper = listOf(Kommunegrense, Fylkesgrense, Riksgrense, AvtaltAvgrensningslinje, Territorialgrense)
            return adminstrativeGrensetyper.contains(grensetype)
        }
    }
}

@Converter(autoApply = true)
class GrenseTypeConverter : AttributeConverter<Grensetype, String> {
    override fun convertToDatabaseColumn(attribute: Grensetype): String {
        return attribute.dbKey
    }

    override fun convertToEntityAttribute(dbKey: String): Grensetype {
        return Grensetype.fromDbKey(dbKey)
    }
}

data class ObjektIdentifikator(val lokalid: Lokalid, val gyldighetsdato: LocalDate)

@Schema(description = "Representasjon av et fylkesnummer")
data class Fylkesnummer(

    @Schema(description = "Unik UUID for fylkesnummeret")
    val id: String,

    @Schema(description = "Det faktiske fylkesnummeret")
    val kodeverdi: String
)

@Schema(description = "Representasjon av et kommunenummer")
data class Kommunenummer(

    @Schema(description = "Unik UUID for kommunenummeret")
    val id: String,

    @Schema(description = "Det faktiske kommunenummeret")
    val kodeverdi: String
)

@Schema(description = "Representasjon av et navn")
data class AdministrativEnhetNavn(

    @Schema(description = "Navnet på en administrativ enhet")
    val navn: String,

    @Schema(description = "Språkkoden til det administrative navnet")
    val spraak: String,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int,

    @Schema(description = "Rekkefølge på navn")
    val rekkefoelge: Int? = null
)

data class GyldighetResponse(
    @Schema(description = "Tidspunktet objektet er gyldig fra")
    val gyldigFra: LocalDate,
    @Schema(description = "Tidspunktet objektet er gyldig til. Kan være tomt/løpende.", nullable = true)
    val gyldigTil: LocalDate?,
)


// TODO: Utvide med felter for sammenslåing og oppsplitting
// Typen (og undertyper) er Serializable for persistering i jsonb-kolonne
// Typene under operasjoner er påkrevet, men har defaults.
@Schema(description = "Representasjon av operasjoner/handlinger som er utført i klienten.")
data class Operasjoner(
    @Schema(description = "Endringer på metadata.")
    val metadataendringer: Metadataendringer = Metadataendringer(),
    @Schema(description = "Endringer på grenser.")
    val grenseendringer: Grenseendringer = Grenseendringer(),
    @Schema(description = "Sammenslåing av stemmekretser i samme kommune.")
    val stemmekretsSammenslaaingsendring: StemmekretsSammenslaaingsendringRequest? = null,
    @Schema(description = "Sammenslåing av stemmekretser i samme kommune.")
    val grunnkretsSammenslaaingsendring: GrunnkretsSammenslaaingsendringRequest? = null,
    @Schema(description = "Deling av en stemmekrets eller grunnkrets")
    val kretsDelingEndringer: List<KretsDelingEndringRequest> = listOf(),
)

@Schema(description = "Representasjon av endringer på metadata.")
data class Metadataendringer(
    @Schema(description = "Endringer på nasjon.")
    val nasjonsendringer: Map<String, NasjonRequest> = emptyMap(),
    @Schema(description = "Endringer på fylke.")
    val fylkesendringer: Map<String, FylkeRequest> = emptyMap(),
    @Schema(description = "Endringer på kommune.")
    val kommuneendringer: Map<String, KommuneRequest> = emptyMap(),
    @Schema(description = "Endringer på grunnkrets.")
    val grunnkretsendringer: Map<String, GrunnkretsRequest> = emptyMap(),
    @Schema(description = "Endringer på stemmekrets.")
    val stemmekretsendringer: Map<String, StemmekretsRequest> = emptyMap(),
    @Schema(description = "Endringer på bopliktomraade.")
    val bopliktomraadeendringer: Map<String, BopliktomraadeRequest> = emptyMap()
)

@Schema(description = "Representasjon av endringer på grensegeometri.")
data class Grenseendringer(
    @Schema(description = "Endringer på features.")
    val endredeFeatures: List<Feature> = emptyList()
)

