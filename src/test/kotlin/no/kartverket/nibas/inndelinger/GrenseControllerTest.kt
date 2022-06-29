package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.sosi.sosiimport.UgyldigGeometriException
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.operation.valid.TopologyValidationError
import org.mockito.Mockito.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever


@WebMvcTest(GrenseController::class)
class GrenseControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var grenseServiceMock: GrenseService

    @MockBean
    private lateinit var historikkService: HistorikkService

    @MockBean
    private lateinit var flateService: FlateService

    @MockBean
    private lateinit var kodelisteServiceMock: KodelisteService

    @MockBean
    private lateinit var grenseRepositoryMock: GrenseRepository

    @Test
    fun thatHentGrenseReturns200_OK() {
        whenever(grenseServiceMock.hentGrense("1"))
            .thenReturn(GrenseBuilder().buildKommunegrense())

        get("/v1/grenser/1", status().isOk)
    }

    @Test
    fun thatHentGrenseReturns404_NotFound() {
        whenever(grenseServiceMock.hentGrense("1"))
            .thenReturn(null)

        get("/v1/grenser/1", status().isNotFound)
    }

    @Test
    fun thatOppdaterGrenserUpdatesCorrectly() {
        val feature1 = kommunegrenseFeature("1")
        val feature2 = kommunegrenseFeature("2")
        val feature3 = fylkesgrenseFeature("3")
        val feature4 = fylkesgrenseFeature("4")
        val featureCollection =
            FeatureCollection(features = listOf(feature1, feature2, feature3, feature4))

        post("/v1/grenser/", featureCollection, status().isOk)

        verify(grenseServiceMock).lagreGrenser(featureCollection)
    }

    @Test
    fun thatLagreGrenserMedUgyldigGeometriGirHTTP400() {
        whenever(grenseServiceMock.lagreGrenser(any(FeatureCollection::class.java))).thenThrow(
            UgyldigGeometriException(
                geometry = GeometryFactory().createPolygon(),
                topologyValidationError = TopologyValidationError(6, Coordinate(0.0, 1.0))))

        val feature1 = kommunegrenseFeature("1")
        val feature2 = kommunegrenseFeature("2")
        val featureCollection =
            FeatureCollection(features = listOf(feature1, feature2))

        val expectedHttpStatus = status().isBadRequest
        val geometryTypeMatcher = jsonPath("$.geometryType").value("Polygon")
        val validationErrorMatcher = jsonPath("$.validationError").value("Ring Self-intersection")
        val feilLokasjonMatcherX = jsonPath("$.feilLokasjon.x").value("0.0")
        val feilLokasjonMatcherY = jsonPath("$.feilLokasjon.y").value("1.0")

        post("/v1/grenser/", featureCollection, expectedHttpStatus, geometryTypeMatcher, validationErrorMatcher, feilLokasjonMatcherX, feilLokasjonMatcherY)

        verify(grenseServiceMock).lagreGrenser(featureCollection)
    }

    private fun fylkesgrenseFeature(id: String) = FeatureBuilder(id).withGrensetype(Grensetype.Fylkesgrense).build()

    private fun kommunegrenseFeature(id: String) = FeatureBuilder(id).withGrensetype(Grensetype.Kommunegrense).build()

}
