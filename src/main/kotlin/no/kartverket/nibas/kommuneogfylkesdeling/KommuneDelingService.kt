package no.kartverket.nibas.kommuneogfylkesdeling

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.StemmekretsService
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
    private val grenseService: GrenseService
) {
    fun delKommuner(kommunedelingManuser: List<KommunedelingManus>) {
        kommunedelingManuser.forEach { delKommune(it) }
    }

    @Transactional
    fun delKommune(kommuneDelingManus: KommunedelingManus) {
        val kommuennummerUuid = kodelisteService.getKommunenummerIdByCodeValue(kommuneDelingManus.gammeltKommuneNummer)
        val kommuneSomSkalDeles = kommuneService.hentKommuneByKommunenummer(kommuennummerUuid, kommuneDelingManus.fraDato)
            ?: throw RuntimeException("Klarte ikke finne kommunen som skulle deles. Gjelder kommunenummer: ${kommuneDelingManus.gammeltKommuneNummer}")

        val nyeKommuner = opprettNyeKommunerForKommunedeling(kommuneDelingManus, kommuneSomSkalDeles)
        var nyeGrunnkretser: List<Grunnkrets> = listOf()

        kommuneDelingManus.nyeKommuner.forEach { nyKommuneManus ->
            val kommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(nyKommuneManus.kommunenummer)
            val nyKommune = nyeKommuner.find { it.kommunenummer == kommunenummerId }!!

            nyeGrunnkretser = flyttGrunnkretserTilNyKommune(
                gyldighetsDato = kommuneDelingManus.fraDato,
                grunnkretserSomFlyttesTilKommune = nyKommuneManus.grunnkretser,
                gammelKommune = kommuneSomSkalDeles,
                nyKommune = nyKommune
            )

            flyttStemmekretserTilNyKommune(
                gyldighetsDato = kommuneDelingManus.fraDato,
                stemmekretserSomFlyttesTilKommune = nyKommuneManus.stemmekretser,
                gammelKommune = kommuneSomSkalDeles,
                nyKommune = nyKommune
            )
        }

        kommuneService.settKommuneSomHistorisk(kommuneSomSkalDeles, kommuneDelingManus.fraDato)

        // Oppdater grensetyper (få riktig kommunegrenser grunnet nye kretser) og flatedanne
        grenseService.endreGrensetypeForGrunnkretserOgFlatedannAdmEnheter(nyeGrunnkretser, kommuneDelingManus.fraDato, Endringstype.VEDTATT_DELING)
        // TODO: Skal også endre grensetype for stemmekretser

        // Sende events om endringene
        // TODO: Vi må huske å sende events!
    }

    private fun flyttGrunnkretserTilNyKommune(gyldighetsDato: LocalDate, grunnkretserSomFlyttesTilKommune: List<GrunnkretsManus>, gammelKommune: Kommune, nyKommune: Kommune)
        : List<Grunnkrets> {
        val grunnkretser = gammelKommune.getGrunnkretser(gyldighetsDato)
            .associateBy { it.grunnkretsnummer }

        val nyeGrunnkretser =
            grunnkretserSomFlyttesTilKommune.map { grunnkretsManus ->
                grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(
                    grunnkrets = grunnkretser[grunnkretsManus.gammeltGrunnkretsNummer]!!,
                    kommune = nyKommune,
                    nyttGrunnkretsNummer = grunnkretsManus.nyttGrunnkretsNummer,
                    gyldigFra = gyldighetsDato,
                    endringstype = Endringstype.VEDTATT_DELING
                )

            }.toList()
        return nyeGrunnkretser
    }

    private fun flyttStemmekretserTilNyKommune(gyldighetsDato: LocalDate, stemmekretserSomFlyttesTilKommune: List<StemmekretsManus>, gammelKommune: Kommune, nyKommune: Kommune) {
        val stemmekretser = gammelKommune.getStemmekretser(gyldighetsDato)
            .associateBy { it.stemmekretsnummer }

        stemmekretserSomFlyttesTilKommune.forEach { stemmekretsManus ->
            stemmekretsService.createNyVersjonAvStemmekretsIAnnenKommune(
                stemmekrets = stemmekretser[stemmekretsManus.stemmekretsId]!!,
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
