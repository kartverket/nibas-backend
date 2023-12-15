package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.FrontendLogApi
import no.kartverket.nibas.api.v1.klient.FrontendLogRequest
import no.kartverket.nibas.api.v1.klient.LogLevel
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RestController

@RestController("FrontendLogController")
class FrontendLogController: FrontendLogApi {
    val logger = LoggerFactory.getLogger(javaClass)

    override fun logMelding(body: FrontendLogRequest) {
        when(body.logLevel) {
            LogLevel.INFO -> logger.info(body.toLogMessage())
            LogLevel.WARN -> logger.warn(body.toLogMessage())
            LogLevel.ERROR -> logger.error(body.toLogMessage())
        }
    }
}
