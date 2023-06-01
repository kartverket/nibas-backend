package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.GrenseRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class FylkesgrenseImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var grenseRepository: GrenseRepository

    @Autowired
    lateinit var administrativGrenseImporter: AdministrativGrenseImporter

    @Autowired
    lateinit var dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter

    fun importerFylkesgrense(sosiFylkesgrense: SplitTilImport): Grense {
        return when (val existing = grenseRepository.findLastByLokalid(sosiFylkesgrense.lokalId())) {
            null -> {
                val fylkesgrense = grenseRepository.save(
                    administrativGrenseImporter.importer(sosiFylkesgrense)
                )

                sosiFylkesgrense.splitRef.basertPaGrense.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, fylkesgrense)
                }
                return fylkesgrense
            }
            else -> existing
        }
    }
}
