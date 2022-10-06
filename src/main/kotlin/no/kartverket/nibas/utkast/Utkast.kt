package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.UtkastApi
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
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

    override fun opprettUtkast(opprettUtkastRequest: OpprettUtkastRequest): UtkastOpprettetResponse =
        UtkastOpprettetResponse(id = utkastService.opprettUtkast(opprettUtkastRequest))

    override fun hentUtkast(id: String): UtkastResponse {
        return utkastService.hentUtkast(id)?.toUtkastResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    override fun oppdaterUtkast(id: String, utkast: OppdaterUtkastRequest): UtkastResponse {
        return utkastService.oppdaterUtkast(id, utkast)?.toUtkastResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

}

@Transactional
@Service
class UtkastService(private val utkastRepository: UtkastRepository) {

    fun opprettUtkast(opprettUtkastRequest: OpprettUtkastRequest): String =
        utkastRepository.save(opprettUtkastRequest.createUtkast()).id

    @Transactional(readOnly = true)
    fun hentUtkast(id: String): Utkast? = utkastRepository.findById(id).unwrap()

    fun oppdaterUtkast(id: String, request: OppdaterUtkastRequest): Utkast? {
        return hentUtkast(id)?.let { utkast ->
            utkast.navn = request.navn
            utkast.endringstype = UtkastEndringstype.fromValue(request.endringstype)
            utkast.gyldigFra = request.gyldigFra!!
            utkast.operasjoner = request.operasjoner
            utkastRepository.save(utkast)
        }
    }

}

private fun OpprettUtkastRequest.createUtkast(): Utkast =
    UtkastBuilder.builder(this.navn, UtkastEndringstype.fromValue(this.endringstype))
        .withOperasjoner(this.operasjoner)
        .build()


private fun Utkast.toUtkastResponse(): UtkastResponse = UtkastResponse(
    id = this.id,
    navn = this.navn,
    endringstype = this.endringstype.value,
    status = this.status.value,
    gyldigFra = this.gyldigFra,
    operasjoner = this.operasjoner,
    opprettetDato = this.opprettetDato,
    auditInfoResponse = this.auditInfo.toAuditInfoResponse()
)

fun AuditInfo.toAuditInfoResponse(): AuditInfoResponse = AuditInfoResponse(this.oppdateringsdato, this.endretAv!!)

