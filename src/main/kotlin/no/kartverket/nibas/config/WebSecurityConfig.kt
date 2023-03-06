package no.kartverket.nibas.config

import no.kartverket.aut.lib.AuthorizerPlugin
import no.kartverket.aut.lib.jwk.JwksRetriever
import no.kartverket.aut.lib.jwt.JwtValidator
import no.kartverket.aut.lib.jwt.impl.DefaultSecurityContextUpdater
import no.kartverket.aut.lib.jwt.impl.JwtAuthenticationRequestFilter
import no.kartverket.aut.lib.jwt.impl.SecurityContextUpdater
import no.kartverket.aut.lib.security.DefaultWebSecurityConfig
import no.kartverket.nibas.common.md5
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
 * Dersom man ikke vil ha på sikkerhet kan man kjøre med spring-profilen 'security-off'.
 * EnableWebSecurity slår på Spring Security.
 * EnableMethodSecurity gir oss mulighet til å bruke Secured-annotasjoner på metodene.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
class WebSecurityConfig @Autowired constructor(private val environment: Environment) : DefaultWebSecurityConfig(environment) {

    /**
     * Sikkerhetskonfigurasjon for path "/v1/ekstern/'**".
     * Har høyest prioritet (lavest Order).
     *
     * Requestene må ha tilstedeværelse av Basic Authorization-header med gyldig apiKey.
     */
    @Order(SecurityConstants.ORDER_OF_API_KEY_FILTER_CHAIN)
    @Bean
    fun apiKeySecurityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        if (startedWithSecurityOff()) {
            return filterChainForSecurityOff(httpSecurity)
        } else {
//             Request-matcher for filter-kjede: Kun for denne pathen denne SecurityFilterChain vil brukes
            return httpSecurity
                .securityMatcher("/v1/ekstern/**")
                .addFilterAfter(apiKeyFilter(), AbstractPreAuthenticatedProcessingFilter::class.java)
                // Defaulter til 401 Unauthorized dersom det kastes exception i filterkjeden
                .exceptionHandling().authenticationEntryPoint(UnauthorizedEntryPoint("API_KEY_FILTER_CHAIN"))
                .and()
                // Slår av CSRF
                .csrf().disable()
                // Ingen sesjon vil opprettes eller brukes av Spring Security
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests {
                    // Krever bare at alle requester er authenticated (ingen rolle)
                    it.anyRequest().authenticated()
                }
                .build()
        }
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
        if (startedWithSecurityOff()) {
            return filterChainForSecurityOff(httpSecurity)
        } else {
            // Request-matcher for filter-kjede: Alle pather skal inn i jwt-filter
            return httpSecurity.securityMatcher("/**")
                // Skal tillate stiene under (anbefalt måte fra Spring Security 5.7 for å ignore)
                .authorizeHttpRequests {

                    // Er åpne for alle
                    it.requestMatchers("/actuator/health", "/actuator/info", "/api-docs/**", "/swagger-ui/**").permitAll()

                    // Krever administrator-rolle ROLE_NIBAS_ADMIN
                    it.requestMatchers("/v1/admin/**").hasRole("NIBAS_ADMIN")

                    // Alle øvrige requests må ha rolle ROLE_NIBAS
                    it.anyRequest().hasRole("NIBAS")
                }
                .addFilterAfter(jwtRequestFilter(), AbstractPreAuthenticatedProcessingFilter::class.java)
                // Defaulter til 401 Unauthorized dersom det kastes exception i filterkjeden
                .exceptionHandling().authenticationEntryPoint(UnauthorizedEntryPoint("JWT_FILTER_CHAIN"))
                .and()
                // Slår av CSRF
                .csrf().disable()
                // Ingen sesjon vil opprettes eller brukes av Spring Security
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .build()
        }
    }

    private fun filterChainForSecurityOff(httpSecurity: HttpSecurity): SecurityFilterChain {
        if (inProduction()) {
            throw RuntimeException("Tjener er feilkonfigurert. Kan ikke starte med profil $SECURITY_OFF_PROFILE_STRING i prod.")
        } else { //Tillater det i andre miljøer
            LOGGER.warn("\n\n\n---------\nOBS!! Started app with SECURITY SWITCHED OFF!\n---------\n\n")
            return httpSecurity
                .csrf().disable()
                .build()
        }
    }

    // Filtere skal være ukjent for containeren så de bør ikke opprettes som Bean. Ref: https://spring.io/guides/topicals/spring-security-architecture#web-security
    fun apiKeyFilter(): ApiKeyAuthenticationFilter {
        LOGGER.info("Creating api key request filter.")
        return ApiKeyAuthenticationFilter({ environment.getRequiredProperty("api.key.matrikkel") })
    }

    override fun jwtRequestFilter(): JwtAuthenticationRequestFilter {
        LOGGER.info("Creating jwt request filter.")
        return NibasJwtAuthenticationRequestFilter(jwtValidator(), getAuthorizerPlugin())
    }

    override fun getAuthorizerPlugin(): AuthorizerPlugin {
        fun envPropertyAsList(propName: String) =
            (environment.getProperty(propName) ?: "").split(",").map { it.trim() }

        val authorizedUsers = envPropertyAsList("nibas.authorized.users")
        val authorizedAdminUsers = envPropertyAsList("nibas.authorized.administrators")

        return NibasAuthorizerPlugin(authorizedUsers, authorizedAdminUsers)
    }

    override fun jwksRetriever(): JwksRetriever {
        val mockJwkRetrieval: Boolean? = environment.getProperty("mock.jwk.retrieval", Boolean::class.java)
        return if (!inProduction() && mockJwkRetrieval != null && mockJwkRetrieval) {
            LOGGER.warn("Fetching public keys from mock!")
            JwksRetrieverMock()
        } else {
            super.jwksRetriever()
        }
    }

    override fun getDateTimeToUseForTokenVerification(): ZonedDateTime? {
        val jwtTimestampString = environment.getProperty("mock.jwt.timestamp")
        if (!jwtTimestampString.isNullOrEmpty()) {
            val dateTimeToUse = ZonedDateTime.of(
                LocalDateTime.parse(jwtTimestampString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")),
                ZoneId.systemDefault())
            LOGGER.warn("Mocket tidspunkt som brukes for jwt-verifikasjon: $dateTimeToUse")
            return dateTimeToUse
        }
        return null
    }

}

