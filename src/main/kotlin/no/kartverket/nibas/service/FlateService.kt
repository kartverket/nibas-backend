package no.kartverket.nibas.service

import no.kartverket.nibas.common.geometry.getGrenserIRiktigRekkefoelge
import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.common.geometry.toMultiPolygon
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.repository.EnhetsGrenseRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class FlateService @Autowired constructor(
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val stemmekretsRepository: StemmekretsRepository,
    private val enhetsGrenseRepository: EnhetsGrenseRepository
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun oppdaterFlaterFor(enheterSomSkalFlatedannes: Set<Flate>) {
        enheterSomSkalFlatedannes.forEach { enhet ->
            when (enhet) {
                is Grunnkrets -> oppdaterGrunnkretsflate(enhet)
                is Stemmekrets -> oppdaterStemmekretsflate(enhet)
                is Kommune -> oppdaterKommuneflate(enhet)
                is Fylke -> oppdaterFylkesflate(enhet)
                is Nasjon -> oppdaterNasjonsflate(enhet)
            }
        }
    }

    private fun oppdaterGrunnkretsflate(grunnkrets: Grunnkrets) {
        logger.info("Oppdaterer grunnkretsflate for ${finnNavn(grunnkrets)}")
        grunnkrets.oppdaterOmraadeOgPosisjon()
        grunnkretsRepository.save(grunnkrets)
    }

    private fun oppdaterStemmekretsflate(stemmekrets: Stemmekrets) {
        logger.info("Oppdaterer stemmekretsflate for ${finnNavn(stemmekrets)}")
        stemmekrets.oppdaterOmraadeOgPosisjon()
        stemmekretsRepository.save(stemmekrets)
    }

    private fun oppdaterKommuneflate(kommune: Kommune) {
        val kommunenummerVerdi = kommune.kommunenummer
        logger.info("Oppdaterer kommuneflate for $kommunenummerVerdi - ${finnNavn(kommune)}")

        val kommunegrenser = enhetsGrenseRepository.hentGrenserAvTyperForEnhet(kommunenummerVerdi, kommune.tillatteGrensetyper(), kommune.gyldighet.gyldigFra)

        kommune.omraade = getGrenserIRiktigRekkefoelge(kommunegrenser.map { it.grense }).toMultiPolygon()
        if (kommune.omraade != null && (kommune.posisjon == null || !kommune.omraade!!.contains(kommune.posisjon))) {
            kommune.posisjon = kommune.omraade?.interiorPoint
        }

        // Om posisjonen er Point Empty setter vi den til null så ting ikke tryner når man senere sammenligner med andre punkter
        if(kommune.posisjon?.isEmpty == true) {
            kommune.posisjon = null
        }

        kommuneRepository.save(kommune)
    }

    private fun oppdaterFylkesflate(fylke: Fylke) {
        logger.info("Oppdaterer fylkesflate for ${fylke.fylkesnummer} - ${finnNavn(fylke)}")
        val fylkesgrenser = enhetsGrenseRepository.hentGrenserAvTyperForEnhet(fylke.fylkesnummer, fylke.tillatteGrensetyper(), fylke.gyldighet.gyldigFra)

        fylke.omraade = getGrenserIRiktigRekkefoelge(fylkesgrenser.map { it.grense }).toMultiPolygon()
        if (fylke.omraade != null && (fylke.posisjon == null || !fylke.omraade!!.contains(fylke.posisjon))) {
            fylke.posisjon = fylke.omraade?.interiorPoint
        }

        // Om posisjonen er Point Empty setter vi den til null så ting ikke tryner når man senere sammenligner med andre punkter
        if (fylke.posisjon?.isEmpty == true) {
            fylke.posisjon = null
        }
        fylkeRepository.save(fylke)
    }

    private fun oppdaterNasjonsflate(nasjon: Nasjon) {
        logger.info("Oppdaterer nasjonsflate for ${finnNavn(nasjon)}")
        val riksgrenser = enhetsGrenseRepository.hentGrenserAvTyperForEnhet("", nasjon.tillatteGrensetyper(), nasjon.gyldighet.gyldigFra)

        nasjon.omraade = getGrenserIRiktigRekkefoelge(riksgrenser.map { it.grense }).toMultiPolygon()
        if (nasjon.omraade != null && (nasjon.posisjon == null || !nasjon.omraade!!.contains(nasjon.posisjon))) {
            nasjon.posisjon = nasjon.omraade?.interiorPoint
        }
        nasjonRepository.save(nasjon)
    }

    fun finnNavn(enhet: Flate): String? = when (enhet) {
        is AdministrativEnhet -> enhet.getAdministrativenhetNavn(enhet.gyldighet.gyldigFra).getInSpraak("nor")
        is Grunnkrets -> enhet.grunnkretsnavn
        is Stemmekrets -> enhet.stemmekretsnavn
        else -> "Ukjent enhet"
    }
}
