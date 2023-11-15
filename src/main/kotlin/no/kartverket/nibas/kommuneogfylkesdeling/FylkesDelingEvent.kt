package no.kartverket.nibas.kommuneogfylkesdeling

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType

fun Fylke.toEventRad(eventType: EventType): EventRad =
    EventRad(
        uuid = this.id,
        eventType = eventType,
        flateType = Flatetype.FYLKE,
        lokalid = this.lokalid
    )

fun Kommune.toEventRad(eventType: EventType): EventRad =
    EventRad(
        uuid = this.id,
        eventType = eventType,
        flateType = Flatetype.KOMMUNE,
        lokalid = this.lokalid
    )

fun Grunnkrets.toEventRad(eventType: EventType): EventRad =
    EventRad(
        uuid = this.id,
        eventType = eventType,
        flateType = Flatetype.GRUNNKRETS,
        lokalid = this.lokalid
    )

fun Stemmekrets.toEventRad(eventType: EventType): EventRad =
    EventRad(
        uuid = this.id,
        eventType = eventType,
        flateType = Flatetype.STEMMEKRETS,
        lokalid = this.lokalid
    )

val EventSortOrder = listOf(
    EventType.ADDED, EventType.MODIFIED, EventType.EXPIRED
)

fun List<EventRad>.sortByType(): List<EventRad> {
    return this.sortedBy { EventSortOrder.indexOf(it.eventType) }
}


