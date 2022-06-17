package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class GrunnkretsgrenseImporter @Autowired constructor(
    private val grenseRepository: GrenseRepository,
) {

    @Autowired
    lateinit var statistiskGrenseImporter: StatistiskGrenseImporter

    fun importerGrunnkretsgrense(sosiGrunnkretsgrense: SosiGeometri): Grense {
        val sosiIdent = sosiGrunnkretsgrense.getIdent()
        return when(val eksisterende = grenseRepository.findByLokalid(sosiIdent?.getLokalId()!!)) {
            null -> {
                var grunnkretsgrense = statistiskGrenseImporter.importer(sosiGrunnkretsgrense)
                grunnkretsgrense = grenseRepository.save(grunnkretsgrense)
                grunnkretsgrense.informasjonstekst = sosiGrunnkretsgrense.getInformasjon()

                return grunnkretsgrense

            }
            else -> eksisterende
        }

    }
}
