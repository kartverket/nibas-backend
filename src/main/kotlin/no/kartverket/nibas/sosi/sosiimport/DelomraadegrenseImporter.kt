package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DelomraadegrenseImporter @Autowired constructor(
    private val grenseRepository: GrenseRepository
) {

    @Autowired
    lateinit var statistiskGrenseImporter: StatistiskGrenseImporter

    fun importerDelomraadegrense(sosiDelomraadegrense: SosiGeometri): Grense {
        val sosiIdent = sosiDelomraadegrense.getIdent()
        return when (val eksisterende = grenseRepository.findByLokalid(sosiIdent?.getLokalId()!!)) {
            null -> {
                return grenseRepository.save(statistiskGrenseImporter.importer(sosiDelomraadegrense))
            }
            else -> eksisterende
        }

    }
}
