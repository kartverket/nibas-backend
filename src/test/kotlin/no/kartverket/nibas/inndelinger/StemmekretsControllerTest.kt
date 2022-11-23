package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.StemmekretsBuilder
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(StemmekretsController::class)
internal class StemmekretsControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var stemmekretsService: StemmekretsService

    @Test
    fun hentStemmekretsReturns200_OK() {
        whenever(kodelisteServiceMock.getCodeValueByKommunenummerId(anyString())).thenReturn("0301")
        whenever(stemmekretsService.hentStemmekrets(anyString())).thenReturn(StemmekretsBuilder.DEFAULT_STEMMEKRETS)
        get("/v1/stemmekretser/1", status().isOk)
    }
}
