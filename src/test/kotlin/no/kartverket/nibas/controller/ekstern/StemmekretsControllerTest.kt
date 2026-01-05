package no.kartverket.nibas.controller.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.ekstern.response.EksternKretsResponse
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.KretsResponseBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.config.WebSecurityConfig
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.utils.IdType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

const val EKSTERN_STEMMEKRETSER_ENDPOINT = "/v1/ekstern/stemmekretser"

@ImportAutoConfiguration(WebSecurityConfig::class)
@WebMvcTest(StemmekretsController::class)
internal class StemmekretsControllerTest : NibasMvcTest() {

    @MockkBean
    private lateinit var stemmekretsService: StemmekretsService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `json-struktur for stemmekretser med gitte id-er validerer`() {
        val tangen = StemmekretsBuilder(stemmekretsnavn = "Tangen").withLokalId("dadd20dc-170c-4c9a-9507-dc571c4d0a3b").build()
        val myra = StemmekretsBuilder(stemmekretsnavn = "Myra").withLokalId("cc680e25-3bd5-4427-bbae-fb47f3d8ce88").build()

        every { stemmekretsService.hentStemmekretserForIds(any(), any()) }  returns listOf(tangen, myra)

        val faktiskRespons = get("$EKSTERN_STEMMEKRETSER_ENDPOINT?ids=123")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentStemmekretserMedGitteIder.json")
    }

