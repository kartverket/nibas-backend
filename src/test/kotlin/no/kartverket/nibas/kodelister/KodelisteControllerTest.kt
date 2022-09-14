package no.kartverket.nibas.kodelister

import no.kartverket.nibas.NibasMvcTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(KodelisteController::class)
class KodelisteControllerTest : NibasMvcTest() {

    @Test
    fun fetchKommunenumreReturns200_OK() {
        whenever(kodelisteServiceMock.getKommunenumre()).thenReturn(
            KodelisteRespons(
                type = KodelisteType.KOMMUNENUMMER,
                items = listOf(
                    KodelisteItem(
                        id = "6d31cee4-eafc-4973-8326-fef67eafee1a",
                        label = "Ulstein"
                    )
                )
            )
        )

        get("/v1/kodeliste/kommunenumre", status().isOk)
    }
}
