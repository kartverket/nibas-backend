package no.kartverket.nibas.config.exception

import no.kartverket.nibas.api.v1.klient.response.ApiErrorResponse
import no.kartverket.nibas.api.v1.klient.response.OptimistiskLaasResponse
import no.kartverket.nibas.api.v1.klient.response.OptimistiskLaasWrapper
import no.kartverket.nibas.api.v1.klient.response.ErrorDescription
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.geometry.IkkeSammenhengendePolygonException
import no.kartverket.nibas.common.geometry.UgyldigFlateInformasjon
import no.kartverket.nibas.common.geometry.UgyldigeFlaterException
import no.kartverket.nibas.common.geometry.UgyldigGeometriException
import no.kartverket.nibas.common.geometry.toCustomText
import no.kartverket.nibas.common.round
import no.kartverket.nibas.kodelister.MissingKodelisteItemException
import no.kartverket.nibas.repository.OptimistiskLaasException
import org.locationtech.jts.geom.Coordinate
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
    fun handleManglendeKodelisteInnslag(missingKodelisteEx: MissingKodelisteItemException): ResponseEntity<Any>? {
        val errorResponse = ErrorDescription(
            description = "Fant ikke ${missingKodelisteEx.kodelisteType} for ${missingKodelisteEx.id}"
        )
        return handleException(missingKodelisteEx, errorResponse, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(value = [UgyldigGeometriException::class])
    fun handleUgyldigGeometriException(ugyldigGEx: UgyldigGeometriException): ResponseEntity<Any>? {
        val errorDescription = ErrorDescription(
            title = "Feil med geometri",
            description = "Det er ble oppdaget feil med geometrien i utkastet: ${ugyldigGEx.topologyValidationError.toCustomText()}"
        )
        return handleException(ugyldigGEx, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [IkkeSammenhengendePolygonException::class])
    fun handleIkkeSammenhengendePolygonException(ex: IkkeSammenhengendePolygonException): ResponseEntity<Any>? {
        val errorDescription = ErrorDescription(
            title = "Feil med geometri",
            description = "Geometrien i utkastet er ikke sammenhengende - og danner derfor ikke fullstendige polygoner."
        )
        return handleException(ex, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [OptimistiskLaasException::class])
    fun handleOptimisticLockException(ex: OptimistiskLaasException): ResponseEntity<Any>? {
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
    fun handleOppdateringException(ex: FeilVedOppdatering.OppdateringException): ResponseEntity<Any>? {
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
    fun handleResponseExceptionException(ex: ResponseException): ResponseEntity<Any>? {
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
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<Any>? {
        val httpStatus = HttpStatus.BAD_REQUEST
        val errorDescription = ErrorDescription(
            description = "En ukjent feil har oppstått. Vennligst prøv igjen senere, eller kontakt Kartverket om feilen fortsetter."
        )
        return handleException(ex, errorDescription, HttpHeaders(), httpStatus, logAsError = true)
    }

    @ExceptionHandler(value = [ResponseStatusException::class])
    fun handleResponseStatusExceptions(ex: ResponseStatusException): ResponseEntity<Any>? {
        val httpStatus = ex.statusCode
        val errorDescription = ErrorDescription(
            description = ex.message
        )
        return handleException(ex, errorDescription, HttpHeaders(), httpStatus, logAsError = true)
    }

    @ExceptionHandler(value = [UgyldigeFlaterException::class])
    fun handleUgydligFlateExceptions(ex: UgyldigeFlaterException) : ResponseEntity<Any>? {

        val gruppertPaaKoordinat: Map<Coordinate, List<UgyldigFlateInformasjon>> = ex.ugyldigeFlater.groupBy { it.koordinat }
        val antallGrupper = gruppertPaaKoordinat.size

        val stringBuilder = StringBuilder()

        gruppertPaaKoordinat.entries.forEachIndexed { index, entry ->
            entry.value.forEach{
                stringBuilder.appendLine("${index + 1}. ${it.flateInformasjon.flatetype} ${it.flateInformasjon.lesbartNavn}: ")
            }
            stringBuilder.append(entry.value[0].topologifeil)
            stringBuilder.appendLine()

            stringBuilder.appendLine("Feilen ble først oppdaget på ${entry.key.y.round(2)}N ${entry.key.x.round(2)}Ø")

            if (index + 1 < antallGrupper) {
                stringBuilder.appendLine()
            }
        }
        stringBuilder.appendLine()
        stringBuilder.appendLine("Dobbeltsjekk endringene dine:")
        stringBuilder.appendLine("- Er tilhørigheten satt riktig?")
        stringBuilder.appendLine("- Har du glemt å arkivere en utgående grense?")
        stringBuilder.appendLine("- Gå inn og ut av utkastet før du prøver å publisere. Det kan ha skjedd en oppdateringsfeil.")

        val errorDescription = ErrorDescription(
            title = "Det ble funnet $antallGrupper feil på utkastet",
            description = stringBuilder.toString()
        )
        return handleException(ex, errorDescription, HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleAllOtherExceptions(ex: Exception): ResponseEntity<Any>? {
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

    private fun <T : Any> createResponse(body: T, headers: HttpHeaders, statusCode: HttpStatusCode): ResponseEntity<T> {
        return ResponseEntity(body, headers, statusCode)
    }
}
