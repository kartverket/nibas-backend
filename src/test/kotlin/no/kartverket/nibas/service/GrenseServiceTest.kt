package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.service.GrenseService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.Month

@SpringBootTest
class GrenseServiceTest {

    @Autowired
    lateinit var grenseService: GrenseService

    @MockkBean
    private lateinit var grenseRepositoryKotlinMock: GrenseRepository

    @MockkBean
    private lateinit var grunnkretsRepositoryKotlinMock: GrunnkretsRepository

    @MockkBean
    private lateinit var stemmekretsRepositoryKotlinMock: StemmekretsRepository

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
    }

    /**
     * Har 1 Stemmekrets med 1 Grense (type Fylkesgrense) tilknyttet
     * Har 1 Grunnkrets med 3 Grense (type Fylkesgrense) tilknyttet
     * Sender inn 4 Features til lagreGrenser for de 4 grensene.
     * Grensekoblingene skal da bli oppdatert.
     */
    @Test
    fun thatLagreGrenserOppdatererEksisterende() {
        val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
        val gyldighetsdato = LocalDate.of(2022, Month.DECEMBER, 8)

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(Coordinate(1.0, 1.0), Coordinate(2.0, 2.0)))

        val nyGrenseGeometri = GeometryFactory().createLineString(arrayOf(Coordinate(2.0, 2.0), Coordinate(3.0, 3.0)))
        val nyGrenseGeometri2 = GeometryFactory().createLineString(arrayOf(Coordinate(3.0, 3.0), Coordinate(4.0, 4.0)))
        val nyGrenseGeometri3 = GeometryFactory().createLineString(arrayOf(Coordinate(4.0, 4.0), Coordinate(5.0, 5.0)))

        val grenseId1 = "11"
        val grenseId2 = "12"
        val grenseId3 = "13"
        val grenseId4 = "14"
        val lokalid1 = Lokalid("1")
        val lokalid2 = Lokalid("2")
        val lokalid3 = Lokalid("3")
        val lokalid4 = Lokalid("4")
        val grenseIdentifikator1 = Identifikator(lokalid1, gyldighetsdato)
        val grenseIdentifikator2 = Identifikator(lokalid2, gyldighetsdato)
        val grenseIdentifikator3 = Identifikator(lokalid3, gyldighetsdato)
        val grenseIdentifikator4 = Identifikator(lokalid4, gyldighetsdato)
        val identifikatorer = listOf(grenseIdentifikator1, grenseIdentifikator2, grenseIdentifikator3, grenseIdentifikator4)

        val eksisterendeGrense: Grense = GrenseBuilder(id = grenseId1, lokalId = lokalid1, gyldigFra = october1In2022).buildFylkesgrense()
        val eksisterendeGrense2: Grense = GrenseBuilder(id = grenseId2, lokalId = lokalid2, gyldigFra = october1In2022).buildFylkesgrense()
        val eksisterendeGrense3: Grense = GrenseBuilder(id = grenseId3, lokalId = lokalid3, gyldigFra = october1In2022).buildFylkesgrense()
        val eksisterendeGrense4: Grense = GrenseBuilder(id = grenseId4, lokalId = lokalid4, gyldigFra = october1In2022).buildFylkesgrense()

        val stemmekretsLokalid = Lokalid("55")
        val stemmekretsIdentifikator = Identifikator(stemmekretsLokalid, gyldighetsdato)

        val grunnkretsLokalid = Lokalid("18")
        val grunnkretsIdentifikator = Identifikator(grunnkretsLokalid, gyldighetsdato)

        val oppdatertFylkesgrensefeatureForStemmekrets = FeatureBuilder(id = lokalid2.value)
            .withKontekstType(KontekstEnhetType.STEMMEKRETS)
            .withKontekstId(
                kontekstId = stemmekretsLokalid.value,
                gyldighetsdato = gyldighetsdato)
            .withGrensetype(Grensetype.Fylkesgrense)
            .withGeometry(nyGrenseGeometri)
            .build()

        val oppdatertGrensefeature = FeatureBuilder(id = lokalid1.value)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId(
                kontekstId = grunnkretsLokalid.value,
                gyldighetsdato = gyldighetsdato)
            .withGrensetype(Grensetype.Fylkesgrense)
            .withGeometry(oppdatertGeometri)
            .withRekkefoelge(0)
            .build()
        val oppdatertFylkesgrensefeatureForGrunnkrets = FeatureBuilder(id = lokalid3.value)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId(
                kontekstId = grunnkretsLokalid.value,
                gyldighetsdato = gyldighetsdato)
            .withGrensetype(Grensetype.Fylkesgrense)
            .withGeometry(nyGrenseGeometri2)
            .withRekkefoelge(1)
            .build()
        val oppdatertFylkesgrensefeature2ForGrunnkrets = FeatureBuilder(id = lokalid4.value)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId(
                kontekstId = grunnkretsLokalid.value,
                gyldighetsdato = gyldighetsdato)
            .withGrensetype(Grensetype.Fylkesgrense)
            .withGeometry(nyGrenseGeometri3)
            .withRekkefoelge(2)
            .withRetning(false)
            .build()

        val stemmekretsSomSkalOppdateres = StemmekretsBuilder(
            id = "997",
            lokalId = stemmekretsLokalid,
            gyldigFra = october1In2022)
            .withGrenser(eksisterendeGrense2)
            .build()
        val grunnkretsSomSkalOppdateres = GrunnkretsBuilder(
            id = "998",
            lokalId = grunnkretsLokalid,
            gyldigFra = october1In2022)
            .withGrenser(eksisterendeGrense, eksisterendeGrense3, eksisterendeGrense4)
            .build()

        every { grenseRepositoryKotlinMock.findAllGyldigeObjekterForIdentifikatorer(identifikatorer) } returns mapOf(
            grenseIdentifikator1 to eksisterendeGrense,
            grenseIdentifikator2 to eksisterendeGrense2,
            grenseIdentifikator3 to eksisterendeGrense3,
            grenseIdentifikator4 to eksisterendeGrense4
        )

        every { stemmekretsRepositoryKotlinMock.findGyldigObjektForIdentifikator(stemmekretsIdentifikator) } returns stemmekretsSomSkalOppdateres
        every { grunnkretsRepositoryKotlinMock.findGyldigObjektForIdentifikator(grunnkretsIdentifikator) } returns grunnkretsSomSkalOppdateres
        every { grenseRepositoryKotlinMock.findGyldigObjektForIdentifikator(grenseIdentifikator1) } returns eksisterendeGrense
        every { grenseRepositoryKotlinMock.findGyldigObjektForIdentifikator(grenseIdentifikator2) } returns eksisterendeGrense2
        every { grenseRepositoryKotlinMock.findGyldigObjektForIdentifikator(grenseIdentifikator3) } returns eksisterendeGrense3
        every { grenseRepositoryKotlinMock.findGyldigObjektForIdentifikator(grenseIdentifikator4) } returns eksisterendeGrense4
        every { grenseRepositoryKotlinMock.saveAllAndFlush(allAny<List<Grense>>()) } returns emptyList() // Vi bruker ikke retur fra saveAll i logikken

        grenseService.lagreGrenser(
            grenser = FeatureCollection(
                features = listOf(
                    oppdatertFylkesgrensefeatureForStemmekrets,
                    oppdatertGrensefeature,
                    oppdatertFylkesgrensefeatureForGrunnkrets,
                    oppdatertFylkesgrensefeature2ForGrunnkrets
                )),
            gyldighetsdato = gyldighetsdato
        )

        val listOfGrenseSlot = slot<List<Grense>>()
        verify(exactly = 1) { grenseRepositoryKotlinMock.saveAllAndFlush(capture(listOfGrenseSlot)) }
        val lagredeGrenser = listOfGrenseSlot.captured
        assertThat(lagredeGrenser)
            .hasSize(4)
            .withFailMessage("Skal lagre 4 grenser til databasen")

        assertThat(stemmekretsSomSkalOppdateres.getGrenserAvTyper(setOf(Grensetype.Fylkesgrense)))
            .hasSize(1)
            .withFailMessage("Stemmekrets skal ha en fylkesgrense")

        val fylkesgrenser = grunnkretsSomSkalOppdateres.getGrenserAvTyper(setOf(Grensetype.Fylkesgrense))
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
        val gyldighetsdato = LocalDate.of(2022, Month.DECEMBER, 8)

        // Har en grunnkretsgrense med lokalid 34
        val grense = GrenseBuilder(id = "3400",
            lokalId = Lokalid("34"))
            .buildGrunnkretsgrense()
        val grenseIdentifikator = Identifikator(Lokalid("34"), gyldighetsdato)

        every { grenseRepositoryKotlinMock.findAllGyldigeObjekterForIdentifikatorer(allAny()) } returns mapOf(grenseIdentifikator to grense)

        // Innkommende feature på ukjent grenseId
        val featureWithUnknownId = FeatureBuilder("33")
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .build()

        assertThatThrownBy { grenseService.lagreGrenser(FeatureCollection(features = listOf(featureWithUnknownId)), gyldighetsdato = gyldighetsdato) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Non-existing grenseidentifikatorer: [Identifikator(lokalid=Lokalid(value=33), gyldighetsdato=2022-12-08)])")
    }

    @Test
    fun hentGrenseReturnsNullInCaseOfNoMatch() {
        val grenseIdentifikator = Identifikator(Lokalid("1"))
        every { grenseRepositoryKotlinMock.findGyldigObjektForIdentifikator(grenseIdentifikator) } returns null

        assertThat(grenseService.hentGrense(grenseIdentifikator)).isNull()
    }

}
