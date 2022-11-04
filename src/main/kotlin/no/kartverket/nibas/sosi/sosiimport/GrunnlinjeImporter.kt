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
class GrunnlinjeImporter @Autowired constructor(
    private val kodelisteService: KodelisteService,
    private val grensestatusRepository: GrensestatusRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val grenseRepository: GrenseRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter
) {

    val logger = Logger.getLogger(this::class.java.name)

    fun importGrunnlinje(skalImporteres: SplitTilImport): Grense {
        val sosiGrunnlinje = skalImporteres.splitRef.basertPaGrense
        return when (val existing = grenseRepository.findByLokalid(skalImporteres.lokalId())) {
            null -> {
                var grunnlinje = createGrense(sosiGrunnlinje.getIdent(), Grensetype.Grunnlinje)
                grunnlinje.datafangstdato = sosiGrunnlinje.getDatafangstDato()
                grunnlinje.datauttaksdato = sosiGrunnlinje.getDataUttaksdato()

                grunnlinje.opphav = sosiGrunnlinje.getOpphav()
                grunnlinje.gyldighet = Gyldighet(sosiGrunnlinje.getGyldigFra(), sosiGrunnlinje.getGyldigTil())

                //Her kan kommunenr være optional
                grunnlinje.kommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(sosiGrunnlinje.getKommunenummer())

                grunnlinje.kvalitet = sosiGrunnlinje.getPosisjonskvalitet(kodelisteService)
                grunnlinje.grensestatus = when (val grenseStatus = sosiGrunnlinje.getGrensestatus()) {
                    null -> null
                    else -> grensestatusRepository.findByIdOrNull(grenseStatus) ?: throw RuntimeException("Ukjent grensestatus")
                }

                grunnlinje.fastsettingstype = when (val fastsettingstype = sosiGrunnlinje.getFastsettingstype()) {
                    null -> null
                    else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype)
                        ?: throw RuntimeException("Ukjent fastsettingstype")
                }
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
