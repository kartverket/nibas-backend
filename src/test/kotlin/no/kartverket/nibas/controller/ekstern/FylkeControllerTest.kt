package no.kartverket.nibas.controller.ekstern

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.utils.IdType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
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
    fun `json-struktur for hent alle fylker validerer`() {
        setupToFylker()
        val faktiskRespons = get("/v1/ekstern/fylker")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentAlleFylker.json", IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for hent alle fylker oppdager endret felt fra string til array`() {
        setupToFylker()
        val faktiskRespons = get("/v1/ekstern/fylker")
        validerAtFaktiskResponsIkkeMatcherForventetRespons(
            faktiskRespons = faktiskRespons,
            forventetMelding = "Felter med samme navn (navn) har ikke samme nodetype, forventet: STRING, faktisk: ARRAY",
            filMedForventetRespons = "api-respons/hentAlleFylkerEndretFeltFraStringTilArray.json",
            idType = IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for hent alle fylker oppdager endret felt fra array til object`() {
        setupToFylker()
        val faktiskRespons = get("/v1/ekstern/fylker")
        validerAtFaktiskResponsIkkeMatcherForventetRespons(
            faktiskRespons = faktiskRespons,
            forventetMelding = "Felter med samme navn (fylkesnummer) har ikke samme nodetype, forventet: ARRAY, faktisk: OBJECT",
            filMedForventetRespons = "api-respons/hentAlleFylkerEndretFeltFraArrayTilObject.json",
            idType = IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for hent alle fylker oppdager endret felt fra object til array`() {
        setupToFylker()
        val faktiskRespons = get("/v1/ekstern/fylker")
        validerAtFaktiskResponsIkkeMatcherForventetRespons(
            faktiskRespons = faktiskRespons,
            forventetMelding = "Felter med samme navn (navn) har ikke samme nodetype, forventet: OBJECT, faktisk: ARRAY",
            filMedForventetRespons = "api-respons/hentAlleFylkerEndretFeltFraObjectTilArray.json",
            idType = IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for hent alle fylker oppdager endret feltnavn`() {
        setupToFylker()
        val faktiskRespons = get("/v1/ekstern/fylker")
        validerAtFaktiskResponsIkkeMatcherForventetRespons(
            faktiskRespons = faktiskRespons,
            forventetMelding = "Node med navn versjon ble ikke funnet i faktisk respons",
            filMedForventetRespons = "api-respons/hentAlleFylkerVersionEndretTilVersjon.json",
            idType = IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for hent alle fylker oppdager felter som er fjernet`() {
        setupToFylker()
        val faktiskRespons = get("/v1/ekstern/fylker")
        validerAtFaktiskResponsIkkeMatcherForventetRespons(
            faktiskRespons = faktiskRespons,
            forventetMelding = "Node med navn nyBeskrivelseSomErFjernetIFaktiskRespons ble ikke funnet i faktisk respons",
            filMedForventetRespons = "api-respons/hentAlleFylkerFjernFeltetNyBeskrivelse.json",
            idType = IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun `json-struktur for hent ett fylke validerer`() {
        val oslo = FylkeBuilder(unikLokalid = UnikLokalid(lokalid = "6fa6cac4-9f86-4cbf-9ae4-716eb5059541", Flatetype.FYLKE))
            .withNavn(navn = "Oslo", spraak = "nor")
            .build()
        every { fylkeService.hentFylke(any()) } returns oslo

        val faktiskRespons = get("/v1/ekstern/fylker/123")
        validerAtFaktiskResponsMatcherForventetRespons(faktiskRespons, "api-respons/hentEttFylke.json", IdType.ID_LOKALID_VALUE)
    }

    @Test
    fun thatHentAlleReturns200_OK() {
        val fylke = FylkeBuilder()
            .withNavn("Vestland")
            .build()

        every { fylkeService.hentAlleFylker() } returns listOf(fylke)
        get("/v1/ekstern/fylker", status().isOk)
    }

    @Test
    fun thatHentEnReturns200_OK() {
        every { fylkeService.hentFylke(any()) } returns (
            FylkeBuilder()
                .withNavn("Vestland")
                .withFylkesnummerUuid("UUID-123")
                .build()
        )

        get("/v1/ekstern/fylker/1", status().isOk)
    }

    @Test
    fun thatHentEnReturns404_NotFound() {
        every { fylkeService.hentFylke(any()) } returns null
        get("/v1/ekstern/fylker/1", status().isNotFound)
    }

    private fun setupToFylker() {
        val oslo = FylkeBuilder(unikLokalid = UnikLokalid(lokalid = "6fa6cac4-9f86-4cbf-9ae4-716eb5059541", Flatetype.FYLKE))
            .withNavn(navn = "Oslo", spraak = "nor")
            .build()
        val rogaland = FylkeBuilder(unikLokalid = UnikLokalid(lokalid = "e70c69ba-5bea-48a8-952c-b4860ada5afa", Flatetype.FYLKE))
            .withNavn(navn = "Rogaland", spraak = "nor")
            .build()

        every { fylkeService.hentAlleFylker() } returns listOf(oslo, rogaland)
    }
}
