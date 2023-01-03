package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class GrunnlinjeImporter @Autowired constructor(
    private val kodelisteService: KodelisteService,
    private val grenseRepository: GrenseRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter
) {

    val logger = Logger.getLogger(this::class.java.name)

    fun importGrunnlinje(skalImporteres: SplitTilImport): Grense {
        val sosiGrunnlinje = skalImporteres.splitRef.basertPaGrense
        return when (val existing = grenseRepository.findByLokalid(skalImporteres.lokalId())) {
            null -> {
                var grunnlinje = createGrense(skalImporteres.lokalId(), Grensetype.Grunnlinje)
                grunnlinje.datafangstdato = sosiGrunnlinje.getDatafangstDato()

                grunnlinje.opphav = sosiGrunnlinje.getOpphav()
                grunnlinje.gyldighet = Gyldighet(sosiGrunnlinje.getGyldigFra(), sosiGrunnlinje.getGyldigTil())

                grunnlinje.kvalitet = sosiGrunnlinje.getPosisjonskvalitet(kodelisteService)

                grunnlinje.grense = skalImporteres.splitRef.splittetGrense
                grunnlinje = grenseRepository.save(grunnlinje)

                sosiGrunnlinje.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, grunnlinje)
                }

                grunnlinje.informasjonstekst = sosiGrunnlinje.getInformasjon()
                return grunnlinje
            }

            else -> existing
        }
    }
}
