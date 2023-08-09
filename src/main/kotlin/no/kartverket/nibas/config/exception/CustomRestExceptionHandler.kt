package no.kartverket.nibas.config.exception

import no.kartverket.nibas.api.v1.response.ApiErrorResponse
import no.kartverket.nibas.api.v1.response.ConflictResponse
import no.kartverket.nibas.api.v1.response.ConflictResponseWrapper
import no.kartverket.nibas.api.v1.response.ErrorDescription
import no.kartverket.nibas.api.v1.response.FramtidigVersjonConflict
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.kodelister.MissingKodelisteItemException
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.sosi.sosiimport.GeometriFailedToBeCreatedException
import no.kartverket.nibas.sosi.sosiimport.IkkeSammenhengendePolygonException
import no.kartverket.nibas.sosi.sosiimport.UgyldigGeometriException
import org.locationtech.jts.operation.valid.TopologyValidationError
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Custom global exception handler på tvers av applikasjonen.
 * Denne overstyrer default exception handler i Spring Boot (DefaultHandlerExceptionResolver).
 */
@ControllerAdvice
class CustomRestExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [MissingKodelisteItemException::class])
    fun handleManglendeKodelisteInnslag(missingKodelisteEx: MissingKodelisteItemException, request: WebRequest): ResponseEntity<Any>? {
        val errorResponse = ErrorDescription(
            description = "Fant ikke ${missingKodelisteEx.kodelisteType} for ${missingKodelisteEx.id}"
        )
        return handleException(missingKodelisteEx, errorResponse, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request)
    }

    @ExceptionHandler(value = [UgyldigGeometriException::class])
    fun handleUgyldigGeometriException(ugyldigGEx: UgyldigGeometriException, request: WebRequest): ResponseEntity<Any>? {
        val errorDescription = ErrorDescription(
            title = "Feil med geometri",
            description = "Det er ble oppdaget feil med geometrien i utkastet: ${topologyErrorToCustomText(ugyldigGEx.topologyValidationError)}"
        )
        return handleException(ugyldigGEx, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST, request)
    }

    @ExceptionHandler(value = [IkkeSammenhengendePolygonException::class])
    fun handleIkkeSammenhengendePolygonException(ex: IkkeSammenhengendePolygonException, request: WebRequest): ResponseEntity<Any>? {
        val errorDescription = ErrorDescription(
            title = "Feil med geometri",
            description = "Geometrien i utkastet er ikke sammenhengende - og danner derfor ikke fullstendige polygoner."
        )
        return handleException(ex, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST, request)
    }

    @ExceptionHandler(value = [GeometriFailedToBeCreatedException::class])
    fun handleGeometriFailedToBeCreatedException(ex: GeometriFailedToBeCreatedException, request: WebRequest): ResponseEntity<Any>? {
        val geomtrifeil = ex.message ?: "Ukjent feil"
        val errorDescription = ErrorDescription(
            title = "Feil med geometri",
            description = "Det er en ukjent feil med geometrien i utkastet. Opprinnelig feilmelding fra baksystem: $geomtrifeil"
        )
        return handleException(ex, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST, request)
    }

    @ExceptionHandler(value = [OptimistiskLaasException::class])
    fun handleOptimisticLockException(ex: OptimistiskLaasException, request: WebRequest): ResponseEntity<Any>? {
        // For grenser kan det være flere optimistiske låsekonflikter
        logger.warn("Feil med optimistisk lås", ex)
        return handleExceptionInternal(ex, ConflictResponseWrapper(
            optimisticLockExceptions = ex.konflikter.map { conflict ->
                ConflictResponse(
                    id = conflict.eksisterende.id,
                    type = conflict.eksisterende::class.java.simpleName,
                    gjeldendeVersjon = conflict.eksisterende.version,
                    versjon = conflict.nyVersjon,
                    melding = conflict.getMessage()
                )
            }), HttpHeaders(), HttpStatus.CONFLICT, request)
    }

    @ExceptionHandler(value = [FeilVedOppdatering.OppdateringException::class])
    fun handleOppdateringException(ex: FeilVedOppdatering.OppdateringException, request: WebRequest): ResponseEntity<Any>? {
        logger.warn("Fremtidig versjons-konflikt", ex)
        val oppdateringInfo = ex.oppdateringInfo
        return handleExceptionInternal(ex, ConflictResponseWrapper(
            framtidigVersjonConflict = FramtidigVersjonConflict(
                id = oppdateringInfo.id.toObjektIdentifikator(),
                type = oppdateringInfo.type.name,
                affectedIds = oppdateringInfo.affectedIds.map { it.toObjektIdentifikator() },
                melding = oppdateringInfo.getMessage()
            )
        ), HttpHeaders(), HttpStatus.CONFLICT, request)
    }

    @ExceptionHandler(value = [ResponseException::class])
    fun handleResponseExceptionException(ex: ResponseException, request: WebRequest): ResponseEntity<Any>? {
        val httpStatus = when (ex) {
            is ResponseException.NotFoundException -> HttpStatus.NOT_FOUND
            is ResponseException.BadRequestException -> HttpStatus.BAD_REQUEST
        }
        val errorDescription = ErrorDescription(
            title = ex.title,
            description = ex.msg,
            additionalInfo = ex.additionalInfo
        )
        return handleException(ex, errorDescription, HttpHeaders(), httpStatus, request)
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleAllOtherExceptions(ex: Exception, request: WebRequest): ResponseEntity<Any>? {
        val httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val errorDescription = ErrorDescription(
            description = ex.message ?: "Ukjent server-feil"
        )
        return handleException(ex, errorDescription, HttpHeaders(), httpStatus, request)
    }

    private fun handleException(ex: Exception, errorDescription: ErrorDescription, headers: HttpHeaders, statusCode: HttpStatusCode, request: WebRequest): ResponseEntity<Any>? {
        this.logger.warn(ex)
        val body = ApiErrorResponse(
            errorCode = MDC.get("requestId") ?: "UKJENT",
            errorDescription = errorDescription
        )
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }

    private fun topologyErrorToCustomText(topologyError: TopologyValidationError): String =
        when(topologyError.errorType) {
            TopologyValidationError.SELF_INTERSECTION -> "En grense kan ikke krysse en annen grense."
            TopologyValidationError.RING_SELF_INTERSECTION -> "En grense kan ikke krysse en annen grense."
            TopologyValidationError.INVALID_COORDINATE -> "Grensen inneholder ett eller flere punkter med ugyldig koordinat."
            TopologyValidationError.TOO_FEW_POINTS -> "For få grensepunkter i flaten."
            TopologyValidationError.RING_NOT_CLOSED -> "Flaten er ikke lukket."
            else -> "Ukjent feil med feilkode [${topologyError.errorType} - ${topologyError.message}]"
        }
}
