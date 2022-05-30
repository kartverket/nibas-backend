package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Stemmekretsgrense
import no.kartverket.nibas.domain.StemmekretsgrenseInformasjon
import no.kartverket.nibas.repository.StemmekretsgrenseInformasjonRepository
import no.kartverket.nibas.repository.StemmekretsgrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StemmekretsgrenseImporter @Autowired constructor(
    private val kommunalKretsgrenseImporter: KommunalKretsgrenseImporter,
    private val stemmekretsgrenseRepository: StemmekretsgrenseRepository,
    private val stemmekretsgrenseInformasjonRepository: StemmekretsgrenseInformasjonRepository
) {

    fun importerStemmekretsgrense(sosiStemmekretsgrense: SosiGeometri): Grense {
        val sosiIdent = sosiStemmekretsgrense.getIdent()
        return when (val eksisterende = stemmekretsgrenseRepository.findByLokalid(sosiIdent?.getLokalId()!!)) {
            null -> {
                val stemmekretsgrense = kommunalKretsgrenseImporter.importer(sosiStemmekretsgrense) as Stemmekretsgrense
                stemmekretsgrenseRepository.save(stemmekretsgrense)

                sosiStemmekretsgrense.getInformasjon()?.let { informasjon ->
                    val stemmekretsgrenseInformasjon = StemmekretsgrenseInformasjon(stemmekretsgrense)
                    stemmekretsgrenseInformasjon.informasjon = informasjon
                    stemmekretsgrenseInformasjonRepository.save(stemmekretsgrenseInformasjon)
                }

                return stemmekretsgrense

            }
            else -> eksisterende
        }
    }
}
