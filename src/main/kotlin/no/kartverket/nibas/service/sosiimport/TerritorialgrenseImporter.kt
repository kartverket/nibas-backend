package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class TerritorialgrenseImporter {


    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var territorialgrenseRepository: TerritorialgrenseRepository

    @Autowired
    lateinit var landKodeRepository: LandKodeRepository

    @Autowired
    lateinit var maalemetodeRepository: MaalemetodeRepository

    @Autowired
    lateinit var grensestatusRepository: GrensestatusRepository

    @Autowired
    lateinit var fastsettingstypeRepository: FastsettingstypeRepository

    @Autowired
    lateinit var dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter


    fun importSosiTerritorialgrense(sosiTerritorialgrense: SosiGeometri): Grense {


        return when (val existing = territorialgrenseRepository.findByLokalid(sosiTerritorialgrense.getIdent()?.getLokalId()!!)) {
            null -> {
                var territorialgrense = Territorialgrense()
                territorialgrense.lokalid = sosiTerritorialgrense.getIdent()?.getLokalId()
                territorialgrense.navnerom = sosiTerritorialgrense.getIdent()?.getNavnerom()
                territorialgrense.versjonid = sosiTerritorialgrense.getIdent()?.getVersjonId()
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
                        else -> maalemetodeRepository.findByIdOrNull(maalemetode) ?: throw RuntimeException("Ukjent maalemetode")
                    }

                territorialgrense.grensestatus = when (val grenseStatus = sosiTerritorialgrense.getGrensestatus()) {
                    null -> null
                    else -> grensestatusRepository.findByIdOrNull(grenseStatus) ?: throw RuntimeException("Ukjent grensestatus")
                }

                territorialgrense.fastsettingstype =
                    when (val fastsettingstype = sosiTerritorialgrense.getFastsettingstype()) {
                        null -> null
                        else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype) ?: throw RuntimeException("Ukjent fastsettingstype")
                    }

                territorialgrense.land = when (val landkode = sosiTerritorialgrense.getLandkode()) {
                    null -> null
                    else -> landKodeRepository.findByIdOrNull(landkode) ?: throw RuntimeException("Ukjent landkode")
                }

                territorialgrense.grense = SosiGeometryFunctions.sosiKoordinaterToLineString(sosiTerritorialgrense.getAlleSosiNordOest(), sosiTerritorialgrense.koordsys)
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