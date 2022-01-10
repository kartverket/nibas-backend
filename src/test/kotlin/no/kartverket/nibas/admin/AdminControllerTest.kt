package no.kartverket.nibas.admin

import no.kartverket.nibas.kodelister.KodelisteService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(AdminController::class)
internal class AdminControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockBean
    private lateinit var kodelisteService: KodelisteService

    @Test
    fun putForKodelisterInvalidate() {
        mvc.perform(
            put("/v1/admin/kodelister/invalidate")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        Mockito.verify(kodelisteService).clearKodelister()
    }

}
