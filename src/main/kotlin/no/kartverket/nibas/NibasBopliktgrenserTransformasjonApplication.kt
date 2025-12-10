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
        // Denne applikasjonen kjøres uten av systemet, så ingen http-filtre setter SecurityContext.
        // Vi trenger likevel en autentisert bruker for sporing.
        SecurityContextHolder.getContext().authentication = DummyAuthenticationToken(
            user = "bopliktgrenserapplication",
            credentials = "bopliktgrenserapplication",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )

        bopliktgrenserTransformasjonService.transform()
    }
}

