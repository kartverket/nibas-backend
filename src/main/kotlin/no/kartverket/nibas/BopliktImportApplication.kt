package no.kartverket.nibas

import no.kartverket.nibas.config.DummyAuthenticationToken
import no.kartverket.nibas.importjob.boplikt.BopliktImportService
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NibasBopliktImportApplication(
    private val bopliktImportService: BopliktImportService
) {


    @Transactional
    fun run() {
        // No SecurityContext set automatically, so set a dummy user
        SecurityContextHolder.getContext().authentication = DummyAuthenticationToken(
            user = "bopliktgrenserapplication",
            credentials = "bopliktgrenserapplication",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )

        bopliktImportService.importFromLocalResource()
    }
}

