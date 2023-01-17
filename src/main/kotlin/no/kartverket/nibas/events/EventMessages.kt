package no.kartverket.nibas.events

import no.kartverket.nibas.api.v1.common.Flatetype
import java.time.LocalDate
import java.util.UUID

data class NibasEvent(
    val uuid: String = UUID.randomUUID().toString(),
    val type: EventType,
    val target: EventTarget,
    val lokalid: String,
    val gyldigfra: LocalDate,
    val gyldigtil: LocalDate?
)

enum class EventType {
    ADDED, MODIFIED, EXPIRED
}

enum class EventTarget {
    STEMMEKRETS, GRUNNKRETS, SKOLEKRETS, KOMMUNE, FYLKE, NASJON;

    companion object {
        fun fromFlatetype(flatetype: Flatetype) = valueOf(flatetype.name)
    }
}
