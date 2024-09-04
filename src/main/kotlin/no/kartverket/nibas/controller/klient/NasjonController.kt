package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.klient.NasjonApi
import no.kartverket.nibas.api.v1.klient.response.NasjonResponse
import no.kartverket.nibas.extensions.flate.toNasjonResponse
import no.kartverket.nibas.service.NasjonService
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController("KlientNasjonController")
class NasjonController(
    val nasjonService: NasjonService
): NasjonApi {
    override fun hentNasjon(gyldighetsdato: LocalDate?): NasjonResponse {
        val gyldighet = gyldighetsdato ?: LocalDate.now()
        val nasjoner = nasjonService.hentAlleNasjoner(gyldighet)
        when (nasjoner.size) {
            1 -> {
                return nasjoner.first().toNasjonResponse()
            }
            else -> {
                throw RuntimeException("Fant ikke nasjon gyldig på dato: $gyldighet")
            }
        }
    }
}
