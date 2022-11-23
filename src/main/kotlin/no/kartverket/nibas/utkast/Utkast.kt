package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.UtkastApi
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.ConflictResolved
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.response.AuditInfoResponse
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.response.UtkastRef
import no.kartverket.nibas.api.v1.response.UtkastResponse
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastEndringstype
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.inndelinger.FylkeService
import no.kartverket.nibas.inndelinger.GrenseService
import no.kartverket.nibas.inndelinger.GrunnkretsRequestWrapper
import no.kartverket.nibas.inndelinger.GrunnkretsService
import no.kartverket.nibas.inndelinger.KommuneService
import no.kartverket.nibas.inndelinger.NasjonService
import no.kartverket.nibas.inndelinger.StemmekretsService
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.sporing.AuditInfo
import no.kartverket.nibas.unwrap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
class UtkastController(private val utkastService: UtkastService,
                       private val hrefProvider: HrefProvider) : UtkastApi {

    override fun opprettUtkast(opprettUtkastRequest: OpprettUtkastRequest): UtkastOpprettetResponse {
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

    override fun publiserUtkast(id: String) {
        utkastService.publiserUtkast(id)
    }

    override fun hentUtkastIStatusOpprettet(): List<UtkastRef> {
        return utkastService.hentUtkastIStatusOpprettet().map { it.toUtkastRef(hrefProvider) }.sortedBy { it.navn }
    }

    override fun resolveConflictForFramtidigeVersjoner(id: String, conflictResolved: ConflictResolved) {
        utkastService.resolveConflictForFramtidigeVersjoner(id, conflictResolved)
    }

}

@Transactional
@Service
class UtkastService(private val utkastRepository: UtkastRepository,
                    private val nasjonService: NasjonService,
                    private val fylkeService: FylkeService,
                    private val kommuneService: KommuneService,
                    private val grunnkretsService: GrunnkretsService,
                    private val stemmekretsService: StemmekretsService,
                    private val grenseService: GrenseService) {

    private val logger: Logger = LoggerFactory.getLogger(UtkastService::class.java)

    fun opprettUtkast(opprettUtkastRequest: OpprettUtkastRequest): String =
        utkastRepository.save(opprettUtkastRequest.createUtkast()).id

    @Transactional(readOnly = true)
    fun hentUtkast(id: String): Utkast? = utkastRepository.findById(id).unwrap()

    @Transactional(readOnly = true)
    fun hentUtkastIStatusOpprettet(): List<Utkast> {
        return utkastRepository.findAllByStatus(UtkastStatus.OPPRETTET)
    }

    fun oppdaterUtkast(id: String, request: OppdaterUtkastRequest): Utkast? {
        return hentUtkast(id)?.let { eksisterendeUtkast ->
            verifiserOptimistiskLaas(request.version, eksisterendeUtkast)?.let { throw OptimistiskLaasException(it) }

            eksisterendeUtkast.navn = request.navn
            eksisterendeUtkast.endringstype = UtkastEndringstype.fromValue(request.endringstype)
            eksisterendeUtkast.gyldigFra = request.gyldigFra!!
            eksisterendeUtkast.operasjoner = request.operasjoner
            utkastRepository.save(eksisterendeUtkast)
        }
    }

    fun forkastUtkast(id: String): Utkast? =
        hentUtkast(id)?.apply {
            status = UtkastStatus.FORKASTET
            operasjoner = null
        }

    fun publiserUtkast(utkastId: String) {
        val utkast = hentUtkast(utkastId) ?: throw NotFoundException("Utkast ble ikke funnet for id: $utkastId.")
        validateUtkastKlartForPublisering(utkast)

        logger.debug(utkast.publiseringsInfo())

        utkast.operasjoner?.let { operasjoner ->
            val metadata: Metadataendringer = operasjoner.metadataendringer
            val grenseendringer = operasjoner.grenseendringer

            metadata.nasjonsendringer.forEach { nasjonService.oppdaterNasjon(it.key, it.value) }
            metadata.fylkesendringer.forEach { fylkeService.oppdaterFylke(it.key, it.value) }
            metadata.kommuneendringer.forEach { kommuneService.oppdaterKommune(it.key, it.value) }
            metadata.grunnkretsendringer.forEach {
                grunnkretsService.oppdaterGrunnkrets(Lokalid(it.key),
                    GrunnkretsRequestWrapper(
                        grunnkretsRequest = it.value,
                        gyldigFra = utkast.gyldigFra,
                        endringstype = utkast.endringstype.name))
            }
            metadata.stemmekretsendringer.forEach { stemmekretsService.oppdaterStemmekrets(it.key, it.value) }

            val endredeFeatures = grenseendringer.endredeFeatures

            if (endredeFeatures.isNotEmpty()) {
                grenseService.lagreGrenser(FeatureCollection(features = endredeFeatures.map { it.value }))
            }
        }

        utkast.settPublisert()
    }

    /**
     * Setter utkastet til PUBLISERT hvis utføringen av requestene som er resolved går bra.
     */
    fun resolveConflictForFramtidigeVersjoner(utkastId: String, conflictResolved: ConflictResolved) {
        val utkast = hentUtkast(utkastId) ?: throw NotFoundException("Utkast ble ikke funnet for id: $utkastId.")

        conflictResolved.grunnkretsRequests.forEach {
            grunnkretsService.oppdaterGrunnkrets(conflictResolved.lokalid, GrunnkretsRequestWrapper(
                grunnkretsRequest = it.grunnkretsRequest,
                gyldigFra = it.gyldigFra,
                endringstype = it.endringstype,
                resolved = true))
        }
        utkast.settPublisert()
    }

    private fun validateUtkastKlartForPublisering(utkast: Utkast) {
        if (utkast.status != UtkastStatus.OPPRETTET) {
            throw BadRequestException("Kan ikke publisere et utkast i status ${utkast.status}.")
        }

        val utkastIkkeHarEndringer = utkast.operasjoner?.harIkkeEndringer() ?: true
        if (utkastIkkeHarEndringer) {
            throw BadRequestException("Kan ikke publisere et utkast uten endringer.")
        }
    }
}

private fun Utkast.publiseringsInfo(): String =
    StringBuilder("Publiserer utkast '${this.navn}' (id: ${this.id}): ")
        .append(this.operasjoner?.metadataendringer?.logFriendlyString() ?: "")
        .append(this.operasjoner?.grenseendringer?.logFriendlyString() ?: "")
        .append("\n")
        .toString()

private fun Metadataendringer.logFriendlyString(): String {
    val logBuilder = StringBuilder("")
    val nasjon = this.nasjonsendringer
    val fylke = this.fylkesendringer
    val kommune = this.kommuneendringer
    val grunnkrets = this.grunnkretsendringer
    val stemmekrets = this.stemmekretsendringer
    val newLine = "\n -> "
    if (nasjon.isNotEmpty()) {
        logBuilder.append(newLine).append("Nasjonsendringer: ${nasjon.size}")
    }
    if (fylke.isNotEmpty()) {
        logBuilder.append(newLine).append("Fylkesendringer: ${fylke.size}")
    }
    if (kommune.isNotEmpty()) {
        logBuilder.append(newLine).append("Kommuneendringer: ${kommune.size}")
    }
    if (grunnkrets.isNotEmpty()) {
        logBuilder.append(newLine).append("Grunnkretsendringer: ${grunnkrets.size}")
    }
    if (stemmekrets.isNotEmpty()) {
        logBuilder.append(newLine).append("Stemmekretsendringer: ${stemmekrets.size}")
    }
    return logBuilder.toString()
}

private fun Grenseendringer.logFriendlyString(): String {
    val logBuilder = StringBuilder("")
    val newLine = "\n -> "
    val endredeFeatures = this.endredeFeatures
    if (endredeFeatures.isNotEmpty()) {
        logBuilder.append(newLine).append("Grenseendringer: ${endredeFeatures.size}")
    }
    return logBuilder.toString()
}

private fun Operasjoner.harEndringer(): Boolean {
    val metadata = this.metadataendringer
    return metadata.nasjonsendringer.isNotEmpty() ||
        metadata.fylkesendringer.isNotEmpty() ||
        metadata.kommuneendringer.isNotEmpty() ||
        metadata.grunnkretsendringer.isNotEmpty() ||
        metadata.stemmekretsendringer.isNotEmpty() ||
        this.grenseendringer.endredeFeatures.isNotEmpty()
}

private fun Operasjoner.harIkkeEndringer(): Boolean = !this.harEndringer()

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
    operasjoner = this.operasjoner ?: Operasjoner(),
    opprettetDato = this.opprettetDato,
    auditInfoResponse = this.auditInfo.toAuditInfoResponse(),
    version = this.version
)

private fun Utkast.toUtkastRef(hrefProvider: HrefProvider): UtkastRef = UtkastRef(
    id = id,
    navn = navn,
    href = hrefProvider.getHref(id)
)

fun AuditInfo.toAuditInfoResponse(): AuditInfoResponse = AuditInfoResponse(this.oppdateringsdato, this.endretAv!!)

