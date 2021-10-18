package no.kartverket.nibas.nibasserverapi.controller


import no.kartverket.nibas.nibasserverapi.api.v1.KommuneApi
import no.kartverket.nibas.nibasserverapi.domain.Kommune
import no.kartverket.nibas.nibasserverapi.domain.Kommunegrense
import no.kartverket.nibas.nibasserverapi.service.KommuneService
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@WebMvcTest(KommuneApi::class)
class KommuneKontrollerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockBean
    private lateinit var kommuneService: KommuneService

    @Test
    fun thatGetOneIsReturning200OkWhenItemIsFound() {
        Mockito.`when`(kommuneService.hentMedId(any(Int::class.java)))
            .thenReturn(Kommune(1, "Test", 1, arrayListOf(
                Kommunegrense(1, "Testgrense", GeometryFactory().createLineString()))))

        mvc.perform(
            get("/v1/kommune/1").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
    }

    @Test
    fun thatGetOneIsReturning404WheItemIsNotFound() {
        Mockito.`when`(kommuneService.hentMedId(any(Int::class.java))).thenReturn(null);

        mvc.perform(
            get("/v1/kommune/2").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound)
    }
}