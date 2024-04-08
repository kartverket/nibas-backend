package no.kartverket.nibas.config

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.SignedJWT
import no.kartverket.nibas.auth.PidMaskerer
import no.kartverket.nibas.common.md5
import no.kartverket.nibas.service.JWTVerifier
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.util.function.Supplier
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse


/**
 * Sikkerhetskonfigurasjon for sikring av endepunkter.
 * Requester evalueres mot 2 SecurityFilterChain:
 * - SecurityFilterChain for path "/v1/ekstern/'**" (høyest pri)
 * - SecurityFilterChain for øvrige pather (lavest pri)
 * En får kun match mot 1 av disse.
 *
 * EnableWebSecurity slår på Spring Security.
 * EnableMethodSecurity gir oss mulighet til å bruke Secured-annotasjoner på metodene.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
class WebSecurityConfig @Autowired constructor(
    private val environment: Environment,
    private val jwtVerifier: JWTVerifier
) {
    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    /**
     * Sikkerhetskonfigurasjon for path "/v1/ekstern/'**".
     * Har høyest prioritet (lavest Order).
     *
     * Requestene må ha tilstedeværelse av Basic Authorization-header med gyldig apiKey.
     */
    @Order(SecurityConstants.ORDER_OF_API_KEY_FILTER_CHAIN)
    @Bean
    fun apiKeySecurityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        // Request-matcher for filter-kjede: Kun for denne pathen denne SecurityFilterChain vil brukes
        return httpSecurity
            .securityMatcher("/v1/ekstern/**")
            .addFilterAfter(apiKeyFilter(), AbstractPreAuthenticatedProcessingFilter::class.java)
            // Slår av CSRF
            .csrf { it.disable() }
            // Ingen sesjon vil opprettes eller brukes av Spring Security
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // Krever bare at alle requester er authenticated (ingen rolle)
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .logout { it.disable() }
            .build()
    }

    /**
     * Sikkerhetskonfigurasjon for generell path "/'**".
     * Har lavere prioritet enn apiKeySecurityFilterChain (høyest Order).
     *
     * Requestene må ha tilstedeværelse av Bearer Authorization-header med gyldig jwt.
     * Bruker foreløpig en hardkodet liste over brukere som kan autentisere seg.
     */
    @Order(SecurityConstants.ORDER_OF_JWT_FILTER_CHAIN)
    @Bean
    fun jwtSecurityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        // Request-matcher for filter-kjede: Alle pather skal inn i jwt-filter
        return httpSecurity.securityMatcher("/**")
            // Skal tillate stiene under (anbefalt måte fra Spring Security 5.7 for å ignore)
            .authorizeHttpRequests {
                // Er åpne for alle
                it.requestMatchers("/actuator/health/**", "/actuator/prometheus", "/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()

                // Alle øvrige requests må ha NIBAS som authority som er satt av filteres om man har tilgang
                it.anyRequest().hasAuthority("NIBAS")
            }
            .addFilterAfter(jwtRequestFilter(), AbstractPreAuthenticatedProcessingFilter::class.java)
            // Slår av CSRF
            .csrf { it.disable() }
            // Ingen sesjon vil opprettes eller brukes av Spring Security
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .logout { it.disable() } // Gjøres av ID-porten
            .build()
    }

    private fun apiKeyFilter(): OncePerRequestFilter {
        LOGGER.info("Creating api key request filter.")
        return if (shouldRunWithSecurityOff()) {
            LOGGER.warn("Startet applikasjonen med API-KEY-FILTER skrudd av (security off)")
            SecurityOffAuthenticationFilter()
        } else {
            ApiKeyAuthenticationFilter({ environment.getRequiredProperty("api.key.matrikkel") })
        }

    }

    private fun jwtRequestFilter(): OncePerRequestFilter {
        LOGGER.info("Creating jwt request filter.")
        return if (shouldRunWithSecurityOff()) {
            LOGGER.warn("Startet applikasjonen med JWT-REQUEST-FILTER skrudd av (security off)")
            SecurityOffAuthenticationFilter()
        } else {
            UserBearerTokenAuthenticationFilter(jwtVerifier, environment)
        }
    }

    private fun shouldRunWithSecurityOff(): Boolean {
        return environment.activeProfiles.contains("security-off") && !environment.activeProfiles.contains("production")
    }
}

