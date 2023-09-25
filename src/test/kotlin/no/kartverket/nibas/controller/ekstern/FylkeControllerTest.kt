package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.service.FylkeService
import org.junit.jupiter.api.BeforeEach
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
    private lateinit var hrefProviderMock: HrefProvider

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun beforeEach() {
        whenever(hrefProviderMock.getHref(any(Identifikator::class.java))).thenReturn(href)
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        val fylke = FylkeBuilder()
            .withNavn("Vestland")
            .build()

        whenever(fylkeService.hentAlleFylker()).thenReturn(listOf(fylke))
        whenever(kodelisteServiceMock.getCodeValueByFylkesnummerId(anyString())).thenReturn("1")
        get("/v1/ekstern/fylker", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(fylkeService.hentFylke(any(Identifikator::class.java))).thenReturn(
            FylkeBuilder()
                .withNavn("Vestland")
                .withFylkesnummer("UUID-123")
                .build()
        )

        whenever(kodelisteServiceMock.getCodeValueByFylkesnummerId(anyString())).thenReturn("1")
        get("/v1/ekstern/fylker/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(fylkeService.hentFylke(any(Identifikator::class.java))).thenReturn(null)
        get("/v1/ekstern/fylker/1", status().isNotFound)
    }
}
