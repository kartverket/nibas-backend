package no.kartverket.nibas.domain.utkast

import javax.persistence.AttributeConverter
import javax.persistence.Converter

/**
 * value-attributt gir mulighet for å kunne ha norske tegn i verdi (ut i klienten), men ikke i enum-konstant-navn.
 * dbKey brukes som konstant i database-tabellen (disse må da endres med omhu).
 */
enum class UtkastStatus(val value: String, val dbKey: String) {
    OPPRETTET("Opprettet", "OPPRETTET"),
    PUBLISERT("Publisert", "PUBLISERT"),
    FORKASTET("Forkastet", "FORKASTET");

    companion object {
        fun fromValue(valueIn: String): UtkastStatus {
            try {
                return values().first { it.value == valueIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid value: $valueIn", e)
            }
        }

        fun fromDbKey(dbKeyIn: String): UtkastStatus {
            try {
                return values().first { it.dbKey == dbKeyIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid dbKey: $dbKeyIn", e)
            }
        }
    }
}

@Converter(autoApply = true)
class UtkastStatusConverter : AttributeConverter<UtkastStatus, String> {
    override fun convertToDatabaseColumn(attribute: UtkastStatus): String {
        return attribute.dbKey
    }

    override fun convertToEntityAttribute(dbKey: String): UtkastStatus {
        return UtkastStatus.fromDbKey(dbKey)
    }
}
