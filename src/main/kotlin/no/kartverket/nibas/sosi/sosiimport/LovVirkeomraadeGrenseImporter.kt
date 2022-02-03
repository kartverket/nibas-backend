package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Lovvirkeomraadegrense
import no.kartverket.nibas.domain.LovvirkeomraadegrenseInformasjon
import no.kartverket.nibas.domain.updateSosiIdentifikasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.LovVirkeomraadeGrenseRepository
import no.kartverket.nibas.repository.LovvirkeomraadeGrenseInformasjonRepository
import no.kartverket.nibas.sosi.model.SosiKurve
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class LovVirkeomraadeGrenseImporter @Autowired constructor(
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val kodelisteService: KodelisteService,
    private val lovVirkeomraadeGrenseRepository: LovVirkeomraadeGrenseRepository,
    private val lovvirkeomraadeGrenseInformasjonRepository: LovvirkeomraadeGrenseInformasjonRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter
) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importLovVirkeomradeGrense(sosiLovvirkeomraadegrense: SosiKurve): Grense {
        val sosiIdent = sosiLovvirkeomraadegrense.getIdent()
        return when (val existing =
            lovVirkeomraadeGrenseRepository.findByLokalid(sosiIdent?.getLokalId()!!)) {
            null -> {
                val kommunenummerIds = sosiLovvirkeomraadegrense.getKommunenumre().map { sosiKommunenummer ->
                    kodelisteService.getKommunenummerIdByCodeValue(sosiKommunenummer)
                }.toMutableSet()


                var lovvirkeomraadegrense = Lovvirkeomraadegrense()
                lovvirkeomraadegrense.updateSosiIdentifikasjon(sosiIdent)
                lovvirkeomraadegrense.datafangstdato = sosiLovvirkeomraadegrense.getDatafangstDato()
                lovvirkeomraadegrense.oppdateringsdato = sosiLovvirkeomraadegrense.getOppdateringsdato()
                lovvirkeomraadegrense.datauttaksdato = sosiLovvirkeomraadegrense.getDataUttaksdato()
                lovvirkeomraadegrense.kommunenummer = kommunenummerIds
                lovvirkeomraadegrense.opphav = sosiLovvirkeomraadegrense.getOpphav()
                lovvirkeomraadegrense.gyldigfra = sosiLovvirkeomraadegrense.getGyldigFra()
                lovvirkeomraadegrense.gyldigtil = sosiLovvirkeomraadegrense.getGyldigTil()

                lovvirkeomraadegrense.noeyaktighet = sosiLovvirkeomraadegrense.getKvalitet()?.getNoyaktighet()
                lovvirkeomraadegrense.maalemetode =
                    when (val maalemetode = sosiLovvirkeomraadegrense.getKvalitet()?.getMalemetode()) {
                        null -> throw RuntimeException("Maalemetode er påkrevd")
                        else -> kodelisteService.getMalemetodeIdByCodeValue(maalemetode)
                    }

                lovvirkeomraadegrense.fastsettingstype =
                    when (val fastsettingstype = sosiLovvirkeomraadegrense.getFastsettingstype()) {
                        null -> null
                        else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype)
                            ?: throw RuntimeException("Ukjent fastsettingstype")
                    }

                lovvirkeomraadegrense.grense = SosiGeometryFunctions.sosiKoordinaterToLineString(
                    sosiLovvirkeomraadegrense.getAlleSosiNordOest(),
                    sosiLovvirkeomraadegrense.koordsys
                )
                lovvirkeomraadegrense = lovVirkeomraadeGrenseRepository.save(lovvirkeomraadegrense)

                if (sosiLovvirkeomraadegrense.getInformasjon() != null) {
                    val lovvirkeomraadegrenseInformasjon = LovvirkeomraadegrenseInformasjon()
                    lovvirkeomraadegrenseInformasjon.informasjon = sosiLovvirkeomraadegrense.getInformasjon()
                    lovvirkeomraadegrenseInformasjon.lovvirkeomraadegrenseFk = lovvirkeomraadegrense
                    lovvirkeomraadeGrenseInformasjonRepository.save(lovvirkeomraadegrenseInformasjon)
                }

                sosiLovvirkeomraadegrense.getDokumentasjonsReferanse().forEach { dokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(dokRef, lovvirkeomraadegrense)
                }
                return lovvirkeomraadegrense
            }
            else -> existing
        }
    }
}
