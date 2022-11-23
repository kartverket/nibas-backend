package no.kartverket.nibas.common

import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Gir mulighet til å overstyre statisk spring-web metode. Tryggere for test.
 */
@Component
class HrefProvider {
    fun getHref(id: String): String = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(id).build().toUriString()
}
