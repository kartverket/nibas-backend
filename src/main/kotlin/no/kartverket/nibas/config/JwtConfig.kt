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

@Configuration
class JwtConfig(
    environment: Environment
) {
    private val ttl = TimeUnit.HOURS.toMillis(10)
    private val outageTtl = TimeUnit.HOURS.toMillis(24)
    private val timeout = TimeUnit.SECONDS.toMillis(10)
    private val url = environment.getRequiredProperty("nibas.idporten.url")

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> =
        JWKSourceBuilder
            .create<SecurityContext>(URI.create("$url/jwks.json").toURL())
            .cache(ttl, timeout)
            .outageTolerant(outageTtl)
            .build()

    @Bean
    fun jwkSelector(): JWKSelector =
        JWKSelector(
            JWKMatcher.Builder()
                .keyType(KeyType.RSA)
                .keyUse(KeyUse.SIGNATURE)
                .build()
        )

}
