package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.builders.FylkeRequestBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.domain.utkast.toEndredeInndelinger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UtkastEndredeInndelingerTest {

    @Test
    fun `toEndredeInndelinger skal registrere kommune metadata endringer`() {
        val kommuneId = "5001"
        val kommuneEndring = KommuneRequestBuilder()
            .withLokalId(kommuneId)
            .build()
        val operasjoner = Operasjoner(
            metadataendringer = Metadataendringer(
                kommuneendringer = mapOf(kommuneId to kommuneEndring)
            )
        )

        val endredeInndelinger = operasjoner.toEndredeInndelinger()

        assertEquals(1, endredeInndelinger.endredeKommuner.size)
        assertTrue(endredeInndelinger.endredeKommuner.contains(kommuneId))
        assertTrue(endredeInndelinger.endredeStemmekretser.isEmpty())
        assertTrue(endredeInndelinger.endredeGrunnkretser.isEmpty())
        assertTrue(endredeInndelinger.endredeFylker.isEmpty())
    }

    @Test
    fun `toEndredeInndelinger skal registrere fylke metadata endringer`() {
        val fylkeId1 = "30"
        val fylkeId2 = "31"
        val fylkeEndring1 = FylkeRequestBuilder()
            .withLokalId(fylkeId1)
            .build()
        val fylkeEndring2 = FylkeRequestBuilder()
            .withLokalId(fylkeId2)
            .build()
        val operasjoner = Operasjoner(
            metadataendringer = Metadataendringer(
                fylkesendringer = mapOf(
                    fylkeId1 to fylkeEndring1,
                    fylkeId2 to fylkeEndring2
                )
            )
        )

        val endredeInndelinger = operasjoner.toEndredeInndelinger()

        assertEquals(2, endredeInndelinger.endredeFylker.size)
        assertTrue(endredeInndelinger.endredeFylker.contains(fylkeId1))
        assertTrue(endredeInndelinger.endredeFylker.contains(fylkeId2))
        assertTrue(endredeInndelinger.endredeStemmekretser.isEmpty())
        assertTrue(endredeInndelinger.endredeGrunnkretser.isEmpty())
        assertTrue(endredeInndelinger.endredeKommuner.isEmpty())
    }

    @Test
    fun `toEndredeInndelinger skal registrere alle typer metadata endringer`() {
        val kommuneId = "5001"
        val fylkeId = "30"
        val grunnkretsId = "grunnkrets1"
        val stemmekretsId = "stemmekrets1"

        val kommuneEndring = KommuneRequestBuilder()
            .withLokalId(kommuneId)
            .build()
        val fylkeEndring = FylkeRequestBuilder()
            .withLokalId(fylkeId)
            .build()
        val grunnkretsEndring = GrunnkretsRequestBuilder()
            .withLokalId(grunnkretsId)
            .build()
        val stemmekretsEndring = StemmekretsRequestBuilder()
            .withLokalId(stemmekretsId)
            .build()

        val operasjoner = Operasjoner(
            metadataendringer = Metadataendringer(
                kommuneendringer = mapOf(kommuneId to kommuneEndring),
                fylkesendringer = mapOf(fylkeId to fylkeEndring),
                grunnkretsendringer = mapOf(grunnkretsId to grunnkretsEndring),
                stemmekretsendringer = mapOf(stemmekretsId to stemmekretsEndring)
            )
        )

        val endredeInndelinger = operasjoner.toEndredeInndelinger()

        assertEquals(1, endredeInndelinger.endredeKommuner.size)
        assertTrue(endredeInndelinger.endredeKommuner.contains(kommuneId))

        assertEquals(1, endredeInndelinger.endredeFylker.size)
        assertTrue(endredeInndelinger.endredeFylker.contains(fylkeId))

        assertEquals(1, endredeInndelinger.endredeGrunnkretser.size)
        assertTrue(endredeInndelinger.endredeGrunnkretser.contains(grunnkretsId))

        assertEquals(1, endredeInndelinger.endredeStemmekretser.size)
        assertTrue(endredeInndelinger.endredeStemmekretser.contains(stemmekretsId))
    }
}
