package no.kartverket.nibas.admin

import no.kartverket.nibas.NibasMvcTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminController::class)
internal class AdminControllerTest : NibasMvcTest() {

    @Test
    fun putForKodelisterInvalidate() {
        put("/v1/admin/kodelister/invalidate", status().isOk)

        verify(kodelisteServiceMock).clearCachedeKodelister()
    }

}
