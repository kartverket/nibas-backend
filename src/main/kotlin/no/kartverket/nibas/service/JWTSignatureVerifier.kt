package no.kartverket.nibas.service

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKMatcher
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.proc.SimpleSecurityContext
import com.nimbusds.jwt.SignedJWT
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.net.URI
import java.util.concurrent.TimeUnit

@Service
class JWTSignatureVerifier(
    environment: Environment
) {
    private val ttl = TimeUnit.HOURS.toMillis(10)
    private val outageTtl = TimeUnit.HOURS.toMillis(24)
    private val timeout = TimeUnit.SECONDS.toMillis(10)
    private val url = environment.getRequiredProperty("nibas.idporten.url")
    private val ctx = SimpleSecurityContext()

    private val jwkSource = JWKSourceBuilder
        .create<SecurityContext>(URI.create("$url/jwks.json").toURL())
        .cache(ttl, timeout)
        .outageTolerant(outageTtl)
        .build()

    private val keySelector = JWKSelector(
        JWKMatcher.Builder()
            .keyType(KeyType.RSA)
            .keyUse(KeyUse.SIGNATURE)
            .build()
    )

    fun verifyJwtSignature(token: SignedJWT): Boolean {
        val publicKey = jwkSource.get(keySelector, ctx).first().toRSAKey()
        val rsaVerifier = RSASSAVerifier(publicKey)
        return token.verify(rsaVerifier)
    }
}
