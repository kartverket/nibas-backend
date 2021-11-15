package no.kartverket.nibas.nibasserverapi.controller


import no.kartverket.nibas.nibasserverapi.api.v1.AdministrativEnhetApi
import no.kartverket.nibas.nibasserverapi.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.nibasserverapi.domain.Kommune
import no.kartverket.nibas.nibasserverapi.domain.Kommunegrense
import no.kartverket.nibas.nibasserverapi.service.AdministrativEnhetService
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@WebMvcTest(AdministrativEnhetApi::class)
class AdministrativEnhetKontrollerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockBean
    private lateinit var administrativenhetService: AdministrativEnhetService

    @Test
    fun thatGetOneIsReturning200OkWhenItemIsFound() {
        Mockito.`when`(administrativenhetService.hentAdministrativEnhet(1, AdministrativEnhetType.KOMMUNE))
            .thenReturn(Kommune(1, "Test", "1", setOf(
                Kommunegrense(1, "Testgrense", GeometryFactory().createLineString()))
            ))

        mvc.perform(
            get("/v1/administrativ-enhet/1?type=kommune").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
    }

    @Test
    fun thatGetOneIsReturning404WheItemIsNotFound() {
        Mockito.`when`(administrativenhetService.hentAdministrativEnhet(1, AdministrativEnhetType.KOMMUNE)).thenReturn(null)

        mvc.perform(
            get("/v1/administrativ-enhet/1?type=kommune").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound)
    }
}