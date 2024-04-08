package no.kartverket.nibas.service

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.proc.SimpleSecurityContext
import com.nimbusds.jwt.SignedJWT
import no.kartverket.nibas.common.CurrentTime
import org.springframework.stereotype.Service

@Service
class JWTVerifier(
    private val jwkSource: JWKSource<SecurityContext>,
    private val jwkSelector: JWKSelector,
    private val currentTime: CurrentTime
) {
    private val ctx = SimpleSecurityContext()

    fun verifyJwt(token: SignedJWT): Boolean {
        return verifyJwtSignature(token) && verifyJwtNotExpired(token)
    }

    private fun verifyJwtSignature(token: SignedJWT): Boolean {
        val publicKey = jwkSource.get(jwkSelector, ctx).first().toRSAKey()
        val rsaVerifier = RSASSAVerifier(publicKey)
        return token.verify(rsaVerifier)
    }

    private fun verifyJwtNotExpired(token: SignedJWT): Boolean {
        return token.jwtClaimsSet.expirationTime.toInstant().isAfter(currentTime.asInstant())
    }
}
