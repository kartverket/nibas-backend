package no.kartverket.nibas.admin

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.kodelister.KodelisteService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminController::class)
internal class AdminControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var kodelisteService: KodelisteService

    @Test
    fun putForKodelisterInvalidate() {
        put("/v1/admin/kodelister/invalidate", status().isOk)

        Mockito.verify(kodelisteService).clearKodelister()
    }

}
