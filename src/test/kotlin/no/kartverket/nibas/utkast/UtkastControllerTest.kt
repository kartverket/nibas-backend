package no.kartverket.nibas.utkast

import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.response.AuditInfoResponse
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.response.UtkastResponse
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastEndringstype
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.sporing.AuditInfo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.Month
import org.mockito.Mockito.`when` as whenever


@WebMvcTest(UtkastController::class)
internal class UtkastControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var utkastServiceMock: UtkastService

    @Test
    fun opprettUtkastGir201Created() {
        val id = "b469a516-9ea4-4162-b8b8-ab1e1f165f40"
        whenever(utkastServiceMock.opprettUtkast(any(OpprettUtkastRequest::class.java))).thenReturn(id)
        val opprettUtkastRequest = OpprettUtkastRequest(
            navn = "Mitt utkast",
            endringstype = UtkastEndringstype.KVALITETSHEVING.value
        )
        post(
            url = "/v1/utkast",
            requestObject = opprettUtkastRequest,
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = UtkastOpprettetResponse(id),
                resultMatchers = listOf(status().isCreated)
            )
        )
    }

    @Test
    fun hentUtkastGir200OK() {
        val navn = "Utkastet mitt"
        val syvendeJuli = LocalDate.of(2022, Month.JULY, 7)
        val dummy = "DUMMY"
        val foersteJanuar = LocalDate.of(2024, Month.JANUARY, 1)
        val kommuneRequests = mapOf("UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST)
        val utkast = UtkastBuilder.builder(navn)
            .withKommuneRequests(kommuneRequests)
            .withAuditInfo(AuditInfo(syvendeJuli, dummy))
            .withGyldigFra(foersteJanuar)
            .build()
        val operasjoner = utkast.operasjoner
        val utkastId = utkast.id
        val opprettetTidspunkt = utkast.opprettetDato
        whenever(utkastServiceMock.hentUtkast(anyString())).thenReturn(utkast)

        val expectedUtkastResponse = UtkastResponse(
            id = utkastId,
            navn = navn,
            endringstype = UtkastEndringstype.KVALITETSHEVING.value,
            status = UtkastStatus.OPPRETTET.value,
            auditInfoResponse = AuditInfoResponse(
                oppdateringsdato = syvendeJuli,
                endretAv = dummy
            ),
            opprettetDato = opprettetTidspunkt,
            gyldigFra = foersteJanuar,
            operasjoner = operasjoner
        )
        get("/v1/utkast/$utkastId", responseAsserts = MockResponseAsserts(
            expectedResponseObject = expectedUtkastResponse,
            resultMatchers = listOf(status().isOk)
        ))
    }

    @Test
    fun hentUtkastMedUkjentIdGir404NotFound() {
        whenever(utkastServiceMock.hentUtkast(anyString())).thenReturn(null)
        get("/v1/utkast/123", responseAsserts = MockResponseAsserts(
            expectedContentType = null,
            resultMatchers = listOf(status().isNotFound)
        ))
    }

    @Test
    fun oppdaterUtkastGir200OK() {
        val gyldigFra = LocalDate.of(2024, Month.JANUARY, 1)
        val utkastNavn = "Navn 345"
        val endretAv = "DUMMY"
        val oppdateringsdato = LocalDate.of(2022, Month.SEPTEMBER, 26)
        val utkastEtterOppdatering = UtkastBuilder.builder(utkastNavn)
            .withKommuneRequests(mapOf("1234" to KommuneRequestBuilder.DEFAULT_REQUEST))
            .withAuditInfo(AuditInfo(oppdateringsdato, endretAv))
            .withGyldigFra(gyldigFra)
            .build()
        val operasjoner = utkastEtterOppdatering.operasjoner
        val opprettetDato = utkastEtterOppdatering.opprettetDato
        val utkastId = utkastEtterOppdatering.id
        whenever(utkastServiceMock.oppdaterUtkast(anyString(), any(OppdaterUtkastRequest::class.java))).thenReturn(utkastEtterOppdatering)

        val requestBody = OppdaterUtkastRequest(
            navn = utkastNavn,
            endringstype = UtkastEndringstype.KVALITETSHEVING.value,
            gyldigFra = gyldigFra,
            operasjoner = operasjoner
        )

        val expectedUtkastResponse = UtkastResponse(
            id = utkastId,
            navn = utkastNavn,
            endringstype = UtkastEndringstype.KVALITETSHEVING.value,
            status = UtkastStatus.OPPRETTET.value,
            auditInfoResponse = AuditInfoResponse(
                oppdateringsdato = oppdateringsdato,
                endretAv = endretAv
            ),
            opprettetDato = opprettetDato,
            gyldigFra = gyldigFra,
            operasjoner = operasjoner
        )

        put(
            url = "/v1/utkast/$utkastId",
            requestObject = requestBody,
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = expectedUtkastResponse,
                resultMatchers = listOf(status().isOk)
            ))
    }

    @Test
    fun oppdaterUtkastMedUkjentIdGir404NotFound() {
        whenever(utkastServiceMock.hentUtkast(anyString())).thenReturn(null)
        put("/v1/utkast/123", UtkastRequestBuilder.DEFAULT_REQUEST, status().isNotFound)
    }
}
