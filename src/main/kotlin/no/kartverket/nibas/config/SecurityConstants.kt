package no.kartverket.nibas.config

/**
 * Lavere verdi for Order har høyere prioritet.
 * 
 * In Spring Boot 4, SecurityProperties was removed. BASIC_AUTH_ORDER was -100.
 */
object SecurityConstants {
    private const val BASIC_AUTH_ORDER = -100
    
    //Høyest pri
    const val ORDER_OF_API_KEY_FILTER_CHAIN: Int = BASIC_AUTH_ORDER - 2

    //Lavere pri
    const val ORDER_OF_FORWARDED_AUTH_FILTER_CHAIN: Int = BASIC_AUTH_ORDER - 1
}
