package no.kartverket.nibas.utkast

import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.UtkastEndringstype
import no.kartverket.nibas.api.v1.request.UtkastRequest
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever


@WebMvcTest(UtkastController::class)
internal class UtkastControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var utkastServiceMock: UtkastService

    @Test
    fun opprett() {
        val id = "b469a516-9ea4-4162-b8b8-ab1e1f165f40"
        whenever(utkastServiceMock.opprettUtkast(any(UtkastRequest::class.java))).thenReturn(id)
        val requestBody = UtkastRequest(
            navn = "Mitt utkast",
            endringstype = UtkastEndringstype.IKKE_DEFINERT
        )
        post(
            url = "/v1/utkast",
            requestObject = requestBody,
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = UtkastOpprettetResponse(id),
                resultMatchers = listOf(status().isCreated)
            )
        )
    }
}
