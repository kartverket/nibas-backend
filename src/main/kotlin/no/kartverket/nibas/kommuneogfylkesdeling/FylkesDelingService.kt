package no.kartverket.nibas.kommuneogfylkesdeling

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.service.FlateService
import no.kartverket.nibas.service.FylkeService
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
class FylkesDelingService(
    private val fylkeService: FylkeService,
    private val kommuneService: KommuneService,
    private val kodelisteService: KodelisteService,
    private val spraakkodeRepository: SpraakkodeRepository,
    private val stemmekretsService: StemmekretsService,
    private val grunnkretsService: GrunnkretsService,
    private val grenseService: GrenseService,
    private val flateService: FlateService
) {
    val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun delFylker(fylkesdelingManuser: List<FylkesdelingManus>) {
        fylkesdelingManuser.forEach { delFylke(it) }
    }

    @Transactional
    fun delFylke(fylkesdelingManus: FylkesdelingManus) {
        logger.info("Deler fylke ${fylkesdelingManus.gammeltFylkesNummer} til ${fylkesdelingManus.nyeFylker.joinToString(", ") { it.fylkesNummer }}")

        val fylkesnummerUuid = kodelisteService.getFylkesnummerIdByCodeValue(fylkesdelingManus.gammeltFylkesNummer)
        val fylkeSomSkalDeles = fylkeService.hentAlleFylker(fylkesdelingManus.fraDato)
            .firstOrNull { it.fylkesnummer == fylkesnummerUuid }
            ?: throw RuntimeException("Klarte ikke finne fylket som skulle deles. Gjelder fylkesnummer: ${fylkesdelingManus.gammeltFylkesNummer}")

        val nyeFylker = opprettNyeFylkerForFylkesdeling(fylkesdelingManus, fylkeSomSkalDeles)

        fylkesdelingManus.nyeFylker.forEach {
            flyttKommunerTilNyttFylke(fylkesdelingManus.fraDato, it.kommuner, fylkeSomSkalDeles)
        }

        fylkeService.settFylkeSomHistorisk(fylkeSomSkalDeles, fylkesdelingManus.fraDato)

        logger.info("Oppdaterer flaten for de nye fylkene")
        flateService.oppdaterFlaterFor(nyeFylker.toSet())

        // 5. Sende events om endringene
        // TODO: Vi må huske å sende events!
    }

    private fun flyttKommunerTilNyttFylke(gyldighetsDato: LocalDate, kommunerSomSkalFlyttesTilNyttFylke: List<FylkesDelingKommuneManus>, gammeltFylke: Fylke) {
        val kommuner = gammeltFylke.getKommuner(gyldighetsDato)
            .associateBy { it.kommunenummer }

        val nyeGrunnkretser = mutableListOf<Grunnkrets>()
        val nyeStemmekretser = mutableListOf<Stemmekrets>()

        kommunerSomSkalFlyttesTilNyttFylke.forEach { fylkesDelingKommuneManus ->
            val gammeltKommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(fylkesDelingKommuneManus.gammeltKommunenummer)
            val gammelKommune = kommuner[gammeltKommunenummerId]!!

            logger.info("Flytter kommune ${fylkesDelingKommuneManus.gammeltKommunenummer} til nytt fylke: ${fylkesDelingKommuneManus.nyttKommuneNummer.take(2)}")

            val kommune = kommuneService.createNyKommuneMedNyUnikLokalid(
                kopierFraKommune = gammelKommune,
                gyldigFra = gyldighetsDato,
                endringstype = Endringstype.VEDTATT_DELING,
                nyttKommunenummerIKlartekst = fylkesDelingKommuneManus.nyttKommuneNummer
            )

            nyeGrunnkretser += flyttGrunnkretserTilNyKommune(kommune, gammelKommune, gyldighetsDato, fylkesDelingKommuneManus.grunnkretser)
            nyeStemmekretser += flyttStemmekretserTilNyKommune(kommune, gammelKommune, gyldighetsDato)

            kommuneService.settKommuneSomHistorisk(gammelKommune, gyldighetsDato)
        }

        logger.info("Endrer grensetyper for splittingen av ${kodelisteService.getCodeValueByFylkesnummerId(gammeltFylke.fylkesnummer)}")
        grenseService.endreGrensetypeForGrunnkretser(nyeGrunnkretser, gyldighetsDato, Endringstype.VEDTATT_DELING)
        grenseService.endreGrensetypeForStemmekretser(nyeStemmekretser, gyldighetsDato, Endringstype.VEDTATT_DELING)

    }
    private fun flyttGrunnkretserTilNyKommune(nyKommune: Kommune, gammelKommune: Kommune, gyldighetsDato: LocalDate, grunnkretsManuser: List<GrunnkretsManus>): List<Grunnkrets> {
        val grunnkretser = gammelKommune.getGrunnkretser(gyldighetsDato)
        return grunnkretser.map { grunnkrets ->
            val grunnkretsManus = grunnkretsManuser.firstOrNull { it.gammeltGrunnkretsNummer == grunnkrets.grunnkretsnummer }
                ?: throw RuntimeException("Fant ikke manus for omnummerering av grunnkrets ${grunnkrets.grunnkretsnummer}")

            grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(grunnkrets, nyKommune, grunnkretsManus.nyttGrunnkretsNummer, gyldighetsDato, Endringstype.VEDTATT_DELING)
        }
    }

    private fun flyttStemmekretserTilNyKommune(nyKommune: Kommune, gammelKommune: Kommune, gyldighetsDato: LocalDate): List<Stemmekrets> {
        val stemmekretser = gammelKommune.getStemmekretser(gyldighetsDato)
        return stemmekretser.map {
            stemmekretsService.createNyVersjonAvStemmekretsIAnnenKommune(it, nyKommune, gyldighetsDato, Endringstype.VEDTATT_DELING)
        }
    }

    private fun opprettNyeFylkerForFylkesdeling(fylkesdelingManus: FylkesdelingManus, originaltFylke: Fylke): List<Fylke> {
        return fylkesdelingManus.nyeFylker.map { fylke ->
            fylkeService.createNyttFylkeMedNyUnikLokalid(
                kopierFraFylke = originaltFylke,
                nyttFylkesnummerIKlartekst = fylke.fylkesNummer,
                gyldigFra = fylkesdelingManus.fraDato,
                endringstype = Endringstype.VEDTATT_DELING,
                admenhetnavnList = fylke.navn.map {
                    Administrativenhetnavn().apply {
                        spraak = spraakkodeRepository.findByIdOrNull(it.spraak) ?: throw RuntimeException("Fant ikke språkkode: ${it.spraak}")
                        navn = it.navn
                        rekkefoelge = it.rekkefoelge
                        setGyldigFra(fylkesdelingManus.fraDato)
                    }
                }
            )
        }
    }
}
