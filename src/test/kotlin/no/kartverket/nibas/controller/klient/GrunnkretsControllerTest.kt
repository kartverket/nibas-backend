package no.kartverket.nibas.controller.klient

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.GyldighetResponse
import no.kartverket.nibas.api.v1.common.Kommunenummer
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.klient.response.GrunnkretsResponse
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.INFORMASJONTEKST
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.extensions.geometry.posisjonToFeature
import no.kartverket.nibas.kodelister.KodelisteType
import no.kartverket.nibas.kodelister.MissingKodelisteItemException
import no.kartverket.nibas.service.GrunnkretsService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@WebMvcTest(GrunnkretsController::class)
internal class GrunnkretsControllerTest : NibasMvcTest() {

    @MockkBean
    private lateinit var grunnkretsService: GrunnkretsService

    @Test
    fun hentGrunnkretsReturns200_OK() {
        every { grunnkretsService.hentGrunnkrets(any()) } returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS
        get("/v1/grunnkretser/1", status().isOk)
    }

    @Test
    fun manglendeItemForKodelisteReturns500_InternalServerError() {
        val id: String = UUID.randomUUID().toString()
        every { grunnkretsService.hentGrunnkrets(any()) } throws MissingKodelisteItemException(KodelisteType.KOMMUNENUMMER, id)
        get("/v1/grunnkretser/$id", status().isInternalServerError)
    }

    @Test
    fun hentGrunnkretsMedGyldighetDatoGir200_OK() {
        every { grunnkretsService.hentGrunnkrets(any()) } returns GrunnkretsBuilder.DEFAULT_GRUNNKRETS
        get("/v1/grunnkretser/1?gyldigDato=2022-01-01", status().isOk)
    }

    @Test
    fun hentFramtidigeVersjonerReturns200_OK() {
        val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
        val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)
        val oppdateringstidspunkt = ZonedDateTime.of(LocalDateTime.of(2022, Month.DECEMBER, 14, 12, 7, 8, 0), ZoneId.systemDefault())

        val kommune = KommuneBuilder().withNavn("Bergen").withGyldigFra(jan1In2023).build()

        val lokalId = Lokalid("123")
        val grunnkrets1 = GrunnkretsBuilder(
            id = "1",
            lokalId = lokalId,
            gyldigFra = jan1In2023,
            gyldigTil = jan1In2024,
            kommune = kommune,
            endretdato = oppdateringstidspunkt,
            datafangstdato = oppdateringstidspunkt
        ).build()

        val grunnkrets2 = GrunnkretsBuilder(
            id = "2",
            lokalId = lokalId,
            gyldigFra = jan1In2024,
            kommune = kommune,
            endretdato = oppdateringstidspunkt,
            datafangstdato = oppdateringstidspunkt
        ).build()

        val expectedResponseList = listOf(
            GrunnkretsResponse(
                id = ObjektIdentifikator(lokalId, gyldighetsdato = jan1In2023),
                navn = "Dåsvatn",
                nummer = "12",
                gyldighet = GyldighetResponse(
                    gyldigFra = jan1In2023,
                    gyldigTil = jan1In2024),
                oppdateringsdato = oppdateringstidspunkt,
                endringstype = "Kvalitetsheving",
                kommunenummer = Kommunenummer(id = "UUID-1", kodeverdi = "0301"),
                datafangstdato = oppdateringstidspunkt,
                version = 1,
                representasjonspunkt = grunnkrets2.posisjon.posisjonToFeature(1),
                kommuneIdentifikator = ObjektIdentifikator(Lokalid(kommune.lokalid), kommune.gyldighet.gyldigFra),
                informasjon = INFORMASJONTEKST
            ),
            GrunnkretsResponse(
                id = ObjektIdentifikator(lokalId, gyldighetsdato = jan1In2024),
                navn = "Dåsvatn",
                nummer = "12",
                gyldighet = GyldighetResponse(
                    gyldigFra = jan1In2024,
                    gyldigTil = null),
                oppdateringsdato = oppdateringstidspunkt,
                endringstype = "Kvalitetsheving",
                kommunenummer = Kommunenummer(id = "UUID-1", kodeverdi = "0301"),
                representasjonspunkt = grunnkrets2.posisjon.posisjonToFeature(1),
                datafangstdato = oppdateringstidspunkt,
                version = 1,
                kommuneIdentifikator = ObjektIdentifikator(Lokalid(kommune.lokalid), kommune.gyldighet.gyldigFra),
                informasjon = INFORMASJONTEKST
            )
        )

        every { grunnkretsService.hentFramtidigeVersjonerForGrunnkrets(any())} returns listOf(grunnkrets1, grunnkrets2)
        // Vi GET-er etter framtidige versjoner etter 14.12.22 for lokalid 123
        // Responsen skal da returnere id-ene (123, 1.1.23) og (123, 1.1.24)
        get(
            url = "/v1/grunnkretser/123/framtidigeversjoner?gyldighetsdato=2022-12-14",
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = expectedResponseList,
                resultMatchers = listOf(status().isOk)
            ))
    }
}
