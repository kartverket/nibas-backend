package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.service.StemmekretsService
import org.junit.jupiter.api.Test
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
        whenever(stemmekretsService.hentStemmekrets(any(Identifikator::class.java))).thenReturn(StemmekretsBuilder.DEFAULT_STEMMEKRETS)
        get("/v1/stemmekretser/1", status().isOk)
    }
}
