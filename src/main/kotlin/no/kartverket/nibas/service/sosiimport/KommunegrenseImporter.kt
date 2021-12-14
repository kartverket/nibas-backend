package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.repository.KommunegrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class KommunegrenseImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var kommunegrenseRepository: KommunegrenseRepository

    @Autowired
    lateinit var administrativGrenseImporter: AdministrativGrenseImporter

    @Autowired
    lateinit var dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter

    fun importerKommunegrense(sosiKommunegrense: SosiGeometri): Grense {
        return when (val existing = kommunegrenseRepository.findByLokalid(sosiKommunegrense.getIdent()?.getLokalId()!!)) {
            null -> {
                val kommunegrense = kommunegrenseRepository.save(
                    administrativGrenseImporter.importer(sosiKommunegrense) as Kommunegrense
                )

                sosiKommunegrense.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, kommunegrense)
                }
                return kommunegrense
            }
            else -> existing
        }
    }
}