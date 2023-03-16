package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.klient.UtkastApi
import no.kartverket.nibas.api.v1.request.ConflictResolved
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.api.v1.response.AuditInfoResponse
import no.kartverket.nibas.api.v1.response.UtkastOpprettetResponse
import no.kartverket.nibas.api.v1.response.UtkastRef
import no.kartverket.nibas.api.v1.response.UtkastResponse
import no.kartverket.nibas.common.HrefProvider
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.common.ResponseException.NotFoundException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.NasjonService
import no.kartverket.nibas.service.StemmekretsService
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

    override fun publiserUtkast(id: String) {
        utkastService.publiserUtkast(id)
    }

    override fun hentUtkastIStatusOpprettet(): List<UtkastRef> {
        return utkastService
            .hentUtkastIStatusOpprettet()
            .map { it.toUtkastRef(hrefProvider) }
            .sortedBy { it.navn }
    }

    override fun resolveConflictForFramtidigeVersjoner(
        id: String,
        conflictResolved: ConflictResolved
    ) {
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
    private val nibasEventPublisher: NibasEventPublisher,
    private val utkastValidatorService: UtkastValidatorService
) {

    private val logger: Logger = LoggerFactory.getLogger(UtkastService::class.java)

    fun opprettUtkast(opprettUtkastRequest: OpprettUtkastRequest): String {
        when (val operasjonerValideringResult = utkastValidatorService.validateGyldigeOperasjoner(opprettUtkastRequest.operasjoner)) {
            is OperasjonerValidering.UgyldigeOperasjoner -> throw operasjonerValideringResult.feil
            is OperasjonerValidering.GyldigeOperasjoner -> return utkastRepository.save(opprettUtkastRequest.createUtkast()).id
        }
    }

    @Transactional(readOnly = true)
    fun hentUtkast(id: String): Utkast? = utkastRepository.findById(id).unwrap()

    @Transactional(readOnly = true)
    fun hentUtkastIStatusOpprettet(): List<Utkast> {
        return utkastRepository.findAllByStatus(UtkastStatus.OPPRETTET)
    }

    fun oppdaterUtkast(id: String, request: OppdaterUtkastRequest): Utkast? {
        when (val operasjonerValideringResult = utkastValidatorService.validateGyldigeOperasjoner(request.operasjoner)) {
            is OperasjonerValidering.UgyldigeOperasjoner -> throw operasjonerValideringResult.feil
            is OperasjonerValidering.GyldigeOperasjoner -> {
                return hentUtkast(id)?.let { eksisterendeUtkast ->
                    verifiserOptimistiskLaas(request.version, eksisterendeUtkast)?.let {
                        throw OptimistiskLaasException(it)
                    }

                    eksisterendeUtkast.navn = request.navn
                    eksisterendeUtkast.auditInfo.typeEndring = Endringstype.fromValue(request.endringstype)
                    eksisterendeUtkast.gyldigFra = request.gyldigFra!!
                    eksisterendeUtkast.operasjoner = request.operasjoner
                    utkastRepository.save(eksisterendeUtkast)
                }
            }
        }
    }

    fun forkastUtkast(id: String): Utkast? =
        hentUtkast(id)?.apply {
            status = UtkastStatus.FORKASTET
            operasjoner = null
        }

    @Transactional
    fun publiserUtkast(utkastId: String, nyGyldigFra: LocalDate = LocalDate.now()) {
        val utkast =
            hentUtkast(utkastId)
                ?: throw NotFoundException("Utkast ble ikke funnet for id: $utkastId.")
        // Vi håndterer ikke fremtidige endringer enda, ved kall fra frontend settes
        // publiseringsdato til dagens dato
        utkast.gyldigFra = nyGyldigFra

        when (
            val utkastValideringResult =
                utkastValidatorService.validateUtkastKlartForPublisering(utkast)
        ) {
            is UtkastValidering.UgyldigUtkast -> throw utkastValideringResult.feil
            is UtkastValidering.KlartForPublisering -> {
                logger.debug(utkast.publiseringsInfo())

                utkast.operasjoner?.let { operasjoner ->
                    val flateEndringer = publiserEndringer(operasjoner, utkast)
                    nibasEventPublisher.publishNibasEvent(flateEndringer, utkast.gyldigFra)
                }
                utkast.settPublisert()
            }
        }
    }

    internal fun publiserEndringer(operasjoner: Operasjoner, utkast: Utkast): List<EventRad> {
        val metadataendringer =
            publiserMetadataEndringer(
                operasjoner.metadataendringer,
                utkast,
            )

        val sammenslaaingsendringer =
            publiserSammenslaaingsendringer(operasjoner.stemmekretsSammenslaaingsendring, utkast)

        publiserGrenseendringer(
            operasjoner.grenseendringer
        ) // OBS - Genererer per nå ikke events for disse!

        return metadataendringer + sammenslaaingsendringer
    }

    private fun publiserMetadataEndringer(
        metadata: Metadataendringer,
        utkast: Utkast,
    ): List<EventRad> {
        val endredeNasjoner =
            metadata.nasjonsendringer.mapNotNull {
                nasjonService.oppdaterNasjon(Lokalid(it.key), it.value)
            }

        val endredeFylker =
            metadata.fylkesendringer.mapNotNull {
                fylkeService.oppdaterFylke(Lokalid(it.key), it.value)
            }

        val endredeKommuner =
            metadata.kommuneendringer.mapNotNull {
                kommuneService.oppdaterKommune(Lokalid(it.key), it.value)
            }

        val endredeGrunnkretser =
            metadata.grunnkretsendringer.mapNotNull { grunnkretsEndring ->
                grunnkretsService.oppdaterGrunnkrets(
                    lokalId = Lokalid(grunnkretsEndring.key),
                    gyldigFra = utkast.gyldigFra,
                    oppdatertGrunnkrets = grunnkretsEndring.value,
                    endringstype = utkast.auditInfo.typeEndring
                )
            }

        val endredeStemmekretser =
            metadata.stemmekretsendringer.mapNotNull { stemmekretsEndring ->
                stemmekretsService.oppdaterStemmekrets(
                    lokalId = Lokalid(stemmekretsEndring.key),
                    gyldigFra = utkast.gyldigFra,
                    oppdatertStemmekrets = stemmekretsEndring.value,
                    endringstype = utkast.auditInfo.typeEndring
                )
            }

        val alleEndringer =
            endredeNasjoner +
            endredeFylker +
            endredeKommuner +
            endredeGrunnkretser +
            endredeStemmekretser
        return alleEndringer.map { lagFlateEndring(it, EventType.MODIFIED) }
    }

    private fun publiserGrenseendringer(grenseendringer: Grenseendringer) {
        val endredeFeatures = grenseendringer.endredeFeatures

        if (endredeFeatures.isNotEmpty()) {
            grenseService.lagreGrenser(
                FeatureCollection(features = endredeFeatures.map { it.value })
            )
        }
    }

    // Bør utvides til å gjelde fler typer sammenslåinger
    private fun publiserSammenslaaingsendringer(
        stemmekretsSammenslaaingsendring: StemmekretsSammenslaaingsendringRequest?,
        utkast: Utkast,
    ): List<EventRad> =
        stemmekretsSammenslaaingsendring?.let { stemmekretsendring ->
            val (modifiedStemmekrets, expiredStemmekretser) = stemmekretsService.slaaSammenStemmekretser(
                sammenslaaingsendringRequest = stemmekretsendring,
                sammenslaaingsDato = utkast.gyldigFra,
                endringstype = utkast.auditInfo.typeEndring
            )
            expiredStemmekretser.map { lagFlateEndring(it, EventType.EXPIRED) } +
                lagFlateEndring(modifiedStemmekrets, EventType.MODIFIED)
        } ?: emptyList()

    /**
     * Setter utkastet til PUBLISERT hvis utføringen av requestene som er resolved går bra.
     *
     * Må refaktoreres, og er foreløpig ikke i bruk da vi ikke har fremtidige endringer - den er ei
     * heller i brukbar tilstand.
     *
     * Håndterer kun konflikter på én lokalID, det betyr at hvis vi har et utkast der det blir
     * konflikt på f. eks to grunnkretser eller en grunnkrets og en stemmekrets, så håndterer den
     * kun den ene.
     *
     * Dette gjelder også i frontend, og vi ser at ConflictResolved kun gir én lokalID. For å
     * forsøke å tydeliggjøre dette bruker vi dermed lokalIDen som sendes med i ConflictResolved for
     * nå.
     *
     * Bør også sees sammen med publiserMetadataendringer
     */
    @Transactional
    fun resolveConflictForFramtidigeVersjoner(
        utkastId: String,
        conflictResolved: ConflictResolved
    ) {
        val utkast =
            hentUtkast(utkastId)
                ?: throw NotFoundException("Utkast ble ikke funnet for id: $utkastId.")

        val lokalid = conflictResolved.lokalid

        val endredeGrunnkretser =
            conflictResolved.grunnkretsRequests
                .filter { it.grunnkretsRequest.identifikasjon.lokalid == lokalid.value }
                .map {
                    val grunnkretsRequest = it.grunnkretsRequest
                    if (utkastValidatorService.isMetadataendringUgyldig(
                            Flatetype.GRUNNKRETS,
                            grunnkretsRequest.navn,
                            grunnkretsRequest.grunnkretsnummer,
                            grunnkretsRequest.identifikasjon.lokalid)
                    ) {
                        throw BadRequestException(
                            "Utkast inneholder ugyldige metadataendringer på grunnkrets"
                        )
                    }

                    if (
                        utkastValidatorService.datoErEtterSisteGyldigFraDatoForGrunnkrets(
                            lokalid = lokalid.value,
                            dato = it.gyldigFra
                        )
                    ) {
                        throw modifisertGyldigFraException(
                            utkastId = utkast.id,
                            lokalId = lokalid.value,
                            type = "Grunnkrets"
                        )
                    }

                    grunnkretsService.oppdaterGrunnkrets(
                        lokalId = lokalid,
                        gyldigFra = it.gyldigFra,
                        oppdatertGrunnkrets = grunnkretsRequest,
                        endringstype = Endringstype.fromValue(it.endringstype),
                    )
                }

        val endredeStemmekretser =
            conflictResolved.stemmekretsRequests
                .filter { it.stemmekretsRequest.identifikasjon.lokalid == lokalid.value }
                .mapNotNull {
                    val stemmekretsRequest = it.stemmekretsRequest
                    if (utkastValidatorService.isMetadataendringUgyldig(
                            Flatetype.STEMMEKRETS,
                            stemmekretsRequest.stemmekretsnavn,
                            stemmekretsRequest.stemmekretsnummer,
                            stemmekretsRequest.identifikasjon.lokalid)
                    ) {
                        throw BadRequestException(
                            "Utkast inneholder ugyldige metadataendringer på stemmekrets"
                        )
                    }
                    if (
                        utkastValidatorService.datoErEtterSisteGyldigFraDatoForStemmekrets(
                            lokalid = lokalid.value,
                            dato = it.gyldigFra
                        )
                    ) {
                        throw modifisertGyldigFraException(
                            utkastId = utkast.id,
                            lokalId = lokalid.value,
                            type = "Stemmekrets"
                        )
                    }

                    stemmekretsService.oppdaterStemmekrets(
                        lokalId = lokalid,
                        gyldigFra = it.gyldigFra,
                        endringstype = Endringstype.fromValue(it.endringstype),
                        oppdatertStemmekrets = stemmekretsRequest
                    )
                }

        val endredeKretser = endredeGrunnkretser + endredeStemmekretser
        endredeKretser.map {
            nibasEventPublisher.publishNibasEvent(
                redigerteFlater = listOf(lagFlateEndring(it, EventType.MODIFIED)),
                inntreffer = it.gyldighet.gyldigFra
            )
        }
        utkast.settPublisert()
    }

    private fun modifisertGyldigFraException(utkastId: String, lokalId: String, type: String) =
        BadRequestException(
            "Kan ikke publisere utkast med id: $utkastId. GyldigFra-feltet på en av endringene i utkastet er etter siste gyldig fra dato for kretsen. " +
                "Dette kan tyde på at GyldigFra-feltet ble modifisert under konflikt-håndteringen. Feilen oppsto på $type med lokalId: $lokalId"
        )

    private fun lagFlateEndring(flate: Flate, eventType: EventType): EventRad {
        return EventRad(
            uuid = flate.id,
            eventType = eventType,
            lokalid = flate.lokalid,
            flateType = flate.type
        )
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

private fun OpprettUtkastRequest.createUtkast(): Utkast =
    UtkastBuilder.builder(this.navn, Endringstype.fromValue(this.endringstype))
        .withOperasjoner(this.operasjoner)
        .build()

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
    UtkastRef(id = id, navn = navn, href = hrefProvider.getHref(id))

fun AuditInfo.toAuditInfoResponse(): AuditInfoResponse = AuditInfoResponse(this.endretdato)
