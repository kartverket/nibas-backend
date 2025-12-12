package no.kartverket.nibas

import no.kartverket.nibas.config.DummyAuthenticationToken
import no.kartverket.nibas.service.bopliktgrensetransformasjon.BopliktgrenserTransformasjonService
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class NibasBopliktgrenserTransformasjonApplication(
    private val bopliktgrenserTransformasjonService: BopliktgrenserTransformasjonService
) {

    fun run() {
        // No SecurityContext set automatically, so set a dummy user
        SecurityContextHolder.getContext().authentication = DummyAuthenticationToken(
            user = "bopliktgrenserapplication",
            credentials = "bopliktgrenserapplication",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )

        bopliktgrenserTransformasjonService.transform()
    }
}

