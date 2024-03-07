package no.kartverket.nibas.controller.klient

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.extensions.flate.toGrunnkretsResponse
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.StemmekretsService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@WebMvcTest(KommuneController::class)
class KommuneControllerTest : NibasMvcTest() {

    @MockkBean
    private lateinit var kommuneService: KommuneService

    @MockkBean
    private lateinit var grunnkretsService: GrunnkretsService

    @MockkBean
    private lateinit var stemmekretsService: StemmekretsService

    @MockkBean
    private lateinit var hrefProviderMock: HrefProvider

    private val href = "http://finPathTilEtObjektHer"

    @BeforeEach
    fun setupMocks() {
        every { hrefProviderMock.getHref(any<Identifikator>()) } returns href
        every { kommuneService.hentAntallGyldigeVersjonerFramITidFor(any())} returns 1
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        val kommune = KommuneBuilder("1")
            .withNavn("Oslo")
            .build()

        every { kommuneService.hentAlleKommuner() } returns listOf(kommune)
        get("/v1/kommuner", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        every { kommuneService.hentKommune(any()) } returns KommuneBuilder.DEFAULT_KOMMUNE
        get("/v1/kommuner/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        every { kommuneService.hentKommune(any()) } returns null
        get("/v1/kommuner/1", status().isNotFound)
    }

    @Test
    fun thatHentGrenserReturns200_Ok() {
        every {
            kommuneService.hentGrensekoblingerForKommune(any())
         } returns setOf(
            GrunnkretsGrenseKobling(
                grunnkrets = GrunnkretsBuilder().build(),
                grense = GrenseBuilder().withGrensetype(Grensetype.Kommunegrense).build()
            )
         )

        get("/v1/kommuner/1/grenser", status().isOk)
    }

    @Test
    fun thatHentGrenserReturns404_NotFound() {
        every { kommuneService.hentGrensekoblingerForKommune(any()) } returns emptySet()
        get("/v1/kommuner/1/grenser", status().isNotFound)
    }

    @Test
    fun hentGrunnkretserForKommuneGir200_OK() {
        val grunnkrets = GrunnkretsBuilder(
            lokalId = Lokalid("123"),
            grunnkretsnavn = "Voi",
            grunnkretsnummer = "8989")
            .build()
        every { kommuneService.hentKommunesGrunnkretser(any()) } returns listOf(grunnkrets)
        every { grunnkretsService.hentAntallGyldigeVersjonerFramITidFor(any()) } returns 1

        get("/v1/kommuner/1/grunnkretser?gyldighetsdato=${formattedDateString(LocalDate.now())}",
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = listOf(grunnkrets.toGrunnkretsResponse(Identifikator(Lokalid(grunnkrets.lokalid), LocalDate.now()))),
                resultMatchers = listOf(status().isOk)
            ))
    }

    @Test
    fun hentGrunnkretserForKommuneMedFremtidigeVersjonerGir200_OK() {
        val kommune = KommuneBuilder()
            .withGyldigFra(LocalDate.of(2022, 1,1))
            .build()

        val grunnkrets = GrunnkretsBuilder(
            lokalId = Lokalid("123"),
            grunnkretsnavn = "Voi",
            grunnkretsnummer = "8989",
            gyldigFra = LocalDate.of(2022, 1,1),
            gyldigTil = LocalDate.of(2024, 1, 1)
        ).withKommune(kommune).build()
        every { kommuneService.hentKommunesGrunnkretser(any()) } returns listOf(grunnkrets)
        every { grunnkretsService.hentAntallGyldigeVersjonerFramITidFor(any()) } returns 1

        get("/v1/kommuner/1/grunnkretser?gyldighetsdato=${formattedDateString(LocalDate.now())}",
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = listOf(grunnkrets.toGrunnkretsResponse(Identifikator(lokalid = Lokalid("123"), gyldighetsdato = LocalDate.now()))),
                resultMatchers = listOf(status().isOk)
            ))
    }

    private fun formattedDateString(date: LocalDate): String? = DateTimeFormatter.ISO_DATE.format(date)

}
