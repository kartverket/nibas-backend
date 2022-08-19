package no.kartverket.nibas.config

import no.kartverket.nibas.api.v1.response.GeometriErrorResponse
import no.kartverket.nibas.kodelister.DefaultKodelisteService
import no.kartverket.nibas.kodelister.KodelisteFetcher
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import no.kartverket.nibas.sosi.sosiimport.GeometriFailedToBeCreatedException
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import no.kartverket.nibas.sosi.sosiimport.UgyldigGeometriException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.log.LogFormatUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@Configuration
class WebConfig @Autowired constructor(val configProps: NibasBackendConfigProps) : WebMvcConfigurer {

    @Bean
    fun kodelisteService(landKodeRepository: LandKodeRepository,
                         terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
                         noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
                         fastsettingstypeRepository: FastsettingstypeRepository,
                         grensestatusRepository: GrensestatusRepository): KodelisteService {
        return DefaultKodelisteService(
            KodelisteFetcher(geoNorgeWebClient()),
            landKodeRepository,
            terrengdetaljkodeRepository,
            noeyaktighetsklassekodeRepository,
            fastsettingstypeRepository,
            grensestatusRepository)
    }

    /**
     * Overstyrer default buffer-størrelse til 10MB, ellers feiler oppslag for historiske.
     */
    @Bean
    fun geoNorgeWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(configProps.geonorgeBaseUrl)
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            .build()
    }
}

/**
 * Custom global exception handler på tvers av applikasjonen.
 * Denne overstyrer default exception handler i Spring Boot (DefaultHandlerExceptionResolver).
 */
@ControllerAdvice
class CustomRestExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [UgyldigGeometriException::class])
    fun handleUgyldigGeometriException(ugyldigGEx: UgyldigGeometriException, request: WebRequest): ResponseEntity<Any> {
        val topologyValidationError = ugyldigGEx.topologyValidationError
        val errorResponse = GeometriErrorResponse(
            geometryType = ugyldigGEx.geometry::class.java.simpleName,
            validationError = topologyValidationError.message,
            feilLokasjon = SosiGeometryFunctions.coordinateToPoint(topologyValidationError.coordinate),
            message = ugyldigGEx.message!!
        )
        return handleExceptionInternal(ugyldigGEx, errorResponse, HttpHeaders(), errorResponse.httpStatus, request)
    }

    @ExceptionHandler(value = [GeometriFailedToBeCreatedException::class])
    fun handleGeometriFailedToBeCreatedException(ex: GeometriFailedToBeCreatedException, request: WebRequest): ResponseEntity<Any> {
        val errorResponse = GeometriErrorResponse(
            feilLokasjon = ex.point,
            message = ex.message!!
        )
        return handleExceptionInternal(ex, errorResponse, HttpHeaders(), errorResponse.httpStatus, request)
    }

    override fun handleExceptionInternal(ex: Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        if (this.logger.isWarnEnabled) {
            this.logger.warn(buildLogMessage(ex))
        }
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    private fun buildLogMessage(ex: Exception): String {
        return "Resolved [" + LogFormatUtils.formatValue(ex, -1, true) + "]"
    }

}

