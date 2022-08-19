package no.kartverket.nibas.config

import no.kartverket.aut.lib.AuthorizerPlugin
import no.kartverket.aut.lib.jwk.JwksRetriever
import no.kartverket.aut.lib.security.DefaultWebSecurityConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


/**
 * Sikkerhetskonfigurasjon for sikring av endepunkter. Matcher generell path "/'**".
 * Bruker foreløpig en hardkodet liste over brukere som kan autentisere seg.
 * Dersom man ikke vil ha på sikkerhet kan man kjøre med spring-profilen security-off.
 */
@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true)
@Order(SecurityConstants.ORDER_OF_JWT_FILTER_CHAIN)
class WebSecurityConfig @Autowired constructor(private val environment: Environment) : DefaultWebSecurityConfig(environment) {

    /**
     * Fra Spring Security 5.7 skal HttpSecurity defineres ved å registrere en SecurityFilterChain-bean.
     */
    @Bean
    fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        if (startedWithSecurityOff()) {
            if (inProduction()) {
                throw RuntimeException("Tjener er feilkonfigurert. Kan ikke starte med profil $SECURITY_OFF_PROFILE_STRING i prod.")
            } else { //Tillater det i andre miljøer
                LOGGER.warn("\n\n\n---------\nOBS!! Started app with SECURITY SWITCHED OFF!\n---------\n\n")
                return httpSecurity
                    .csrf().disable()
                    .build()
            }
        } else {
            // Request-matcher for filter-kjede: Alle pather skal inn i jwt-filter
            return httpSecurity.antMatcher("/**")
                // Skal tillate stiene under (anbefalt måte fra Spring Security 5.7 for å ignore)
                .authorizeHttpRequests {
                    it.antMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/api-docs/**",
                        "/swagger-ui/**")
                        .permitAll()

                    // Alle øvrige requests må ha rolle ROLE_NIBAS
                    it.anyRequest().hasRole("NIBAS")
                }
                .addFilterAfter(jwtRequestFilter(), AbstractPreAuthenticatedProcessingFilter::class.java)
                .exceptionHandling()
                .authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                // Slår av CSRF
                .csrf().disable()
                // Ingen sesjon vil opprettes eller brukes av Spring Security
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .build()
        }
    }

    override fun getAuthorizerPlugin(): AuthorizerPlugin = DummyNibasAuthorizerPlugin()

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

class DummyNibasAuthorizerPlugin : AuthorizerPlugin {
    private val nibasSingletonList = listOf("NIBAS")
    private val authoritiesByPidMap = mapOf(
        "21079408678" to nibasSingletonList,
        "21079408597" to nibasSingletonList,
        "21079408406" to nibasSingletonList,
        "21079408325" to nibasSingletonList,
        "21079408244" to nibasSingletonList,
    )

    override fun getAuthoritiesForPid(pid: String): Collection<GrantedAuthority> {
        if (authoritiesByPidMap.containsKey(pid)) {
            return authoritiesByPidMap[pid]!!.map { role -> SimpleGrantedAuthority("ROLE_$role") }
        }
        return emptyList()
    }

}
