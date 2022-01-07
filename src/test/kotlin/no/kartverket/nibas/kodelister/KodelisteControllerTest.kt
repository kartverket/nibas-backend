package no.kartverket.nibas.kodelister

import no.kartverket.nibas.NibasMvcTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when` as whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(KodelisteController::class)
class KodelisteControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var kodelisteService: KodelisteService

    @Test
    fun fetchKommunenumreReturns200_OK() {
        whenever(kodelisteService.getKommunenumre()).thenReturn(
            listOf(
                KodelisteItem(
                    KodelisteType.KOMMUNENUMMER, GeonorgeKodelisteItem(
                        id = "https://register.geonorge.no/sosi-kodelister/kommunenummer/1516/6d31cee4-eafc-4973-8326-fef67eafee1a",
                        label = "1516",
                        lang = "no",
                        uuid = "6d31cee4-eafc-4973-8326-fef67eafee1a",
                        status = "Gyldig",
                        description = "Ulstein",
                        codevalue = "1516"
                    )
                )
            )
        )

        get("/v1/kodeliste/kommunenumre", status().isOk)
    }
}
