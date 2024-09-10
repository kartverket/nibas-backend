package no.kartverket.nibas.utkast

import io.mockk.every
import io.mockk.mockkStatic
import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.klient.response.ApiErrorResponse
import no.kartverket.nibas.api.v1.klient.response.AuditInfoResponse
import no.kartverket.nibas.api.v1.klient.response.ErrorDescription
import no.kartverket.nibas.api.v1.klient.response.OptimistiskLaasResponse
import no.kartverket.nibas.api.v1.klient.response.OptimistiskLaasWrapper
import no.kartverket.nibas.api.v1.klient.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.klient.response.UtkastResponse
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.common.geometry.UgyldigGeometriException
import no.kartverket.nibas.common.geometry.UgyldigeFlaterException
import no.kartverket.nibas.controller.klient.UtkastController
import no.kartverket.nibas.controller.klient.toUtkastResponse
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.repository.KonfliktVersjon
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.service.utkast.UtkastService
import no.kartverket.nibas.sporing.AuditInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.operation.valid.TopologyValidationError
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import org.mockito.Mockito.`when` as whenever


@WebMvcTest(UtkastController::class)
internal class UtkastControllerTest : NibasMvcTest() {

    private val publiserUrl = "/v1/utkast/123/publiser"
    private val mockUuid = UUID.randomUUID()

    @MockBean
    private lateinit var utkastServiceMock: UtkastService

    @MockBean
    private lateinit var hrefProviderMock: HrefProvider

    @MockBean
    private lateinit var currentTime: CurrentTime

