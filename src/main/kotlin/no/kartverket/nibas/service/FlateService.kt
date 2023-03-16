package no.kartverket.nibas.service

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
import no.kartverket.nibas.repository.findAllGyldigPaaDato
import no.kartverket.nibas.sosi.sosiimport.GrenseMedLokalid
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

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
                is Grunnkrets -> oppdaterGrunnkretsflater(listOf(enhet))
                is Stemmekrets -> oppdaterStemmekretsflater(listOf(enhet))
                is Kommune -> oppdaterKommuneflater(listOf(enhet))
                is Fylke -> oppdaterFylkesflater(listOf(enhet))
                is Nasjon -> oppdaterNasjonsflate(listOf(enhet))
            }
        }
    }

    @Transactional
    fun settFlaterPaaAlleAdministrativeEnheter(gyldighetsdato: LocalDate = LocalDate.now()) {
        oppdaterFylkesflater(fylkeRepository.findAllGyldigPaaDato(gyldighetsdato).toList())
        oppdaterKommuneflater(kommuneRepository.findAllGyldigPaaDato(gyldighetsdato).toList())

        try {
            oppdaterNasjonsflate(nasjonRepository.findAllGyldigPaaDato(gyldighetsdato).toList())
        } catch (e: Exception) {
            logger.warn("Flatedanning for nasjon feilet, dette er forventet dersom man ikke importerer alle fylker", e)
        }
    }

    private fun oppdaterGrunnkretsflater(grunnkretser: List<Grunnkrets>) {
        grunnkretser.forEach { grunnkrets ->
            logger.info("Oppdaterer flate for ${finnNavn(grunnkrets)}")
            grunnkrets.oppdaterOmraade()
            grunnkretsRepository.save(grunnkrets)
        }
    }

    private fun oppdaterStemmekretsflater(stemmekretser: List<Stemmekrets>) {
        stemmekretser.forEach { stemmekrets ->
            logger.info("Oppdaterer flate for ${finnNavn(stemmekrets)}")
            stemmekrets.oppdaterOmraade()
            stemmekretsRepository.save(stemmekrets)
        }
    }

    private fun oppdaterKommuneflater(kommuner: List<Kommune>) {
        kommuner.forEach { kommune ->
            logger.info("Oppdaterer flate for ${finnNavn(kommune)}")
            val kommunenummerVerdi = kodelisteService.getCodeValueByKommunenummerId(kommune.kommunenummer)
            val kommunegrenser = enhetsGrenseRepository.hentGrenserMedRiktigRetning(kommunenummerVerdi, kommune.tillatteGrensetyper())
            kommune.omraade = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(kommunegrenser.map{ GrenseMedLokalid(it.grense, it.lokalid) }, kommune.lokalid)
            kommuneRepository.save(kommune)
        }
    }

    private fun oppdaterFylkesflater(fylker: List<Fylke>) {
        fylker.forEach { fylke ->
            logger.info("Oppdaterer flate for ${finnNavn(fylke)}")
            val fylkesnummerVerdi = kodelisteService.getCodeValueByFylkesnummerId(fylke.fylkesnummer)
            val fylkesgrenser = enhetsGrenseRepository.hentGrenserMedRiktigRetning(fylkesnummerVerdi!!, fylke.tillatteGrensetyper())
            fylke.omraade = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(fylkesgrenser.map{ GrenseMedLokalid(it.grense, it.lokalid) })
            fylkeRepository.save(fylke)
        }
    }

    private fun oppdaterNasjonsflate(nasjoner: List<Nasjon>) {
        nasjoner.forEach { nasjon ->
            logger.info("Oppdaterer flate for ${finnNavn(nasjon)}")
            val riksgrenser = enhetsGrenseRepository.hentGrenserMedRiktigRetning("", nasjon.tillatteGrensetyper())
            nasjon.omraade = SosiGeometryFunctions.dannFlaterMedHullBasertPaaGeometri(riksgrenser.map{ GrenseMedLokalid(it.grense, it.lokalid) })
            nasjonRepository.save(nasjon)
        }
    }

    private fun finnNavn(enhet: Flate): String? = when (enhet) {
        is AdministrativEnhet -> enhet.administrativenhetnavn.map { it.navn }.toString()
        is Grunnkrets -> enhet.grunnkretsnavn
        is Stemmekrets -> enhet.stemmekretsnavn
        else -> "Ukjent enhet"
    }
}
