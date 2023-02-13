package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.api.v1.response.ObjektIdentifikator
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(KommuneController::class)
class KommuneControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var kommuneService: KommuneService

    @MockBean
    private lateinit var grunnkretsService: GrunnkretsService

    @MockBean
    private lateinit var stemmekretsService: StemmekretsService

    @MockBean
    private lateinit var hrefProviderMock: HrefProvider

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun mockKodelisteService() {
        whenever(kodelisteServiceMock.getCodeValueByKommunenummerId(anyString())).thenReturn("0301")
        whenever(hrefProviderMock.getHref(any(Identifikator::class.java))).thenReturn(href)
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
        whenever(kommuneService.hentKommune(any(Identifikator::class.java))).thenReturn(KommuneBuilder.DEFAULT_KOMMUNE)
        get("/v1/kommuner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        whenever(kommuneService.hentKommune(any(Identifikator::class.java))).thenReturn(null)
        get("/v1/kommuner/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        whenever(
            kommuneService.hentGrenserForKommune(any(Identifikator::class.java))
        ).thenReturn(
            setOf(GrunnkretsGrenseKobling(grunnkrets = GrunnkretsBuilder().build(), grense = GrenseBuilder().buildKommunegrense(), GrenseKoblingAttributter()))
        )
        get("/v1/kommuner/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        whenever(kommuneService.hentGrenserForKommune(any(Identifikator::class.java))).thenReturn(emptySet())
        get("/v1/kommuner/1/grenser", status().isNotFound)
    }

    @Test
    fun hentGrunnkretserForKommuneGir200_OK() {
        val grunnkrets = GrunnkretsBuilder(
            lokalId = Lokalid("123"),
            grunnkretsnavn = "Voi",
            grunnkretsnummer = "8989")
            .build()
        whenever(kommuneService.hentKommunesGrunnkretser(any(Identifikator::class.java))).thenReturn(listOf(grunnkrets))
        whenever(grunnkretsService.hentAntallGyldigeVersjonerFramITidFor(any(Identifikator::class.java))).thenReturn(1)

        val november1In2022 = LocalDate.of(2022, Month.NOVEMBER, 1)
        get("/v1/kommuner/1/grunnkretser?gyldighetsdato=${formattedDateString(november1In2022)}",
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = listOf(
                    GrunnkretsRef(
                        id = ObjektIdentifikator(lokalid = Lokalid("123"), gyldighetsdato = november1In2022),
                        navn = "Voi",
                        grunnkretsnummer = "8989",
                        href = href,
                        antallFramtidigeVersjoner = 1
                    )),
                resultMatchers = listOf(status().isOk)
            ))
    }

    private fun formattedDateString(date: LocalDate): String? = DateTimeFormatter.ISO_DATE.format(date)

}
