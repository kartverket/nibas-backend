package no.kartverket.nibas.kommuneogfylkesdeling

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FylkesDelingEventTest {

    @Test
    fun `skal sortere eventrader i rekkefoelgen ADDED, MODIFED, EXPIRED`() {
        val eventRader = listOf(
            EventRad("1", EventType.ADDED, Flatetype.KOMMUNE, "1"),
            EventRad("2", EventType.EXPIRED, Flatetype.KOMMUNE, "2"),
            EventRad("3", EventType.MODIFIED, Flatetype.KOMMUNE, "3"),
            EventRad("4", EventType.EXPIRED, Flatetype.KOMMUNE, "4"),
            EventRad("5", EventType.ADDED, Flatetype.KOMMUNE, "5"),
            EventRad("6", EventType.MODIFIED, Flatetype.KOMMUNE, "6"),
        )

        val eventRaderSortert = eventRader.sortByType()

        assertThat(eventRaderSortert[0].eventType).isEqualTo(EventType.ADDED)
        assertThat(eventRaderSortert[1].eventType).isEqualTo(EventType.ADDED)
        assertThat(eventRaderSortert[2].eventType).isEqualTo(EventType.MODIFIED)
        assertThat(eventRaderSortert[3].eventType).isEqualTo(EventType.MODIFIED)
        assertThat(eventRaderSortert[4].eventType).isEqualTo(EventType.EXPIRED)
        assertThat(eventRaderSortert[5].eventType).isEqualTo(EventType.EXPIRED)
    }
}
