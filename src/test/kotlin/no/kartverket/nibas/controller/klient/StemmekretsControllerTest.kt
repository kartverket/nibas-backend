package no.kartverket.nibas.controller.klient

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.service.StemmekretsService
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(StemmekretsController::class)
internal class StemmekretsControllerTest : NibasMvcTest() {

    @MockkBean
    private lateinit var stemmekretsService: StemmekretsService

    @Test
    fun hentStemmekretsReturns200_OK() {
        every { stemmekretsService.hentStemmekrets(any()) } returns StemmekretsBuilder.DEFAULT_STEMMEKRETS
        get("/v1/stemmekretser/1", status().isOk)
    }
}
