package no.kartverket.nibas.config

import no.kartverket.aut.lib.jwt.impl.DefaultSecurityContextUpdater
import no.kartverket.aut.lib.jwt.impl.SecurityContextUpdater
import no.kartverket.aut.lib.security.DefaultWebSecurityConfig.Companion.PRODUCTION_PROFILE_STRING
import no.kartverket.aut.lib.security.DefaultWebSecurityConfig.Companion.SECURITY_OFF_PROFILE_STRING
import no.kartverket.aut.lib.security.hasActiveProfile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.util.function.Supplier
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Sikkerhetskonfigurasjon for path "/v1/ekstern/matrikkel/'**".
 * Disse requestene krever Basic Auth med apiKey tilstede i request-header i motsetning til WebSecurityConfig som krever tilstedeværelse av jwt. Denne
 * Configuration har lavere Order for at requester først skal matches mot denne, så blir WebSecurityConfig fallback for alle andre.
 */
@EnableWebSecurity
@Configuration
@Order(SecurityConstants.ORDER_OF_API_KEY_FILTER_CHAIN)
class ApiKeyWebSecurityConfig(private val environment: Environment) {

    private val logger: Logger = LoggerFactory.getLogger(ApiKeyWebSecurityConfig::class.java)

    @Bean
    fun securityFilterChainApiKey(httpSecurity: HttpSecurity): SecurityFilterChain {
        if (startedWithSecurityOff()) {
            if (inProduction()) {
                throw RuntimeException("Tjener er feilkonfigurert. Kan ikke starte med profil $SECURITY_OFF_PROFILE_STRING i prod.")
            } else { //Tillater det i andre miljøer
                logger.warn("\n\n\n---------\nOBS!! Started app with SECURITY SWITCHED OFF!\n---------\n\n")
                return httpSecurity
                    .csrf().disable()
                    .build()
            }
        } else {
            // Request-matcher for filter-kjede: Kun for denne pathen denne SecurityFilterChain vil brukes
            return httpSecurity.antMatcher("/v1/ekstern/matrikkel/**")
                .addFilterAfter(apiKeyFilter(), AbstractPreAuthenticatedProcessingFilter::class.java)
                // Defaulter til 401 Unauthorized dersom det kastes exception i filterkjeden
                .exceptionHandling().authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                // Slår av CSRF
                .csrf().disable()
                // Ingen sesjon vil opprettes eller brukes av Spring Security
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // Krever bare at alle requester er authenticated (ingen rolle)
                .authorizeRequests().anyRequest().authenticated()
                .and()
                .build()
        }
    }

    // Filtere skal være ukjent for containeren så de bør ikke opprettes som Bean. Ref: https://spring.io/guides/topicals/spring-security-architecture#web-security
    fun apiKeyFilter(): ApiKeyAuthenticationFilter {
        return ApiKeyAuthenticationFilter({ environment.getRequiredProperty("api.key.matrikkel") })
    }

    fun inProduction() = environment.hasActiveProfile(PRODUCTION_PROFILE_STRING)
    fun startedWithSecurityOff() = environment.hasActiveProfile(SECURITY_OFF_PROFILE_STRING)

}

/**
 * Filter som autentiserer innkommende requests som inneholder gyldig api key. Sender api-nøkkel inn som en supplier, da er det først hvis en ønsker å bruke
 * filteret at verdien vil resolves.
 */
class ApiKeyAuthenticationFilter(private val validApiKeySupplier: Supplier<String>,
                                 private val securityContextUpdater: SecurityContextUpdater = DefaultSecurityContextUpdater())
    : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
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

class ApiKeyAuthenticationToken(private val apiKey: String) : AbstractAuthenticationToken(emptyList()) {
    override fun getCredentials(): Any {
        return apiKey
    }

    // Vi har ikke noe fornuftig principal/brukernavn her nå
    override fun getPrincipal(): Any {
        return "api-key-user"
    }
}


