package no.kartverket.nibas.config.exception

import no.kartverket.nibas.api.v1.klient.response.ApiErrorResponse
import no.kartverket.nibas.api.v1.klient.response.OptimistiskLaasResponse
import no.kartverket.nibas.api.v1.klient.response.OptimistiskLaasWrapper
import no.kartverket.nibas.api.v1.klient.response.ErrorDescription
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.geometry.IkkeSammenhengendePolygonException
import no.kartverket.nibas.common.geometry.UgyldigGeometriException
import no.kartverket.nibas.kodelister.MissingKodelisteItemException
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.sosi.sosiimport.GeometriFailedToBeCreatedException
import org.locationtech.jts.operation.valid.TopologyValidationError
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException

/**
 * Custom global exception handler på tvers av applikasjonen.
 * Denne overstyrer default exception handler i Spring Boot (DefaultHandlerExceptionResolver).
 */
@ControllerAdvice
class CustomRestExceptionHandler {
    val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(value = [MissingKodelisteItemException::class])
    fun handleManglendeKodelisteInnslag(missingKodelisteEx: MissingKodelisteItemException, request: WebRequest): ResponseEntity<Any>? {
        val errorResponse = ErrorDescription(
            description = "Fant ikke ${missingKodelisteEx.kodelisteType} for ${missingKodelisteEx.id}"
        )
        return handleException(missingKodelisteEx, errorResponse, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(value = [UgyldigGeometriException::class])
    fun handleUgyldigGeometriException(ugyldigGEx: UgyldigGeometriException, request: WebRequest): ResponseEntity<Any>? {
        val errorDescription = ErrorDescription(
            title = "Feil med geometri",
            description = "Det er ble oppdaget feil med geometrien i utkastet: ${topologyErrorToCustomText(ugyldigGEx.topologyValidationError)}"
        )
        return handleException(ugyldigGEx, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [IkkeSammenhengendePolygonException::class])
    fun handleIkkeSammenhengendePolygonException(ex: IkkeSammenhengendePolygonException, request: WebRequest): ResponseEntity<Any>? {
        val errorDescription = ErrorDescription(
            title = "Feil med geometri",
            description = "Geometrien i utkastet er ikke sammenhengende - og danner derfor ikke fullstendige polygoner."
        )
        return handleException(ex, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [GeometriFailedToBeCreatedException::class])
    fun handleGeometriFailedToBeCreatedException(ex: GeometriFailedToBeCreatedException, request: WebRequest): ResponseEntity<Any>? {
        val geomtrifeil = ex.message ?: "Ukjent feil"
        val errorDescription = ErrorDescription(
            title = "Feil med geometri",
            description = "Det er en ukjent feil med geometrien i utkastet. Opprinnelig feilmelding fra baksystem: $geomtrifeil"
        )
        return handleException(ex, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [OptimistiskLaasException::class])
    fun handleOptimisticLockException(ex: OptimistiskLaasException, request: WebRequest): ResponseEntity<Any>? {
        // For grenser kan det være flere optimistiske låsekonflikter
        logger.warn("Feil med optimistisk lås", ex)
        return createResponse(OptimistiskLaasWrapper(
            optimisticLockExceptions = ex.konflikter.map { conflict ->
                OptimistiskLaasResponse(
                    id = conflict.eksisterende.id,
                    type = conflict.eksisterende::class.java.simpleName,
                    gjeldendeVersjon = conflict.eksisterende.version,
                    versjon = conflict.nyVersjon,
                    melding = conflict.getMessage()
                )
            }), HttpHeaders(), HttpStatus.CONFLICT)
    }

    @ExceptionHandler(value = [FeilVedOppdatering.OppdateringException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleOppdateringException(ex: FeilVedOppdatering.OppdateringException, request: WebRequest): ResponseEntity<Any>? {
        logger.warn("Fremtidig versjons-konflikt", ex)

        // Dette er bare en veldig enkel feilmelding. Vi har en egen oppgave på en bedre MVP av konflikthåndtering.
        // Så dette er bare en enkel feilmelding frem til vi får implementert oppgaven med bedre konflikt-håndtering.
        val errorDescription = ErrorDescription(
            title = "Konflikt med fremtidig versjon",
            description = "En eller flere av endringene i utkastet er i konflikt en publisert fremtidig endring. Du kan ikke publisere endringer på en grense eller krets som har publiserte endringer på en senere dato."
        )
        return handleException(ex, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST)
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
        return handleException(ex, errorDescription, HttpHeaders(), httpStatus)
    }

    @ExceptionHandler(value = [HttpMessageNotReadableException::class])
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException, request: WebRequest): ResponseEntity<Any>? {
        val httpStatus = HttpStatus.BAD_REQUEST
        val errorDescription = ErrorDescription(
            description = "En ukjent feil har oppstått. Vennligst prøv igjen senere, eller kontakt Kartverket om feilen fortsetter."
        )
        return handleException(ex, errorDescription, HttpHeaders(), httpStatus, logAsError = true)
    }

    @ExceptionHandler(value = [ResponseStatusException::class])
    fun handleResponseStatusExceptions(ex: ResponseStatusException, request: WebRequest): ResponseEntity<Any>? {
        val httpStatus = ex.statusCode
        val errorDescription = ErrorDescription(
            description = ex.message
        )
        return handleException(ex, errorDescription, HttpHeaders(), httpStatus, logAsError = true)
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleAllOtherExceptions(ex: Exception, request: WebRequest): ResponseEntity<Any>? {
        val httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val errorDescription = ErrorDescription(
            description = ex.message ?: "Ukjent server-feil"
        )
        return handleException(ex, errorDescription, HttpHeaders(), httpStatus, logAsError = true)
    }

    private fun handleException(ex: Exception, errorDescription: ErrorDescription, headers: HttpHeaders, statusCode: HttpStatusCode, logAsError: Boolean = false): ResponseEntity<Any> {
        if (logAsError) {
            this.logger.error(errorDescription.description, ex)
        } else {
            this.logger.warn(errorDescription.description, ex)
        }

        val body = ApiErrorResponse(
            errorCode = MDC.get("requestId") ?: "UKJENT",
            errorDescription = errorDescription
        )
        return createResponse(body, headers, statusCode)
    }

    private fun <T> createResponse(body: T, headers: HttpHeaders, statusCode: HttpStatusCode): ResponseEntity<T> {
        return ResponseEntity(body, headers, statusCode)
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
