package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.service.NasjonService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(NasjonController::class)
class NasjonControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var nasjonService: NasjonService

    @MockBean
    private lateinit var hrefProviderMock: HrefProvider

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun beforeEach() {
        whenever(hrefProviderMock.getHref(any(Identifikator::class.java))).thenReturn(href)
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        whenever(nasjonService.hentAlleNasjoner()).thenReturn(listOf(NasjonBuilder.DEFAULT_NASJON))
        get("/v1/nasjoner", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(nasjonService.hentNasjon(any(Identifikator::class.java))).thenReturn(NasjonBuilder.DEFAULT_NASJON)
        get("/v1/nasjoner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(nasjonService.hentNasjon(any(Identifikator::class.java))).thenReturn(null)
        get("/v1/nasjoner/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        whenever(
            nasjonService.hentGrenserForNasjon(any(Identifikator::class.java))
        ).thenReturn(
            setOf(GrunnkretsGrenseKobling(grunnkrets = GrunnkretsBuilder().build(),
                grense = GrenseBuilder().withGrensetype(Grensetype.Riksgrense).build(), GrenseKoblingAttributter()))
        )
        get("/v1/nasjoner/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        whenever(nasjonService.hentGrenserForNasjon(any(Identifikator::class.java))).thenReturn(emptySet())
        get("/v1/nasjoner/1/grenser", status().isNotFound)
    }

}
