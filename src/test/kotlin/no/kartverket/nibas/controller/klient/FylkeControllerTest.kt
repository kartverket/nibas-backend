package no.kartverket.nibas.controller.klient

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.service.FylkeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(FylkeController::class)
class FylkeControllerTest : NibasMvcTest() {

    @MockkBean
    private lateinit var fylkeService: FylkeService

    @MockkBean
    private lateinit var hrefProviderMock: HrefProvider

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun beforeEach() {
        every { hrefProviderMock.getHref(any<Identifikator>()) } returns href
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        val fylke = FylkeBuilder()
            .withNavn("Vestland")
            .withFylkesnummer("1")
            .build()

        every { fylkeService.hentAlleFylker() } returns listOf(fylke)
        get("/v1/fylker", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        every { fylkeService.hentFylke(any()) } returns
            FylkeBuilder()
                .withNavn("Vestland")
                .withFylkesnummerUuid("UUID-123")
                .withFylkesnummer("1")
                .build()


        get("/v1/fylker/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        every { fylkeService.hentFylke(any()) } returns null
        get("/v1/fylker/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        every {
            fylkeService.hentGrensekoblingerForFylke(any())
         } returns setOf(
            GrunnkretsGrenseKobling(
                grunnkrets = GrunnkretsBuilder().build(),
                grense = GrenseBuilder().withGrensetype(Grensetype.Fylkesgrense).build(),
                grenseKoblingAttributter = GrenseKoblingAttributter())
            )

        get("/v1/fylker/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        every { fylkeService.hentGrensekoblingerForFylke(any()) } returns emptySet()
        get("/v1/fylker/1/grenser", status().isNotFound)
    }

}
