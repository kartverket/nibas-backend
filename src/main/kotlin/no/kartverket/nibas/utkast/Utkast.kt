package no.kartverket.nibas.utkast
import no.kartverket.nibas.api.v1.UtkastApi
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.ConflictResolved
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.api.v1.response.AuditInfoResponse
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.response.UtkastRef
import no.kartverket.nibas.api.v1.response.UtkastResponse
import no.kartverket.nibas.common.FeilVedOppdatering
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
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
import java.time.LocalDate


@RestController
class UtkastController(
    private val utkastService: UtkastService,
    private val hrefProvider: HrefProvider
) : UtkastApi {

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
class UtkastService(
    private val utkastRepository: UtkastRepository,
    private val nasjonService: NasjonService,
    private val fylkeService: FylkeService,
    private val kommuneService: KommuneService,
    private val grunnkretsService: GrunnkretsService,
    private val stemmekretsService: StemmekretsService,
    private val grenseService: GrenseService,
    private val utkastValidatorService: UtkastValidatorService
) {

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
            eksisterendeUtkast.auditInfo.typeEndring = Endringstype.fromValue(request.endringstype)
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

        when (val utkastValideringResult = utkastValidatorService.validateUtkastKlartForPublisering(utkast)) {
            is UtkastValidering.UgyldigUtkast -> throw utkastValideringResult.feil
            is UtkastValidering.KlartForPublisering -> {
                logger.debug(utkast.publiseringsInfo())

                utkast.operasjoner?.let { operasjoner ->
                    val metadata: Metadataendringer = operasjoner.metadataendringer
                    val grenseendringer = operasjoner.grenseendringer

                    metadata.nasjonsendringer.forEach { nasjonService.oppdaterNasjon(Lokalid(it.key), it.value) }
                    metadata.fylkesendringer.forEach { fylkeService.oppdaterFylke(Lokalid(it.key), it.value) }
                    metadata.kommuneendringer.forEach { kommuneService.oppdaterKommune(Lokalid(it.key), it.value) }

                    publiserGrunnkretsEndringer(metadata.grunnkretsendringer, utkast)
                    publiserStemmekretsEndringer(metadata.stemmekretsendringer, utkast)

                    val endredeFeatures = grenseendringer.endredeFeatures

                    if (endredeFeatures.isNotEmpty()) {
                        grenseService.lagreGrenser(FeatureCollection(features = endredeFeatures.map { it.value }))
                    }

                    operasjoner.stemmekretsSammenslaaingsendring?.let { stemmekretsendring ->
                        stemmekretsService.slaaSammenStemmekretser(
                            viderefortStemmekretsId = lagIdentifikator(lokalId = stemmekretsendring.viderefoertStemmekrets, gyldigDato = utkast.gyldigFra),
                            stemmekretserTilSammenSlaaing = stemmekretsendring.stemmekretserTilSammenslaaing.map { lagIdentifikator(lokalId = it, gyldigDato = utkast.gyldigFra) },
                            viderefortNavn = stemmekretsendring.stemmekretsNavn,
                            viderefortNummer = stemmekretsendring.stemmekretsNummer,
                            sammenslaaingsDato = utkast.gyldigFra,
                            endringstype = utkast.auditInfo.typeEndring
                        )
                    }
                }

                utkast.settPublisert()
            }
        }
    }

    private fun publiserGrunnkretsEndringer(grunnkretsendringer: Map<String, GrunnkretsRequest>, utkast: Utkast) {
        grunnkretsendringer.forEach { grunnkretsEndring ->
            grunnkretsService.oppdaterGrunnkrets(Lokalid(grunnkretsEndring.key),
                GrunnkretsRequestWrapper(
                    grunnkretsRequest = grunnkretsEndring.value,
                    gyldigFra = utkast.gyldigFra,
                    endringstype = utkast.auditInfo.typeEndring
                )
            )
        }
    }

    private fun publiserStemmekretsEndringer(stemmekretsendringer: Map<String, StemmekretsRequest>, utkast: Utkast) {
        stemmekretsendringer.forEach { stemmekretsEndring ->
            stemmekretsService.oppdaterStemmekrets(
                lokalId = Lokalid(stemmekretsEndring.key),
                gyldigFra = utkast.gyldigFra,
                oppdatertStemmekrets = stemmekretsEndring.value,
                endringstype = utkast.auditInfo.typeEndring
            )
        }
    }

    private fun lagIdentifikator(lokalId: String, gyldigDato: LocalDate): Identifikator =
        Identifikator(lokalid = Lokalid(lokalId), gyldighetsdato = gyldigDato)

    /**
     * Setter utkastet til PUBLISERT hvis utføringen av requestene som er resolved går bra.
     */
    fun resolveConflictForFramtidigeVersjoner(utkastId: String, conflictResolved: ConflictResolved) {
        val utkast = hentUtkast(utkastId) ?: throw NotFoundException("Utkast ble ikke funnet for id: $utkastId.")

        conflictResolved.grunnkretsRequests.forEach {
            val lokalid = it.grunnkretsRequest.identifikasjon.lokalid
            if (utkastValidatorService.datoErEtterSisteGyldigFraDatoForGrunnkrets(lokalid = lokalid, dato = it.gyldigFra)) {
                throw modifisertGyldigFraException(utkastId = utkast.id, lokalId = lokalid, type = "Grunnkrets")
            }

            grunnkretsService.oppdaterGrunnkrets(
                conflictResolved.lokalid, GrunnkretsRequestWrapper(
                grunnkretsRequest = it.grunnkretsRequest,
                gyldigFra = it.gyldigFra,
                endringstype = Endringstype.fromValue(it.endringstype),
            )
            )
        }

        conflictResolved.stemmekretsRequests.forEach {
            val lokalId = it.stemmekretsRequest.identifikasjon.lokalid
            if (utkastValidatorService.datoErEtterSisteGyldigFraDatoForStemmekrets(lokalid = lokalId, dato = it.gyldigFra)) {
                throw modifisertGyldigFraException(utkastId = utkast.id, lokalId = lokalId, type = "Stemmekrets")
            }

            stemmekretsService.oppdaterStemmekrets(
                lokalId = conflictResolved.lokalid,
                gyldigFra = it.gyldigFra,
                endringstype = Endringstype.fromValue(it.endringstype),
                oppdatertStemmekrets = it.stemmekretsRequest
            )
        }

        utkast.settPublisert()
    }

    private fun modifisertGyldigFraException(utkastId: String, lokalId: String, type: String) =
        BadRequestException("Kan ikke publisere utkast med id: $utkastId. GyldigFra-feltet på en av endringene i utkastet er etter siste gyldig fra dato for kretsen. " +
            "Dette kan tyde på at GyldigFra-feltet ble modifisert under konflikt-håndteringen. Feilen oppsto på $type med lokalId: $lokalId")
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

private fun OpprettUtkastRequest.createUtkast(): Utkast =
    UtkastBuilder.builder(this.navn, Endringstype.fromValue(this.endringstype))
        .withOperasjoner(this.operasjoner)
        .build()

private fun Utkast.toUtkastResponse(): UtkastResponse = UtkastResponse(
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

private fun Utkast.toUtkastRef(hrefProvider: HrefProvider): UtkastRef = UtkastRef(
    id = id,
    navn = navn,
    href = hrefProvider.getHref(id)
)

fun AuditInfo.toAuditInfoResponse(): AuditInfoResponse = AuditInfoResponse(this.endretdato)

