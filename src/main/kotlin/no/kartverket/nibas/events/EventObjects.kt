package no.kartverket.nibas.events

import no.kartverket.nibas.api.v1.common.Flatetype
import java.time.LocalDate

data class NibasEvent (
    val inntreffer: LocalDate,
    val eventRader: Set<EventRad>,
)

data class EventRad (
    val uuid: String,
    val eventType: EventType,
    val flateType: Flatetype,
    val lokalid: String,
)

enum class EventType {
    EXPIRED, ADDED, MODIFIED
}
