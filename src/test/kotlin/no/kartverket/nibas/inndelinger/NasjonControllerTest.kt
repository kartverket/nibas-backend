package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(NasjonController::class)
class NasjonControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var nasjonService: NasjonService

    @Test
    fun thatHentAlleReturns200_OK() {
        whenever(nasjonService.hentAlleNasjoner()).thenReturn(listOf(NasjonBuilder.DEFAULT_NASJON))
        get("/v1/nasjoner", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(nasjonService.hentNasjon(anyString())).thenReturn(NasjonBuilder.DEFAULT_NASJON)
        get("/v1/nasjoner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(nasjonService.hentNasjon(anyString())).thenReturn(null)
        get("/v1/nasjoner/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        whenever(
            nasjonService.hentGrenserForNasjon(anyString())
        ).thenReturn(
            setOf(GrunnkretsGrenseKobling(grunnkrets = GrunnkretsBuilder().build(), grense = GrenseBuilder().buildRiksgrense(), GrenseKoblingAttributter()))
        )
        get("/v1/nasjoner/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        whenever(nasjonService.hentGrenserForNasjon(anyString())).thenReturn(emptySet())
        get("/v1/nasjoner/1/grenser", status().isNotFound)
    }

}
