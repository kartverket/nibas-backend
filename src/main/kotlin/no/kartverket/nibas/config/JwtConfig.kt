package no.kartverket.nibas.config

import com.nimbusds.jose.jwk.JWKMatcher
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.net.URI
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Configuration
class JwtConfig(
    environment: Environment
) {
    private val ttl = TimeUnit.HOURS.toMillis(10)
    private val outageTtl = TimeUnit.HOURS.toMillis(24)
    private val timeout = TimeUnit.SECONDS.toMillis(10)
    private val url = environment.getRequiredProperty("nibas.idporten.url")
    private val jwksPath = environment.getProperty("nibas.idporten.jwks-path", "/jwks.json")
    private val logger: Logger = LoggerFactory.getLogger(JwtConfig::class.java)

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val jwkUrl = "$url$jwksPath"
        logger.info("===============================================")
        logger.info("*** CONFIGURING JWT VERIFIER ***")
        logger.info("ID-porten URL: $url")
        logger.info("JWK path: $jwksPath")
        logger.info("Full JWK URL: $jwkUrl")
        logger.info("===============================================")
        return JWKSourceBuilder
            .create<SecurityContext>(URI.create(jwkUrl).toURL())
            .cache(ttl, timeout)
            .outageTolerant(outageTtl)
            .build()
    }

    @Bean
    fun jwkSelector(): JWKSelector =
        JWKSelector(
            JWKMatcher.Builder()
                .keyType(KeyType.RSA)
                .keyUse(KeyUse.SIGNATURE)
                .build()
        )

}
