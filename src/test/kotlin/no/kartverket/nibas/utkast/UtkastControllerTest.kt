package no.kartverket.nibas.utkast

import no.kartverket.nibas.MockResponseAsserts
import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.api.v1.common.geojson.metadata.Identifikasjon
import no.kartverket.nibas.api.v1.request.ConflictResolved
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsRequestResolved
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.response.ApiErrorResponse
import no.kartverket.nibas.api.v1.response.AuditInfoResponse
import no.kartverket.nibas.api.v1.response.ConflictResponse
import no.kartverket.nibas.api.v1.response.ConflictResponseWrapper
import no.kartverket.nibas.api.v1.response.FramtidigVersjonConflict
import no.kartverket.nibas.api.v1.response.ObjektIdentifikator
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.response.UtkastRef
import no.kartverket.nibas.api.v1.response.UtkastResponse
import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.controller.klient.UtkastController
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.repository.KonfliktVersjon
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.service.utkast.UtkastService
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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
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
            endringstype = Endringstype.KVALITETSHEVING.value
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
        val foersteJanuar = LocalDate.of(2024, Month.JANUARY, 1)
        val kommuneRequests = mapOf("UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST)
        val utkast = UtkastBuilder.builder(navn)
            .withKommuneRequests(kommuneRequests)
            .withAuditInfo(AuditInfo(syvendeJuli, dummy, Endringstype.KVALITETSHEVING))
            .withGyldigFra(foersteJanuar)
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
            expectedContentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            resultMatchers = listOf(status().isNotFound)
        ))
    }

    @Test
    fun oppdaterUtkastGir200OK() {
        val gyldigFra = LocalDate.of(2024, Month.JANUARY, 1)
        val utkastNavn = "Navn 345"
        val endretAv = "DUMMY"
        val oppdateringsdato = ZonedDateTime.of(LocalDateTime.of(2022, Month.SEPTEMBER, 26, 12, 0), ZoneId.systemDefault())
        val utkastEtterOppdatering = UtkastBuilder.builder(utkastNavn)
            .withKommuneRequests(mapOf("1234" to KommuneRequestBuilder.DEFAULT_REQUEST))
            .withAuditInfo(AuditInfo(oppdateringsdato, endretAv, Endringstype.KVALITETSHEVING))
            .withGyldigFra(gyldigFra)
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
        verify(utkastServiceMock).publiserUtkast(anyString(), any(LocalDate::class.java))
    }

    @Test
    fun publiserUtkastIkkeFunnetGir404NotFound() {
        val errorMsg = "Utkast ble ikke funnet for id: 123"
        whenever(utkastServiceMock.publiserUtkast(anyString(), any(LocalDate::class.java))).thenThrow(NotFoundException(errorMsg))
        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = ApiErrorResponse(HttpStatus.NOT_FOUND, errorMsg),
                resultMatchers = listOf(status().isNotFound)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString(), any(LocalDate::class.java))
    }

    @Test
    fun publiserUtkastIFeilStatusGir400BadRequest() {
        val msg = "Kan ikke publisere et utkast i status FORKASTET."
        whenever(utkastServiceMock.publiserUtkast(anyString(), any(LocalDate::class.java))).thenThrow(BadRequestException(msg))
        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = ApiErrorResponse(HttpStatus.BAD_REQUEST, msg),
                resultMatchers = listOf(status().isBadRequest)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString(), any(LocalDate::class.java))
    }

    @Test
    fun publiserUtkastOppdateringAvFylkeGir409Conflict_PgaOptimistiskLaasefeil() {
        val eksisterendeFylke = FylkeBuilder()
            .withVersion(4)
            .build()
        val fylkeId = eksisterendeFylke.id

        whenever(utkastServiceMock.publiserUtkast(anyString(), any(LocalDate::class.java))).thenThrow(OptimistiskLaasException(KonfliktVersjon(
            nyVersjon = 3,
            eksisterende = eksisterendeFylke)))

        post(
            url = publiserUrl,
            responseAsserts = MockResponseAsserts(
                expectedResponseObject = ConflictResponseWrapper(
                    optimisticLockExceptions = listOf(
                        ConflictResponse(
                            id = fylkeId,
                            type = "Fylke",
                            gjeldendeVersjon = 4,
                            versjon = 3,
                            melding = "Det er ikke samsvar med versjon. Ønsket versjon = 3, eksisterende versjon = 4 for objekt med id = $fylkeId"
                        )
                    )),
                resultMatchers = listOf(status().isConflict)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString(), any(LocalDate::class.java))
    }

    @Test
    fun publiserUtkastMedUgyldigGeometriGirHTTP400() {
        whenever(utkastServiceMock.publiserUtkast(anyString(), any(LocalDate::class.java))).thenThrow(
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

        verify(utkastServiceMock).publiserUtkast(anyString(), any(LocalDate::class.java))
    }

    @Test
    fun publiserUtkastMedIkkeSluttetRingGirHTTP400() {
        whenever(utkastServiceMock.publiserUtkast(anyString(), any(LocalDate::class.java))).thenThrow(
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

        verify(utkastServiceMock).publiserUtkast(anyString(), any(LocalDate::class.java))
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

    @Test
    fun publiserUtkastOppdateringAvFylkeGir409Conflict_PgaKonfliktMedFramtidigeVersjoner() {
        // Lokalid+gyldigFra som blir truffet av utkastet
        val lokalid = Lokalid("abcd-38383")
        val foersteJanuar2024 = LocalDate.of(2024, Month.JANUARY, 1)

        // Fantes en Grunnkrets fra før med: Gyldig fra 1.1.25
        val foersteJanuar2025 = LocalDate.of(2025, Month.JANUARY, 1)

        // Fantes en Grunnkrets fra før med: Gyldig fra 1.1.26
        val foersteJanuar2026 = LocalDate.of(2026, Month.JANUARY, 1)

        whenever(utkastServiceMock.publiserUtkast(anyString(), any(LocalDate::class.java))).thenThrow(FeilVedOppdatering.OppdateringException(
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
                expectedResponseObject = ConflictResponseWrapper(
                    framtidigVersjonConflict = FramtidigVersjonConflict(
                        id = ObjektIdentifikator(lokalid, foersteJanuar2024),
                        type = "GRUNNKRETS",
                        affectedIds = listOf(
                            ObjektIdentifikator(lokalid, foersteJanuar2025),
                            ObjektIdentifikator(lokalid, foersteJanuar2026)
                        ),
                        melding = "Det er ikke mulig å oppdatere objekt med identifikator " +
                            "Identifikator(lokalid=Lokalid(value=${lokalid.value}), gyldighetsdato=2024-01-01) " +
                            "av type GRUNNKRETS. Dette berører følgende objekter med senere gyldigFra-tidspunkt: " +
                            "[" +
                            "Identifikator(lokalid=Lokalid(value=${lokalid.value}), gyldighetsdato=2025-01-01), " +
                            "Identifikator(lokalid=Lokalid(value=${lokalid.value}), gyldighetsdato=2026-01-01)" +
                            "]."
                    )),
                resultMatchers = listOf(status().isConflict)
            )
        )
        verify(utkastServiceMock).publiserUtkast(anyString(), any(LocalDate::class.java))
    }

    @Test
    fun framtidigeVersjonerKonfliktResolvedGir200OK() {
        val lokalid = Lokalid("abcd-38383")
        val july1In2023 = LocalDate.of(2023, Month.JULY, 1)
        val january1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

        post(
            url = "/v1/utkast/123/resolved",
            requestObject = ConflictResolved(
                lokalid = lokalid,
                grunnkretsRequests = listOf(
                    GrunnkretsRequestResolved(
                        gyldigFra = july1In2023,
                        endringstype = Endringstype.KVALITETSHEVING.value,
                        grunnkretsRequest = createGrunnkretsRequest("Moi", 2)),
                    GrunnkretsRequestResolved(
                        gyldigFra = january1In2024,
                        endringstype = Endringstype.KVALITETSHEVING.value,
                        grunnkretsRequest = createGrunnkretsRequest("Voi", 3)
                    )
                )
            ),
            expected = status().isOk
        )
        verify(utkastServiceMock).resolveConflictForFramtidigeVersjoner(anyString(), any(ConflictResolved::class.java))
    }

    private fun createGrunnkretsRequest(grkrNavn: String, version: Int): GrunnkretsRequest =
        GrunnkretsRequest(
            navn = grkrNavn,
            grunnkretsnummer = "4589",
            identifikasjon = Identifikasjon(lokalid = "abcd-38383"),
            version = version,
        )
}
