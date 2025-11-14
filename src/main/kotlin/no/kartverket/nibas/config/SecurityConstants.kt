package no.kartverket.nibas.config

import org.springframework.boot.autoconfigure.security.SecurityProperties

/**
 * Lavere verdi for Order har høyere prioritet.
 */
object SecurityConstants {
    //Høyest pri
    const val ORDER_OF_API_KEY_FILTER_CHAIN: Int = SecurityProperties.BASIC_AUTH_ORDER - 2

    //Lavere pri
    const val ORDER_OF_FORWARDED_AUTH_FILTER_CHAIN: Int = SecurityProperties.BASIC_AUTH_ORDER - 1
}
