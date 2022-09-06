package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.updateSosiIdentifikasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class AvtaltAvgrensningslinjeImporter @Autowired constructor(
    private val grenseRepository: GrenseRepository,
    private val landKodeRepository: LandKodeRepository,
    private val kodelisteService: KodelisteService,
    private val grensestatusRepository: GrensestatusRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter
) {
    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importAvtaltAvgrensningslinje(skalImporteres: SplitTilImport): Grense {
        val sosiAvtaltAvgrensningslinje = skalImporteres.splitRef.basertPaGrense

        return when (val existing =
            grenseRepository.findByLokalid(skalImporteres.lokalId())) {
            null -> {
                var avtaltavgrensningslinje = Grense(Grensetype.AvtaltAvgrensningslinje)
                avtaltavgrensningslinje.updateSosiIdentifikasjon(sosiAvtaltAvgrensningslinje.getIdent())
                avtaltavgrensningslinje.lokalid = skalImporteres.lokalId()
                avtaltavgrensningslinje.opphav = sosiAvtaltAvgrensningslinje.getOpphav()
                avtaltavgrensningslinje.datauttaksdato = sosiAvtaltAvgrensningslinje.getDataUttaksdato()

                avtaltavgrensningslinje.setLand(sosiAvtaltAvgrensningslinje.getLandkoder().map { landkode ->
                    when (val land = landKodeRepository.findById(landkode).unwrap()) {
                        null -> throw RuntimeException("Usupportert landkode") // TODO: Hvordan håndtere feil
                        else -> land
                    }
                })

                avtaltavgrensningslinje.gyldigFra = sosiAvtaltAvgrensningslinje.getGyldigFra()
                avtaltavgrensningslinje.kvalitet = sosiAvtaltAvgrensningslinje.getPosisjonskvalitet(kodelisteService)
                avtaltavgrensningslinje.grensestatus =
                    when (val grenseStatus = sosiAvtaltAvgrensningslinje.getGrensestatus()) {
                        null -> null
                        else -> grensestatusRepository.findByIdOrNull(grenseStatus)
                            ?: throw RuntimeException("Ukjent grensestatus")
                    }

                avtaltavgrensningslinje.fastsettingstype =
                    when (val fastsettingstype = sosiAvtaltAvgrensningslinje.getFastsettingstype()) {
                        null -> null
                        else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype)
                            ?: throw RuntimeException("Ukjent fastsettingstype")
                    }

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
