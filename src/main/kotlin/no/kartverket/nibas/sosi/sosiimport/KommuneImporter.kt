package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.KommuneInformasjon
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneInformasjonRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.repository.UnikLokalidRepository
import no.kartverket.nibas.repository.findGyldigFylkeForFylkesnummer
import no.kartverket.nibas.sosi.common.ImportUtils
import no.kartverket.nibas.sosi.model.SosiFlate
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
    private var unikLokalidRepository: UnikLokalidRepository,
    private val administrativEnhetImporter: AdministrativEnhetImporter
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiKommune(flater: List<SosiFlate>) {

        // Henter egenskapene bare fra første flate. Egenskapene er/skal være like på tvers av flatene.
        val forsteFlate = flater.first()

        var kommune = administrativEnhetImporter.importer(forsteFlate) as Kommune
        kommune.kommunenummer = forsteFlate.getKommunenummer()
        kommune.kommunenummerUuid = kodelisteService.getKommunenummerIdByCodeValue(forsteFlate.getKommunenummer())
        kommune.samiskforvaltningsomraade = forsteFlate.isSamiskForvaltningsomrade()

        var unikLokalid = UnikLokalid(kommune.lokalid, Flatetype.KOMMUNE)
        unikLokalid = unikLokalidRepository.save(unikLokalid)

        kommune = kommuneRepository.save(kommune)
        kommune.addKommuneTilUnikLokalid(unikLokalid)

        // Fylkesnummer er de to første sifferne i kommunenummeret
        val fylkesnummer = forsteFlate.getKommunenummer().substring(0, 2)
        //Henter fylket for dagens dato
        val gyldighetsdato = ImportUtils.importTime
        val fylke = fylkeRepository.findGyldigFylkeForFylkesnummer(fylkesnummer, gyldighetsdato)
            ?: throw RuntimeException("Fant ikke fylke med fylkesnummer $fylkesnummer og gyldig fra $gyldighetsdato")

        fylke.addKommune(kommune)

        if (forsteFlate.getInformasjon() != null) {
            val kommuneInformasjon = KommuneInformasjon(kommune)
            kommuneInformasjon.informasjon = forsteFlate.getInformasjon()
            kommuneInformasjon.kommuneFk = kommune
            kommuneInformasjonRepository.save(kommuneInformasjon)
        }

        forsteFlate.getKommunenavn().forEach { sosiKommunenavn ->
            var kommunenavn = Administrativenhetnavn()
            kommunenavn.navn = sosiKommunenavn.getNavn()
            kommunenavn.spraak = sosiKommunenavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
            kommunenavn.rekkefoelge = sosiKommunenavn.getRekkefolge()
            kommunenavn = administrativenhetnavnRepository.save(kommunenavn)

            kommune.addAdministrativenhetNavn(kommunenavn)
        }
    }
}
