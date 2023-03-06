package no.kartverket.nibas.domain

import jakarta.persistence.*

@Table(name = "typeendring")
@Entity
class Typeendring {
    @Id
    @Column(name = "identifier", nullable = false)
    var id: String? = null
}

/**
 * value-attributt gir mulighet for å kunne ha norske tegn i verdi (ut i klienten), men ikke i enum-konstant-navn.
 * dbKey brukes som konstant i database-tabellen (disse må da endres med omhu).
 */
enum class Endringstype(val value: String, val dbKey: String) {
    KVALITETSHEVING("Kvalitetsheving", "kvalitetsheving"),
    RETTING("Retting", "retting"),
    VEDTATT_DELING("Vedtatt deling", "vedtattDeling"),
    VEDTATT_SLETTING("Vedtatt sletting", "vedtattSletting"),
    VEDTATT_SAMMENSLAAING("Vedtatt sammenslåing", "vedtattSammenslåing"),
    VEDTATT_GRENSEJUSTERING("Vedtatt grensejustering", "vedtattGrensejustering"),
    FASTSETTING("Fastsetting", "fastsetting"),
    NAVNEENDRING("Navneendring", "navneendring"),
    NUMMERENDRING("Nummerendring", "nummerendring"),
    IMPORT("Import", "import");

    companion object {
        fun fromValue(valueIn: String): Endringstype {
            try {
                return values().first { it.value == valueIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid value: $valueIn", e)
            }
        }

        fun fromDbKey(dbKeyIn: String): Endringstype {
            try {
                return values().first { it.dbKey == dbKeyIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid dbKey: $dbKeyIn", e)
            }
        }
    }
}

@Converter(autoApply = true)
class EndringstypeConverter : AttributeConverter<Endringstype, String> {
    override fun convertToDatabaseColumn(attribute: Endringstype): String {
        return attribute.dbKey
    }

    override fun convertToEntityAttribute(dbKey: String?): Endringstype? {
        return dbKey?.let { Endringstype.fromDbKey(it) }
    }
}
