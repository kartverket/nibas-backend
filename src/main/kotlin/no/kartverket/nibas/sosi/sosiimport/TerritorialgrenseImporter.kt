package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.domain.updateSosiIdentifikasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.repository.TerritorialgrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class TerritorialgrenseImporter @Autowired constructor(
    private val territorialgrenseRepository: TerritorialgrenseRepository,
    private val landKodeRepository: LandKodeRepository,
    private val grensestatusRepository: GrensestatusRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter,
    private val kodelisteService: KodelisteService
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importSosiTerritorialgrense(sosiTerritorialgrense: SosiGeometri): Grense {
        val sosiIdent = sosiTerritorialgrense.getIdent()
        return when (val existing =
            territorialgrenseRepository.findByLokalid(sosiIdent?.getLokalId()!!)) {
            null -> {
                var territorialgrense = Territorialgrense()
                territorialgrense.updateSosiIdentifikasjon(sosiIdent)
                territorialgrense.datafangstdato = sosiTerritorialgrense.getDatafangstDato()
                territorialgrense.oppdateringsdato = sosiTerritorialgrense.getOppdateringsdato()
                territorialgrense.datauttaksdato = sosiTerritorialgrense.getDataUttaksdato()

                territorialgrense.opphav = sosiTerritorialgrense.getOpphav()
                territorialgrense.gyldigfra = sosiTerritorialgrense.getGyldigFra()
                territorialgrense.gyldigtil = sosiTerritorialgrense.getGyldigTil()

                territorialgrense.noeyaktighet = sosiTerritorialgrense.getKvalitet()?.getNoyaktighet()
                territorialgrense.maalemetode =
                    when (val maalemetode = sosiTerritorialgrense.getKvalitet()?.getMalemetode()) {
                        null -> throw RuntimeException("Maalemetode er påkrevd")
                        else -> kodelisteService.getMalemetodeIdByCodeValue(maalemetode)
                    }

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

                territorialgrense.land = when (val landkode = sosiTerritorialgrense.getLandkode()) {
                    null -> null
                    else -> landKodeRepository.findByIdOrNull(landkode) ?: throw RuntimeException("Ukjent landkode")
                }

                territorialgrense.grense = SosiGeometryFunctions.sosiKoordinaterToLineString(
                    sosiTerritorialgrense.getAlleSosiNordOest(),
                    sosiTerritorialgrense.koordsys
                )
                territorialgrense = territorialgrenseRepository.save(territorialgrense)

                sosiTerritorialgrense.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, territorialgrense)
                }
                return territorialgrense
            }
            else -> existing
        }
    }
}
