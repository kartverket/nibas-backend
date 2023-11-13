package no.kartverket.nibas.kommuneogfylkesdeling

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import jakarta.transaction.Transactional

@Service
class KommuneOgFylkesDelingJob(
    val kommuneDelingService: KommuneDelingService,
    val fylkesDelingService: FylkesDelingService,
) {
    val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun delFylkerOgKommuner() {
        logger.info("Starter å lese inn manus")
        val kommuneendringer = ManusReader.getKommuneEndringer()
        val grunnkretsEndringer = ManusReader.getGrunnkretsEndringer()

        val fylkesdelingManus = ManusMapper.mapToFylkesDelingerManus(
            fylkesendringer = ManusReader.getFylkeEndringer(),
            nyeNavn = ManusReader.getFylkesnavn(),
            kommuneendringer = kommuneendringer,
            grunnkretsEndringer = grunnkretsEndringer
        )

        val kommunedelingManus = ManusMapper.mapToKommuneDelingerManus(
            kommuneendringer = kommuneendringer,
            nyeKommuneNavn = ManusReader.getKommunenavn(),
            grunnkretserMedEndringer = grunnkretsEndringer,
            stemmekretserMedEndringer = ManusReader.getStemmekretsEndringer()
        )

        logger.info("Starter fylkesdeling")
        fylkesDelingService.delFylker(fylkesdelingManus)

        logger.info("Starter kommunedeling")
        kommuneDelingService.delKommuner(kommunedelingManus)

        logger.info("Ferdig med kommune og fylkesdelingen")
    }
}
