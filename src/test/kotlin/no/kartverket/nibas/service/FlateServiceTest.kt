package no.kartverket.nibas.service

import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.EnhetsGrenseRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.Grenserad
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.sosi.sosiimport.GrenseMedLokalid
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import no.kartverket.nibas.sporing.Gyldighet
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate
import kotlin.test.assertEquals
import org.mockito.Mockito.`when` as whenever

@SpringBootTest(classes = [FlateService::class])
class FlateServiceTest {

    @Autowired
    lateinit var flateService: FlateService

    @MockBean
    lateinit var kommuneRepository: KommuneRepository

    @MockBean
    lateinit var fylkeRepository: FylkeRepository

    @MockBean
    lateinit var nasjonRepository: NasjonRepository

    @MockBean
    lateinit var grunnkretsRepository: GrunnkretsRepository

    @MockBean
    lateinit var stemmekretsRepository: StemmekretsRepository

    @MockBean
    lateinit var grenseRepository: GrenseRepository

    @MockBean
    lateinit var kodelisteService: KodelisteService

    @MockBean
    lateinit var enhetsGrenseRepository: EnhetsGrenseRepository

    @Test
    fun thatOppdaterFlaterSetterFlaterPaaAlleEnheter() {
        val kommunegrenser: MutableList<Grenserad> = mutableListOf(createGrenserad(GeometryFactory().createLineString(arrayOf(
            Coordinate(1.0, 1.0),
            Coordinate(2.0, 1.0),
            Coordinate(20.0, 20.0),
            Coordinate(10.0, 20.0),
            Coordinate(1.0, 1.0)))
        ))

        val fylkesgrenser = mutableListOf(createGrenserad(GeometryFactory().createLineString(arrayOf(
            Coordinate(1.0, 1.0),
            Coordinate(2.0, 1.0),
            Coordinate(200.0, 200.0),
            Coordinate(100.0, 200.0),
            Coordinate(1.0, 1.0))
        )))

        val grunnkrets = GrunnkretsBuilder().build()
        val kommune = KommuneBuilder().build()
        val fylke = FylkeBuilder().build()

        whenever(kodelisteService.getCodeValueByKommunenummerId(kommune.kommunenummer)).thenReturn("123")
        whenever(enhetsGrenseRepository.hentGrenserMedRiktigRetning("123", kommune.tillatteGrensetyper())).thenReturn(kommunegrenser)

        whenever(kodelisteService.getCodeValueByFylkesnummerId(fylke.fylkesnummer)).thenReturn("234")
        whenever(enhetsGrenseRepository.hentGrenserMedRiktigRetning("234", fylke.tillatteGrensetyper())).thenReturn(fylkesgrenser)

        val kommuneflate = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(kommunegrenser.map{ GrenseMedLokalid(it.grense, it.lokalid) })
        val fylkesflate = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(fylkesgrenser.map{ GrenseMedLokalid(it.grense, it.lokalid) })

        flateService.oppdaterFlaterFor(setOf(grunnkrets, kommune, fylke))

        verify(kommuneRepository).save(kommune)
        assertEquals(kommuneflate, kommune.omraade)
        verify(fylkeRepository).save(fylke)
        assertEquals(fylkesflate, fylke.omraade)
    }

    private fun createGrenserad(grense: LineString): Grenserad {
        return Grenserad(
            id = "123",
            lokalid = Lokalid("L123"),
            grense = grense,
            gyldighet = Gyldighet(LocalDate.now(), LocalDate.now().plusDays(2))
        )
    }

}
