package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import org.junit.jupiter.api.BeforeEach
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

    @BeforeEach
    fun mockKodelisteService() {
        whenever(kodelisteServiceMock.getCodeValueByKommunenummerId(anyString())).thenReturn("0301")
    }

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
            setOf(GrunnkretsGrenseKobling(grunnkrets = GrunnkretsBuilder().build(), grense = GrenseBuilder().buildKommunegrense(), GrenseKoblingAttributter()))
        )
        get("/v1/kommuner/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        whenever(kommuneService.hentGrenserForKommune(anyString())).thenReturn(emptySet())
        get("/v1/kommuner/1/grenser", status().isNotFound)
    }

}
