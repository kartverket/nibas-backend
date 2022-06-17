package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.FylkeRequestBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.kodelister.KodelisteService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(FylkeController::class)
class FylkeControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var fylkeService: FylkeService

    @MockBean
    private lateinit var kodelisteService: KodelisteService

    @MockBean
    private lateinit var historikkService: HistorikkService

    @Test
    fun thatHentAlleReturns200_OK() {
        val fylke = FylkeBuilder()
            .withNavn("Vestland")
            .build()

        whenever(fylkeService.hentAlleFylker()).thenReturn(listOf(fylke))
        whenever(kodelisteService.getCodeValueByFylkesnummerId(anyString())).thenReturn("1")
        get("/v1/fylker", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(fylkeService.hentFylke(anyString())).thenReturn(
            FylkeBuilder()
                .withNavn("Vestland")
                .withLokalId("123")
                .withNavnerom("ABC")
                .withFylkesnummer("UUID-123")
                .build()
        )

        whenever(kodelisteService.getCodeValueByFylkesnummerId(anyString())).thenReturn("1")
        get("/v1/fylker/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(fylkeService.hentFylke(anyString())).thenReturn(null)
        get("/v1/fylker/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        whenever(
            fylkeService.hentGrenserForFylke(anyString())
        ).thenReturn(
            setOf(GrunnkretsGrenseKobling(grunnkrets = GrunnkretsBuilder().build(), grense = GrenseBuilder().buildFylkesgrense(), GrenseKoblingAttributter()))
        )
        get("/v1/fylker/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        whenever(fylkeService.hentGrenserForFylke(anyString())).thenReturn(emptySet())
        get("/v1/fylker/1/grenser", status().isNotFound)
    }


    @Test
    fun thatOppdaterFylkeReturns200_Ok() {
        whenever(
            fylkeService.oppdaterFylke(anyString(), any(FylkeRequest::class.java))
        ).thenReturn(FylkeBuilder().build())
        whenever(kodelisteService.getCodeValueByFylkesnummerId(anyString())).thenReturn("1")
        put("/v1/fylker/1", FylkeRequestBuilder.DEFAULT_REQUEST, status().isOk)
    }

    @Test
    fun thatOppdaterFylkeReturns404_NotFound() {
        whenever(
            fylkeService.oppdaterFylke(anyString(), any(FylkeRequest::class.java))
        ).thenReturn(null)
        put("/v1/fylker/1", FylkeRequestBuilder.DEFAULT_REQUEST, status().isNotFound)
    }
}
