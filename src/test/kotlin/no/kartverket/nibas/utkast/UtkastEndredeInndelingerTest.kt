package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.domain.utkast.toEndredeInndelinger
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtkastEndredeInndelingerTest {

    @Test
    fun `toEndredeInndelinger skal registrere kommune metadata endringer`() {
        val kommuneId = "5001"
        val kommuneEvent = EventRad(kommuneId, EventType.ADDED, Flatetype.KOMMUNE, kommuneId)

        val endredeInndelinger = listOf(kommuneEvent).toEndredeInndelinger()

        assertThat(endredeInndelinger.endredeKommuner).hasSize(1)
        assertThat(endredeInndelinger.endredeKommuner).contains(kommuneId)
        assertThat(endredeInndelinger.endredeStemmekretser).isEmpty()
        assertThat(endredeInndelinger.endredeGrunnkretser).isEmpty()
        assertThat(endredeInndelinger.endredeFylker).isEmpty()
        assertThat(endredeInndelinger.endredeNasjoner).isEmpty()
    }

    @Test
    fun `toEndredeInndelinger skal registrere fylke metadata endringer`() {
        val fylkeId1 = "30"
        val fylkeId2 = "31"
        val fylkeEvent1 = EventRad(fylkeId1, EventType.ADDED, Flatetype.FYLKE, fylkeId1)
        val fylkeEvent2 = EventRad(fylkeId2, EventType.EXPIRED, Flatetype.FYLKE, fylkeId2)

        val endredeInndelinger = listOf(fylkeEvent1, fylkeEvent2).toEndredeInndelinger()

        assertThat(endredeInndelinger.endredeFylker).hasSize(2)
        assertThat(endredeInndelinger.endredeFylker).containsExactlyInAnyOrder(fylkeId1, fylkeId2)
        assertThat(endredeInndelinger.endredeStemmekretser).isEmpty()
        assertThat(endredeInndelinger.endredeGrunnkretser).isEmpty()
        assertThat(endredeInndelinger.endredeKommuner).isEmpty()
        assertThat(endredeInndelinger.endredeNasjoner).isEmpty()
    }

    @Test
    fun `toEndredeInndelinger skal registrere alle typer metadata endringer`() {
        val nasjonId = "Norge"
        val kommuneId = "5001"
        val fylkeId = "30"
        val grunnkretsId = "grunnkrets1"
        val stemmekretsId = "stemmekrets1"

        val nasjonEvent = EventRad(nasjonId, EventType.MODIFIED, Flatetype.NASJON, nasjonId)
        val kommuneEvent = EventRad(kommuneId, EventType.MODIFIED, Flatetype.KOMMUNE, kommuneId)
        val fylkeEvent = EventRad(fylkeId, EventType.MODIFIED, Flatetype.FYLKE, fylkeId)
        val grunnkretsEvent = EventRad(grunnkretsId, EventType.MODIFIED, Flatetype.GRUNNKRETS, grunnkretsId)
        val stemmekretsEvent = EventRad(stemmekretsId, EventType.MODIFIED, Flatetype.STEMMEKRETS, stemmekretsId)

        val eventrader: List<EventRad> = listOf(kommuneEvent, fylkeEvent, grunnkretsEvent, stemmekretsEvent, nasjonEvent)
        val endredeInndelinger = eventrader.toEndredeInndelinger()

        assertThat(endredeInndelinger.endredeKommuner).hasSize(1)
        assertThat(endredeInndelinger.endredeKommuner).containsExactly(kommuneId)

        assertThat(endredeInndelinger.endredeFylker).hasSize(1)
        assertThat(endredeInndelinger.endredeFylker).containsExactly(fylkeId)

        assertThat(endredeInndelinger.endredeGrunnkretser).hasSize(1)
        assertThat(endredeInndelinger.endredeGrunnkretser).containsExactly(grunnkretsId)

        assertThat(endredeInndelinger.endredeStemmekretser).hasSize(1)
        assertThat(endredeInndelinger.endredeStemmekretser).containsExactly(stemmekretsId)

        assertThat(endredeInndelinger.endredeNasjoner).hasSize(1)
        assertThat(endredeInndelinger.endredeNasjoner).containsExactly(nasjonId)
    }
}
