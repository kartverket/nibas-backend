package no.kartverket.nibas.controller.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.response.KretsResponse
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KretsResponseBuilder
import no.kartverket.nibas.config.WebSecurityConfig
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.utils.IdType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

const val EKSTERN_GRUNNKRETSER_ENDPOINT = "/v1/ekstern/grunnkretser"

@ImportAutoConfiguration(WebSecurityConfig::class)
@WebMvcTest(GrunnkretsController::class)
internal class GrunnkretsControllerTest : NibasMvcTest() {

    @MockkBean
    private lateinit var grunnkretsService: GrunnkretsService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `json-struktur for grunnkretser med gitte id-er validerer`() {
        val tangen = GrunnkretsBuilder(grunnkretsnavn = "Tangen").withLokalId("3efb5575-22e3-45da-829e-707a4d01b919").build()
        val myraVest = GrunnkretsBuilder(grunnkretsnavn = "Myra vest").withLokalId("d0dee964-2b38-4311-aa92-5e0aeeab3440").build()

        every { grunnkretsService.hentGrunnkretserForIds(any(), any()) } returns listOf(tangen, myraVest)

        val faktiskRespons = get("$EKSTERN_GRUNNKRETSER_ENDPOINT?ids=123")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentGrunnkretserMedGitteIder.json")
    }

    @Test
    fun `json-struktur for grunnkretsgrenser validerer`() {
        val fellesGrenseMellomTangenOgSelaas = GrenseBuilder()
            .withLokalId("d6398e5e-c63c-3772-873a-8f530400e2f9")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .withGrenseGeometri(listOf(Coordinate(10.0, 0.0), Coordinate(5.0, 0.0), Coordinate(0.0, 0.0)))
            .build()

        val fellesGrenserMellomTangenOgFlaten = listOf(
            GrenseBuilder()
                .withLokalId("d0bb6894-b90e-346f-b91d-a90858eafc03")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(Coordinate(0.0, 0.0), Coordinate(0.0, 5.0), Coordinate(0.0, 10.0)))
                .build(),
            GrenseBuilder()
                .withLokalId("fe14f774-e792-3991-8bb1-3f21fc8e4ab3")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(Coordinate(0.0, 10.0), Coordinate(5.0, 10.0), Coordinate(10.0, 10.0)))
                .build(),
            GrenseBuilder()
                .withLokalId("c82748ce-c53e-3020-bd27-e719244aa960")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(Coordinate(10.0, 10.0), Coordinate(10.0, 5.0), Coordinate(10.0, 0.0)))
                .build())

        //Trenger forskjellige typer grenser ettersom de har forskjellige metadata knyttet til seg
        val grenserForGrunnkretsTangen = fellesGrenserMellomTangenOgFlaten + fellesGrenseMellomTangenOgSelaas

        val tangenGrunnkrets = GrunnkretsBuilder(grunnkretsnavn = "Tangen")
            .withLokalId("3efb5575-22e3-45da-829e-707a4d01b919")
            .withGrenser(grenserForGrunnkretsTangen)
            .build()

        val grenserForGrunnkretsSelaas = listOf(
            GrenseBuilder()
                .withLokalId("NOT_IMPORTANT")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(
                    Coordinate(0.0, 0.0), Coordinate(0.0, -10.0), Coordinate(10.0, -10.0), Coordinate(10.0, 0.0)
                ))
                .build(),
            fellesGrenseMellomTangenOgSelaas
        )

        val grenseForKunGrunnkretsFlaten =
            GrenseBuilder()
                .withLokalId("NOT_IMPORTANT")
                .withGrensetype(Grensetype.Kommunegrense)
                .withGrenseGeometri(listOf(
                    Coordinate(0.0, 0.0), Coordinate(-5.0, 0.0), Coordinate(-5.0, 15.0),
                    Coordinate(15.0, 15.0), Coordinate(15.0, 0.0), Coordinate(10.0, 0.0)
                ))
                .build()

        //Ved å lage de andre grunnkretsene får alle grenser koblinger til begge grunnkretsene og dermed blir kontektstegenskapene satt korrekt iht forventet respons.
        GrunnkretsBuilder(grunnkretsnavn = "Selås")
            .withLokalId("33fdcd55-13a8-4cf6-a44b-1dd334a1af50")
            .withGrenser(grenserForGrunnkretsSelaas)
            .build()

        GrunnkretsBuilder(grunnkretsnavn = "Flaten")
            .withLokalId("972bacdb-9c42-441f-aa1d-ca6b99b2d0c4")
            .withGrenser(fellesGrenserMellomTangenOgFlaten + grenseForKunGrunnkretsFlaten)
            .build()

        every { grunnkretsService.hentGrensekoblingerForGrunnkrets(any()) } returns tangenGrunnkrets.grenser.toList()

        val faktiskRespons = get("$EKSTERN_GRUNNKRETSER_ENDPOINT/123/grenser")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentGrunnkretsMedGrenser.json", IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun hentGrunnkretserForEnIdGir200_OK() {
        val grunnkrets = grunnkrets("1")
        val grunnkretsResponse = grunnkretsKretsResponse(grunnkrets)
        val grunnkretsResponseList = listOf(grunnkretsResponse)
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        every { grunnkretsService.hentGrunnkretserForIds(any(), any()) } returns listOf(grunnkrets)

        val responseAsString = invokeAndExpectStatusOK("$EKSTERN_GRUNNKRETSER_ENDPOINT?ids=1")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

    /**
     * Sender inn b7,a5 og forventer sortert respons a5,b7
     */
    @Test
    fun hentGrunnkretserForToIdsGir200_OK() {
        val grunnkrets1 = grunnkrets("a5")
        val grunnkrets2 = grunnkrets("b7")
        val grunnkretsResponse1 = grunnkretsKretsResponse(grunnkrets1)
        val grunnkretsResponse2 = grunnkretsKretsResponse(grunnkrets2)
        val grunnkretsResponseList = listOf(grunnkretsResponse1, grunnkretsResponse2)
        val expectedResponseAsJson = asJson(grunnkretsResponseList)

        every { grunnkretsService.hentGrunnkretserForIds(any(), any()) } returns listOf(grunnkrets1, grunnkrets2)

        val responseAsString = invokeAndExpectStatusOK("$EKSTERN_GRUNNKRETSER_ENDPOINT?ids=b7,a5&gyldighetsdato=2023-02-27")
        Assertions.assertThat(responseAsString).isEqualTo(expectedResponseAsJson)
    }

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

    private fun grunnkretsKretsResponse(grunnkrets: Grunnkrets): KretsResponse {
        return KretsResponseBuilder(grunnkrets = grunnkrets).buildForGrunnkrets()
    }

    private fun grunnkrets(id: String): Grunnkrets {
        return GrunnkretsBuilder(id).build()
    }

    private fun asJson(someObject: Any): String? = objectMapper.writeValueAsString(someObject)

}
