package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.geometry.UgyldigFlateInformasjon
import no.kartverket.nibas.common.geometry.UgyldigeFlaterException
import no.kartverket.nibas.common.geometry.UgyldigGeometriException
import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FlateService @Autowired constructor(
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val stemmekretsRepository: StemmekretsRepository,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun oppdaterFlaterFor(enheterSomSkalFlatedannes: Set<Flate>) {
        val flatefeil: MutableList<UgyldigFlateInformasjon> = mutableListOf()

        enheterSomSkalFlatedannes.forEach { enhet ->
            try {
                when (enhet) {
                    is Grunnkrets -> oppdaterGrunnkretsflate(enhet)
                    is Stemmekrets -> oppdaterStemmekretsflate(enhet)
                    is Kommune -> oppdaterKommuneflate(enhet)
                    is Fylke -> oppdaterFylkesflate(enhet)
                    is Nasjon -> oppdaterNasjonsflate(enhet)
                }
            } catch (ex: UgyldigGeometriException){
                flatefeil.add(UgyldigFlateInformasjon(enhet.lokalid, enhet.getFlateInformasjon(), ex.topologyValidationError))
            }
        }

        if (flatefeil.isNotEmpty()) {
            throw UgyldigeFlaterException(flatefeil)
        }
    }

    private fun oppdaterGrunnkretsflate(grunnkrets: Grunnkrets) {
        logger.info("Oppdaterer grunnkretsflate for ${grunnkrets.grunnkretsnummer} ${finnNavn(grunnkrets)}")
        grunnkrets.oppdaterOmraadeOgPosisjon()
        grunnkretsRepository.save(grunnkrets)
    }

    private fun oppdaterStemmekretsflate(stemmekrets: Stemmekrets) {
        logger.info("Oppdaterer stemmekretsflate for ${stemmekrets.stemmekretsnummer} ${finnNavn(stemmekrets)} i kommune ${stemmekrets.getKommune(stemmekrets.gyldighet.gyldigFra)?.kommunenummer}")
        stemmekrets.oppdaterOmraadeOgPosisjon()
        stemmekretsRepository.save(stemmekrets)
    }

    private fun oppdaterKommuneflate(kommune: Kommune) {
        logger.info("Oppdaterer kommuneflate for ${kommune.kommunenummer} ${finnNavn(kommune)}")
        val kommunegrenser = getTilatteGrenserForEnhet(kommune, kommune.gyldighet.gyldigFra, Kommune.tillatteGrensetyper())
        kommune.oppdaterOmraadeOgPosisjon(kommunegrenser)
        kommuneRepository.save(kommune)
    }

    private fun oppdaterFylkesflate(fylke: Fylke) {
        logger.info("Oppdaterer fylkesflate for ${fylke.fylkesnummer} ${finnNavn(fylke)}")
        val fylkesgrenser = getTilatteGrenserForEnhet(fylke, fylke.gyldighet.gyldigFra, Fylke.tillatteGrensetyper())
        fylke.oppdaterOmraadeOgPosisjon(fylkesgrenser)
        fylkeRepository.save(fylke)
    }

    private fun oppdaterNasjonsflate(nasjon: Nasjon) {
        logger.info("Oppdaterer nasjonsflate for ${finnNavn(nasjon)}")
        val riksgrenser = getTilatteGrenserForEnhet(nasjon, nasjon.gyldighet.gyldigFra, Nasjon.tillatteGrensetyper())
        nasjon.oppdaterOmraadeOgPosisjon(riksgrenser)
        nasjonRepository.save(nasjon)
    }

    fun getTilatteGrenserForEnhet(enhet: Flate, gyldigFra: LocalDate, tilatteGrenseTyper: Set<Grensetype>): List<Grense> {
        return enhet.getGrensekoblinger(gyldigFra)
            .map { it.grense }
            .filter { tilatteGrenseTyper.contains(it.grensetype) }
    }

    fun finnNavn(enhet: Flate): String? = when (enhet) {
        is AdministrativEnhet -> enhet.getAdministrativenhetNavn(enhet.gyldighet.gyldigFra).getInSpraak("nor")
        is Grunnkrets -> enhet.grunnkretsnavn
        is Stemmekrets -> enhet.stemmekretsnavn
        else -> "Ukjent enhet"
    }
}
