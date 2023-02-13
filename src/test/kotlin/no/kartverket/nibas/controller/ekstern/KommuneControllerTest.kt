package no.kartverket.nibas.controller.ekstern

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.service.KommuneService
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

    @MockBean
    private lateinit var hrefProviderMock: HrefProvider

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun mockKodelisteService() {
        whenever(kodelisteServiceMock.getCodeValueByKommunenummerId(anyString())).thenReturn("0301")
        whenever(hrefProviderMock.getHref(any(Identifikator::class.java))).thenReturn(href)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(kommuneService.hentKommune(any(Identifikator::class.java))).thenReturn(KommuneBuilder.DEFAULT_KOMMUNE)
        get("/v1/ekstern/kommuner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(kommuneService.hentKommune(any(Identifikator::class.java))).thenReturn(null)
        get("/v1/ekstern/kommuner/1", status().isNotFound)
    }
}
