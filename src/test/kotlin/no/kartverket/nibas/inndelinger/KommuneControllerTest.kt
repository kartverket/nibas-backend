package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.kodelister.KodelisteService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyInt
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import no.kartverket.nibas.api.v1.request.KommuneRequest as KommuneRequest
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(KommuneController::class)
class KommuneControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var kommuneService: KommuneService

    @MockBean
    private lateinit var kodelisteService: KodelisteService

    @MockBean
    private lateinit var historikkService: HistorikkService

    @Test
    fun thatHentAlleReturns200_OK() {
        val kommune = KommuneBuilder(1)
            .withNavn("Oslo")
            .build()

        whenever(kommuneService.hentAlleKommuner()).thenReturn(listOf(kommune))
        get("/v1/kommuner", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(kommuneService.hentKommune(anyInt())).thenReturn(KommuneBuilder.DEFAULT_KOMMUNE)
        get("/v1/kommuner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(kommuneService.hentKommune(anyInt())).thenReturn(null)
        get("/v1/kommuner/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        whenever(
            kommuneService.hentGrenserForKommune(anyInt())
        ).thenReturn(
            setOf(GrenseBuilder().buildKommunegrense())
        )
        get("/v1/kommuner/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        whenever(kommuneService.hentGrenserForKommune(anyInt())).thenReturn(null)
        get("/v1/kommuner/1/grenser", status().isNotFound)
    }


    @Test
    fun thatOppdaterKommuneReturns200_Ok() {
        whenever(
            kommuneService.oppdaterKommune(anyInt(), any(KommuneRequest::class.java))
        ).thenReturn(KommuneBuilder.DEFAULT_KOMMUNE)
        put("/v1/kommuner/1", KommuneRequestBuilder.DEFAULT_REQUEST, status().isOk)
    }

    @Test
    fun thatOppdaterFylkeReturns404_NotFound() {
        whenever(
            kommuneService.oppdaterKommune(anyInt(), any(KommuneRequest::class.java))
        ).thenReturn(null)
        put("/v1/kommuner/1", KommuneRequestBuilder.DEFAULT_REQUEST, status().isNotFound)
    }
}