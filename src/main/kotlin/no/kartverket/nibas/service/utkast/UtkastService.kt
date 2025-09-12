package no.kartverket.nibas.service.utkast

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.getUnikeKretsDelingEndringRequests
import no.kartverket.nibas.api.v1.request.GrunnkretsSammenslaaingsendringRequest
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.OppdaterUtkastRequest
import no.kartverket.nibas.api.v1.request.OpprettUtkastRequest
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.common.CurrentTime
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.toNorskFormat
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.EventType
import no.kartverket.nibas.events.NibasEvent
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.StemmekretsRepository
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
    private val currentTime: CurrentTime,
    private val stemmekretsRepository: StemmekretsRepository,
    private val grunnkretsRepository: GrunnkretsRepository,
) {

    private val logger: Logger = LoggerFactory.getLogger(UtkastService::class.java)

    fun opprettUtkast(opprettUtkastRequest: OpprettUtkastRequest): String {
        utkastValidatorService.validateUtkastAttributter(opprettUtkastRequest.navn, opprettUtkastRequest.endringstype, opprettUtkastRequest.gyldigFra)
            ?.let { throw ResponseException.BadRequestException(title = "Kan ikke opprette utkastet", msg = it) }
        return utkastRepository.saveAndFlush(opprettUtkastRequest.createUtkast()).id
    }

    @Transactional(readOnly = true)
    fun hentUtkast(id: String): Utkast? = utkastRepository.findById(id).unwrap()

    @Transactional(readOnly = true)
    fun hentUtkast(utkastStatus: List<UtkastStatus>, gyldigFra: LocalDate?): List<Utkast> {
        return utkastRepository.findByStatusAndGyldigFra(statuses = utkastStatus.map { it.dbKey }, gyldigFra = gyldigFra)
    }

    @Transactional(readOnly = true)
    fun validerUtkast(utkastId: String, request: OppdaterUtkastRequest): Boolean {
        val (utkast, gyldigFra) = hentUtkastOppdaterEvtFraRequestValiderEndret(utkastId, request)

        val utkastValideringResult = utkastValidatorService.validateUtkastKlartForPublisering(utkast, gyldigFra)
        if (utkastValideringResult.isNotEmpty()) {
            throw ResponseException.BadRequestException(title = "Det ble funnet ${utkastValideringResult.size} feil på utkastet",
                msg = utkastValideringResult.joinToString("\n"))
        }
        return true
    }

    fun oppdaterUtkast(utkastId: String, request: OppdaterUtkastRequest): Utkast? {
        val (utkast, gyldigFra) = hentUtkastOppdaterEvtFraRequestValiderEndret(utkastId, request)

        val utkastValideringResult = utkastValidatorService.validateUtkastMedOperasjoner(utkast, request.endringstype, gyldigFra)

        if (utkastValideringResult.isNotEmpty()) {
            throw ResponseException.BadRequestException(title = "Kan ikke lagre utkastet", msg = utkastValideringResult.joinToString("\n"))
        }
        utkast.auditInfo.typeEndring = Endringstype.fromValue(request.endringstype)
        return utkastRepository.save(utkast)
    }

    private fun hentUtkastOppdaterEvtFraRequestValiderEndret(
        utkastId: String,
        request: OppdaterUtkastRequest? = null
    ): Pair<Utkast, LocalDate> {
        val utkast = hentUtkast(utkastId)
            ?: throw ResponseException.NotFoundException("Utkast ble ikke funnet for id: $utkastId.")

        if (request != null) {
            utkast.operasjoner = request.operasjoner
            utkast.navn = request.navn
        }

        val gyldigFra = maxOf(utkast.gyldigFra, currentTime.asLocalDate())

        //Valideringer som brukeren ikke kan rette opp i
        validateOptimistiskLaasOgFremtidigeEndringer(utkast, request?.version, gyldigFra)
        return Pair(utkast, gyldigFra)
    }

    /**
     * Sjekker at alle objekter som skal oppdateres finnes, og at de har det versjonsnummeret utkastet mener de skal ha.
     * Sjekker også at de ikke har noen senere forekomster etter ønsket dato.
     * utkastetsVersion er gitt ved oppdatering av utkastet, ved publisering hentes utkastet fra basen.
     */
    private fun validateOptimistiskLaasOgFremtidigeEndringer(utkast: Utkast, utkastetsVersion: Int? = null, gyldigFra: LocalDate) {
        if (utkast.status != UtkastStatus.OPPRETTET) {
            throw ResponseException.BadRequestException(
                msg = "Kan ikke " + let { if (utkastetsVersion != null) "oppdatere" else "publisere" } + " et utkast med status ${utkast.status}"
            )
        }
        if (utkast.operasjoner == null) {
            throw ResponseException.BadRequestException(
                msg = "Kan ikke " + let { if (utkastetsVersion != null) "oppdatere" else "publisere" } + " et utkast uten operasjoner"
            )
        }

        //Lager liste med alle objekter som utkastet ønsker å oppdatere, og hvilken versjon utkastet mener de skal ha
        val laasbarMedOensketVersjonListe = validerLaasbarForEndringerFinnesOgLagListeMedLaasbarOgOensketVersjon(utkast, gyldigFra)
        utkastetsVersion?.let { laasbarMedOensketVersjonListe.addFirst(Pair(utkast, it)) }

        //Sjekker at de låsbare (grense, krets, administrativ enhet pluss evt utkastet selv) som utkastet vil oppdatere på har ønsket versjon
        val optimistiskLaasKonflikter = laasbarMedOensketVersjonListe
            .mapNotNull { (laasbar, versjon) ->
                verifiserOptimistiskLaas(versjon, laasbar)
            }

        if (optimistiskLaasKonflikter.isNotEmpty()) {
            throw OptimistiskLaasException(*optimistiskLaasKonflikter.toTypedArray())
        }

        utkastValidatorService.validerFremtidigeEndringer(utkast, gyldigFra)?.let { throw it }
    }

    fun forkastUtkast(id: String): Utkast? =
        hentUtkast(id)?.apply {
            if (status == UtkastStatus.PUBLISERT) {
                throw ResponseException.BadRequestException(title = "Kan ikke slette utkastet",
                    msg = "Kan ikke forkaste et utkast som er publisert"
                )
            }

            status = UtkastStatus.FORKASTET
            operasjoner = null
        }

    @Transactional
    fun publiserUtkast(utkastId: String): NibasEvent {
        val eventsForPublisering: NibasEvent
        val (utkast, gyldigFra) = hentUtkastOppdaterEvtFraRequestValiderEndret(utkastId)

        utkast.gyldigFra = gyldigFra
        val utkastValideringResult = utkastValidatorService.validateUtkastKlartForPublisering(utkast, gyldigFra)
        if (utkastValideringResult.isNotEmpty()) {
            throw ResponseException.BadRequestException(title = "Kan ikke publisere utkastet", msg = utkastValideringResult.joinToString("\n"))
        }

        logger.debug(utkast.publiseringsInfo())
        val flateEndringer: List<EventRad> = publiserEndringerOgValiderUnikeKretsnumre(utkast.operasjoner!!, utkast, utkast.gyldigFra)

        utkast.settPublisert(flateEndringer)
        eventsForPublisering = nibasEventPublisher.publishNibasEvent(flateEndringer, utkast.gyldigFra)

        return eventsForPublisering
    }

    private fun validerUnikeKretsnumre(flateEndringer: List<EventRad>) {
        flateEndringer.filter { it.flateType == Flatetype.STEMMEKRETS && it.eventType != EventType.EXPIRED }
            .forEach { eventRad ->
                stemmekretsRepository.findById(eventRad.uuid).get().let { stemmekrets ->
                    if (stemmekretsService.finnesAnnenStemmekretsMedSammeKommuneOgStemmekretsnummer(stemmekrets))
                        throw stemmekretsService.createDuplikatStemmekretsnummerException(stemmekrets)
                }
            }

        flateEndringer.filter { it.flateType == Flatetype.GRUNNKRETS && it.eventType != EventType.EXPIRED }
            .forEach { eventRad ->
                grunnkretsRepository.findById(eventRad.uuid).get().let { grunnkrets ->
                    if (grunnkretsService.finnesAnnenGrunnkretsMedSammeGrunnkretsnummer(grunnkrets))
                        throw grunnkretsService.createDuplikatGrunnkretsnummerException(grunnkrets)
                }
            }
    }

    internal fun publiserEndringerOgValiderUnikeKretsnumre(operasjoner: Operasjoner, utkast: Utkast, gyldigFra: LocalDate): List<EventRad> {
        val kretsDelingEndringer = publiserKretsDelingEndringer(operasjoner.kretsDelingEndringer, utkast, gyldigFra)

        val sammenslaaingsendringer =
            publiserSammenslaaingsendringer(operasjoner.stemmekretsSammenslaaingsendring, utkast, gyldigFra) +
            publiserSammenslaaingsendringer(operasjoner.grunnkretsSammenslaaingsendring, utkast, gyldigFra)

        val metadataendringer =
            publiserMetadataEndringer(
                operasjoner.metadataendringer,
                utkast,
                gyldigFra
            )

        validerUnikeKretsnumre(kretsDelingEndringer + sammenslaaingsendringer + metadataendringer)
        val grenseendringer = publiserGrenseendringer(operasjoner.grenseendringer, utkast, gyldigFra)

        return metadataendringer + sammenslaaingsendringer + grenseendringer + kretsDelingEndringer
    }

    /**
     * Sjekker at det som utkastet ønsker å oppdatere finnes. Alt som kan oppdateres er en subklasse av Laasbar:
     * Grense, Grunnkrets, Stemmekrets, Kommune, Fylke, Nasjon
     * Returnerer liste med alle låsbare og hvilken versjon utkastet mener de skal ha
     * Den kallende tjenesten validerer at de låsbare har ønsket versjon
     */
    private fun validerLaasbarForEndringerFinnesOgLagListeMedLaasbarOgOensketVersjon(utkast: Utkast, gyldigFra: LocalDate): MutableList<Pair<Laasbar, Int>> {
        val laasbarMedOensketVersjonListe = mutableListOf<Pair<Laasbar, Int>>()
        utkast.operasjoner?.let { operasjoner ->
            operasjoner.stemmekretsSammenslaaingsendring?.let {
                laasbarMedOensketVersjonListe.addAll(stemmekretsService.validerStemmekretserForSammenslaaingFinnesOgLagListeMedStemmekretsOgOensketVersjon(it, gyldigFra))
            }
            operasjoner.grunnkretsSammenslaaingsendring?.let {
                laasbarMedOensketVersjonListe.addAll(grunnkretsService.validerGrunnkretserForSammenslaaingFinnesOgLagListeMedGrunnkretsOgOensketVersjon(it, gyldigFra))
            }
            laasbarMedOensketVersjonListe.addAll(grenseService.validerLaasbareForGrenseendringerFinnesOgLagListeMedLaasbarOgOensketVersjon(operasjoner.grenseendringer.endredeFeatures, gyldigFra))
            laasbarMedOensketVersjonListe.addAll(validerLaasbarForMetadataEndringerFinnesOgLagListeMedLaasbarOgOensketVersjon(operasjoner.metadataendringer, gyldigFra))

            operasjoner.kretsDelingEndringer.forEach {
                laasbarMedOensketVersjonListe.add(when (it.flatetype) {
                    Flatetype.STEMMEKRETS -> stemmekretsService.validerStemmekretsForDelingFinnesOgLagPairMedStemmekretsOgOensketVersjon(it, gyldigFra)
                    Flatetype.GRUNNKRETS -> grunnkretsService.validerGrunnkretsForDelingFinnesOgLagPairMedGrunnkretsOgOensketVersjon(it, gyldigFra)
                    else -> throw ResponseException.BadRequestException("Støtter kun deling av grunnkrets eller stemmekrets, ikke ${it.flatetype}")
                }
                )
            }

        }
        return laasbarMedOensketVersjonListe
    }

    private fun validerLaasbarForMetadataEndringerFinnesOgLagListeMedLaasbarOgOensketVersjon(metadataendringer: Metadataendringer, gyldigFra: LocalDate)
        : List<Pair<Laasbar, Int>> {
        val laasbarMedOensketVersjonListe = mutableListOf<Pair<Laasbar, Int>>()
        laasbarMedOensketVersjonListe.addAll(metadataendringer.nasjonsendringer.mapNotNull {
            val nasjon = nasjonService.hentNasjon(Identifikator(Lokalid(it.value.lokalid), gyldigFra))
                ?: throw ResponseException.NotFoundException("Fant ikke nasjon med id ${it.value.lokalid} som er gyldig ${gyldigFra.toNorskFormat()}")
            Pair(nasjon, it.value.version)
        })

        laasbarMedOensketVersjonListe.addAll(metadataendringer.fylkesendringer.mapNotNull {
            val fylke = fylkeService.hentFylke(Identifikator(Lokalid(it.value.lokalid), gyldigFra))
                ?: throw ResponseException.NotFoundException("Fant ikke fylke med id ${it.value.lokalid} som er gyldig ${gyldigFra.toNorskFormat()}")
            Pair(fylke, it.value.version)
        })

        laasbarMedOensketVersjonListe.addAll(metadataendringer.kommuneendringer.mapNotNull {
            val kommune = kommuneService.hentKommune(Identifikator(Lokalid(it.value.lokalid), gyldigFra))
                ?: throw ResponseException.NotFoundException("Fant ikke kommune med id ${it.value.lokalid} som er gyldig ${gyldigFra.toNorskFormat()}")
            Pair(kommune, it.value.version)
        })

        laasbarMedOensketVersjonListe.addAll(metadataendringer.stemmekretsendringer.mapNotNull {
            val stemmekrets = (stemmekretsService.hentStemmekrets(Identifikator(Lokalid(it.value.identifikasjon.lokalid), gyldigFra))
                ?: throw ResponseException.NotFoundException("Fant ikke stemmekrets med id ${it.value.identifikasjon.lokalid} som er gyldig ${gyldigFra.toNorskFormat()}"))
            Pair(stemmekrets, it.value.version)
        })

        laasbarMedOensketVersjonListe.addAll(metadataendringer.grunnkretsendringer.mapNotNull {
            val stemmekrets = (grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(it.value.identifikasjon.lokalid), gyldigFra))
                ?: throw ResponseException.NotFoundException("Fant ikke stemmekrets med id ${it.value.identifikasjon.lokalid} som er gyldig ${gyldigFra.toNorskFormat()}"))
            Pair(stemmekrets, it.value.version)
        })

        return laasbarMedOensketVersjonListe
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

    private fun publiserGrenseendringer(grenseendringer: Grenseendringer, utkast: Utkast, gyldigFra: LocalDate): List<EventRad> {
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

    private fun publiserSammenslaaingsendringer(
        grunnkretsSammenslaaingsendring: GrunnkretsSammenslaaingsendringRequest?,
        utkast: Utkast,
        gyldigFra: LocalDate
    ): List<EventRad> =
        grunnkretsSammenslaaingsendring?.let { grunnkretsendring ->
            val (modifiedGrunnkrets, expiredGrunnkretser) = grunnkretsService.slaaSammenGrunnkretser(
                sammenslaaingsendringRequest = grunnkretsendring,
                sammenslaaingsDato = gyldigFra,
                endringstype = utkast.auditInfo.typeEndring
            )
            expiredGrunnkretser.map { it.toEventRad(EventType.EXPIRED) } +
                modifiedGrunnkrets.toEventRad(EventType.MODIFIED)
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
    UtkastBuilder.builder(this.navn, Endringstype.fromValue(this.endringstype), gyldigFra)
        .withOperasjoner(Operasjoner())
        .build()
