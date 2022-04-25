package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.KommuneKommunegrense
import no.kartverket.nibas.kodelister.KodelisteService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(KommuneController::class)
class KommuneControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var kommuneService: KommuneService

    //Disse ubrukte servicene må være tilstede for å få bygget konteksten
    @MockBean
    private lateinit var kodelisteServiceMock: KodelisteService

    @MockBean
    private lateinit var historikkService: HistorikkService

    @Test
    fun thatHentAlleReturns200_OK() {
        val kommune = KommuneBuilder("1")
            .withNavn("Oslo")
            .build()

        whenever(kommuneService.hentAlleKommuner()).thenReturn(listOf(kommune))
        get("/v1/kommuner", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(kommuneService.hentKommune(anyString())).thenReturn(KommuneBuilder.DEFAULT_KOMMUNE)
        get("/v1/kommuner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(kommuneService.hentKommune(anyString())).thenReturn(null)
        get("/v1/kommuner/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        whenever(
            kommuneService.hentGrenserForKommune(anyString())
        ).thenReturn(
            setOf(KommuneKommunegrense(KommuneBuilder.DEFAULT_KOMMUNE, GrenseBuilder().buildKommunegrense(), GrenseKoblingAttributter()))
        )
        get("/v1/kommuner/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        whenever(kommuneService.hentGrenserForKommune(anyString())).thenReturn(null)
        get("/v1/kommuner/1/grenser", status().isNotFound)
    }


    @Test
    fun thatOppdaterKommuneReturns200_Ok() {
        whenever(
            kommuneService.oppdaterKommune(anyString(), any(KommuneRequest::class.java))
        ).thenReturn(KommuneBuilder.DEFAULT_KOMMUNE)
        put("/v1/kommuner/1", KommuneRequestBuilder.DEFAULT_REQUEST, status().isOk)
    }

    @Test
    fun thatOppdaterFylkeReturns404_NotFound() {
        whenever(
            kommuneService.oppdaterKommune(anyString(), any(KommuneRequest::class.java))
        ).thenReturn(null)
        put("/v1/kommuner/1", KommuneRequestBuilder.DEFAULT_REQUEST, status().isNotFound)
    }
}
