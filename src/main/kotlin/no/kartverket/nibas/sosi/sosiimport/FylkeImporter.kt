package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.FylkeInformasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeInformasjonRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.sosi.model.SosiFlate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger


@Component
class FylkeImporter @Autowired constructor(
    private val kodelisteService: KodelisteService,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository,
    private val fylkeInformasjonRepository: FylkeInformasjonRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
    private val spraakkodeRepository: SpraakkodeRepository,
    private val administrativEnhetImporter: AdministrativEnhetImporter
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    @Transactional
    fun importerSosiFylke(flate: SosiFlate) {
        val lokalid = flate.getIdent()?.getLokalId()!!
        when (fylkeRepository.findByLokalid(lokalid)) {
            null -> {
                var fylke = administrativEnhetImporter.importer(flate) as Fylke

                fylke.fylkesnummer =
                    kodelisteService.getFylkesnummerIdByCodeValue(
                        flate.getFylkesnummer()
                            ?: throw RuntimeException("Fylkesnummer ikke satt i modellen for fylke med flate-id $lokalid")
                    )
                fylke.samiskforvaltningsomraade = flate.isSamiskForvaltningsomrade()

                fylke.nasjon = nasjonRepository.findAll().first()
                fylke = fylkeRepository.save(fylke)

                if (flate.getInformasjon() != null) {
                    val fylkeInformasjon = FylkeInformasjon(fylke)
                    fylkeInformasjon.informasjon = flate.getInformasjon()
                    fylkeInformasjon.fylkeFk = fylke
                    fylkeInformasjonRepository.save(fylkeInformasjon)
                }

                flate.getFylkesnavn().forEach { sosiFylkesnavn ->
                    val fylkesnavn = Administrativenhetnavn()
                    fylkesnavn.navn = sosiFylkesnavn.getNavn()
                    fylkesnavn.spraak = sosiFylkesnavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
                    fylkesnavn.rekkefoelge = sosiFylkesnavn.getRekkefolge()
                    fylkesnavn.fylkeFk = fylke
                    administrativenhetnavnRepository.save(fylkesnavn)
                }
            }
            else -> {}
        }
    }
}
