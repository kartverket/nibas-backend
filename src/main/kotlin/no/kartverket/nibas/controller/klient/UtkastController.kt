package no.kartverket.nibas.controller.klient

import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.klient.UtkastApi
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.request.PubliserUtkastRequest
import no.kartverket.nibas.api.v1.response.AuditInfoResponse
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.response.UtkastRef
import no.kartverket.nibas.api.v1.response.UtkastResponse
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.service.utkast.UtkastService
import no.kartverket.nibas.sporing.AuditInfo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class UtkastController(
    private val utkastService: UtkastService,
    private val hrefProvider: HrefProvider,
    private val currentTime: CurrentTime
) : UtkastApi {

    override fun opprettUtkast(
        opprettUtkastRequest: OpprettUtkastRequest
    ): UtkastOpprettetResponse {
        return UtkastOpprettetResponse(id = utkastService.opprettUtkast(opprettUtkastRequest))
    }

    override fun hentUtkast(id: String): UtkastResponse {
        return utkastService.hentUtkast(id)?.toUtkastResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    override fun oppdaterUtkast(id: String, utkast: OppdaterUtkastRequest): UtkastResponse {
        return utkastService.oppdaterUtkast(id, utkast)?.toUtkastResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    override fun forkastUtkast(id: String) {
        utkastService.forkastUtkast(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    override fun publiserUtkast(id: String, body: PubliserUtkastRequest?) {
        utkastService.publiserUtkast(id, body?.publiseringsdato ?: currentTime.asLocalDate())
    }

    override fun hentUtkastIStatusOpprettet(): List<UtkastRef> {
        return utkastService
            .hentUtkastIStatusOpprettet()
            .map { it.toUtkastRef(hrefProvider) }
    }
}

private fun Utkast.toUtkastResponse(): UtkastResponse =
    UtkastResponse(
        id = this.id,
        navn = this.navn,
        endringstype = this.auditInfo.typeEndring.value,
        status = this.status.value,
        gyldigFra = this.gyldigFra,
        operasjoner = this.operasjoner ?: Operasjoner(),
        opprettetDato = this.opprettetDato,
        auditInfoResponse = this.auditInfo.toAuditInfoResponse(),
        version = this.version
    )

private fun Utkast.toUtkastRef(hrefProvider: HrefProvider): UtkastRef =
    UtkastRef(
        id = id,
        navn = navn,
        href = hrefProvider.getHref(id),
        opprettetDato = opprettetDato
    )

fun AuditInfo.toAuditInfoResponse(): AuditInfoResponse = AuditInfoResponse(this.endretdato)
