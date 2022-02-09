package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.updateSosiIdentifikasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.RiksgrenseRepository
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class RiksgrenseImporter @Autowired constructor(
    private val riksgrenseRepository: RiksgrenseRepository,
    private val grensestatusRepository: GrensestatusRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter,
    private val kodelisteService: KodelisteService
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importSosiRiksgrense(sosiRiksgrense: SosiGeometri): Grense {
        val sosiIdent = sosiRiksgrense.getIdent()
        return when (val existing = riksgrenseRepository.findByLokalid(sosiIdent?.getLokalId()!!)) {
            null -> {
                var riksgrense = Riksgrense()
                riksgrense.updateSosiIdentifikasjon(sosiIdent)
                riksgrense.datafangstdato = sosiRiksgrense.getDatafangstDato()
                riksgrense.oppdateringsdato = sosiRiksgrense.getOppdateringsdato()
                riksgrense.datauttaksdato = sosiRiksgrense.getDataUttaksdato()

                riksgrense.opphav = sosiRiksgrense.getOpphav()
                riksgrense.gyldigfra = sosiRiksgrense.getGyldigFra()
                riksgrense.gyldigtil = sosiRiksgrense.getGyldigTil()

                riksgrense.noeyaktighet = sosiRiksgrense.getKvalitet()?.getNoyaktighet()
                riksgrense.maalemetode = when (val maalemetode = sosiRiksgrense.getKvalitet()?.getMalemetode()) {
                    null -> throw RuntimeException("Maalemetode er påkrevd")
                    else -> kodelisteService.getMalemetodeIdByCodeValue(maalemetode)
                }
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
                riksgrense.grense = SosiGeometryFunctions.sosiKoordinaterToLineString(
                    sosiRiksgrense.getAlleSosiNordOest(),
                    sosiRiksgrense.koordsys
                )

                riksgrense = riksgrenseRepository.save(riksgrense)

                sosiRiksgrense.getDokumentasjonsReferanse().forEach {
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(it, riksgrense)
                }
                return riksgrense

            }
            else -> existing
        }
    }
}
