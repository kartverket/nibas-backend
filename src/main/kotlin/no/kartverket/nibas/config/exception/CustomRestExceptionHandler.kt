package no.kartverket.nibas.config.exception

import no.kartverket.nibas.api.v1.response.ApiErrorResponse
import no.kartverket.nibas.api.v1.response.ConflictResponse
import no.kartverket.nibas.api.v1.response.ConflictResponseWrapper
import no.kartverket.nibas.api.v1.response.FramtidigVersjonConflict
import no.kartverket.nibas.api.v1.response.GeometriErrorResponse
import no.kartverket.nibas.api.v1.response.MissingKodelisteItemResponse
import no.kartverket.nibas.api.v1.response.toObjektIdentifikator
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.events.PublishEventException
import no.kartverket.nibas.kodelister.MissingKodelisteItemException
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.extensions.geometry.posisjonToDto
import no.kartverket.nibas.sosi.sosiimport.GeometriFailedToBeCreatedException
import no.kartverket.nibas.sosi.sosiimport.IkkeSammenhengendePolygonException
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import no.kartverket.nibas.sosi.sosiimport.UgyldigGeometriException
import org.springframework.core.log.LogFormatUtils
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
        val errorResponse = MissingKodelisteItemResponse(message = "Fant ikke ${missingKodelisteEx.kodelisteType} for ${missingKodelisteEx.id}")
        return handleExceptionInternal(missingKodelisteEx, errorResponse, HttpHeaders(), errorResponse.httpStatus, request)
    }

    @ExceptionHandler(value = [UgyldigGeometriException::class])
    fun handleUgyldigGeometriException(ugyldigGEx: UgyldigGeometriException, request: WebRequest): ResponseEntity<Any>? {
        val topologyValidationError = ugyldigGEx.topologyValidationError
        val errorResponse = GeometriErrorResponse(
            geometryType = ugyldigGEx.geometry::class.java.simpleName,
            validationError = topologyValidationError.message,
            feilLokasjon = SosiGeometryFunctions.coordinateToPoint(topologyValidationError.coordinate).posisjonToDto(),
            message = ugyldigGEx?.message ?: "Ukjent feil"
        )
        return handleExceptionInternal(ugyldigGEx, errorResponse, HttpHeaders(), errorResponse.httpStatus, request)
    }

    @ExceptionHandler(value = [IkkeSammenhengendePolygonException::class])
    fun handleIkkeSammenhengendePolygonException(ex: IkkeSammenhengendePolygonException, request: WebRequest): ResponseEntity<Any>? {
        val errorResponse = GeometriErrorResponse(
            feilLokasjon = ex.startPunkt.posisjonToDto(),
            message = ex?.message ?: "Ukjent feil",
            validationError = "Ikke-sammenhengende grenser"
        )
        return handleExceptionInternal(ex, errorResponse, HttpHeaders(), errorResponse.httpStatus, request)
    }

    @ExceptionHandler(value = [GeometriFailedToBeCreatedException::class])
    fun handleGeometriFailedToBeCreatedException(ex: GeometriFailedToBeCreatedException, request: WebRequest): ResponseEntity<Any>? {
        val errorResponse = GeometriErrorResponse(
            feilLokasjon = ex.point.posisjonToDto(),
            message = ex?.message ?: "Ukjent feil",
        )
        return handleExceptionInternal(ex, errorResponse, HttpHeaders(), errorResponse.httpStatus, request)
    }

    @ExceptionHandler(value = [OptimistiskLaasException::class])
    fun handleOptimisticLockException(ex: OptimistiskLaasException, request: WebRequest): ResponseEntity<Any>? {
        // For grenser kan det være flere optimistiske låsekonflikter
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
        return handleExceptionInternal(ex, ApiErrorResponse(httpStatus, ex.message), HttpHeaders(), httpStatus, request)
    }

    @ExceptionHandler(value = [PublishEventException::class])
    fun handlePublishEventException(ex: PublishEventException, request: WebRequest): ResponseEntity<Any>? {
        val httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
        return handleExceptionInternal(ex, ApiErrorResponse(httpStatus, ex.message), HttpHeaders(), httpStatus, request)
    }

    override fun handleExceptionInternal(ex: Exception, body: Any?, headers: HttpHeaders, statusCode: HttpStatusCode, request: WebRequest): ResponseEntity<Any>? {
        this.logger.warn(buildLogMessage(ex))
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }

    private fun buildLogMessage(ex: Exception): String {
        val builder = StringBuilder("Resolved ${prettyPrintEx(ex)}")
        val cause = ex.cause
        if (cause != null) {
            builder.appendLine()
                .appendLine("--> Cause ${prettyPrintEx(cause)}")
        }
        return builder.toString()
    }

    private fun prettyPrintEx(ex: Throwable): String = "[" + LogFormatUtils.formatValue(ex, -1, true) + "]"

}
