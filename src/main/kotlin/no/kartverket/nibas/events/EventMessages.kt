package no.kartverket.nibas.events

import no.kartverket.nibas.api.v1.common.Flatetype
import java.util.UUID

data class NibasEvent(
    val uuid: String = UUID.randomUUID().toString(),
    val type: EventType,
    val target: EventTarget,
    val id: String,
    val revision: Int
)

enum class EventType {
    ADDED, MODIFIED, DELETED
}

enum class EventTarget {
    STEMMEKRETS, GRUNNKRETS, SKOLEKRETS, KOMMUNE, FYLKE, NASJON;

    companion object {
        fun fromFlatetype(flatetype: Flatetype) = valueOf(flatetype.name)
    }
}
