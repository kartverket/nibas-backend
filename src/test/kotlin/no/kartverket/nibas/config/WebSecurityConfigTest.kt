package no.kartverket.nibas.config

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.inndelinger.FylkeController
import no.kartverket.nibas.inndelinger.FylkeService
import no.kartverket.nibas.inndelinger.HistorikkService
import no.kartverket.nibas.kodelister.KodelisteService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("dev", inheritProfiles = false)
@WebMvcTest(FylkeController::class)
class WebSecurityConfigTest : NibasMvcTest(){

    @MockBean
    private lateinit var fylkeService: FylkeService

    @MockBean
    private lateinit var kodelisteService: KodelisteService

    @MockBean
    private lateinit var historikkService: HistorikkService

    @Test
    fun thatManglendeAuthorizationHeaderGir401() {
        get("/v1/fylker", status().isUnauthorized)
    }

    @Test
    fun thatUgyldigAuthorizationHeaderGir401() {
        getWithToken("/v1/fylker", "Bearer Incorrect_token", status().isUnauthorized)
    }

}