    @Test
    fun `json-struktur for stemmekrets med gitt id validerer`() {
        val tangen = StemmekretsBuilder(stemmekretsnavn = "Tangen").withLokalId("dadd20dc-170c-4c9a-9507-dc571c4d0a3b").build()

        every { stemmekretsService.hentStemmekrets(any()) } returns tangen

        val faktiskRespons = get("$EKSTERN_STEMMEKRETSER_ENDPOINT/123")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentStemmekretsMedGittId.json", IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for stemmekretsgrenser validerer`() {
        val fellesGrenseMellomTangenOgMyra = GrenseBuilder()
            .withLokalId("0310d8ec-e4a7-34fb-8e3f-d9329d4e2675")
            .withGrensetype(Grensetype.Stemmekretsgrense)
            .withGrenseGeometri(listOf(Coordinate(10.0, 0.0), Coordinate(5.0, 0.0), Coordinate(0.0, 0.0)))
            .build()

        //Trenger forskjellige typer grenser ettersom de har forskjellige metadata knyttet til seg
        val fellesGrenserMellomTangenOgNelaug = listOf(
            GrenseBuilder()
                .withLokalId("4991cf31-3dd4-346d-ad0d-ca6981ceb90e")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(Coordinate(0.0, 0.0), Coordinate(0.0, 5.0), Coordinate(0.0, 10.0)))
                .build(),
            GrenseBuilder()
                .withLokalId("bedb41be-62ce-38d6-adc8-792ee9d62847")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(Coordinate(0.0, 10.0), Coordinate(5.0, 10.0), Coordinate(10.0, 10.0)))
                .build(),
            GrenseBuilder()
                .withLokalId("93558ebb-fef4-35e9-b408-1a9d69342690")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(Coordinate(10.0, 10.0), Coordinate(10.0, 5.0), Coordinate(10.0, 0.0)))
                .build()
        )

        val grenserForStemmekretsTangen = fellesGrenserMellomTangenOgNelaug + fellesGrenseMellomTangenOgMyra

        val tangenStemmekrets = StemmekretsBuilder(stemmekretsnavn = "Tangen")
            .withLokalId("dadd20dc-170c-4c9a-9507-dc571c4d0a3b")
            .withGrenser(grenserForStemmekretsTangen)
            .build()

        val grenserForStemmekretsMyra = listOf(
            GrenseBuilder()
                .withLokalId("NOT_IMPORTANT")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(
                    Coordinate(0.0, 0.0), Coordinate(0.0, -10.0), Coordinate(10.0, -10.0), Coordinate(10.0, 0.0)
                ))
                .build(),
            fellesGrenseMellomTangenOgMyra
        )

        val grenseForKunStemmekretsNelaug = listOf(
            GrenseBuilder()
                .withLokalId("NOT_IMPORTANT")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(
                    Coordinate(0.0, 0.0), Coordinate(-5.0, 0.0), Coordinate(-5.0, 15.0),
                    Coordinate(15.0, 15.0), Coordinate(15.0, 0.0), Coordinate(10.0, 0.0)
                ))
                .build(),
        )

        //Ved å lage de andre stemmekretsene får alle grenser koblinger til begge stemmekretsene og dermed blir kontektstegenskapene satt korrekt iht forventet respons.
        StemmekretsBuilder(stemmekretsnavn = "Myra")
            .withLokalId("cc680e25-3bd5-4427-bbae-fb47f3d8ce88")
            .withGrenser(grenserForStemmekretsMyra)
            .build()

        StemmekretsBuilder(stemmekretsnavn = "Nelalug")
            .withLokalId("862c3a69-986c-435a-8cda-2f0732b786f2")
            .withGrenser(fellesGrenserMellomTangenOgNelaug + grenseForKunStemmekretsNelaug)
            .build()

        every { stemmekretsService.hentGrensekoblingerForStemmekrets(any()) } returns tangenStemmekrets.grenser.toList()

        val faktiskRespons = get("$EKSTERN_STEMMEKRETSER_ENDPOINT/123/grenser")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentStemmekretsMedGrenser.json", IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun hentStemmekretsReturns200_OK() {
        every { stemmekretsService.hentStemmekrets(any()) } returns StemmekretsBuilder.DEFAULT_STEMMEKRETS
        get("$EKSTERN_STEMMEKRETSER_ENDPOINT/1", status().isOk)
    }

    @Test
    fun hentStemmekretserForEnIdGir200_OK() {
        val stemmekrets = stemmekrets("1")
        val stemmekretsResponse = stemmekretsKretsResponse(stemmekrets)
        val stemmekretsResponseList = listOf(stemmekretsResponse)
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        every { stemmekretsService.hentStemmekretserForIds(any(), any()) } returns listOf(stemmekrets)

        val responseAsString = invokeAndExpectStatusOK("$EKSTERN_STEMMEKRETSER_ENDPOINT?ids=1")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    /** Sender inn id 2,1 og forventer sortert respons 1,2 */
    @Test
    fun hentStemmekretserForToIdsGir200_OK() {
        val stemmekrets1 = stemmekrets("1")
        val stemmekrets2 = stemmekrets("2")
        val stemmekretsResponse1 = stemmekretsKretsResponse(stemmekrets1)
        val stemmekretsResponse2 = stemmekretsKretsResponse(stemmekrets2)
        val stemmekretsResponseList = listOf(stemmekretsResponse1, stemmekretsResponse2)
        val expectedResponseAsJson = asJson(stemmekretsResponseList)

        every { stemmekretsService.hentStemmekretserForIds(any(), any()) } returns listOf(stemmekrets1, stemmekrets2)

        val responseAsString = invokeAndExpectStatusOK("$EKSTERN_STEMMEKRETSER_ENDPOINT?ids=2,1&gyldighetsdato=2023-02-27")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    private fun stemmekrets(id: String): Stemmekrets {
        return StemmekretsBuilder(id)
            .build()
    }

    private fun stemmekretsKretsResponse(stemmekrets: Stemmekrets): EksternKretsResponse {
        return KretsResponseBuilder(
            stemmekrets = stemmekrets)
            .buildForStemmekrets()
    }

    private fun asJson(someObject: Any): String? = objectMapper.writeValueAsString(someObject)

    private fun invokeAndExpectStatusOK(url: String): String {
        return mvc.perform(
            MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .response
            .getContentAsString(Charsets.UTF_8)
    }
}

