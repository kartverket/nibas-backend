package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.geometry.UgyldigeFlaterException
import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.geometry.toMultiPolygon
import no.kartverket.nibas.common.geometry.topologyValidationErrorToCustomText
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.BopliktomraadeRepository
import no.kartverket.nibas.repository.EnhetsGrenseRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.operation.valid.TopologyValidationError
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest(classes = [FlateService::class])
class FlateServiceTest {

    @Autowired
    lateinit var flateService: FlateService

    @MockitoBean
    lateinit var kommuneRepository: KommuneRepository

    @MockitoBean
    lateinit var fylkeRepository: FylkeRepository

    @Suppress("unused")
    @MockitoBean
    lateinit var nasjonRepository: NasjonRepository

    @Suppress("unused")
    @MockitoBean
    lateinit var grunnkretsRepository: GrunnkretsRepository

    @Suppress("unused")
    @MockitoBean
    lateinit var stemmekretsRepository: StemmekretsRepository

    @Suppress("unused")
    @MockitoBean
    lateinit var bopliktomraadeRepository: BopliktomraadeRepository

    @Suppress("unused")
    @MockitoBean
    lateinit var grenseRepository: GrenseRepository

    @Suppress("unused")
    @MockitoBean
    lateinit var kodelisteService: KodelisteService

    @Suppress("unused")
    @MockitoBean
    lateinit var enhetsGrenseRepository: EnhetsGrenseRepository

