package no.kartverket.nibas.domain.utkast

import javax.persistence.AttributeConverter
import javax.persistence.Converter

/**
 * value-attributt gir mulighet for å kunne ha norske tegn i verdi (ut i klienten), men ikke i enum-konstant-navn.
 * dbKey brukes som konstant i database-tabellen (disse må da endres med omhu).
 */
enum class UtkastEndringstype(val value: String, val dbKey: String) {
    KVALITETSHEVING("Kvalitetsheving", "KVALITETSHEVING"),
    RETTING("Retting", "RETTING"),
    VEDTATT_DELING("Vedtatt deling", "VEDTATT_DELING"),
    VEDTATT_SAMMENSLAAING("Vedtatt sammenslåing", "VEDTATT_SAMMENSLAAING"),
    VEDTATT_GRENSEJUSTERING("Vedtatt grensejustering", "VEDTATT_GRENSEJUSTERING"),
    FASTSETTING("Fastsetting", "FASTSETTING"),
    NAVNEENDRING("Navneendring", "NAVNEENDRING"),
    NUMMERENDRING("Nummerendring", "NUMMERENDRING");

    companion object {
        fun fromValue(valueIn: String): UtkastEndringstype {
            try {
                return values().first { it.value == valueIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid value: $valueIn")
            }
        }

        fun fromDbKey(dbKeyIn: String): UtkastEndringstype {
            try {
                return values().first { it.dbKey == dbKeyIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid dbKey: $dbKeyIn")
            }
        }
    }
}

@Converter(autoApply = true)
class UtkastEndringstypeConverter : AttributeConverter<UtkastEndringstype, String> {
    override fun convertToDatabaseColumn(attribute: UtkastEndringstype): String {
        return attribute.dbKey
    }

    override fun convertToEntityAttribute(dbKey: String): UtkastEndringstype {
        return UtkastEndringstype.fromDbKey(dbKey)
    }
}
