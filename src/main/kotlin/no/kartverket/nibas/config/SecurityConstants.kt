package no.kartverket.nibas.config

import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties.BASIC_AUTH_ORDER

/**
 * Lavere verdi for Order har høyere prioritet.
 */
object SecurityConstants {
    //Høyest pri
    const val ORDER_OF_API_KEY_FILTER_CHAIN: Int = BASIC_AUTH_ORDER - 2

    //Lavere pri
    const val ORDER_OF_FORWARDED_AUTH_FILTER_CHAIN: Int = BASIC_AUTH_ORDER - 1
}
