package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class KommunegrenseImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var grenseRepository: GrenseRepository

    @Autowired
    lateinit var administrativGrenseImporter: AdministrativGrenseImporter

    @Autowired
    lateinit var dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter

    fun importerKommunegrense(sosiKommunegrense: SplitTilImport): Grense {
        return when (val existing = grenseRepository.findByLokalid(sosiKommunegrense.lokalId())) {
            null -> {
                val kommunegrense = grenseRepository.save(
                    administrativGrenseImporter.importer(sosiKommunegrense)
                )

                sosiKommunegrense.splitRef.basertPaGrense.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, kommunegrense)
                }
                return kommunegrense
            }
            else -> existing
        }
    }
}
