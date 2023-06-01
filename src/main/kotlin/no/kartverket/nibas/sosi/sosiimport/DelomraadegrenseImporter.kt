package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.GrenseRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DelomraadegrenseImporter @Autowired constructor(
    private val grenseRepository: GrenseRepository
) {

    @Autowired
    lateinit var statistiskGrenseImporter: StatistiskGrenseImporter

    fun importerDelomraadegrense(skalImporteres: SplitTilImport): Grense {
        return when (val eksisterende = grenseRepository.findLastByLokalid(skalImporteres.lokalId())) {
            null -> {
                return grenseRepository.save(statistiskGrenseImporter.importer(skalImporteres))
            }
            else -> eksisterende
        }

    }
}
