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
class AvtaltAvgrensningslinjeImporter @Autowired constructor(
    private val grenseRepository: GrenseRepository,
    private val kodelisteService: KodelisteService,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter
) {
    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importAvtaltAvgrensningslinje(skalImporteres: SplitTilImport): Grense {
        val sosiAvtaltAvgrensningslinje = skalImporteres.splitRef.basertPaGrense

        return when (val existing =
            grenseRepository.findLastByLokalid(skalImporteres.lokalId())) {
            null -> {
                var avtaltavgrensningslinje = createGrense(skalImporteres.lokalId(), Grensetype.AvtaltAvgrensningslinje)
                avtaltavgrensningslinje.opphav = sosiAvtaltAvgrensningslinje.getOpphav()

                avtaltavgrensningslinje.gyldighet = Gyldighet(sosiAvtaltAvgrensningslinje.getGyldigFra(), sosiAvtaltAvgrensningslinje.getGyldigTil())
                avtaltavgrensningslinje.kvalitet = sosiAvtaltAvgrensningslinje.getPosisjonskvalitet(kodelisteService)
                avtaltavgrensningslinje.grense = skalImporteres.splitRef.splittetGrense
                avtaltavgrensningslinje = grenseRepository.save(avtaltavgrensningslinje)

                avtaltavgrensningslinje.informasjonstekst = sosiAvtaltAvgrensningslinje.getInformasjon()

                sosiAvtaltAvgrensningslinje.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, avtaltavgrensningslinje)
                }

                return avtaltavgrensningslinje
            }

            else -> existing
        }
    }
}
