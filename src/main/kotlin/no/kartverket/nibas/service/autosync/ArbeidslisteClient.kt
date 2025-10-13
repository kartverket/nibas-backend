package no.kartverket.nibas.service.autosync

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class ArbeidslisteClient(
    @param:Value($$"${nibas.arbeidsliste.host}") private val arbeidslisteBaseUrl: String,
    @param:Value($$"${nibas.arbeidsliste.api-key}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restTemplate = RestTemplate().apply {
        requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5000)
            setReadTimeout(30000)
        }
    }

    private val headers = HttpHeaders().apply {
        set("X-API-Key", apiKey)
    }

    fun getKommunePar(
        grensetyper: List<String>,
        side: Int = 0,
        antall: Int = 10
    ): KommuneParPage {
        val url = UriComponentsBuilder
            .fromUriString(arbeidslisteBaseUrl)
            .path("/internal-api/api/v1/avvik/kommunepar")
            .queryParam("side", side)
            .queryParam("antall", antall)
            .apply {
                grensetyper.forEach { queryParam("grensetyper", it) }
            }
            .build()
            .toUri()

        log.info("Fetching kommune pairs with avvik from: {}", url)

        return safeApiCall(KommuneParPage(emptyList(), 0, 0)) {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                KommuneParPage::class.java
            )

            log.info("Got {} kommune pairs with avvik (total: {})",
                response.body?.content?.size ?: 0,
                response.body?.totalElements ?: 0)

            response.body ?: KommuneParPage(emptyList(), 0, 0)
        }
    }

    fun getAvvikForKommunePar(
        kommuneId1: String,
        kommuneId2: String,
        grensetyper: List<String>
    ): List<AvvikDto> {
        val url = UriComponentsBuilder
            .fromUriString(arbeidslisteBaseUrl)
            .path("/internal-api/api/v1/avvik/kommunepar/{id1}/{id2}")
            .apply {
                grensetyper.forEach { queryParam("grensetyper", it) }
            }
            .buildAndExpand(kommuneId1, kommuneId2)
            .toUri()

        log.info("Fetching avvik for kommune pair from: {}", url)

        return safeApiCall(emptyList()) {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<List<AvvikDto>>() {}
            )

            log.info("Got {} avvik for kommune pair", response.body?.size ?: 0)

            response.body ?: emptyList()
        }
    }

    fun getMatrikkelGrenselinjer(kommunenummer: String): M22FeatureCollection {
        val url = UriComponentsBuilder
            .fromUriString(arbeidslisteBaseUrl)
            .path("/internal-api/api/v1/matrikkel/grenselinjer")
            .queryParam("kommunenummer", kommunenummer)
            .build()
            .toUri()

        log.info("Fetching M22 grenselinjer for kommune {} from: {}", kommunenummer, url)

        return safeApiCall(M22FeatureCollection("FeatureCollection", emptyList())) {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                M22FeatureCollection::class.java
            )

            log.info("Got {} M22 grenselinjer for kommune {}", response.body?.features?.size ?: 0, kommunenummer)

            response.body ?: M22FeatureCollection("FeatureCollection", emptyList())
        }
    }

    fun markAvvikAsFikset(avvikIds: List<Long>) {
        val url = "$arbeidslisteBaseUrl/internal-api/api/v1/avvik"
        val requestBody = AvvikUpdateRequest(avvikIds.map { AvvikUpdate(it, "FIKSET") })

        log.info("Marking {} avvik as FIKSET", avvikIds.size)

        safeApiCall(Unit) {
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(requestBody, headers.apply { set("Content-Type", "application/json") }),
                Void::class.java
            )
            log.info("Successfully marked avvik as FIKSET")
        }
    }

    private fun <T> safeApiCall(fallback: T, apiCall: () -> T): T {
        return try {
            apiCall()
        } catch (e: HttpClientErrorException) {
            log.error("Client error calling arbeidsliste: {} - {}", e.statusCode, e.message)
            fallback
        } catch (e: HttpServerErrorException) {
            log.error("Server error from arbeidsliste: {} - {}", e.statusCode, e.message)
            fallback
        } catch (e: ResourceAccessException) {
            log.error("Cannot reach arbeidsliste (timeout or network error): {}", e.message)
            fallback
        }
    }
}

data class AvvikUpdate(val id: Long, val status: String)
data class AvvikUpdateRequest(val avvikUpdates: List<AvvikUpdate>)
