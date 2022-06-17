package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.kodelister.KodelisteService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever


@WebMvcTest(GrenseController::class)
class GrenseControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var grenseService: GrenseService

    @MockBean
    private lateinit var historikkService: HistorikkService

    @MockBean
    private lateinit var flateService: FlateService

    @MockBean
    private lateinit var kodelisteServiceMock: KodelisteService

    @Test
    fun thatHentGrenseReturns200_OK() {
        whenever(grenseService.hentGrense("1"))
            .thenReturn(GrenseBuilder().buildKommunegrense())

        get("/v1/grenser/1", status().isOk)
    }

    @Test
    fun thatHentGrenseReturns404_NotFound() {
        whenever(grenseService.hentGrense("1"))
            .thenReturn(null)

        get("/v1/grenser/1", status().isNotFound)
    }

    @Test
    fun thatOppdaterGrenserUpdatesCorrectly() {
        val kommunegrense1 = FeatureBuilder("1").withGrensetype(Grensetype.Kommunegrense).build()
        val kommunegrense2 = FeatureBuilder("2").withGrensetype(Grensetype.Kommunegrense).build()
        val fylkesgrense1 = FeatureBuilder("1").withGrensetype(Grensetype.Fylkesgrense).build()
        val fylkesgrense2 = FeatureBuilder("2").withGrensetype(Grensetype.Fylkesgrense).build()

        val featureCollection =
            FeatureCollection(features = listOf(kommunegrense1, kommunegrense2, fylkesgrense1, fylkesgrense2))

        post("/v1/grenser/", featureCollection, status().isOk)
        verify(grenseService).lagreGrenser(listOf(kommunegrense1, kommunegrense2))
        verify(grenseService).lagreGrenser(listOf(fylkesgrense1, fylkesgrense2))
    }
}
