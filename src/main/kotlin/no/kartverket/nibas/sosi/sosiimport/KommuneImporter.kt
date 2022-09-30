package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.KommuneInformasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneInformasjonRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiObjekt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger

@Component
class KommuneImporter @Autowired constructor(
    private val kodelisteService: KodelisteService,
    private val kommuneInformasjonRepository: KommuneInformasjonRepository,
    private val fylkeRepository: FylkeRepository,
    private val kommuneRepository: KommuneRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
    private val spraakkodeRepository: SpraakkodeRepository,
    private val administrativEnhetImporter: AdministrativEnhetImporter
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiKommune(flater: List<SosiFlate>) {

        // Henter egenskapene bare fra første flate. Egenskapene er/skal være like på tvers av flatene.
        val forsteFlate = flater.first()

        var kommune = administrativEnhetImporter.importer(forsteFlate) as Kommune
        kommune.kommunenummer = kodelisteService.getKommunenummerIdByCodeValue(forsteFlate.getKommunenummer())
        kommune.samiskforvaltningsomraade = forsteFlate.isSamiskForvaltningsomrade()



        // Fylkesnummer er de to første sifferne i kommunenummeret
        kommune.fylke = fylkeRepository.findByFylkesnummer(
            kodelisteService.getFylkesnummerIdByCodeValue(
                forsteFlate.getKommunenummer().substring(0, 2)
            )
        )!!
        kommune = kommuneRepository.save(kommune)

        if (forsteFlate.getInformasjon() != null) {
            val kommuneInformasjon = KommuneInformasjon(kommune)
            kommuneInformasjon.informasjon = forsteFlate.getInformasjon()
            kommuneInformasjon.kommuneFk = kommune
            kommuneInformasjonRepository.save(kommuneInformasjon)
        }

        forsteFlate.getKommunenavn().forEach { sosiKommunenavn ->
            val kommunenavn = Administrativenhetnavn()
            kommunenavn.navn = sosiKommunenavn.getNavn()
            kommunenavn.spraak = sosiKommunenavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
            kommunenavn.rekkefoelge = sosiKommunenavn.getRekkefolge()
            kommunenavn.kommuneFk = kommune
            administrativenhetnavnRepository.save(kommunenavn)
        }
    }
}
