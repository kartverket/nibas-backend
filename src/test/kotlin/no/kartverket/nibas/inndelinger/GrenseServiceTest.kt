package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.TestConfiguration
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import java.util.*
import org.mockito.Mockito.`when` as whenever

@SpringBootTest
@Import(TestConfiguration::class)
class GrenseServiceTest {

    @Autowired
    lateinit var grenseService: GrenseService

    @MockBean
    private lateinit var grenseRepositoryMock: GrenseRepository

    @MockBean
    private lateinit var grunnkretsRepositoryMock: GrunnkretsRepository

    @MockBean
    private lateinit var stemmekretsRepositoryMock: StemmekretsRepository

    @Captor
    lateinit var fylkesgrenseCaptor: ArgumentCaptor<List<Grense>>

    /**
     * Har 1 Stemmekrets med 1 Grense (type Fylkesgrense) tilknyttet
     * Har 1 Grunnkrets med 3 Grense (type Fylkesgrense) tilknyttet
     * Sender inn 4 Features til lagreGrenser for de 4 grensene.
     * Grensekoblingene skal da bli oppdatert.
     */
    @Test
    fun thatLagreGrenserOppdatererEksisterende() {
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(Coordinate(1.0, 1.0), Coordinate(2.0, 2.0)))

        val nyGrenseGeometri = GeometryFactory().createLineString(arrayOf(Coordinate(2.0, 2.0), Coordinate(3.0, 3.0)))
        val nyGrenseGeometri2 = GeometryFactory().createLineString(arrayOf(Coordinate(3.0, 3.0), Coordinate(4.0, 4.0)))
        val nyGrenseGeometri3 = GeometryFactory().createLineString(arrayOf(Coordinate(4.0, 4.0), Coordinate(5.0, 5.0)))

        val grenseId1 = "1"
        val grenseId2 = "2"
        val grenseId3 = "3"
        val grenseId4 = "4"
        val eksisterendeGrense: Grense = GrenseBuilder(grenseId1).buildFylkesgrense()
        val eksisterendeGrense2: Grense = GrenseBuilder(grenseId2).buildFylkesgrense()
        val eksisterendeGrense3: Grense = GrenseBuilder(grenseId3).buildFylkesgrense()
        val eksisterendeGrense4: Grense = GrenseBuilder(grenseId4).buildFylkesgrense()

        val stemmekretsId = "55"
        val grunnkretsId = "18"

        val oppdatertFylkesgrensefeatureForStemmekrets = FeatureBuilder(grenseId2)
            .withKontekstType(KontekstEnhetType.STEMMEKRETS)
            .withKontekstId(stemmekretsId)
            .withGrensetype(Grensetype.Fylkesgrense)
            .withGeometry(nyGrenseGeometri)
            .build()

        val oppdatertGrensefeature = FeatureBuilder(grenseId1)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId(grunnkretsId)
            .withGrensetype(Grensetype.Fylkesgrense)
            .withGeometry(oppdatertGeometri)
            .withRekkefoelge(0)
            .build()
        val oppdatertFylkesgrensefeatureForGrunnkrets = FeatureBuilder(grenseId3)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId(grunnkretsId)
            .withGrensetype(Grensetype.Fylkesgrense)
            .withGeometry(nyGrenseGeometri2)
            .withRekkefoelge(1)
            .build()
        val oppdatertFylkesgrensefeature2ForGrunnkrets = FeatureBuilder(grenseId4)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId(grunnkretsId)
            .withGrensetype(Grensetype.Fylkesgrense)
            .withGeometry(nyGrenseGeometri3)
            .withRekkefoelge(2)
            .withRetning(false)
            .build()

        val stemmekretsSomSkalOppdateres = StemmekretsBuilder(stemmekretsId)
            .withGrenser(eksisterendeGrense2)
            .build()
        val grunnkretsSomSkalOppdateres = GrunnkretsBuilder(grunnkretsId)
            .withGrenser(eksisterendeGrense, eksisterendeGrense3, eksisterendeGrense4)
            .build()

        whenever(grenseRepositoryMock.findAllById(anyCollection())).thenReturn(listOf(eksisterendeGrense, eksisterendeGrense2,
            eksisterendeGrense3, eksisterendeGrense4))
        whenever(grenseRepositoryMock.findById(grenseId1)).thenReturn(Optional.of(eksisterendeGrense))
        whenever(grenseRepositoryMock.findById(grenseId2)).thenReturn(Optional.of(eksisterendeGrense2))
        whenever(grenseRepositoryMock.findById(grenseId3)).thenReturn(Optional.of(eksisterendeGrense3))
        whenever(grenseRepositoryMock.findById(grenseId4)).thenReturn(Optional.of(eksisterendeGrense4))
        whenever(stemmekretsRepositoryMock.findAllById(anyCollection())).thenReturn(listOf(stemmekretsSomSkalOppdateres))
        whenever(grunnkretsRepositoryMock.findAllById(anyCollection())).thenReturn(listOf(grunnkretsSomSkalOppdateres))

