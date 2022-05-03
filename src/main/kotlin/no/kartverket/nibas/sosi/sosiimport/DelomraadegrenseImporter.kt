package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Delomraadegrense
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DelomraadegrenseImporter @Autowired constructor(
    private val delomraadegrenseRepository: DelomraadegrenseRepository
) {

    @Autowired
    lateinit var statistiskGrenseImporter: StatistiskGrenseImporter

    fun importerDelomraadegrense(sosiDelomraadegrense: SosiGeometri): Grense {
        val sosiIdent = sosiDelomraadegrense.getIdent()
        return when(val eksisterende = delomraadegrenseRepository.findByLokalid(sosiIdent?.getLokalId()!!)) {
            null -> {
                return delomraadegrenseRepository.save(statistiskGrenseImporter.importer(sosiDelomraadegrense) as Delomraadegrense)
            }
            else -> eksisterende
        }

    }
}