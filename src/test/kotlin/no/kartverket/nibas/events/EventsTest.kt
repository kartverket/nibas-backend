package no.kartverket.nibas.events

import java.time.LocalDate
import no.kartverket.nibas.api.v1.common.Flatetype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.springframework.core.env.Environment

internal class NibasEventPublisherTest {

    @Mock private lateinit var nibasEventsServiceMock: NibasEventsService
    @Mock private lateinit var environmentMock: Environment

    private lateinit var nibasEventPublisher: NibasEventPublisher

    private val eventRadForModifiedStemmekrets1 =
        EventRad(
            uuid = "1",
            eventType = EventType.MODIFIED,
            flateType = Flatetype.STEMMEKRETS,
            lokalid = "1"
        )
    private val eventRadForModifiedStemmekrets2 =
        EventRad(
            uuid = "2",
            eventType = EventType.MODIFIED,
            flateType = Flatetype.STEMMEKRETS,
            lokalid = "2"
        )
    private val eventRadForExpiredStemmekrets1 =
        EventRad(
            uuid = "1",
            eventType = EventType.EXPIRED,
            flateType = Flatetype.STEMMEKRETS,
            lokalid = "1"
        )
    private val inntreffelsesDato = LocalDate.of(2025, 10, 1)

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        nibasEventPublisher = NibasEventPublisher(nibasEventsServiceMock, environmentMock)

        whenever(environmentMock.activeProfiles).thenReturn(emptyArray())
    }

    @Test
    fun `publishNibasEvent skal publisere EXPIRED om man har både EXPIRED og MODIFIED på samme lokalid`() {
        val redigerteFlater =
            listOf(
                eventRadForModifiedStemmekrets1,
                eventRadForModifiedStemmekrets2,
                eventRadForExpiredStemmekrets1
            )
        val expectedEvent =
            NibasEvent(
                eventRader = setOf(eventRadForExpiredStemmekrets1, eventRadForModifiedStemmekrets2),
                inntreffer = inntreffelsesDato
            )

        nibasEventPublisher.publishNibasEvent(
            redigerteFlater = redigerteFlater,
            inntreffer = inntreffelsesDato
        )
        verify(nibasEventsServiceMock).publishEvent(eq(expectedEvent))
    }
}
