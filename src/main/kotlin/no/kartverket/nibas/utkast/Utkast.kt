package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.UtkastApi
import no.kartverket.nibas.api.v1.request.UtkastRequest
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import no.kartverket.nibas.domain.Utkast
import no.kartverket.nibas.domain.UtkastBuilder
import no.kartverket.nibas.repository.UtkastRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController

@RestController
class UtkastController(private val utkastService: UtkastService) : UtkastApi {

    override fun opprettUtkast(utkastRequest: UtkastRequest): UtkastOpprettetResponse {
        return UtkastOpprettetResponse(id = utkastService.opprettUtkast(utkastRequest))
    }

}

@Transactional
@Service
class UtkastService(private val utkastRepository: UtkastRepository) {

    fun opprettUtkast(utkastRequest: UtkastRequest): String {
        val savedUtkast = utkastRepository.save(utkastRequest.createUtkast())
        return savedUtkast.id
    }

}

private fun UtkastRequest.createUtkast(): Utkast {
    return UtkastBuilder.builder(this.navn)
        .withGyldigFra(this.gyldigFra)
        .withEndringstype(this.endringstype)
        .withNasjonRequests(this.nasjonsendringer)
        .withFylkeRequests(this.fylkesendringer)
        .withKommuneRequests(this.kommuneendringer)
        .withGrunnkretsRequests(this.grunnkretsendringer)
        .withStemmekretsRequests(this.stemmekretsendringer)
        .withEndredeFeatures(this.endredeFeatures)
        .build()
}
