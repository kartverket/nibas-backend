package no.kartverket.nibas.utkast

import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.response.ApiErrorResponse
import no.kartverket.nibas.api.v1.response.AuditInfoResponse
import no.kartverket.nibas.api.v1.response.ConflictResponse
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.response.UtkastRef
import no.kartverket.nibas.api.v1.response.UtkastResponse
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastEndringstype
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.repository.KonfliktVersjon
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.sosi.sosiimport.GeometriFailedToBeCreatedException
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import no.kartverket.nibas.sosi.sosiimport.UgyldigGeometriException
import no.kartverket.nibas.sporing.AuditInfo
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.operation.valid.TopologyValidationError
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.Month
import org.mockito.Mockito.`when` as whenever


@WebMvcTest(UtkastController::class)
internal class UtkastControllerTest : NibasMvcTest() {

    private val publiserUrl = "/v1/utkast/123/publiser"

    @MockBean
    private lateinit var utkastServiceMock: UtkastService

    @MockBean
    private lateinit var hrefProviderMock: HrefProvider

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
            operasjoner = operasjoner!!,
            version = 1
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

        val requestBody = UtkastRequestBuilder()
            .withNavn(utkastNavn)
            .withEndringstype(UtkastEndringstype.KVALITETSHEVING)
            .withGyldigFra(gyldigFra)
            .withOperasjoner(operasjoner!!)
            .build()

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
            operasjoner = operasjoner,
            version = 1
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
                expectedResponseObject = ApiErrorResponse(HttpStatus.NOT_FOUND, errorMsg),
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
                expectedResponseObject = ApiErrorResponse(HttpStatus.BAD_REQUEST, msg),
                resultMatchers = listOf(status().isBadRequest)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun publiserUtkastOppdateringAvFylkeGir409Conflict() {
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
                expectedResponseObject = listOf(
                    ConflictResponse(
                        id = fylkeId,
                        type = "Fylke",
                        gjeldendeVersjon = 4,
                        versjon = 3,
                        melding = "Det er ikke samsvar med versjon. Ønsket versjon = 3, eksisterende versjon = 4 for objekt med id = $fylkeId"
                    )
                ),
                resultMatchers = listOf(status().isConflict)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun publiserUtkastMedUgyldigGeometriGirHTTP400() {
        whenever(utkastServiceMock.publiserUtkast(anyString())).thenThrow(
            UgyldigGeometriException(
                geometry = GeometryFactory().createPolygon(),
                topologyValidationError = TopologyValidationError(6, Coordinate(0.0, 1.0))))

        val expectedHttpStatus = status().isBadRequest
        val geometryTypeMatcher = MockMvcResultMatchers.jsonPath("$.geometryType").value("Polygon")
        val validationErrorMatcher = MockMvcResultMatchers.jsonPath("$.validationError").value("Ring Self-intersection")
        val feilLokasjonMatcherType = MockMvcResultMatchers.jsonPath("$.feilLokasjon.type").value("Point")
        val feilLokasjonMatcherCoordinatesX = MockMvcResultMatchers.jsonPath("$.feilLokasjon.coordinates[0]").value("0.0")
        val feilLokasjonMatcherCoordinatesY = MockMvcResultMatchers.jsonPath("$.feilLokasjon.coordinates[1]").value("1.0")

        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                resultMatchers = listOf(
                    expectedHttpStatus, geometryTypeMatcher, validationErrorMatcher, feilLokasjonMatcherType,
                    feilLokasjonMatcherCoordinatesX, feilLokasjonMatcherCoordinatesY))
        )

        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun publiserUtkastMedIkkeSluttetRingGirHTTP400() {
        whenever(utkastServiceMock.publiserUtkast(anyString())).thenThrow(
            GeometriFailedToBeCreatedException(point = SosiGeometryFunctions.coordinateToPoint(Coordinate(0.0, 1.0))))

        val expectedHttpStatus = status().isBadRequest
        val geometryTypeMatcher = MockMvcResultMatchers.jsonPath("$.geometryType").value(null)
        val validationErrorMatcher = MockMvcResultMatchers.jsonPath("$.validationError").value(null)
        val feilLokasjonMatcherType = MockMvcResultMatchers.jsonPath("$.feilLokasjon.type").value("Point")
        val feilLokasjonMatcherCoordinatesX = MockMvcResultMatchers.jsonPath("$.feilLokasjon.coordinates[0]").value("0.0")
        val feilLokasjonMatcherCoordinatesY = MockMvcResultMatchers.jsonPath("$.feilLokasjon.coordinates[1]").value("1.0")

        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                resultMatchers = listOf(expectedHttpStatus, geometryTypeMatcher, validationErrorMatcher, feilLokasjonMatcherType,
                    feilLokasjonMatcherCoordinatesX, feilLokasjonMatcherCoordinatesY)))

        verify(utkastServiceMock).publiserUtkast(anyString())
    }

    @Test
    fun hentAktiveUtkastGir200OK() {
        val kommuneRequests = mapOf("UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST)
        val navn2 = "Utkast 2"
        val utkast2 = UtkastBuilder.builder(navn2)
            .withStatus(UtkastStatus.OPPRETTET)
            .withKommuneRequests(kommuneRequests)
            .build()
        val navn = "Utkast 1"
        val utkast = UtkastBuilder.builder(navn)
            .withStatus(UtkastStatus.OPPRETTET)
            .withKommuneRequests(kommuneRequests)
            .build()
        whenever(utkastServiceMock.hentUtkastIStatusOpprettet()).thenReturn(listOf(utkast2, utkast))
        val href = "http://localhost:8080/v1/utkast/UUID-TIL_UTKAST"
        whenever(hrefProviderMock.getHref(anyString())).thenReturn(href)

        val utkastRefs = listOf(
            UtkastRef(
                id = utkast.id,
                navn = navn,
                href = href
            ),
            UtkastRef(
                id = utkast2.id,
                navn = navn2,
                href = href
            )
        )
        get(
            url = "/v1/utkast",
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = utkastRefs,
                resultMatchers = listOf(status().isOk)
            )
        )
    }

    @Test
    fun forkastEksisterendeUtkastGir200OK() {
        val kommuneRequests = mapOf("UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST)
        val navn = "Utkast 1"
        val forkastetUtkast = UtkastBuilder.builder(navn)
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
}
