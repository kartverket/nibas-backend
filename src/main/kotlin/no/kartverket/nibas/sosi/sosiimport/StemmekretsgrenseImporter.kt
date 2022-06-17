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

    fun importerStemmekretsgrense(sosiStemmekretsgrense: SosiGeometri): Grense {
        val sosiIdent = sosiStemmekretsgrense.getIdent()
        return when (val eksisterende = grenseRepository.findByLokalid(sosiIdent?.getLokalId()!!)) {
            null -> {
                val stemmekretsgrense = kommunalKretsgrenseImporter.importer(sosiStemmekretsgrense)
                grenseRepository.save(stemmekretsgrense)

                stemmekretsgrense.informasjonstekst = sosiStemmekretsgrense.getInformasjon()

                return stemmekretsgrense

            }
            else -> eksisterende
        }
    }
}
