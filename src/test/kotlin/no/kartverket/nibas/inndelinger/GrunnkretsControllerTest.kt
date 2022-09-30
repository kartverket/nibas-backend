package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.GrunnkretsBuilder
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(GrunnkretsController::class)
internal class GrunnkretsControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var grunnkretsService: GrunnkretsService

    @MockBean
    private lateinit var grunnkretsControllerService: GrunnkretsControllerService

    @Test
    fun hentGrunnkretsReturns200_OK() {
        whenever(grunnkretsService.hentGrunnkrets(anyString())).thenReturn(GrunnkretsBuilder.DEFAULT_GRUNNKRETS)
        get("/v1/grunnkretser/1", status().isOk)
    }
}
