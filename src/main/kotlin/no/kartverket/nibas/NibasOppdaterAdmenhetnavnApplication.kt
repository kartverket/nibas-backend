package no.kartverket.nibas

import no.kartverket.nibas.config.DummyAuthenticationToken
import no.kartverket.nibas.service.administrativenhetnavn.OppdaterAdmenhetnavnService
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class NibasOppdaterAdmenhetnavnApplication(
    val oppdaterAdmenhetnavnService: OppdaterAdmenhetnavnService
) {

    fun run() {
        // Ingen SecurityContext settes automatisk, så vi setter en dummy-bruker.
        SecurityContextHolder.getContext().authentication = DummyAuthenticationToken(
            user = "oppdaterAdmenhetnavnApplication",
            credentials = "oppdaterAdmenhetnavnApplication",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )

        oppdaterAdmenhetnavnService.oppdaterAdministrativenhetnavn()
    }
}