    @BeforeEach
    fun setup() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns mockUuid
        whenever(currentTime.asLocalDate()).thenReturn(LocalDate.of(2023, 1, 1))
    }

    @Test
    fun opprettUtkastGir201Created() {
        val id = "b469a516-9ea4-4162-b8b8-ab1e1f165f40"
        whenever(utkastServiceMock.opprettUtkast(any(OpprettUtkastRequest::class.java))).thenReturn(id)
        val opprettUtkastRequest = OpprettUtkastRequest(
            navn = "Mitt utkast",
            endringstype = Endringstype.KVALITETSHEVING.value,
            gyldigFra = LocalDate.of(2021, 1, 1),
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

        val syvendeJuli = ZonedDateTime.of(LocalDateTime.of(2022, Month.JULY, 7, 12, 0), ZoneId.systemDefault())
        val dummy = "DUMMY"
        val kommuneRequests = mapOf("UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST)
        val utkast = UtkastBuilder.builder(navn, gyldigFra = syvendeJuli.toLocalDate())
            .withKommuneRequests(kommuneRequests)
            .withAuditInfo(AuditInfo(syvendeJuli, dummy, Endringstype.KVALITETSHEVING))
            .build()
        val operasjoner = utkast.operasjoner
        val utkastId = utkast.id
        val opprettetTidspunkt = utkast.opprettetDato
        whenever(utkastServiceMock.hentUtkast(anyString())).thenReturn(utkast)

        val expectedUtkastResponse = UtkastResponse(
            id = utkastId,
            navn = navn,
            endringstype = Endringstype.KVALITETSHEVING.value,
            status = UtkastStatus.OPPRETTET.value,
            auditInfoResponse = AuditInfoResponse(oppdateringsdato = syvendeJuli),
            opprettetDato = opprettetTidspunkt,
            operasjoner = operasjoner!!,
            version = 1,
            gyldigFra = LocalDate.of(2022, Month.JULY, 7),
            endredeInndelinger = emptyList()
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
            expectedContentType = MediaType.APPLICATION_JSON_VALUE,
            resultMatchers = listOf(status().isNotFound)
        ))
    }

    @Test
    fun oppdaterUtkastGir200OK() {
        val gyldigFra = LocalDate.of(2024, Month.JANUARY, 1)
        val utkastNavn = "Navn 345"
        val endretAv = "DUMMY"
        val oppdateringsdato = ZonedDateTime.of(LocalDateTime.of(2022, Month.SEPTEMBER, 26, 12, 0), ZoneId.systemDefault())
        val utkastEtterOppdatering = UtkastBuilder.builder(utkastNavn, gyldigFra = gyldigFra)
            .withKommuneRequests(mapOf("1234" to KommuneRequestBuilder.DEFAULT_REQUEST))
            .withAuditInfo(AuditInfo(oppdateringsdato, endretAv, Endringstype.KVALITETSHEVING))
            .build()
        val operasjoner = utkastEtterOppdatering.operasjoner
        val opprettetDato = utkastEtterOppdatering.opprettetDato
        val utkastId = utkastEtterOppdatering.id
        whenever(utkastServiceMock.oppdaterUtkast(anyString(), any(OppdaterUtkastRequest::class.java))).thenReturn(utkastEtterOppdatering)

        val requestBody = UtkastRequestBuilder()
            .withNavn(utkastNavn)
            .withEndringstype(Endringstype.KVALITETSHEVING)
            .withGyldigFra(gyldigFra)
            .withOperasjoner(operasjoner!!)
            .build()

        val expectedUtkastResponse = UtkastResponse(
            id = utkastId,
            navn = utkastNavn,
            endringstype = Endringstype.KVALITETSHEVING.value,
            status = UtkastStatus.OPPRETTET.value,
            auditInfoResponse = AuditInfoResponse(oppdateringsdato = oppdateringsdato),
            opprettetDato = opprettetDato,
            operasjoner = operasjoner,
            version = 1,
            gyldigFra = gyldigFra,
            endredeInndelinger = emptyList()
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
        whenever(utkastServiceMock.oppdaterUtkast(anyString(), any(OppdaterUtkastRequest::class.java))).thenReturn(null)
        put("/v1/utkast/123", UtkastRequestBuilder.DEFAULT_REQUEST, status().isNotFound)
    }

    @Test
    fun publiserUtkastGir200OK() {
        post(
            url = publiserUrl,
            expected = status().isOk
        )
        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun publiserUtkastIkkeFunnetGir404NotFound() {
        val errorMsg = "Utkast ble ikke funnet for id: 123"
        whenever(utkastServiceMock.publiserUtkast(anyString())).thenThrow(NotFoundException(errorMsg))
        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = ApiErrorResponse(errorCode = mockUuid.toString(), errorDescription = ErrorDescription(title = "404: Ikke funnet", description = errorMsg)),
                resultMatchers = listOf(status().isNotFound)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun publiserUtkastIFeilStatusGir400BadRequest() {
        val msg = "Kan ikke publisere et utkast i status FORKASTET."
        whenever(utkastServiceMock.publiserUtkast(anyString())).thenThrow(BadRequestException(msg))
        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = ApiErrorResponse(errorCode = mockUuid.toString(), errorDescription = ErrorDescription(title = "400: Ukjent feil", description = msg)),
                resultMatchers = listOf(status().isBadRequest)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun publiserUtkastOppdateringAvFylkeGir409Conflict_PgaOptimistiskLaasefeil() {
        val eksisterendeFylke = FylkeBuilder()
            .withVersion(4)
            .build()
        val fylkeId = eksisterendeFylke.id

        whenever(utkastServiceMock.publiserUtkast(anyString())).thenThrow(OptimistiskLaasException(KonfliktVersjon(
            nyVersjon = 3,
            eksisterende = eksisterendeFylke)))

        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = OptimistiskLaasWrapper(
                    optimisticLockExceptions = listOf(
                        OptimistiskLaasResponse(
                            id = fylkeId,
                            type = "Fylke",
                            gjeldendeVersjon = 4,
                            versjon = 3,
                            melding = "Det er ikke samsvar med versjon for fylke 89 Ukjent navn. Ønsket versjon = 3, eksisterende versjon = 4 for objekt med id = $fylkeId"
                        )
                    )),
                resultMatchers = listOf(status().isConflict)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun publiserUtkastMedUgyldigGeometriGirHTTP400() {
        whenever(utkastServiceMock.publiserUtkast(anyString())).thenThrow(
            UgyldigGeometriException(
                topologyValidationError = TopologyValidationError(6, Coordinate(0.0, 1.0))))

        val expectedHttpStatus = status().isBadRequest

        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                resultMatchers = listOf(expectedHttpStatus)
            )
        )

        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun publiserUtkastMedIkkeSluttetRingGirHTTP400() {
        whenever(utkastServiceMock.publiserUtkast(anyString())).thenThrow(UgyldigeFlaterException())

        val expectedHttpStatus = status().isBadRequest

        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                resultMatchers = listOf(expectedHttpStatus)
            )
        )

        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun hentAktiveUtkastGir200OK() {
        val idag = ZonedDateTime.now()
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns idag

        val kommuneRequests = mapOf("UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST)
        val navn2 = "Utkast 2"
        val utkast2 = UtkastBuilder.builder(navn2, gyldigFra = idag.toLocalDate())
            .withStatus(UtkastStatus.OPPRETTET)
            .withKommuneRequests(kommuneRequests)
            .build()
        val navn = "Utkast 1"
        val utkast = UtkastBuilder.builder(navn, gyldigFra = idag.toLocalDate())
            .withStatus(UtkastStatus.OPPRETTET)
            .withKommuneRequests(kommuneRequests)
            .build()

        whenever(utkastServiceMock.hentUtkast(utkastStatus = listOf(UtkastStatus.OPPRETTET), gyldigFra = idag.toLocalDate())).thenReturn(listOf(utkast, utkast2))

        val utkastResponses = listOf(utkast, utkast2).map { it.toUtkastResponse() }

        get(
            url = "/v1/utkast?utkastStatus=${UtkastStatus.OPPRETTET}&gyldigFra=${idag.toLocalDate()}",
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = utkastResponses,
                resultMatchers = listOf(status().isOk)
            )
        )
    }

    @Test
    fun forkastEksisterendeUtkastGir200OK() {
        val kommuneRequests = mapOf("UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST)
        val navn = "Utkast 1"
        val forkastetUtkast = UtkastBuilder.builder(navn, gyldigFra = LocalDate.now())
            .withStatus(UtkastStatus.FORKASTET)
            .withKommuneRequests(kommuneRequests)
            .build()
        whenever(utkastServiceMock.forkastUtkast(anyString())).thenReturn(forkastetUtkast)

        delete(
            url = "/v1/utkast/123",
            responseAsserts = MockResponseAsserts(
                resultMatchers = listOf(status().is2xxSuccessful)
            )
        )
    }

    @Test
    fun forkastUtkastSomIkkeFinnesGir404NotFound() {
        val ikkeGyldigId = "12341515"
        val href = "http://localhost:8080/v1/utkast/UUID-TIL_UTKAST"
        whenever(hrefProviderMock.getHref(anyString())).thenReturn(href)

        delete(
            url = "/v1/utkast/${ikkeGyldigId}",
            responseAsserts = MockResponseAsserts(
                resultMatchers = listOf(status().isNotFound)
            )
        )
    }

    @Test
    fun publiserUtkastOppdateringAvFylkeGir400BadRquest_PgaKonfliktMedFramtidigeVersjoner() {
        // Lokalid+gyldigFra som blir truffet av utkastet
        val lokalid = Lokalid("abcd-38383")
        val foersteJanuar2024 = LocalDate.of(2024, Month.JANUARY, 1)

        // Fantes en Grunnkrets fra før med: Gyldig fra 1.1.25
        val foersteJanuar2025 = LocalDate.of(2025, Month.JANUARY, 1)

        // Fantes en Grunnkrets fra før med: Gyldig fra 1.1.26
        val foersteJanuar2026 = LocalDate.of(2026, Month.JANUARY, 1)

        whenever(utkastServiceMock.publiserUtkast(anyString())).thenThrow(FeilVedOppdatering.OppdateringException(
            oppdateringInfo = FeilVedOppdatering.OppdateringInfo(
                id = Identifikator(lokalid, foersteJanuar2024),
                type = FeilVedOppdatering.Type.GRUNNKRETS,
                affectedIds = listOf(
                    Identifikator(lokalid, foersteJanuar2025),
                    Identifikator(lokalid, foersteJanuar2026)
                )
            )
        ))

        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                resultMatchers = listOf(status().isBadRequest)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString())
    }
}
