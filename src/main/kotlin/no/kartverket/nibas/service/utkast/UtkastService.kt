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
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
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
        return utkastRepository.save(opprettUtkastRequest.createUtkast()).id
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
        when (val operasjonerValideringResult = utkastValidatorService.validateGyldigeOperasjoner(request.operasjoner)) {
            is OperasjonerValidering.UgyldigeOperasjoner -> throw operasjonerValideringResult.feil
            is OperasjonerValidering.GyldigeOperasjoner -> {
                return hentUtkast(id)?.let { eksisterendeUtkast ->
                    verifiserOptimistiskLaas(request.version, eksisterendeUtkast)?.throwException()

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
    fun publiserUtkast(utkastId: String, gyldigFra: LocalDate = LocalDate.now()) {
        val utkast =
            hentUtkast(utkastId)
                ?: throw ResponseException.NotFoundException("Utkast ble ikke funnet for id: $utkastId.")

        utkast.gyldigFra = gyldigFra

        if (utkastValidatorService.sjekkUgyldigUtkastNavnOgEndringstype(utkast.navn, utkast.auditInfo.typeEndring.value)) {
            throw ResponseException.BadRequestException("Husk å fylle inn både navnet og endringstypen på utkastet.")
        }

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
        val kretsDelingEndringer = publiserKretsDelingEndringer(operasjoner.kretsDelingEndringer, utkast)

        val sammenslaaingsendringer =
            publiserSammenslaaingsendringer(operasjoner.stemmekretsSammenslaaingsendring, utkast)

        val metadataendringer =
            publiserMetadataEndringer(
                operasjoner.metadataendringer,
                utkast
            )

        val grenseendringer = publiserGrenseendringer(operasjoner.grenseendringer, utkast)

        return metadataendringer + sammenslaaingsendringer + grenseendringer + kretsDelingEndringer
    }

    private fun publiserMetadataEndringer(
        metadata: Metadataendringer,
        utkast: Utkast,
    ): List<EventRad> {
        val endredeNasjoner =
            metadata.nasjonsendringer.mapNotNull { nasjonEndring ->
                nasjonService.oppdaterNasjon(
                    lokalid = Lokalid(value = nasjonEndring.key),
                    gyldigFra = utkast.gyldigFra,
                    endringstype = utkast.auditInfo.typeEndring,
                    oppdatertNasjon = nasjonEndring.value
                )
            }

        val endredeFylker =
            metadata.fylkesendringer.mapNotNull { fylkeEndring ->
                fylkeService.oppdaterFylke(
                    lokalid = Lokalid(value = fylkeEndring.key),
                    gyldigFra = utkast.gyldigFra,
                    endringstype = utkast.auditInfo.typeEndring,
                    oppdatertFylke = fylkeEndring.value
                )
            }

        val endredeKommuner =
            metadata.kommuneendringer.mapNotNull { kommuneEndring ->
                kommuneService.oppdaterKommune(
                    lokalid = Lokalid(value = kommuneEndring.key),
                    gyldigFra = utkast.gyldigFra,
                    oppdatertKommune = kommuneEndring.value,
                    endringstype = utkast.auditInfo.typeEndring
                )
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

    private fun publiserGrenseendringer(grenseendringer: Grenseendringer, utkast: Utkast) : List<EventRad> {
        val endredeFeatures = grenseendringer.endredeFeatures
        var eventer: List<EventRad> = emptyList()

        if (endredeFeatures.isNotEmpty()) {
            val flaterMedGrenseendring = grenseService.lagreGrenser(
                FeatureCollection(features = endredeFeatures),
                gyldighetsdato = utkast.gyldigFra,
                endringstype = utkast.auditInfo.typeEndring
            )
            eventer = flaterMedGrenseendring.map { lagFlateEndring(it, EventType.MODIFIED) }
        }
        return eventer
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

    private fun publiserKretsDelingEndringer(
        kretsDelingEndringRequests: List<KretsDelingEndringRequest>,
        utkast: Utkast
    ): List<EventRad> = getUnikeKretsDelingEndringRequests(kretsDelingEndringRequests).flatMap {
            it.let { unikKretsDelingEndring ->
                val nyKrets = when(unikKretsDelingEndring.flatetype) {
                    Flatetype.STEMMEKRETS -> stemmekretsService.delStemmekrets(
                        unikKretsDelingEndring,
                        utkast.gyldigFra,
                        utkast.auditInfo.typeEndring
                    )
                    Flatetype.GRUNNKRETS -> grunnkretsService.delGrunnkrets(
                        unikKretsDelingEndring,
                        utkast.gyldigFra,
                        utkast.auditInfo.typeEndring
                    )
                    else -> throw ResponseException.BadRequestException("Støtter kun deling av grunnkrets eller stemmekrets, ikke ${unikKretsDelingEndring.flatetype}")
                }
                listOf(
                    lagFlateEndring(nyKrets, EventType.ADDED)
                )
            }
        }

    private fun lagFlateEndring(flate: Flate, eventType: EventType): EventRad {
        return EventRad(
            uuid = flate.id,
            eventType = eventType,
            lokalid = flate.lokalid,
            flateType = flate.type
        )
    }
}

private fun OpprettUtkastRequest.createUtkast(): Utkast =
    UtkastBuilder.builder(this.navn, Endringstype.fromValue(this.endringstype))
        .withOperasjoner(Operasjoner())
        .build()