    @Test
    fun thatOppdaterFlaterSetterFlaterPaaAlleEnheter() {
        val fylkesgrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Fylkesgrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(1.0, 1.0),
                    Coordinate(2.0, 1.0),
                    Coordinate(200.0, 200.0)
                ))
            },
            Grense("2", "2", Grensetype.Fylkesgrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(200.0, 200.0),
                    Coordinate(10.0, 20.0),
                    Coordinate(1.0, 1.0)))
            }
        )
        val grunnkrets = GrunnkretsBuilder().build()
        val grenseKoblinger = (fylkesgrenser)
            .map { GrunnkretsGrenseKobling(grunnkrets, it) }
        grunnkrets.apply { grenser = grenseKoblinger.toMutableSet() }
        val kommune = KommuneBuilder().withKommunenummer("1234").build().apply { addGrunnkrets(grunnkrets) }
        val fylke = FylkeBuilder().withFylkesnummer("12").build().apply { addKommune(kommune) }

        val kommuneflate = getGrenserIRiktigRekkefoelge(fylkesgrenser.map { it.grense }).toMultiPolygon()
        val fylkesflate = getGrenserIRiktigRekkefoelge(fylkesgrenser.map { it.grense }).toMultiPolygon()

        flateService.oppdaterFlaterFor(setOf(grunnkrets, kommune, fylke))

        verify(kommuneRepository).save(kommune)
        assertEquals(kommuneflate, kommune.omraade)
        verify(fylkeRepository).save(fylke)
        assertEquals(fylkesflate, fylke.omraade)
    }

    @Test
    fun `oppdater flater kaster feilmelding for alle flater som feiler`() {
        val korrekteGrunnkretsgrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Grunnkretsgrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(1.0, 1.0),
                    Coordinate(2.0, 1.0),
                    Coordinate(200.0, 200.0)
                ))
            },
            Grense("2", "2", Grensetype.Grunnkretsgrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(200.0, 200.0),
                    Coordinate(10.0, 20.0),
                    Coordinate(1.0, 1.0)))
            }
        )
        val grunnkretsMedKorrekteGrenser = GrunnkretsBuilder().build()
        val korrekteGrenseKoblinger = korrekteGrunnkretsgrenser.map { GrunnkretsGrenseKobling(grunnkretsMedKorrekteGrenser, it) }
        grunnkretsMedKorrekteGrenser.apply { grenser = korrekteGrenseKoblinger.toMutableSet() }

        val feilaktigeGrunnkretsgrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Grunnkretsgrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(1.0, 1.0),
                    Coordinate(2.0, 1.0),
                    Coordinate(20.0, 20.0),
                    Coordinate(10.0, 20.0),
                ))
            }
        )

        val feilaktigeKommunegrenser: List<Grense> = listOf(
            Grense("2", "2", Grensetype.Kommunegrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(1.0, 1.0),
                    Coordinate(2.0, 1.0),
                    Coordinate(20.0, 20.0),
                    Coordinate(10.0, 15.0),
                ))
            }
        )

        val feilaktigGrunnkrets1 = GrunnkretsBuilder(grunnkretsnavn = "grunnkrets1", lokalId = Lokalid("gk1")).build()
        val feailaktigeGrenseKoblinger = feilaktigeGrunnkretsgrenser.map { GrunnkretsGrenseKobling(feilaktigGrunnkrets1, it) }
        feilaktigGrunnkrets1.apply { grenser = feailaktigeGrenseKoblinger.toMutableSet() }

        val feilaktigGrunnkrets2 = GrunnkretsBuilder(grunnkretsnavn = "grunnkrets2", lokalId = Lokalid("gk2")).build()
        val feailaktigeGrenseKoblinger2 = feilaktigeKommunegrenser.map { GrunnkretsGrenseKobling(feilaktigGrunnkrets2, it) }
        feilaktigGrunnkrets2.apply { grenser = feailaktigeGrenseKoblinger2.toMutableSet() }
        val kommune = KommuneBuilder(navn = "Ringerike").withKommunenummer("123").build().apply { addGrunnkrets(feilaktigGrunnkrets2) }

        val exception = assertFailsWith<UgyldigeFlaterException> {
            flateService.oppdaterFlaterFor(setOf(grunnkretsMedKorrekteGrenser, feilaktigGrunnkrets1, kommune))
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(2)
        val ugyldigFlateInformasjonForGrunnkrets1 = ugyldigeFlater.find { it.lokalid == feilaktigGrunnkrets1.lokalid }
        ugyldigFlateInformasjonForGrunnkrets1?.let {
            assertThat(it.flateInformasjon).isEqualTo(feilaktigGrunnkrets1.getFlateInformasjon())
            assertThat(it.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
            assertThat(it.koordinat).isEqualTo(Coordinate(10.0, 20.0))
        }

        val ugyldigFlateInformasjonForKommune = ugyldigeFlater.find { it.lokalid == kommune.lokalid }
        ugyldigFlateInformasjonForKommune?.let {
            assertThat(it.flateInformasjon).isEqualTo(kommune.getFlateInformasjon())
            assertThat(it.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
            assertThat(it.koordinat).isEqualTo(Coordinate(10.0, 15.0))
        }
    }

    @Test
    fun `oppdater kommuneflater som feiler kaster feil`() {
        val kommunegrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Kommunegrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(1.0, 1.0),
                    Coordinate(2.0, 1.0),
                    Coordinate(20.0, 20.0),
                    Coordinate(10.0, 20.0),
                ))
            }
        )
        val grunnkrets = GrunnkretsBuilder().build()
        val grenseKoblinger = (kommunegrenser)
            .map { GrunnkretsGrenseKobling(grunnkrets, it) }
        grunnkrets.apply { grenser = grenseKoblinger.toMutableSet() }

        val kommune = KommuneBuilder(navn = "Ringerike").withKommunenummer("123").build().apply { addGrunnkrets(grunnkrets) }

        val exception = assertFailsWith<UgyldigeFlaterException> {
            flateService.oppdaterFlaterFor(setOf(kommune))
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(1)
        val ugyldigFlate = ugyldigeFlater.first()
        assertThat(ugyldigFlate.flateInformasjon).isEqualTo(kommune.getFlateInformasjon())
        assertThat(ugyldigFlate.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
        assertThat(ugyldigFlate.koordinat).isEqualTo(Coordinate(10.0, 20.0))
    }

    @Test
    fun `oppdater fylkesflater som feiler kaster feil`() {

        val fylkesgrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Fylkesgrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(1.0, 1.0),
                    Coordinate(2.0, 1.0),
                    Coordinate(20.0, 20.0),
                    Coordinate(10.0, 20.0),
                ))
            },
            Grense("2", "2", Grensetype.Fylkesgrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(12.0, 20.0),
                    Coordinate(1.0, 1.0)))
            }
        )

        val grunnkrets = GrunnkretsBuilder().build()
        val grenseKoblinger = (fylkesgrenser)
            .map { GrunnkretsGrenseKobling(grunnkrets, it) }
        grunnkrets.apply { grenser = grenseKoblinger.toMutableSet() }
        val kommune = KommuneBuilder().withKommunenummer("1234").build().apply { addGrunnkrets(grunnkrets) }

        val administrativenhetnavn = Administrativenhetnavn().apply { navn = "Buskerud" }
        val fylke = FylkeBuilder(administrativenhetnavn = listOf(administrativenhetnavn)).withFylkesnummer("123").build().apply { addKommune(kommune) }

        val exception = assertFailsWith<UgyldigeFlaterException> {
            flateService.oppdaterFlaterFor(setOf(fylke))
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(1)
        val ugyldigFlate = ugyldigeFlater.first()
        assertThat(ugyldigFlate.flateInformasjon).isEqualTo(fylke.getFlateInformasjon())
        assertThat(ugyldigFlate.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
        assertThat(ugyldigFlate.koordinat).isEqualTo(Coordinate(10.0, 20.0))
    }

    @Test
    fun `skal gi feilmelding om flaten ikke har noen grenser`() {
        val grunnkrets = GrunnkretsBuilder().build()
        assertThatThrownBy { flateService.oppdaterFlaterFor(setOf(grunnkrets)) }
            .isInstanceOf(UgyldigeFlaterException::class.java)
            .hasMessageContaining("Feil ved flatedanning: [lokalId1 - Flaten har ingen grenser]")
    }

    @Test
    fun `oppdater nasjon som feiler kaster feil`() {
        val fylkesgrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Riksgrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(1.0, 1.0),
                    Coordinate(2.0, 1.0),
                    Coordinate(20.0, 20.0),
                    Coordinate(10.0, 20.0),
                ))
            },
            Grense("2", "2", Grensetype.Riksgrense).apply {
                grense = GeometryFactory().createLineString(arrayOf(
                    Coordinate(12.0, 20.0),
                    Coordinate(1.0, 1.0)))
            }
        )

        val grunnkrets = GrunnkretsBuilder().build()
        val grenseKoblinger = (fylkesgrenser)
            .map { GrunnkretsGrenseKobling(grunnkrets, it) }
        grunnkrets.apply { grenser = grenseKoblinger.toMutableSet() }
        val kommune = KommuneBuilder().withKommunenummer("1234").build().apply { addGrunnkrets(grunnkrets) }
        val fylke = FylkeBuilder().withFylkesnummer("123").build().apply { addKommune(kommune) }

        val nasjon = NasjonBuilder(navn = "Norge").build().apply { addFylke(fylke) }

        val exception = assertFailsWith<UgyldigeFlaterException> {
            flateService.oppdaterFlaterFor(setOf(nasjon))
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(1)
        val ugyldigFlate = ugyldigeFlater.first()
        assertThat(ugyldigFlate.flateInformasjon).isEqualTo(nasjon.getFlateInformasjon())
        assertThat(ugyldigFlate.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
        assertThat(ugyldigFlate.koordinat).isEqualTo(Coordinate(10.0, 20.0))
    }
}