/**
 * En midlertidig AuthorizerPlugin-implementasjon som gir alle autoriserte brukere NIBAS-rollen.
 */
class NibasAuthorizerPlugin constructor(private val users: List<String>, private val adminUsers: List<String>) : AuthorizerPlugin {

    override fun getAuthoritiesForPid(pid: String): Collection<GrantedAuthority> {
        val hashedPid = pid.md5()
        val grantedRoles = mutableListOf<GrantedAuthority>()
        if (users.contains(hashedPid)) {
            grantedRoles.add(SimpleGrantedAuthority("ROLE_NIBAS"))
        }
        if(adminUsers.contains(hashedPid)){
            grantedRoles.add(SimpleGrantedAuthority("ROLE_NIBAS_ADMIN"))
        }

        return grantedRoles
    }
}

/**
 * AuthenticationEntryPoint som setter status til 401 Unauthorized. Gir mulighet til å logge utfall.
 */
class UnauthorizedEntryPoint(private val nameOfFilterChain: String) : AuthenticationEntryPoint {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        logger.debug("Returnerer 401 Unauthorized for filterkjede '$nameOfFilterChain'")
        response.status = HttpStatus.UNAUTHORIZED.value()
    }

}

/**
 * Tynn wrapper rundt JwtAuthenticationRequestFilter for å ha mulighet til å logge.
 */
class NibasJwtAuthenticationRequestFilter(jwtValidator: JwtValidator, authorizerPlugin: AuthorizerPlugin) :
    JwtAuthenticationRequestFilter(jwtValidator, authorizerPlugin) {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        logger.debug("Traff filter $this for request: ${request.servletPath}")
        super.doFilterInternal(request, response, filterChain)
    }
}

/**
 * Filter som autentiserer innkommende requests som inneholder gyldig api key. Sender api-nøkkel inn som en supplier, da er det først hvis en ønsker å bruke
 * filteret at verdien vil resolves.
 */
class ApiKeyAuthenticationFilter(private val validApiKeySupplier: Supplier<String>,
                                 private val securityContextUpdater: SecurityContextUpdater = DefaultSecurityContextUpdater())
    : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        logger.debug("Traff filter $this for request: ${request.servletPath}")
        val authorizationHeader = request.getHeader("Authorization")
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Basic ")) {
            val apiKey = authorizationHeader.substring(6)
            if (StringUtils.hasText(apiKey) && apiKey == validApiKeySupplier.get()) {
                val authentication = ApiKeyAuthenticationToken(apiKey)
                authentication.isAuthenticated = true
                securityContextUpdater.setAuthentication(authentication)
                logger.debug("Authenticated api key-user.")
            }
        }
        filterChain.doFilter(request, response)
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
