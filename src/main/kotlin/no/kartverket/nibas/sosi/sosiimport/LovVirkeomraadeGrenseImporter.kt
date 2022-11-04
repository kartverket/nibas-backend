package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class LovVirkeomraadeGrenseImporter @Autowired constructor(
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val kodelisteService: KodelisteService,
    private val grenseRepository: GrenseRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importLovVirkeomradeGrense(skalImporteres: SplitTilImport): Grense {
        val sosiLovvirkeomraadegrense = skalImporteres.splitRef.basertPaGrense
        return when (val existing =
            grenseRepository.findByLokalid(skalImporteres.lokalId())) {
            null -> {
                var lovvirkeomraadegrense = createGrense(sosiLovvirkeomraadegrense.getIdent(), Grensetype.Lovvirkeomraadegrense)
                lovvirkeomraadegrense.datafangstdato = sosiLovvirkeomraadegrense.getDatafangstDato()
                lovvirkeomraadegrense.datauttaksdato = sosiLovvirkeomraadegrense.getDataUttaksdato()
                lovvirkeomraadegrense.kommunenummer = sosiLovvirkeomraadegrense.getKommunenumre().map { sosiKommunenummer ->
                    kodelisteService.getKommunenummerIdByCodeValue(sosiKommunenummer)
                }.toMutableSet()
                lovvirkeomraadegrense.opphav = sosiLovvirkeomraadegrense.getOpphav()
                lovvirkeomraadegrense.gyldighet = Gyldighet(sosiLovvirkeomraadegrense.getGyldigFra(), sosiLovvirkeomraadegrense.getGyldigTil())

                lovvirkeomraadegrense.kvalitet = sosiLovvirkeomraadegrense.getPosisjonskvalitet(kodelisteService)
                lovvirkeomraadegrense.fastsettingstype =
                    when (val fastsettingstype = sosiLovvirkeomraadegrense.getFastsettingstype()) {
                        null -> null
                        else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype)
                            ?: throw RuntimeException("Ukjent fastsettingstype")
                    }

                lovvirkeomraadegrense.grense = skalImporteres.splitRef.splittetGrense
                lovvirkeomraadegrense = grenseRepository.save(lovvirkeomraadegrense)

                lovvirkeomraadegrense.informasjonstekst = sosiLovvirkeomraadegrense.getInformasjon()

                sosiLovvirkeomraadegrense.getDokumentasjonsReferanse().forEach { dokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(dokRef, lovvirkeomraadegrense)
                }
                return lovvirkeomraadegrense
            }

            else -> existing
        }
    }
}
