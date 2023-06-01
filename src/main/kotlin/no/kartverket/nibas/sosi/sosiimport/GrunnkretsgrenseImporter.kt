package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.GrenseRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class GrunnkretsgrenseImporter @Autowired constructor(
    private val grenseRepository: GrenseRepository,
) {

    @Autowired
    lateinit var statistiskGrenseImporter: StatistiskGrenseImporter

    fun importerGrunnkretsgrense(skalImporteres: SplitTilImport): Grense {
        val sosiGrunnkretsgrense = skalImporteres.splitRef.basertPaGrense
        return when(val eksisterende = grenseRepository.findLastByLokalid(skalImporteres.lokalId())) {
            null -> {
                var grunnkretsgrense = statistiskGrenseImporter.importer(skalImporteres)
                grunnkretsgrense = grenseRepository.save(grunnkretsgrense)
                grunnkretsgrense.informasjonstekst = sosiGrunnkretsgrense.getInformasjon()

                return grunnkretsgrense

            }
            else -> eksisterende
        }

    }
}