        grenseService.lagreGrenser(
            FeatureCollection(features = listOf(
                oppdatertFylkesgrensefeatureForStemmekrets,
                oppdatertGrensefeature,
                oppdatertFylkesgrensefeatureForGrunnkrets,
                oppdatertFylkesgrensefeature2ForGrunnkrets
            ))
        )

        verify(grenseRepositoryMock, times(1)).saveAll(fylkesgrenseCaptor.capture())
        val lagredeGrenser: MutableList<List<Grense>> = fylkesgrenseCaptor.allValues
        assertThat(lagredeGrenser.flatten()).hasSize(4).withFailMessage("Skal lagre 4 grenser til databasen")

        assertThat(stemmekretsSomSkalOppdateres.getGrenserAvType(Grensetype.Fylkesgrense))
            .hasSize(1)
            .withFailMessage("Stemmekrets skal ha en fylkesgrense")

        val fylkesgrenser = grunnkretsSomSkalOppdateres.getGrenserAvType(Grensetype.Fylkesgrense)
        assertThat(fylkesgrenser)
            .hasSize(3)
            .withFailMessage("Grunnkrets skal ha tre fylkesgrenser")
        val lineStrings = fylkesgrenser.map { it.grense.grense }.toSet()
        assertThat(lineStrings).isEqualTo(setOf(oppdatertGeometri, nyGrenseGeometri2, nyGrenseGeometri3))

        val grunnkretsGrenseKobling1 = grunnkretsGrenseKobling(grunnkretsSomSkalOppdateres, grenseId1)
        val grunnkretsGrenseKobling2 = grunnkretsGrenseKobling(grunnkretsSomSkalOppdateres, grenseId3)
        val grunnkretsGrenseKobling3 = grunnkretsGrenseKobling(grunnkretsSomSkalOppdateres, grenseId4)

        assertRekkefoelge(grunnkretsGrenseKobling1, 0)
        assertRekkefoelge(grunnkretsGrenseKobling2, 1)
        assertRekkefoelge(grunnkretsGrenseKobling3, 2)
        assertRetningMotKlokka(grunnkretsGrenseKobling3)
    }

    private fun assertRekkefoelge(grunnkretsGrenseKobling: GrunnkretsGrenseKobling, rekkefoelge: Int) {
        assertThat(grunnkretsGrenseKobling.grenseKoblingAttributter.rekkefoelge).isEqualTo(rekkefoelge)
    }

    private fun assertRetningMotKlokka(grunnkretsGrenseKobling: GrunnkretsGrenseKobling) {
        assertThat(grunnkretsGrenseKobling.grenseKoblingAttributter.retningMedKlokken).isEqualTo(false)
    }

    private fun grunnkretsGrenseKobling(grunnkretsSomSkalOppdateres: Grunnkrets, grenseId: String): GrunnkretsGrenseKobling =
        grunnkretsSomSkalOppdateres.grenser.first { it.grense.id == grenseId }

    @Test
    fun thatIncomingFeatureWithoutIdThrowsException() {
        // Innkommende feature uten id
        val nyStemmekretsgrensefeatureForGrunnkrets = FeatureBuilder(null)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId("1")
            .withGrensetype(Grensetype.Stemmekretsgrense)
            .build()

        assertThatThrownBy {
            grenseService.lagreGrenser(FeatureCollection(features = listOf(nyStemmekretsgrensefeatureForGrunnkrets)))
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Missing feature id")
    }

    @Test
    fun thatIncomingFeatureWithUnknownIdThrowsException() {
        // Har en grunnkretsgrense med id 34
        whenever(grenseRepositoryMock.findAllById(anyCollection())).thenReturn(listOf(
            GrenseBuilder("34")
                .buildGrunnkretsgrense()))

        // Innkommende feature på ukjent grenseId
        val featureWithUnknownId = FeatureBuilder("33")
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .build()

        assertThatThrownBy { grenseService.lagreGrenser(FeatureCollection(features = listOf(featureWithUnknownId))) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Non-existing grenseIds: [33]")
    }

    @Test
    fun hentGrenseReturnsNullInCaseOfNoMatch() {
        val emptyKommunegrense = Optional.empty<Grense>()
        whenever(grenseRepositoryMock.findById(anyString())).thenReturn(emptyKommunegrense)
        assertThat(grenseService.hentGrense("1")).isNull()
    }

}
