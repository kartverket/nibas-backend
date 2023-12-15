package no.kartverket.nibas.api.v1.klient

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/v1/frontendlogger")
@Tag(
    name = "v1/frontendlogger",
    description = "Endepunkt for å logge ting fra frontenden"
)
interface FrontendLogApi {
    @Operation(summary = "Logger en gitt melding så utviklere får tak i den.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Successful operation",
        )
    )
    @PostMapping
    fun logMelding(@RequestBody body: FrontendLogRequest)
}

data class FrontendLogRequest(
    val logLevel: LogLevel,
    val message: String,
    val stacktrace: String?
) {
    fun toLogMessage(): String {

        if (stacktrace != null) {
            return "$message cause: $stacktrace"
        }
        return message
    }
}

enum class LogLevel {
    INFO, WARN, ERROR
}
