package no.kartverket.nibas.controller

import no.kartverket.nibas.kodelister.GeonorgeKodelisteItem
import no.kartverket.nibas.kodelister.KodelisteItem
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.KodelisteType
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(KodelisteController::class)
class KodelisteControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockBean
    private lateinit var kodelisteService: KodelisteService

    @Test
    fun fetchKommunenumreReturns200_OK() {
        `when`(kodelisteService.getGyldigeKommunenumre()).thenReturn(
            listOf(
                KodelisteItem(
                    KodelisteType.KOMMUNENUMMER, GeonorgeKodelisteItem(
                        id = "https://register.geonorge.no/sosi-kodelister/kommunenummer-alle/1516/6d31cee4-eafc-4973-8326-fef67eafee1a",
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

        mvc.perform(
            get("/v1/kodeliste/kommunenumre")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }
}
