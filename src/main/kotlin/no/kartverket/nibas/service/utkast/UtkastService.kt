package no.kartverket.nibas.service.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.getUnikeKretsDelingEndringRequests
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import no.kartverket.nibas.events.NibasEvent
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.NasjonService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.utkast.extensions.publiseringsInfo
import no.kartverket.nibas.sporing.Laasbar
import no.kartverket.nibas.unwrap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

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
    private val utkastValidatorService: UtkastValidatorService,
) {

    private val logger: Logger = LoggerFactory.getLogger(UtkastService::class.java)

    fun opprettUtkast(opprettUtkastRequest: OpprettUtkastRequest): String {
        if (utkastValidatorService.sjekkUgyldigUtkastNavnOgEndringstype(opprettUtkastRequest.navn, opprettUtkastRequest.endringstype)) {
            throw ResponseException.BadRequestException("Husk å fylle inn både navnet og endringstypen på utkastet.")
        }
        return utkastRepository.saveAndFlush(opprettUtkastRequest.createUtkast()).id
    }

    @Transactional(readOnly = true)
    fun hentUtkast(id: String): Utkast? = utkastRepository.findById(id).unwrap()

    @Transactional(readOnly = true)
    fun hentUtkastIStatusOpprettet(): List<Utkast> {
        return utkastRepository.findAllByStatus(UtkastStatus.OPPRETTET)
    }

    fun oppdaterUtkast(id: String, request: OppdaterUtkastRequest): Utkast? {
        if (utkastValidatorService.sjekkUgyldigUtkastNavnOgEndringstype(request.navn, request.endringstype)) {
            throw ResponseException.BadRequestException("Husk å fylle inn både navnet og endringstypen på utkastet.")
        }
        val utkast = hentUtkast(id)
            ?: throw ResponseException.NotFoundException("Utkast ble ikke funnet for id: $id.")

        verifiserOptimistiskLaas(request.version, utkast)?.throwException()
            validerOptimistiskLaasForEndringer(utkast, LocalDate.now())

        utkast.navn = request.navn
        utkast.auditInfo.typeEndring = Endringstype.fromValue(request.endringstype)
        
        utkast.operasjoner = request.operasjoner

        val utkastValideringResult =
            utkastValidatorService.validateGyldigUtkast(utkast, LocalDate.now())

        if (utkastValideringResult is UtkastValidering.UgyldigUtkast) {
            throw utkastValideringResult.feil
        }
        return utkastRepository.save(utkast)
    }

    fun forkastUtkast(id: String): Utkast? =
        hentUtkast(id)?.apply {
            status = UtkastStatus.FORKASTET
            operasjoner = null
        }

    @Transactional
    fun publiserUtkast(utkastId: String, gyldigFra: LocalDate = LocalDate.now()): NibasEvent {
        val eventsForPublisering: NibasEvent
        val utkast =
            hentUtkast(utkastId)
                ?: throw ResponseException.NotFoundException("Utkast ble ikke funnet for id: $utkastId.")

        when (
            val utkastValideringResult =
                utkastValidatorService.validateUtkastKlartForPublisering(utkast, gyldigFra)
        ) {
            is UtkastValidering.UgyldigUtkast -> throw utkastValideringResult.feil
            is UtkastValidering.GyldigUtkast -> throw ResponseException.BadRequestException("Noe gikk galt! Utkastet ble markert som GyldigUtkast i stedet for KlartForPublisering.")
            is UtkastValidering.KlartForPublisering -> {
                logger.debug(utkast.publiseringsInfo())
                validerOptimistiskLaasForEndringer(utkast, gyldigFra)

                var flateEndringer: List<EventRad> = emptyList()
                utkast.operasjoner?.let { operasjoner ->
                    flateEndringer = publiserEndringer(operasjoner, utkast, gyldigFra)
                    nibasEventPublisher.publishNibasEvent(flateEndringer, gyldigFra)
                }

                utkast.settPublisert()
                eventsForPublisering = nibasEventPublisher.publishNibasEvent(flateEndringer, gyldigFra)
            }
        }
        return eventsForPublisering
    }

    internal fun publiserEndringer(operasjoner: Operasjoner, utkast: Utkast, gyldigFra: LocalDate): List<EventRad> {
        val kretsDelingEndringer = publiserKretsDelingEndringer(operasjoner.kretsDelingEndringer, utkast, gyldigFra)

        val sammenslaaingsendringer =
            publiserSammenslaaingsendringer(operasjoner.stemmekretsSammenslaaingsendring, utkast, gyldigFra)

        val metadataendringer =
            publiserMetadataEndringer(
                operasjoner.metadataendringer,
                utkast,
                gyldigFra
            )

        val grenseendringer = publiserGrenseendringer(operasjoner.grenseendringer, utkast, gyldigFra)

        return metadataendringer + sammenslaaingsendringer + grenseendringer + kretsDelingEndringer
    }

    private fun validerOptimistiskLaasForEndringer(utkast: Utkast, gyldigFra: LocalDate) {
        utkast.operasjoner?.let { operasjoner ->
            operasjoner.stemmekretsSammenslaaingsendring?.let {
                stemmekretsService.validerOptimistiskLaasForStemmekretsSammenslaaing(it, gyldigFra)
            }
            operasjoner.grunnkretsSammenslaaingsendring?.let {
                grunnkretsService.validerOptimistiskLaasForGrunnkretsSammenslaaing(it, gyldigFra)
            }
            grenseService.validerOptimistiskLaasKonflikter(operasjoner.grenseendringer.endredeFeatures, gyldigFra)
            validerOptimistiskLaasForMetadataEndringer(operasjoner.metadataendringer, gyldigFra)
        }
    }

    private fun validerOptimistiskLaasForMetadataEndringer(metadataendringer: Metadataendringer, gyldigFra: LocalDate) {
        metadataendringer.nasjonsendringer.forEach { (lokalId, nasjonRequest) ->
            nasjonService.hentNasjon(Identifikator(Lokalid(lokalId), gyldigFra))
                ?.verifiserOptimistiskLaas(nasjonRequest.version)
        }

        metadataendringer.fylkesendringer.forEach { (lokalId, fylkeRequest) ->
            fylkeService.hentFylke(Identifikator(Lokalid(lokalId), gyldigFra))
                ?.verifiserOptimistiskLaas(fylkeRequest.version)
        }

        metadataendringer.kommuneendringer.forEach { (lokalId, kommuneRequest) ->
            kommuneService.hentKommune(Identifikator(Lokalid(lokalId), gyldigFra))
                ?.verifiserOptimistiskLaas(kommuneRequest.version)
        }

        metadataendringer.stemmekretsendringer.forEach { (lokalId, stemmekretsRequest) ->
            stemmekretsService.hentStemmekrets(Identifikator(Lokalid(lokalId), gyldigFra))
                ?.verifiserOptimistiskLaas(stemmekretsRequest.version)
        }

        metadataendringer.grunnkretsendringer.forEach { (lokalId, grunnkretsRequest) ->
            grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(lokalId), gyldigFra))
                ?.verifiserOptimistiskLaas(grunnkretsRequest.version)
        }
    }

    private fun Laasbar.verifiserOptimistiskLaas(versjonFraRequest: Int) {
        verifiserOptimistiskLaas(versjonFraRequest, this)?.throwException()
    }

    private fun publiserMetadataEndringer(
        metadata: Metadataendringer,
        utkast: Utkast,
        gyldigFra: LocalDate
    ): List<EventRad> {
        val endredeNasjoner =
            metadata.nasjonsendringer.mapNotNull { nasjonEndring ->
                nasjonService.oppdaterNasjon(
                    lokalid = Lokalid(value = nasjonEndring.key),
                    gyldigFra = gyldigFra,
                    endringstype = utkast.auditInfo.typeEndring,
                    oppdatertNasjon = nasjonEndring.value
                )
            }

        val endredeFylker =
            metadata.fylkesendringer.mapNotNull { fylkeEndring ->
                fylkeService.oppdaterFylke(
                    lokalid = Lokalid(value = fylkeEndring.key),
                    gyldigFra = gyldigFra,
                    endringstype = utkast.auditInfo.typeEndring,
                    oppdatertFylke = fylkeEndring.value
                )
            }

        val endredeKommuner =
            metadata.kommuneendringer.mapNotNull { kommuneEndring ->
                kommuneService.oppdaterKommune(
                    lokalid = Lokalid(value = kommuneEndring.key),
                    gyldigFra = gyldigFra,
                    oppdatertKommune = kommuneEndring.value,
                    endringstype = utkast.auditInfo.typeEndring
                )
            }

        val endredeGrunnkretser =
            metadata.grunnkretsendringer.mapNotNull { grunnkretsEndring ->
                grunnkretsService.oppdaterGrunnkrets(
                    lokalId = Lokalid(grunnkretsEndring.key),
                    gyldigFra = gyldigFra,
                    oppdatertGrunnkrets = grunnkretsEndring.value,
                    endringstype = utkast.auditInfo.typeEndring
                )
            }

        val endredeStemmekretser =
            metadata.stemmekretsendringer.mapNotNull { stemmekretsEndring ->
                stemmekretsService.oppdaterStemmekrets(
                    lokalId = Lokalid(stemmekretsEndring.key),
                    gyldigFra = gyldigFra,
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
        return alleEndringer.map { it.toEventRad(EventType.MODIFIED) }
    }

    private fun publiserGrenseendringer(grenseendringer: Grenseendringer, utkast: Utkast, gyldigFra: LocalDate) : List<EventRad> {
        val endredeFeatures = grenseendringer.endredeFeatures
        var eventer: List<EventRad> = emptyList()

        if (endredeFeatures.isNotEmpty()) {
            val flaterMedGrenseendring = grenseService.lagreGrenser(
                FeatureCollection(features = endredeFeatures),
                gyldighetsdato = gyldigFra,
                endringstype = utkast.auditInfo.typeEndring
            )
            eventer = flaterMedGrenseendring.map { it.toEventRad(EventType.MODIFIED) }
        }
        return eventer
    }

    // Bør utvides til å gjelde fler typer sammenslåinger
    private fun publiserSammenslaaingsendringer(
        stemmekretsSammenslaaingsendring: StemmekretsSammenslaaingsendringRequest?,
        utkast: Utkast,
        gyldigFra: LocalDate
    ): List<EventRad> =
        stemmekretsSammenslaaingsendring?.let { stemmekretsendring ->
            val (modifiedStemmekrets, expiredStemmekretser) = stemmekretsService.slaaSammenStemmekretser(
                sammenslaaingsendringRequest = stemmekretsendring,
                sammenslaaingsDato = gyldigFra,
                endringstype = utkast.auditInfo.typeEndring
            )
            expiredStemmekretser.map { it.toEventRad(EventType.EXPIRED) } +
                modifiedStemmekrets.toEventRad(EventType.MODIFIED)
        } ?: emptyList()

    private fun publiserKretsDelingEndringer(
        kretsDelingEndringRequests: List<KretsDelingEndringRequest>,
        utkast: Utkast,
        gyldigFra: LocalDate
    ): List<EventRad> = getUnikeKretsDelingEndringRequests(kretsDelingEndringRequests).flatMap {
        it.let { unikKretsDelingEndring ->
            val nyKrets = when (unikKretsDelingEndring.flatetype) {
                Flatetype.STEMMEKRETS -> stemmekretsService.delStemmekrets(
                    unikKretsDelingEndring,
                    gyldigFra,
                    utkast.auditInfo.typeEndring
                )
                Flatetype.GRUNNKRETS -> grunnkretsService.delGrunnkrets(
                    unikKretsDelingEndring,
                    gyldigFra,
                    utkast.auditInfo.typeEndring
                )
                else -> throw ResponseException.BadRequestException("Støtter kun deling av grunnkrets eller stemmekrets, ikke ${unikKretsDelingEndring.flatetype}")
            }
            listOf(
                nyKrets.toEventRad(EventType.ADDED)
            )
        }
    }
}

private fun OpprettUtkastRequest.createUtkast(): Utkast =
    UtkastBuilder.builder(this.navn, Endringstype.fromValue(this.endringstype))
        .withOperasjoner(Operasjoner())
        .build()
