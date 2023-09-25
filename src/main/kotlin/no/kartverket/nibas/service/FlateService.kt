package no.kartverket.nibas.service

import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.EnhetsGrenseRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.sosi.sosiimport.GrenseMedLokalid
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
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
    private val kodelisteService: KodelisteService,
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
        grunnkrets.oppdaterOmraade()
        grunnkretsRepository.save(grunnkrets)
    }

    private fun oppdaterStemmekretsflate(stemmekrets: Stemmekrets) {
        logger.info("Oppdaterer stemmekretsflate for ${finnNavn(stemmekrets)}")
        stemmekrets.oppdaterOmraade()
        stemmekretsRepository.save(stemmekrets)
    }

    private fun oppdaterKommuneflate(kommune: Kommune) {
        logger.info("Oppdaterer kommuneflate for ${finnNavn(kommune)}")
        val kommunenummerVerdi = kodelisteService.getCodeValueByKommunenummerId(kommune.kommunenummer)
        val kommunegrenser = enhetsGrenseRepository.hentGrenserMedRiktigRetning(kommunenummerVerdi, kommune.tillatteGrensetyper())
        kommune.omraade = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(kommunegrenser.map{ GrenseMedLokalid(it.grense, it.lokalid) }, kommune.lokalid)
        kommuneRepository.save(kommune)
    }

    private fun oppdaterFylkesflate(fylke: Fylke) {
        logger.info("Oppdaterer fylkesflate for ${finnNavn(fylke)}")
        val fylkesnummerVerdi = kodelisteService.getCodeValueByFylkesnummerId(fylke.fylkesnummer)
        val fylkesgrenser = enhetsGrenseRepository.hentGrenserMedRiktigRetning(fylkesnummerVerdi!!, fylke.tillatteGrensetyper())
        fylke.omraade = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(fylkesgrenser.map{ GrenseMedLokalid(it.grense, it.lokalid) })
        fylkeRepository.save(fylke)
    }

    private fun oppdaterNasjonsflate(nasjon: Nasjon) {
        logger.info("Oppdaterer nasjonsflate for ${finnNavn(nasjon)}")
        val riksgrenser = enhetsGrenseRepository.hentGrenserMedRiktigRetning("", nasjon.tillatteGrensetyper())
        nasjon.omraade = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(riksgrenser.map { GrenseMedLokalid(it.grense, it.lokalid) })
        nasjonRepository.save(nasjon)
    }

    fun finnNavn(enhet: Flate): String? = when (enhet) {
        is AdministrativEnhet -> enhet.getAdministrativenhetNavn(enhet.gyldighet.gyldigFra).getInSpraak("nor")
        is Grunnkrets -> enhet.grunnkretsnavn
        is Stemmekrets -> enhet.stemmekretsnavn
        else -> "Ukjent enhet"
    }
}
