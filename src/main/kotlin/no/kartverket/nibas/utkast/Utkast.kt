package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.UtkastApi
import no.kartverket.nibas.api.v1.request.UtkastRequest
import no.kartverket.nibas.api.v1.response.AuditInfoResponse
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.response.UtkastResponse
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastEndringstype
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.unwrap
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class UtkastController(private val utkastService: UtkastService) : UtkastApi {

    override fun opprettUtkast(utkastRequest: UtkastRequest): UtkastOpprettetResponse {
        return UtkastOpprettetResponse(id = utkastService.opprettUtkast(utkastRequest))
    }

    override fun hentUtkast(id: String): UtkastResponse {
        return utkastService.hentUtkast(id)?.toUtkastResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

}

@Transactional
@Service
class UtkastService(private val utkastRepository: UtkastRepository) {

    fun opprettUtkast(utkastRequest: UtkastRequest): String {
        val savedUtkast = utkastRepository.save(utkastRequest.createUtkast())
        return savedUtkast.id
    }

    @Transactional(readOnly = true)
    fun hentUtkast(id: String): Utkast? {
        return utkastRepository.findById(id).unwrap()
    }

}

private fun UtkastRequest.createUtkast(): Utkast {
    return UtkastBuilder.builder(this.navn, UtkastEndringstype.fromValue(this.endringstype))
        .withGyldigFra(this.gyldigFra)
        .withNasjonRequests(this.nasjonsendringer)
        .withFylkeRequests(this.fylkesendringer)
        .withKommuneRequests(this.kommuneendringer)
        .withGrunnkretsRequests(this.grunnkretsendringer)
        .withStemmekretsRequests(this.stemmekretsendringer)
        .withEndredeFeatures(this.endredeFeatures)
        .build()
}

private fun Utkast.toUtkastResponse(): UtkastResponse {
    val metadataEndringer = this.operasjoner.metadataEndringer
    return UtkastResponse(
        id = this.id,
        navn = this.navn,
        endringstype = this.endringstype.value,
        status = this.status.value,
        gyldigFra = this.gyldigFra,
        nasjonsendringer = metadataEndringer.nasjonsendringer,
        fylkesendringer = metadataEndringer.fylkesendringer,
        kommuneendringer = metadataEndringer.kommuneendringer,
        grunnkretsendringer = metadataEndringer.grunnkretsendringer,
        stemmekretsendringer = metadataEndringer.stemmekretsendringer,
        endredeFeatures = this.operasjoner.grensejusteringer.featureCollections,
        opprettetDato = this.opprettetDato,
        auditInfoResponse = this.auditInfo.toAuditInfoResponse()
    )
}

fun AuditInfo.toAuditInfoResponse(): AuditInfoResponse = AuditInfoResponse(this.oppdateringsdato, this.endretAv!!)

