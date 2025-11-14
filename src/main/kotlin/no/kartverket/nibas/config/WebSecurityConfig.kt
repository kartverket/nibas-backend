package no.kartverket.nibas.config

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
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import java.util.function.Supplier
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
class WebSecurityConfig @Autowired constructor(
    private val environment: Environment
) {
    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Order(SecurityConstants.ORDER_OF_API_KEY_FILTER_CHAIN)
    @Bean
    fun apiKeySecurityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        return httpSecurity
            .securityMatcher("/v1/ekstern/**")
            .addFilterAfter(apiKeyFilter(), AbstractPreAuthenticatedProcessingFilter::class.java)
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .logout { it.disable() }
            .build()
    }

    private fun apiKeyFilter(): OncePerRequestFilter {
        return if (shouldRunWithSecurityOff()) {
            LOGGER.warn("Startet applikasjonen med API-KEY-FILTER skrudd av (security off)")
            SecurityOffAuthenticationFilter()
        } else {
            LOGGER.info("Creating api key filter.")
            ApiKeyAuthenticationFilter({ environment.getRequiredProperty("api.key.matrikkel") })
        }

    }

    @Order(SecurityConstants.ORDER_OF_FORWARDED_AUTH_FILTER_CHAIN)
    @Bean
    fun forwardedAuthenticationSecurityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        return httpSecurity
            .securityMatcher("/**")
            .addFilterAfter(forwardedAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter::class.java)
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .logout { it.disable() }
            .build()
    }

    private fun forwardedAuthenticationFilter(): OncePerRequestFilter {
        return if (shouldRunWithSecurityOff()) {
            LOGGER.warn("Startet applikasjonen med FORWARDED_ATUH_FILTER skrudd av (security off)")
            SecurityOffAuthenticationFilter()
        } else {
            LOGGER.info("Creating forwarded authentication filter.")
            ForwardedAuthenticationFilter()
        }
    }

    private fun shouldRunWithSecurityOff(): Boolean {
        return environment.activeProfiles.contains("localhost") && !environment.activeProfiles.contains("prod")
    }

}

class ForwardedAuthenticationFilter : OncePerRequestFilter() {
    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val username = request.getHeader("X-Auth-Name")
        if (username != null) {
            val authorities: List<GrantedAuthority> = listOf(SimpleGrantedAuthority("NIBAS"))
            // Vi lagrer ikke bearer token fra ztoperator i credentials da det er tryggere å ignorere den
            val authentication = PreAuthenticatedAuthenticationToken(username, null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
            LOGGER.debug("Authenticated forwarded username=$username")
        }
        filterChain.doFilter(request, response)
    }
}

class ApiKeyAuthenticationFilter(
    private val validApiKeySupplier: Supplier<String>,
) : OncePerRequestFilter() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authorizationHeader = request.getHeader("Authorization")
        if (authorizationHeader?.startsWith("Basic ") == true) {
            val apiKey = authorizationHeader.substring(6)
            if (apiKey == validApiKeySupplier.get()) {
                val authentication = ApiKeyAuthenticationToken(apiKey)
                authentication.isAuthenticated = true
                SecurityContextHolder.getContext().authentication = authentication
                LOGGER.debug("Authenticated API key user.")
            }
        }
        filterChain.doFilter(request, response)
    }
}

class ApiKeyAuthenticationToken(private val apiKey: String) : AbstractAuthenticationToken(emptyList()) {
    override fun getCredentials(): Any = apiKey
    override fun getPrincipal(): Any = "api-key-user"
}

/**
 * If dummy-filter som setter en user til DUMMY om sikkerhet er skrudd av (localhost)
 */
class SecurityOffAuthenticationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        logger.debug("Traff filter $this for request: ${request.servletPath}")
        SecurityContextHolder.getContext().authentication = DummyAuthenticationToken(
            user = "DUMMY",
            credentials = "DUMMY",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )
        filterChain.doFilter(request, response)
    }
}

class DummyAuthenticationToken(
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
