package no.kartverket.nibas.api.v1.common

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import java.io.Serializable
import javax.persistence.AttributeConverter
import javax.persistence.Converter
import javax.validation.constraints.NotNull

/**
 * Definerer flatene/enhetene i domenet.
 */
enum class Flatetype {
    FYLKE, KOMMUNE, NASJON, GRUNNKRETS, STEMMEKRETS, SKOLEKRETS
}

/**
 * Definerer hvilken kontekst en skal se grense/feature i.
 */
enum class KontekstEnhetType {
    GRUNNKRETS, STEMMEKRETS
}

/**
 * Introduserte en value-attributt for å kunne ha norske tegn i verdi (ut i klienten), men ikke i enum-konstant-navn.
 * NB: dbKey brukes som konstant i database-tabellen for grenser (disse må da endres med omhu).
 */
enum class Grensetype(val value: String, val dbKey: String) {
    Kommunegrense("Kommunegrense", "Kommunegrense"),
    Fylkesgrense("Fylkesgrense", "Fylkesgrense"),
    Riksgrense("Riksgrense", "Riksgrense"),
    AvtaltAvgrensningslinje("AvtaltAvgrensningslinje", "AvtaltAvgrensningslinje"),
    Territorialgrense("Territorialgrense", "Territorialgrense"),
    Grunnlinje("Grunnlinje", "Grunnlinje"),
    Lovvirkeomraadegrense("LovVirkeområdeGrense", "LovVirkeområdeGrense"),
    Grunnkretsgrense("Grunnkretsgrense", "Grunnkretsgrense"),
    Delomraadegrense("Delområdegrense", "Delområdegrense"),
    Stemmekretsgrense("Stemmekretsgrense", "Stemmekretsgrense");

    companion object {
        fun fromValue(valueIn: String): Grensetype {
            try {
                return values().first { it.value == valueIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid value: $valueIn")
            }
        }

        fun fromDbKey(dbKeyIn: String): Grensetype {
            try {
                return values().first { it.dbKey == dbKeyIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid dbKey: $dbKeyIn")
            }
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

@Schema(description = "Representasjon av et fylkesnummer")
data class Fylkesnummer(

    @Schema(description = "Unik UUID for fylkesnummeret")
    val id: String,

    @Schema(description = "Det faktiske fylkesnummeret")
    val kodeverdi: Int
)

@Schema(description = "Representasjon av et navn")
data class AdministrativEnhetNavn(

    @Schema(description = "Navnet på en administrativ enhet")
    val navn: String,

    @Schema(description = "Språkkoden til det administrative navnet")
    val spraak: String,

    @Schema(description = "Teknisk versjon for å støtte samhandling og redigering")
    @NotNull
    val version: Int
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
    val stemmekretsSammenslaaingsendring: StemmekretsSammenslaaingsendring? = null,
) : Serializable

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
    val stemmekretsendringer: Map<String, StemmekretsRequest> = emptyMap()
) : Serializable

@Schema(description = "Representasjon av endringer på grensegeometri.")
data class Grenseendringer(
    @Schema(description = "Endringer på features.")
    val endredeFeatures: Map<String, Feature> = emptyMap()
) : Serializable

@Schema(description = "Representasjon av sammenslåing av en stemmekrets med 1 eller flere andre stemmekretser i samme kommune")
data class StemmekretsSammenslaaingsendring (
    @Schema (description = "LokalIden til den stemmekretsen som skal videreføres")
    val viderefoertStemmekrets: String,
    @Schema(description = "Liste av LokalIder som skal bli del av den videreførte stemmekretsen")
    val stemmekretserTilSammenslaaing: List<String>,
    @Schema (description = "Navnet til den sammenslåtte stemmekretsen")
    val stemmekretsNavn: String,
    @Schema (description = "Stemmekretsnummeret til den sammenslåtte stemmekretsen")
    val stemmekretsNummer: String
): Serializable

