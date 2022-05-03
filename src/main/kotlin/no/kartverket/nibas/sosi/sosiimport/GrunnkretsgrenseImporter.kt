package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkretsgrense
import no.kartverket.nibas.domain.GrunnkretsgrenseInformasjon
import no.kartverket.nibas.repository.GrunnkretsgrenseInformasjonRepository
import no.kartverket.nibas.repository.GrunnkretsgrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class GrunnkretsgrenseImporter @Autowired constructor(
    private val grunnkretsgrenseRepository: GrunnkretsgrenseRepository,
    private val grunnkretsgrenseInformasjonRepository: GrunnkretsgrenseInformasjonRepository
) {

    @Autowired
    lateinit var statistiskGrenseImporter: StatistiskGrenseImporter

    fun importerGrunnkretsgrense(sosiGrunnkretsgrense: SosiGeometri): Grense {
        val sosiIdent = sosiGrunnkretsgrense.getIdent()
        return when(val eksisterende = grunnkretsgrenseRepository.findByLokalid(sosiIdent?.getLokalId()!!)) {
            null -> {
                var grunnkretsgrense = statistiskGrenseImporter.importer(sosiGrunnkretsgrense) as Grunnkretsgrense
                grunnkretsgrense = grunnkretsgrenseRepository.save(grunnkretsgrense)

                if (sosiGrunnkretsgrense.getInformasjon() != null) {
                    val grunnkretsgrenseInformasjon = GrunnkretsgrenseInformasjon(grunnkretsgrense)
                    grunnkretsgrenseInformasjon.informasjon = sosiGrunnkretsgrense.getInformasjon()
                    grunnkretsgrenseInformasjonRepository.save(grunnkretsgrenseInformasjon)
                }

                return grunnkretsgrense

            }
            else -> eksisterende
        }

    }
}
