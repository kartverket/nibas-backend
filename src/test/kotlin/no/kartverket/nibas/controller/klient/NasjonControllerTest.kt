package no.kartverket.nibas.controller.klient

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.service.NasjonService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(NasjonController::class)
class NasjonControllerTest: NibasMvcTest() {

    @MockkBean
    private lateinit var nasjonService: NasjonService

    @Test
    fun `hentNasjon returnerer 200_OK hvis kun en nasjon er gyldig paa dato`() {
        val nasjon = NasjonBuilder("1")
            .withNavn("Norge")
            .build()

        every { nasjonService.hentAlleNasjoner() } returns listOf(nasjon)
        get("/v1/nasjon/", MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `hentNasjon feiler hvis ingen nasjon er gyldig paa dato`() {
        every { nasjonService.hentAlleNasjoner() } returns emptyList()
        get("/v1/nasjon/", MockMvcResultMatchers.status().isInternalServerError)
    }

    @Test
    fun `hentNasjon feiler hvis mer enn en nasjon er gyldig paa dato`() {
        val nasjon1 = NasjonBuilder("1")
            .withNavn("Norge1")
            .build()

        val nasjon2 = NasjonBuilder("2")
            .withNavn("Norge2")
            .build()

        every { nasjonService.hentAlleNasjoner() } returns listOf(nasjon1, nasjon2)
        get("/v1/nasjon/", MockMvcResultMatchers.status().isInternalServerError)
    }
}
