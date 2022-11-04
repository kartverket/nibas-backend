package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class TerritorialgrenseImporter @Autowired constructor(
    private val grenseRepository: GrenseRepository,
    private val landKodeRepository: LandKodeRepository,
    private val grensestatusRepository: GrensestatusRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter,
    private val kodelisteService: KodelisteService
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importSosiTerritorialgrense(skalImporteres: SplitTilImport): Grense {
        val sosiTerritorialgrense = skalImporteres.splitRef.basertPaGrense
        return when (val existing =
            grenseRepository.findByLokalid(skalImporteres.lokalId())) {
            null -> {
                var territorialgrense = createGrense(sosiTerritorialgrense.getIdent(), Grensetype.Territorialgrense)

                territorialgrense.datafangstdato = sosiTerritorialgrense.getDatafangstDato()
                territorialgrense.datauttaksdato = sosiTerritorialgrense.getDataUttaksdato()
                territorialgrense.opphav = sosiTerritorialgrense.getOpphav()
                territorialgrense.gyldighet = Gyldighet(sosiTerritorialgrense.getGyldigFra(), sosiTerritorialgrense.getGyldigTil())
                territorialgrense.kvalitet = sosiTerritorialgrense.getPosisjonskvalitet(kodelisteService)
                territorialgrense.grensestatus = when (val grenseStatus = sosiTerritorialgrense.getGrensestatus()) {
                    null -> null
                    else -> grensestatusRepository.findByIdOrNull(grenseStatus)
                        ?: throw RuntimeException("Ukjent grensestatus")
                }

                territorialgrense.fastsettingstype =
                    when (val fastsettingstype = sosiTerritorialgrense.getFastsettingstype()) {
                        null -> null
                        else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype)
                            ?: throw RuntimeException("Ukjent fastsettingstype")
                    }

                val landkode = sosiTerritorialgrense.getLandkode()
                if (landkode != null) {
                    territorialgrense.setLand(listOf(
                        landKodeRepository.findByIdOrNull(landkode) ?: throw RuntimeException("Ukjent landkode")))
                }

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