/**
 * Filter som autentiserer innkommende requests som inneholder gyldig api key. Sender api-nøkkel inn som en supplier, da er det først hvis en ønsker å bruke
 * filteret at verdien vil resolves.
 */
class ApiKeyAuthenticationFilter(
    private val validApiKeySupplier: Supplier<String>,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        logger.debug("Traff filter $this for request: ${request.servletPath}")
        val authorizationHeader = request.getHeader("Authorization")
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Basic ")) {
            val apiKey = authorizationHeader.substring(6)
            if (StringUtils.hasText(apiKey) && apiKey == validApiKeySupplier.get()) {
                val authentication = ApiKeyAuthenticationToken(apiKey)
                authentication.isAuthenticated = true
                SecurityContextHolder.getContext().authentication = authentication
                logger.debug("Authenticated api key-user.")
            }
        }
        filterChain.doFilter(request, response)
    }
}
class UserBearerTokenAuthenticationFilter(
    private val jwtVerifier: JWTVerifier,
    private val environment: Environment
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        logger.debug("Traff filter $this for request: ${request.servletPath}")
        val token = getTokenStringFromRequest(request)
        if (!token.isNullOrBlank()) {
            try {
                val signedJWT = SignedJWT.parse(token)
                if (signedJWT != null && jwtVerifier.verifyJwt(signedJWT)) {
                    val authentication = createAuthenticationFrom(signedJWT, request)
                    SecurityContextHolder.getContext().authentication = authentication
                    logger.info("Authenticated user ${PidMaskerer.getMaskedName(authentication.name)},  with authorities: ${grantedAuthoritiesString(authentication.authorities)}")
                }
            } catch (e: Exception) {
                logger.error("Error while checking the bearer-token for the request", e)
            }
        }
        filterChain.doFilter(request, response)
    }


    private fun createAuthenticationFrom(jwt: JWT, request: HttpServletRequest): Authentication {
        val pid = jwt.jwtClaimsSet.subject
        val authorities = getAuthoritiesForUsers(pid)
        return SimpleAuthenticationToken(
            user = pid,
            credentials = jwt.serialize(),
            authorities = authorities
        ).apply {
            details = WebAuthenticationDetailsSource().buildDetails(request)
        }
    }

    private fun grantedAuthoritiesString(authorities: Collection<GrantedAuthority>) =
        "[${authorities.joinToString(",") { grantedAuthority -> grantedAuthority.authority }}]"

    private fun getTokenStringFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (!bearerToken.isNullOrEmpty() && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }

    private fun getAuthoritiesForUsers(pid: String): List<GrantedAuthority> {
        val hashedPid = pid.md5()
        val authorizedUsers = (environment.getProperty("nibas.authorized.users") ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return if (authorizedUsers.contains(hashedPid)) listOf(SimpleGrantedAuthority("NIBAS")) else emptyList()
    }
}

/**
 * En Authentication-implementasjon for vellykket autentisering for et api-key-kall.
 */
class ApiKeyAuthenticationToken(private val apiKey: String) : AbstractAuthenticationToken(emptyList()) {
    override fun getCredentials(): Any {
        return apiKey
    }

    // Vi har ikke noe fornuftig principal/brukernavn her nå
    override fun getPrincipal(): Any {
        return "api-key-user"
    }
}

/**
 * If dummy-filter som setter en user til DUMMY om sikkerhet er skrudd av
 */
class SecurityOffAuthenticationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        logger.debug("Traff filter $this for request: ${request.servletPath}")
        SecurityContextHolder.getContext().authentication = SimpleAuthenticationToken(
            user = "DUMMY",
            credentials = "DUMMY",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )
        filterChain.doFilter(request, response)
    }
}

class SimpleAuthenticationToken(
    private val user: String,
    private val credentials: Any,
    authorities: Collection<GrantedAuthority>
) : AbstractAuthenticationToken(authorities) {
    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any {
        return credentials
    }

    override fun getPrincipal(): Any {
        return user
    }
}
