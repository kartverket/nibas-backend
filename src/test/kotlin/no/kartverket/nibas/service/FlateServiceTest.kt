package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.EnhetsGrenseRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.sosi.sosiimport.GrenseMedLokalid
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import kotlin.test.assertEquals

@SpringBootTest(classes = [FlateService::class])
class FlateServiceTest {

    @Autowired
    lateinit var flateService: FlateService

    @MockBean
    lateinit var kommuneRepository: KommuneRepository

    @MockBean
    lateinit var fylkeRepository: FylkeRepository

    @Suppress("unused")
    @MockBean
    lateinit var nasjonRepository: NasjonRepository

    @Suppress("unused")
    @MockBean
    lateinit var grunnkretsRepository: GrunnkretsRepository

    @Suppress("unused")
    @MockBean
    lateinit var stemmekretsRepository: StemmekretsRepository

    @Suppress("unused")
    @MockBean
    lateinit var grenseRepository: GrenseRepository

    @Suppress("unused")
    @MockBean
    lateinit var kodelisteService: KodelisteService

    @Suppress("unused")
    @MockBean
    lateinit var enhetsGrenseRepository: EnhetsGrenseRepository

    @Test
    fun thatOppdaterFlaterSetterFlaterPaaAlleEnheter() {
        val fylkesgrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Fylkesgrense).apply { grense = GeometryFactory().createLineString(arrayOf(
                Coordinate(1.0, 1.0),
                Coordinate(2.0, 1.0),
                Coordinate(200.0, 200.0)
            )) },
            Grense("2", "2", Grensetype.Fylkesgrense).apply {grense = GeometryFactory().createLineString(arrayOf(
                Coordinate(200.0, 200.0),
                Coordinate(10.0, 20.0),
                Coordinate(1.0, 1.0)))
            }
        )
        val grunnkrets = GrunnkretsBuilder().build()
        val grenseKoblinger = (fylkesgrenser)
            .map { GrunnkretsGrenseKobling(grunnkrets, it, GrenseKoblingAttributter()) }
        grunnkrets.apply { grenser = grenseKoblinger.toMutableSet() }
        val kommune = KommuneBuilder().withKommunenummer("1234").build().apply { addGrunnkrets(grunnkrets) }
        val fylke = FylkeBuilder().withFylkesnummer("12").build().apply { addKommune(kommune) }

        val kommuneflate = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(fylkesgrenser.map{ GrenseMedLokalid(it.grense, Lokalid(it.lokalid)) })
        val fylkesflate = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(fylkesgrenser.map{ GrenseMedLokalid(it.grense, Lokalid(it.lokalid)) })

        flateService.oppdaterFlaterFor(setOf(grunnkrets, kommune, fylke))

        verify(kommuneRepository).save(kommune)
        assertEquals(kommuneflate, kommune.omraade)
        verify(fylkeRepository).save(fylke)
        assertEquals(fylkesflate, fylke.omraade)
    }

    @Test
    fun `oppdater kommuneflater som feiler kaster feil`() {
        val kommunegrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Kommunegrense).apply { grense = GeometryFactory().createLineString(arrayOf(
                Coordinate(1.0, 1.0),
                Coordinate(2.0, 1.0),
                Coordinate(20.0, 20.0),
                Coordinate(10.0, 20.0),
            )) }
        )
        val grunnkrets = GrunnkretsBuilder().build()
        val grenseKoblinger = (kommunegrenser)
            .map { GrunnkretsGrenseKobling(grunnkrets, it, GrenseKoblingAttributter()) }
        grunnkrets.apply { grenser = grenseKoblinger.toMutableSet() }
        val kommune = KommuneBuilder().withKommunenummer("123").build().apply { addGrunnkrets(grunnkrets) }

        Assertions.assertThatThrownBy { flateService.oppdaterFlaterFor(setOf(kommune)) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Klarer ikke å sortere grensene slik at de danner et gyldig polygon")
    }

    @Test
    fun `oppdater fylkesflater som feiler kaster feil`() {

        val fylkesgrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Fylkesgrense).apply { grense = GeometryFactory().createLineString(arrayOf(
                Coordinate(1.0, 1.0),
                Coordinate(2.0, 1.0),
                Coordinate(20.0, 20.0),
                Coordinate(10.0, 20.0),
            )) },
            Grense("2", "2", Grensetype.Fylkesgrense).apply {grense = GeometryFactory().createLineString(arrayOf(
                Coordinate(12.0, 20.0),
                Coordinate(1.0, 1.0)))
            }
        )

        val grunnkrets = GrunnkretsBuilder().build()
        val grenseKoblinger = (fylkesgrenser)
            .map { GrunnkretsGrenseKobling(grunnkrets, it, GrenseKoblingAttributter()) }
        grunnkrets.apply { grenser = grenseKoblinger.toMutableSet() }
        val kommune = KommuneBuilder().withKommunenummer("1234").build().apply { addGrunnkrets(grunnkrets) }
        val fylke = FylkeBuilder().withFylkesnummer("123").build().apply { addKommune(kommune) }

        Assertions.assertThatThrownBy { flateService.oppdaterFlaterFor(setOf(fylke)) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Klarer ikke å sortere grensene slik at de danner et gyldig polygon.")
    }

    @Test
    fun `oppdater nasjon som feiler kaster feil`() {
        val fylkesgrenser: List<Grense> = listOf(
            Grense("1", "1", Grensetype.Riksgrense).apply { grense = GeometryFactory().createLineString(arrayOf(
                Coordinate(1.0, 1.0),
                Coordinate(2.0, 1.0),
                Coordinate(20.0, 20.0),
                Coordinate(10.0, 20.0),
            )) },
            Grense("2", "2", Grensetype.Riksgrense).apply {grense = GeometryFactory().createLineString(arrayOf(
                Coordinate(12.0, 20.0),
                Coordinate(1.0, 1.0)))
            }
        )

        val grunnkrets = GrunnkretsBuilder().build()
        val grenseKoblinger = (fylkesgrenser)
            .map { GrunnkretsGrenseKobling(grunnkrets, it, GrenseKoblingAttributter()) }
        grunnkrets.apply { grenser = grenseKoblinger.toMutableSet() }
        val kommune = KommuneBuilder().withKommunenummer("1234").build().apply { addGrunnkrets(grunnkrets) }
        val fylke = FylkeBuilder().withFylkesnummer("123").build().apply { addKommune(kommune) }
        val nasjon = NasjonBuilder().build().apply { addFylke(fylke) }

        Assertions.assertThatThrownBy { flateService.oppdaterFlaterFor(setOf(nasjon)) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Klarer ikke å sortere grensene slik at de danner et gyldig polygon")
    }
}
