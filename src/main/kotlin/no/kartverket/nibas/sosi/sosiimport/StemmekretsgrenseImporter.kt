package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StemmekretsgrenseImporter @Autowired constructor(
    private val kommunalKretsgrenseImporter: KommunalKretsgrenseImporter,
    private val grenseRepository: GrenseRepository,
) {

    fun importerStemmekretsgrense(skalImporteres: SplitTilImport): Grense {
        val sosiStemmekretsgrense = skalImporteres.splitRef.basertPaGrense
        return when (val eksisterende = grenseRepository.findByLokalid(skalImporteres.lokalId())) {
            null -> {
                val stemmekretsgrense = kommunalKretsgrenseImporter.importer(skalImporteres)
                grenseRepository.save(stemmekretsgrense)

                stemmekretsgrense.informasjonstekst = sosiStemmekretsgrense.getInformasjon()

                return stemmekretsgrense

            }
            else -> eksisterende
        }
    }
}
