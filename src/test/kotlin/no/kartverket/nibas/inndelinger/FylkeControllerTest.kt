package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
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

    @Test
    fun thatHentAlleReturns200_OK() {
        val fylke = FylkeBuilder()
            .withNavn("Vestland")
            .build()

        whenever(fylkeService.hentAlleFylker()).thenReturn(listOf(fylke))
        whenever(kodelisteServiceMock.getCodeValueByFylkesnummerId(anyString())).thenReturn("1")
        get("/v1/fylker", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(fylkeService.hentFylke(anyString())).thenReturn(
            FylkeBuilder()
                .withNavn("Vestland")
                .withLokalId("123")
                .withFylkesnummer("UUID-123")
                .build()
        )

        whenever(kodelisteServiceMock.getCodeValueByFylkesnummerId(anyString())).thenReturn("1")
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

}
