package no.kartverket.nibas.config

import no.kartverket.aut.lib.AuthorizerPlugin
import no.kartverket.aut.lib.security.DefaultWebSecurityConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter


/**
 * Sikkerhetskonfigurasjon for sikring av endepunkter. Bruker foreløpig en hardkodet liste over brukere som kan autentisere seg.
 * Dersom man ikke vil ha på sikkerhet kan man kjøre med spring-profilen security-off
 */
@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true)
class WebSecurityConfig @Autowired constructor(environment: Environment) : DefaultWebSecurityConfig(environment) {

    override fun getAuthorizerPlugin(): AuthorizerPlugin = DummyNibasAuthorizerPlugin()

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        if (startedWithSecurityOff()) {
            if (inProduction()) {
                throw RuntimeException("Tjener er feilkonfigurert. Kan ikke starte med profil $SECURITY_OFF_PROFILE_STRING i prod.")
            } else { //Tillater det i andre miljøer
                LOGGER.warn("\n\n\n---------\nOBS!! Started app with SECURITY SWITCHED OFF!\n---------\n\n")
                http.csrf().disable()
            }
        } else {
            http
                .addFilterAfter(jwtRequestFilter(), AbstractPreAuthenticatedProcessingFilter::class.java)
                .exceptionHandling()
                .authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No session will be created or used by spring security
                .and()
                .authorizeRequests()
                .anyRequest()
                .hasRole("NIBAS")
        }
    }

    @Throws(Exception::class)
    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers(
            "/actuator/health",
            "/actuator/info",
            "/api-docs/**",
            "/swagger-ui/**"
        )
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
