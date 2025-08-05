package no.kartverket.nibas.controller.klient

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.net.URI
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/v1/proxy/arbeidsliste")
class ArbeidslisteProxyController(
    @Value("\${nibas.arbeidsliste.host}") private val arbeidslisteBaseUrl: String,
    @Value("\${nibas.arbeidsliste.api-key}") private val arbeidslisteAPIKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate().apply {
        setRequestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5000)
            setReadTimeout(10000)
        })
    }

    /**
     * Proxy-forespørsel til arbeidsliste.
     *
     * @param request [HttpServletRequest] som inneholder den innkommende forespørselen fra klienten
     * @param response [HttpServletResponse] som skal skrives til
     */
    @RequestMapping("/**")
    fun proxyRequest(request: HttpServletRequest, response: HttpServletResponse) {
        // Fjern /proxy/arbeidsliste fra request path og legg til /internal-api prefix
        val downstreamPath = "/internal-api" + request.servletPath.removePrefix("/v1/proxy/arbeidsliste")
        val method = request.method

        if (!isPathMethodAllowed(downstreamPath, method)) {
            log.warn("Proxy request feilet: Path eller metode er ikke tillatt: {} {}", method, downstreamPath)
            response.status = HttpServletResponse.SC_NOT_FOUND
            response.writer.write("Error: Path eller metode er ikke tillatt.")
            return
        }

        if (!areQueryParametersAllowed(request)) {
            log.warn("Proxy request feilet: Ugyldige query-parametere funnet: {}", request.parameterMap.keys)
            response.status = HttpServletResponse.SC_BAD_REQUEST
            response.writer.write("Error: Request inneholder ugyldige eller ikke tillatte query-parametere.")
            return
        }

        val targetUrl = UriComponentsBuilder
            .fromUriString(arbeidslisteBaseUrl)
            .path(downstreamPath)
            .query(request.queryString)
            .build(true)
            .toUri()

        log.info("Validerte proxy-forespørsel: {} {} -> {}", request.method, downstreamPath, targetUrl)

        try {
            val headers: HttpHeaders = copyRequestHeaders(request)
            val requestEntity: HttpEntity<ByteArray> = createRequestEntity(request, headers)
            val responseEntity: ResponseEntity<ByteArray> = executeRequest(targetUrl, request.method, requestEntity)
            processResponse(response, responseEntity)
        } catch (e: HttpStatusCodeException) {
            log.warn("Proxy request til {} feilet med status {}: {}", targetUrl, e.statusCode, e.responseBodyAsString)
            processResponse(response, ResponseEntity(e.responseBodyAsByteArray, e.responseHeaders, e.statusCode))
        } catch (e: Exception) {
            handleError(response, targetUrl, e)
        }
    }


    /**
     * Kopierer HTTP-headere fra den opprinnelige request til en ny HttpHeaders-instans.
     * Funksjonen filtrerer ut headere som ikke trengs videresendes til arbeidsliste.
     *
     * @param request HTTP-request klienten.
     * @return Et nytt HttpHeaders med riktige headere for arbeidsliste
     */
    private fun copyRequestHeaders(request: HttpServletRequest): HttpHeaders {
        val headers = HttpHeaders()
        val headerNames = request.headerNames
        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            if (!IGNORED_REQUEST_HEADERS.contains(headerName.lowercase(Locale.getDefault()))) {
                val headerValue = request.getHeader(headerName)
                headers.set(headerName, headerValue)
            }
        }
        headers.set("X-API-Key", arbeidslisteAPIKey)
        return headers
    }

    /**
     * Oppretter et HttpEntity-objekt basert på den innkommende HTTP-request.
     *
     * @param request HTTP-request fra klienten.
     * @param headers HttpHeaders-objektet som inneholder alle godkjente headere
     * @return En HttpEntity-objekt basert på den innkommende request
     */
    private fun createRequestEntity(request: HttpServletRequest, headers: HttpHeaders): HttpEntity<ByteArray> {
        return if (shouldHaveRequestBody(request.method)) {
            HttpEntity<ByteArray>(request.inputStream.readAllBytes(), headers)
        } else {
            HttpEntity<ByteArray>(headers)
        }
    }

    /**
     * Utfører en HTTP-request til targetUrl og returnerer svaret.
     *
     * @param targetUrl Target-URL for HTTP-request
     * @param method HTTP-metode
     * @param requestEntity HttpEntity-objektet som inneholder body og headere
     * @return [ResponseEntity<ByteArray>] Respons fra arbeidsliste
     */
    private fun executeRequest(targetUrl: URI, method: String, requestEntity: HttpEntity<ByteArray>): ResponseEntity<ByteArray> {
        return restTemplate.exchange(
            targetUrl,
            HttpMethod.valueOf(method),
            requestEntity,
            ByteArray::class.java
        )
    }

    /**
     * Behandler svaret fra arbeidsliste og skriver det til HttpServletResponse.
     *
     * @param response [HttpServletResponse] som skal skrives til
     * @param responseEntity [ResponseEntity<ByteArray>] Respons fra arbeidsliste
     */
    private fun processResponse(response: HttpServletResponse, responseEntity: ResponseEntity<ByteArray>) {
        response.status = responseEntity.statusCode.value()

        responseEntity.headers.forEach { name, values ->
            if (!IGNORED_RESPONSE_HEADERS.contains(name.lowercase(Locale.getDefault()))) {
                values.forEach { value ->
                    response.addHeader(name, value)
                }
            }
        }

        responseEntity.body?.let {
            response.contentType = responseEntity.headers.contentType?.toString() ?: response.contentType ?: "application/octet-stream"
            response.setContentLength(it.size)
            response.outputStream.write(it)
            response.outputStream.flush()
        }
    }

    /**
     * Håndterer feil ved proxy-forespørsel og skriver feilrespons til HttpServletResponse.
     *
     * @param response [HttpServletResponse] som skal skrives til
     * @param targetUrl Den forsøkte mål-URLen
     * @param e [Exception] som oppstod under proxy-forespørselen
     */
    private fun handleError(response: HttpServletResponse, targetUrl: URI, e: Exception) {
        log.error("Proxy request to {} failed unexpectedly: {}", targetUrl, e.message, e)
        if (!response.isCommitted) {
            response.status = HttpServletResponse.SC_BAD_GATEWAY // 502 indicates issue talking to backend
            try {
                response.contentType = "text/plain"
                response.writer.write("Proxy Error: Upstream service unavailable or encountered an error.")
            } catch (ioe: IOException) {
                log.error("Could not write error response after proxy failure", ioe)
            }
        } else {
            log.warn("Response already committed when proxy error occurred for {}", targetUrl)
        }
    }

    /**
     * Avgjør om en forespørsel har en body basert på HTTP-metoden.
     *
     * @param [method] HTTP-metode
     * @return true hvis forespørselen har en body, ellers false
     */
    private fun shouldHaveRequestBody(method: String): Boolean {
        return method.equals("POST", ignoreCase = true) ||
            method.equals("PUT", ignoreCase = true) ||
            method.equals("PATCH", ignoreCase = true)
    }

    /**
     * Sjekker om kombinasjonen av sti og HTTP-metode er tillatt.
     * @param [path]
     * @param [method]
     */
    private fun isPathMethodAllowed(path: String, method: String): Boolean {
        // Check all path pattern rules
        for ((pathPattern, allowedMethods) in PATH_METHOD_ALLOWLIST) {
            if (pathPattern is String) {
                if (path == pathPattern && method in allowedMethods) {
                    return true
                }
            } else if (pathPattern is Regex) {
                if (pathPattern.matches(path) && method in allowedMethods) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Sjekker om alle query-parametere i forespørselen er tillatt.
     * @param request [HttpServletRequest] som inneholder forespørselen
     * @return [Boolean]
     */
    private fun areQueryParametersAllowed(request: HttpServletRequest): Boolean {
        // If no query parameters, always valid
        if (request.parameterMap.isEmpty()) {
            return true
        }

        return request.parameterMap.keys.all { it in ALLOWED_QUERY_PARAMETERS }
    }

    companion object {

        private val PATH_METHOD_ALLOWLIST: Map<Any, Set<String>> = mapOf(
            "/internal-api/v1/avvik" to setOf("GET", "POST"),
            "/internal-api/v1/avvik/kommuner" to setOf("GET"),
            "/internal-api/v1/matrikkel/grenselinjer" to setOf("GET"),
            "/internal-api/v1/matrikkel/grenselinjer/kommuner" to setOf("GET"),
            Regex("^/internal-api/v1/avvik/kommune/[^/]+$") to setOf("GET")
        )

        private val ALLOWED_QUERY_PARAMETERS: Set<String> = setOf(
            "side", "antall", "grensetyper", "kommunenummer"
        )

        private val IGNORED_REQUEST_HEADERS = setOf(
            "host",
            "connection",
            "content-length",
            "transfer-encoding",
            "cookie",
            "authorization",
            "x-api-key"
        )

        private val IGNORED_RESPONSE_HEADERS = setOf(
            "connection",
            "transfer-encoding",
            "server",
            "set-cookie"
        )
    }
}
