package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class TerritorialgrenseImporter @Autowired constructor(
    private val grenseRepository: GrenseRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter,
    private val kodelisteService: KodelisteService
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importSosiTerritorialgrense(skalImporteres: SplitTilImport): Grense {
        val sosiTerritorialgrense = skalImporteres.splitRef.basertPaGrense
        return when (val existing =
            grenseRepository.findLastByLokalid(skalImporteres.lokalId())) {
            null -> {
                var territorialgrense = createGrense(skalImporteres.lokalId(), Grensetype.Territorialgrense)

                territorialgrense.datafangstdato = sosiTerritorialgrense.getDatafangstDato()
                territorialgrense.opphav = sosiTerritorialgrense.getOpphav()
                territorialgrense.gyldighet = Gyldighet(sosiTerritorialgrense.getGyldigFra(), sosiTerritorialgrense.getGyldigTil())
                territorialgrense.kvalitet = sosiTerritorialgrense.getPosisjonskvalitet(kodelisteService)
                territorialgrense.grense = skalImporteres.splitRef.splittetGrense
                territorialgrense = grenseRepository.save(territorialgrense)

                sosiTerritorialgrense.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, territorialgrense)
                }
                return territorialgrense
            }
            else -> existing
        }
    }
}
