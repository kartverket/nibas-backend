package no.kartverket.nibas.kommuneogfylkesdeling

import org.springframework.stereotype.Service

@Service
class KommuneOgFylkesDelingJob(
    val kommuneDelingService: KommuneDelingService,
    val fylkesDelingService: FylkesDelingService,
) {
    fun delFylkerOgKommuner() {
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

        fylkesDelingService.delFylker(fylkesdelingManus)
        kommuneDelingService.delKommuner(kommunedelingManus)
    }
}
