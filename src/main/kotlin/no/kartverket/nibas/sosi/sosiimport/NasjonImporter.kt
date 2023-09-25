package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.repository.UnikLokalidRepository
import no.kartverket.nibas.sosi.model.SosiFlate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger

@Component
class NasjonImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var unikLokalidRepository: UnikLokalidRepository

    @Autowired
    lateinit var nasjonRepository: NasjonRepository

    @Autowired
    lateinit var spraakkodeRepository: SpraakkodeRepository

    @Autowired
    lateinit var administrativenhetnavnRepository: AdministrativenhetnavnRepository

    @Autowired
    lateinit var administrativEnhetImporter: AdministrativEnhetImporter

    @Transactional
    fun importerSosiNasjon(flate: SosiFlate) {
        when (nasjonRepository.findLastByLokalid(flate.getIdent()?.getLokalId()!!)) {
            null -> {
                var nasjon = administrativEnhetImporter.importer(flate) as Nasjon
                var unikLokalid = UnikLokalid(nasjon.lokalid, Flatetype.NASJON)
                unikLokalid = unikLokalidRepository.save(unikLokalid)

                nasjon = nasjonRepository.save(nasjon)
                nasjon.addNasjonTilUnikLokalid(unikLokalid)

                flate.getNasjonnavn().forEach { sosiNasjonnavn ->
                    var nasjonnavn = Administrativenhetnavn()
                    nasjonnavn.navn = sosiNasjonnavn.getNavn()
                    nasjonnavn.spraak = sosiNasjonnavn.getSprak()?.let { spraakkodeRepository.findByIdOrNull(it) }
                    nasjonnavn.rekkefoelge = sosiNasjonnavn.getRekkefolge()

                    nasjonnavn = administrativenhetnavnRepository.save(nasjonnavn)
                    nasjon.addAdministrativenhetNavn(nasjonnavn)
                }
            }
            else -> {}
        }
    }

}
