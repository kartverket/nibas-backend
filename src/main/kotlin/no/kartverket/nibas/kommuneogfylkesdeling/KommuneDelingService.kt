package no.kartverket.nibas.kommuneogfylkesdeling

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.events.EventType
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.service.FlateService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.StemmekretsService
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class KommuneDelingService(
    private val kommuneService: KommuneService,
    private val kodelisteService: KodelisteService,
    private val spraakkodeRepository: SpraakkodeRepository,
    private val grunnkretsService: GrunnkretsService,
    private val stemmekretsService: StemmekretsService,
    private val grenseService: GrenseService,
    private val flateService: FlateService,
    private val nibasEventPublisher: NibasEventPublisher
) {
    val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun delKommuner(kommunedelingManuser: List<KommunedelingManus>) {
        kommunedelingManuser.map { delKommune(it) }
    }

    @Transactional
    fun delKommune(kommuneDelingManus: KommunedelingManus) {
        logger.info("Deler kommune ${kommuneDelingManus.gammeltKommuneNummer} til ${kommuneDelingManus.nyeKommuner.joinToString(", ") { it.kommunenummer }}")

        val kommuennummerUuid = kodelisteService.getKommunenummerIdByCodeValue(kommuneDelingManus.gammeltKommuneNummer)
        val kommuneSomSkalDeles = kommuneService.hentKommuneByKommunenummer(kommuennummerUuid, kommuneDelingManus.fraDato)
            ?: throw RuntimeException("Klarte ikke finne kommunen som skulle deles. Gjelder kommunenummer: ${kommuneDelingManus.gammeltKommuneNummer}")

        val nyeKommuner = opprettNyeKommunerForKommunedeling(kommuneDelingManus, kommuneSomSkalDeles)

        val nyeVersjonerAvGrunnkretser = kommuneDelingManus.nyeKommuner.flatMap { nyKommuneManus ->
            val kommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(nyKommuneManus.kommunenummer)
            val nyKommune = nyeKommuner.find { it.kommunenummer == kommunenummerId }!!

            flyttGrunnkretserTilNyKommune(
                gyldighetsDato = kommuneDelingManus.fraDato,
                grunnkretserSomFlyttesTilKommune = nyKommuneManus.grunnkretser,
                gammelKommune = kommuneSomSkalDeles,
                nyKommune = nyKommune
            )
        }

        val nyeVersjonerAvStemmekretser = kommuneDelingManus.nyeKommuner.flatMap { nyKommuneManus ->
            val kommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(nyKommuneManus.kommunenummer)
            val nyKommune = nyeKommuner.find { it.kommunenummer == kommunenummerId }!!

            flyttStemmekretserTilNyKommune(
                gyldighetsDato = kommuneDelingManus.fraDato,
                stemmekretserSomFlyttesTilKommune = nyKommuneManus.stemmekretser,
                gammelKommune = kommuneSomSkalDeles,
                nyKommune = nyKommune
            )
        }

        logger.info("Endrer grensetyper for splittingen av ${kodelisteService.getCodeValueByKommunenummerId(kommuneSomSkalDeles.kommunenummer)}")
        kommuneService.settKommuneSomHistorisk(kommuneSomSkalDeles, kommuneDelingManus.fraDato)
        grenseService.endreGrensetypeForGrunnkretser(nyeVersjonerAvGrunnkretser, kommuneDelingManus.fraDato, Endringstype.VEDTATT_DELING)
        grenseService.endreGrensetypeForStemmekretser(nyeVersjonerAvStemmekretser, kommuneDelingManus.fraDato, Endringstype.VEDTATT_DELING)

        logger.info("Danner flater for de nye kommunene")
        flateService.oppdaterFlaterFor(nyeKommuner.toSet())

        val kommuneEventRader = nyeKommuner.map { it.toEventRad(EventType.ADDED) } + kommuneSomSkalDeles.toEventRad(EventType.EXPIRED)
        val grunnkretsEventRader = nyeVersjonerAvGrunnkretser.map { it.toEventRad(EventType.MODIFIED) }
        val stemmekretsEventRader = nyeVersjonerAvStemmekretser.map { it.toEventRad(EventType.MODIFIED) }

        nibasEventPublisher.publishNibasEvent(
            inntreffer = kommuneDelingManus.fraDato,
            redigerteFlater = (kommuneEventRader + grunnkretsEventRader + stemmekretsEventRader).sortByType()
        )
    }

    private fun flyttGrunnkretserTilNyKommune(gyldighetsDato: LocalDate, grunnkretserSomFlyttesTilKommune: List<GrunnkretsManus>, gammelKommune: Kommune, nyKommune: Kommune)
        : List<Grunnkrets> {
        val grunnkretser = gammelKommune.getGrunnkretser(gyldighetsDato)
            .associateBy { it.grunnkretsnummer }

        return grunnkretserSomFlyttesTilKommune.map { grunnkretsManus ->
                grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(
                    grunnkrets = grunnkretser[grunnkretsManus.gammeltGrunnkretsNummer] ?: throw RuntimeException("Fant ikke grunnkrets med grunnkretsnummer ${grunnkretsManus.gammeltGrunnkretsNummer}"),
                    kommune = nyKommune,
                    nyttGrunnkretsNummer = grunnkretsManus.nyttGrunnkretsNummer,
                    gyldigFra = gyldighetsDato,
                    endringstype = Endringstype.VEDTATT_DELING
                )
            }
    }

    private fun flyttStemmekretserTilNyKommune(gyldighetsDato: LocalDate, stemmekretserSomFlyttesTilKommune: List<StemmekretsManus>, gammelKommune: Kommune, nyKommune: Kommune): List<Stemmekrets> {
        val stemmekretser = gammelKommune.getStemmekretser(gyldighetsDato)
            .associateBy { it.stemmekretsnummer }

        return stemmekretserSomFlyttesTilKommune.map { stemmekretsManus ->
            stemmekretsService.createNyVersjonAvStemmekretsIAnnenKommune(
                stemmekrets = stemmekretser[stemmekretsManus.stemmekretsId]  ?: throw RuntimeException("Fant ikke stemmekrets med stemmekretsnummer ${stemmekretsManus.stemmekretsId}"),
                kommune = nyKommune,
                gyldigFra = gyldighetsDato,
                endringstype = Endringstype.VEDTATT_DELING
            )
        }
    }

    private fun opprettNyeKommunerForKommunedeling(kommunedelingManus: KommunedelingManus, originalKommune: Kommune): List<Kommune> =
        kommunedelingManus.nyeKommuner.map { kommune ->
            kommuneService.createNyKommuneMedNyUnikLokalid(
                kopierFraKommune = originalKommune,
                nyttKommunenummerIKlartekst = kommune.kommunenummer,
                gyldigFra =  kommunedelingManus.fraDato,
                endringstype = Endringstype.VEDTATT_DELING,
                admenhetnavnList = kommune.navn.map {
                    Administrativenhetnavn().apply {
                        spraak = spraakkodeRepository.findByIdOrNull(it.spraak) ?: throw RuntimeException("Fant ikke språkkode: ${it.spraak}")
                        navn = it.navn
                        rekkefoelge = it.rekkefoelge
                        setGyldigFra(kommunedelingManus.fraDato)
                    }
                }
            )
        }

}
