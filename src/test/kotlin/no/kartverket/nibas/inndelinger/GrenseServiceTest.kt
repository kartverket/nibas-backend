package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.repository.AvtaltAvgrensningslinjeRepository
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.FylkesgrenseRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.KommunegrenseRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.RiksgrenseRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import no.kartverket.nibas.repository.TerritorialgrenseRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import kotlin.test.assertEquals
import org.mockito.Mockito.`when` as whenever

@SpringBootTest(classes = [GrenseService::class])
class GrenseServiceTest {

    @Autowired
    lateinit var grenseService: GrenseService

    //Disse ubrukte servicene må være tilstede for å få bygget konteksten
    @MockBean
    private lateinit var kommunegrenseRepository: KommunegrenseRepository

    @MockBean
    private lateinit var fylkesgrenseRepository: FylkesgrenseRepository

    @MockBean
    private lateinit var territorialgrenseRepository: TerritorialgrenseRepository

    @MockBean
    private lateinit var avtaltAvgrensningslinjeRepository: AvtaltAvgrensningslinjeRepository

    @MockBean
    private lateinit var riksgrenseRepository: RiksgrenseRepository

    @MockBean
    private lateinit var kommuneRepository: KommuneRepository

    @MockBean
    private lateinit var fylkeRepository: FylkeRepository

    @MockBean
    private lateinit var nasjonRepository: NasjonRepository

    @MockBean
    private lateinit var landKodeRepository: LandKodeRepository

    @MockBean
    private lateinit var fastsettingstypeRepositoryMock: FastsettingstypeRepository

    @MockBean
    private lateinit var terrengdetaljkodeRepositoryMock: TerrengdetaljkodeRepository

    @MockBean
    private lateinit var noeyaktighetsklassekodeRepositoryMock: NoeyaktighetsklassekodeRepository

    @MockBean
    private lateinit var grensestatusRepositoryMock: GrensestatusRepository

    @Captor
    var fylkesgrenseCaptor: ArgumentCaptor<List<Fylkesgrense>>? = null

    @Test
    fun thatOppdaterGrenseOppdatererEksisterendeOgOppretterNye() {
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(Coordinate(1.0, 1.0), Coordinate(2.0, 2.0)))

        val nyGrenseGeometri = GeometryFactory().createLineString(arrayOf(Coordinate(2.0, 2.0), Coordinate(3.0, 3.0)))
        val nyGrenseGeometri2 = GeometryFactory().createLineString(arrayOf(Coordinate(3.0, 3.0), Coordinate(4.0, 4.0)))
        val nyGrenseGeometri3 = GeometryFactory().createLineString(arrayOf(Coordinate(4.0, 4.0), Coordinate(5.0, 5.0)))

        val eksisterendeGrense = GrenseBuilder("1").buildFylkesgrense()

        val oppdatertGrensefeature = FeatureBuilder("1").withGrensetype(GrenseType.Fylkesgrense).withGeometry(oppdatertGeometri).build()
        val nyFylkesgrensefeatureForFylke = FeatureBuilder(null)
            .withKontekstType(AdministrativEnhetType.FYLKE)
            .withKontekstId("1")
            .withGrensetype(GrenseType.Fylkesgrense)
            .withGeometry(nyGrenseGeometri).build()
        val nyFylkesgrensefeatureForKommune = FeatureBuilder(null)
            .withKontekstType(AdministrativEnhetType.KOMMUNE)
            .withKontekstId("1")
            .withGrensetype(GrenseType.Fylkesgrense)
            .withGeometry(nyGrenseGeometri2).build()
        val nyFylkesgrensefeature2ForKommune = FeatureBuilder(null)
            .withKontekstType(AdministrativEnhetType.KOMMUNE)
            .withKontekstId("1")
            .withGrensetype(GrenseType.Fylkesgrense)
            .withGeometry(nyGrenseGeometri3).build()

        val fylkeSomSkalOppdateres = FylkeBuilder.DEFAULT_FYLKE
        val kommuneSomSkalOppdateres = KommuneBuilder.DEFAULT_KOMMUNE

        whenever(fylkesgrenseRepository.findAllById(anyCollection())).thenReturn(listOf(eksisterendeGrense))
        whenever(fylkeRepository.findAllById(anyCollection())).thenReturn(listOf(fylkeSomSkalOppdateres))
        whenever(kommuneRepository.findAllById(anyCollection())).thenReturn(listOf(kommuneSomSkalOppdateres))
        grenseService.lagreFylkesgrenser(
            listOf(
                oppdatertGrensefeature,
                nyFylkesgrensefeatureForFylke,
                nyFylkesgrensefeatureForKommune,
                nyFylkesgrensefeature2ForKommune
            )
        )

        verify(fylkesgrenseRepository, times(1)).saveAll(fylkesgrenseCaptor!!.capture())

        val lagredeGrenser: MutableList<List<Fylkesgrense>> = fylkesgrenseCaptor!!.allValues
        assertEquals(4, lagredeGrenser.flatten().size, "Skal lagre 4 grenser til databasen")

        verify(fylkeRepository).saveAll(listOf(fylkeSomSkalOppdateres))
        assertEquals(1, fylkeSomSkalOppdateres.fylkesgrenser.size, "Fylket skal ha fått en fylkesgrense")

        verify(kommuneRepository).saveAll(listOf(kommuneSomSkalOppdateres))
        assertEquals(2, kommuneSomSkalOppdateres.fylkesgrenser.size, "Kommunen skal ha fått to fylkesgrenser")
    }

    @Test
    fun thatKommunegrenseIkkeKanLeggesTilFylke() {
        val nyKommunegrensefeatureForFylke = FeatureBuilder(null)
            .withKontekstType(AdministrativEnhetType.FYLKE)
            .withKontekstId("1")
            .withGrensetype(GrenseType.Kommunegrense)
            .build()

        whenever(fylkeRepository.findAllById(anyCollection())).thenReturn(listOf(FylkeBuilder.DEFAULT_FYLKE))

        val exception =
            assertThrows<RuntimeException>("Skal kaste exception når det legges til en ny kommunegrense på et fylke") {
                grenseService.lagreKommunegrenser(listOf(nyKommunegrensefeatureForFylke))
            }

        assertEquals("Kan ikke legge til Kommunegrense på enhet Fylke", exception.message)
    }

}
