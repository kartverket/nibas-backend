package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class RiksgrenseImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var riksgrenseRepository: RiksgrenseRepository

    @Autowired
    lateinit var maalemetodeRepository: MaalemetodeRepository

    @Autowired
    lateinit var grensestatusRepository: GrensestatusRepository

    @Autowired
    lateinit var fastsettingstypeRepository: FastsettingstypeRepository

    @Autowired
    lateinit var dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter

    fun importSosiRiksgrense(sosiRiksgrense: SosiGeometri): Grense {
        return when (val existing = riksgrenseRepository.findByLokalid(sosiRiksgrense.getIdent()?.getLokalId()!!)) {
            null -> {
                var riksgrense = Riksgrense()
                riksgrense.lokalid = sosiRiksgrense.getIdent()?.getLokalId()
                riksgrense.navnerom = sosiRiksgrense.getIdent()?.getNavnerom()
                riksgrense.versjonid = sosiRiksgrense.getIdent()?.getVersjonId()
                riksgrense.datafangstdato = sosiRiksgrense.getDatafangstDato()
                riksgrense.oppdateringsdato = sosiRiksgrense.getOppdateringsdato()
                riksgrense.datauttaksdato = sosiRiksgrense.getDataUttaksdato()

                riksgrense.opphav = sosiRiksgrense.getOpphav()
                riksgrense.gyldigfra = sosiRiksgrense.getGyldigFra()
                riksgrense.gyldigtil = sosiRiksgrense.getGyldigTil()

                riksgrense.noeyaktighet = sosiRiksgrense.getKvalitet()?.getNoyaktighet()
                riksgrense.maalemetode = when (val maalemetode = sosiRiksgrense.getKvalitet()?.getMalemetode()) {
                    null -> throw RuntimeException("Maalemetode er påkrevd")
                    else -> maalemetodeRepository.findByIdOrNull(maalemetode) ?: throw RuntimeException("Ukjent maalemetode")
                }
                riksgrense.grensestatus = when (val grenseStatus = sosiRiksgrense.getGrensestatus()) {
                    null -> null
                    else -> grensestatusRepository.findByIdOrNull(grenseStatus) ?: throw RuntimeException("Ukjent grensestatus")
                }

                riksgrense.fastsettingstype = when (val fastsettingstype = sosiRiksgrense.getFastsettingstype()) {
                    null -> null
                    else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype) ?: throw RuntimeException("Ukjent fastsettingstype")
                }
                riksgrense.grense = SosiGeometryFunctions.sosiKoordinaterToLineString(sosiRiksgrense.getAlleSosiNordOest(), sosiRiksgrense.koordsys)

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