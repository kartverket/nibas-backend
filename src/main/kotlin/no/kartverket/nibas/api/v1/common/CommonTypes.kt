package no.kartverket.nibas.api.v1.common

import io.swagger.v3.oas.annotations.media.Schema
import javax.persistence.AttributeConverter
import javax.persistence.Converter

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
    val spraak: String
)

@Schema(description = "Representasjon av ulike former for endring som kan gjøres i kontekst av utkast.")
enum class UtkastEndringstype { IKKE_DEFINERT, VEDTATT_GRENSEENDRING, VEDTATT_SAMMENSLAAING }
@Schema(description = "Definerer tillatte statuser for utkast.")
enum class UtkastStatus { OPPRETTET, PUBLISERT, FORKASTET }
