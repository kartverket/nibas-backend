package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.repository.GrenseRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever


@WebMvcTest(GrenseController::class)
class GrenseControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var grenseServiceMock: GrenseService

    @MockBean
    private lateinit var flateService: FlateService

    @MockBean
    private lateinit var grenseRepositoryMock: GrenseRepository

    @Test
    fun thatHentGrenseReturns200_OK() {
        whenever(grenseServiceMock.hentGrense("1"))
            .thenReturn(GrenseBuilder().buildKommunegrense())

        get("/v1/grenser/1", status().isOk)
    }

    @Test
    fun thatHentGrenseReturns404_NotFound() {
        whenever(grenseServiceMock.hentGrense("1"))
            .thenReturn(null)

        get("/v1/grenser/1", status().isNotFound)
    }

}
