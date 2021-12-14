package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.repository.FylkesgrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class FylkesgrenseImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var fylkesgrenseRepository: FylkesgrenseRepository

    @Autowired
    lateinit var administrativGrenseImporter: AdministrativGrenseImporter

    @Autowired
    lateinit var dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter

    fun importerFylkesgrense(sosiFylkesgrense: SosiGeometri): Grense {
        return when (val existing = fylkesgrenseRepository.findByLokalid(sosiFylkesgrense.getIdent()?.getLokalId()!!)) {
            null -> {
                val fylkesgrense = fylkesgrenseRepository.save(
                    administrativGrenseImporter.importer(sosiFylkesgrense) as Fylkesgrense
                )

                sosiFylkesgrense.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, fylkesgrense)
                }
                return fylkesgrense
            }
            else -> existing
        }
    }
}