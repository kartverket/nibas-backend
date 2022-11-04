package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class RiksgrenseImporter @Autowired constructor(
    private val grenseRepository: GrenseRepository,
    private val grensestatusRepository: GrensestatusRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter,
    private val kodelisteService: KodelisteService
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importSosiRiksgrense(skalImporteres: SplitTilImport): Grense {
        val sosiRiksgrense = skalImporteres.splitRef.basertPaGrense

        return when (val existing = grenseRepository.findByLokalid(skalImporteres.lokalId())) {
            null -> {
                var riksgrense = createGrense(sosiRiksgrense.getIdent(), Grensetype.Riksgrense)
                riksgrense.datafangstdato = sosiRiksgrense.getDatafangstDato()

                riksgrense.datauttaksdato = sosiRiksgrense.getDataUttaksdato()

                riksgrense.opphav = sosiRiksgrense.getOpphav()
                riksgrense.gyldighet = Gyldighet(sosiRiksgrense.getGyldigFra(), sosiRiksgrense.getGyldigTil())

                riksgrense.kvalitet = sosiRiksgrense.getPosisjonskvalitet(kodelisteService)
                riksgrense.grensestatus = when (val grenseStatus = sosiRiksgrense.getGrensestatus()) {
                    null -> null
                    else -> grensestatusRepository.findByIdOrNull(grenseStatus)
                        ?: throw RuntimeException("Ukjent grensestatus")
                }

                riksgrense.fastsettingstype = when (val fastsettingstype = sosiRiksgrense.getFastsettingstype()) {
                    null -> null
                    else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype)
                        ?: throw RuntimeException("Ukjent fastsettingstype")
                }
                riksgrense.grense = skalImporteres.splitRef.splittetGrense

                riksgrense = grenseRepository.save(riksgrense)

                sosiRiksgrense.getDokumentasjonsReferanse().forEach {
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(it, riksgrense)
                }
                return riksgrense

            }

            else -> existing
        }
    }
}
