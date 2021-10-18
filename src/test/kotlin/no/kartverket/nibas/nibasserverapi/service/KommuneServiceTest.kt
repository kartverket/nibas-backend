package no.kartverket.nibas.nibasserverapi.service

import no.kartverket.nibas.nibasserverapi.domain.Kommune
import no.kartverket.nibas.nibasserverapi.domain.Kommunegrense
import no.kartverket.nibas.nibasserverapi.repository.KommuneRepository
import no.kartverket.nibas.nibasserverapi.repository.KommunegrenseRepository
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(classes = [KommuneService::class])
class KommuneServiceTest {

    @Autowired lateinit var kommuneService: KommuneService

    @MockBean
    private lateinit var kommunegrenseRepository: KommunegrenseRepository

    @MockBean
    private lateinit var kommuneRepository: KommuneRepository

    @Test
    fun thatOppdaterKommuneLagrerKommunegrenseneSine() {
        val kommunegrense1 = Kommunegrense(1, "Testgrense", GeometryFactory().createLineString())
        val kommunegrense2 = Kommunegrense(1, "Testgrense", GeometryFactory().createLineString())
        val kommune = Kommune(1, "Test", 1, arrayListOf(kommunegrense1, kommunegrense2))

        kommuneService.oppdaterKommune(kommune)
        Mockito.verify(kommuneRepository).save(kommune)
        Mockito.verify(kommunegrenseRepository).save(kommunegrense1)
        Mockito.verify(kommunegrenseRepository).save(kommunegrense2)
    }

    @Test
    fun thatOppdaterKommuneHaandtererAtManIkkeHarKommunegrenser() {
        val kommune = Kommune(1, "Test", 1)

        kommuneService.oppdaterKommune(kommune)
        Mockito.verify(kommuneRepository).save(kommune)
        Mockito.verifyNoInteractions(kommunegrenseRepository)
    }
}