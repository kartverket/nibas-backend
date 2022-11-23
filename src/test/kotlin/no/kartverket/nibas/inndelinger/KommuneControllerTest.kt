package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(KommuneController::class)
class KommuneControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var kommuneService: KommuneService

    @MockBean
    private lateinit var grunnkretsService: GrunnkretsService

    @MockBean
    private lateinit var hrefProviderMock: HrefProvider

    @BeforeEach
    fun mockKodelisteService() {
        whenever(kodelisteServiceMock.getCodeValueByKommunenummerId(anyString())).thenReturn("0301")
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        val kommune = KommuneBuilder("1")
            .withNavn("Oslo")
            .build()

        whenever(kommuneService.hentAlleKommuner()).thenReturn(listOf(kommune))
        get("/v1/kommuner", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        whenever(kommuneService.hentKommune(anyString())).thenReturn(KommuneBuilder.DEFAULT_KOMMUNE)
        get("/v1/kommuner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(kommuneService.hentKommune(anyString())).thenReturn(null)
        get("/v1/kommuner/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        whenever(
            kommuneService.hentGrenserForKommune(anyString())
        ).thenReturn(
            setOf(GrunnkretsGrenseKobling(grunnkrets = GrunnkretsBuilder().build(), grense = GrenseBuilder().buildKommunegrense(), GrenseKoblingAttributter()))
        )
        get("/v1/kommuner/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        whenever(kommuneService.hentGrenserForKommune(anyString())).thenReturn(emptySet())
        get("/v1/kommuner/1/grenser", status().isNotFound)
    }

    @Test
    fun henGrunnkretserForKommuneGir200_OK() {
        val grunnkrets = GrunnkretsBuilder(
            lokalId = Lokalid("123"),
            grunnkretsnavn = "Voi",
            grunnkretsnummer = "8989")
            .build()
        val href = "http://finPathTilGrunnkretsHer"
        whenever(kommuneService.hentKommunesGrunnkretser(anyString())).thenReturn(listOf(grunnkrets))
        whenever(grunnkretsService.hentAntallGyldigeVersjonerFramITidForLokalid(any(Lokalid::class.java), any(LocalDate::class.java))).thenReturn(1)
        whenever(hrefProviderMock.getHref(anyString())).thenReturn(href)

        get("/v1/kommuner/1/grunnkretser",
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = listOf(
                    GrunnkretsRef(
                        id = "123",
                        navn = "Voi",
                        grunnkretsnummer = "8989",
                        href = href,
                        antallFramtidigeVersjoner = 1
                    )),
                resultMatchers = listOf(status().isOk)
            ))
    }

}
