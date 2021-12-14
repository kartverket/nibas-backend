package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.sosi.model.SosiKurve
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class LovVirkeomraadeGrenseImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var maalemetodeRepository: MaalemetodeRepository

    @Autowired
    lateinit var fastsettingstypeRepository: FastsettingstypeRepository

    @Autowired
    lateinit var dokumentasjonsreferanseRepository: DokumentasjonsreferanseRepository

    @Autowired
    lateinit var dokumentasjonsreferanseDokumentlinkRepository: DokumentasjonsreferanseDokumentlinkRepository

    @Autowired
    lateinit var kommunenummerRepository: KommunenummerRepository

    @Autowired
    lateinit var lovVirkeomraadeGrenseRepository: LovVirkeomraadeGrenseRepository

    @Autowired
    lateinit var lovvirkeomraadeGrenseInformasjonRepository: LovvirkeomraadeGrenseInformasjonRepository

    @Autowired
    lateinit var dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter


    fun importLovVirkeomradeGrense(sosiLovvirkeomraadegrense: SosiKurve): Grense {
        return when (val existing = lovVirkeomraadeGrenseRepository.findByLokalid(sosiLovvirkeomraadegrense.getIdent()?.getLokalId()!!)) {
            null -> {
                val kommunenumre = sosiLovvirkeomraadegrense.getKommunenumre().map { sosiKommunenummer ->
                    when (val kommunenummer = kommunenummerRepository.findById(sosiKommunenummer).unwrap()) {
                        null -> throw RuntimeException("Ugyldig kommunenummer") // TODO: Hvordan håndtere feil
                        else -> kommunenummer
                    }
                }.toMutableSet()


                var lovvirkeomraadegrense = Lovvirkeomraadegrense()
                lovvirkeomraadegrense.lokalid = sosiLovvirkeomraadegrense.getIdent()?.getLokalId()
                lovvirkeomraadegrense.navnerom = sosiLovvirkeomraadegrense.getIdent()?.getNavnerom()
                lovvirkeomraadegrense.versjonid = sosiLovvirkeomraadegrense.getIdent()?.getVersjonId()
                lovvirkeomraadegrense.datafangstdato = sosiLovvirkeomraadegrense.getDatafangstDato()
                lovvirkeomraadegrense.oppdateringsdato = sosiLovvirkeomraadegrense.getOppdateringsdato()
                lovvirkeomraadegrense.datauttaksdato = sosiLovvirkeomraadegrense.getDataUttaksdato()
                lovvirkeomraadegrense.kommunenummer = kommunenumre
                lovvirkeomraadegrense.opphav = sosiLovvirkeomraadegrense.getOpphav()
                lovvirkeomraadegrense.gyldigfra = sosiLovvirkeomraadegrense.getGyldigFra()
                lovvirkeomraadegrense.gyldigtil = sosiLovvirkeomraadegrense.getGyldigTil()

                lovvirkeomraadegrense.noeyaktighet = sosiLovvirkeomraadegrense.getKvalitet()?.getNoyaktighet()
                lovvirkeomraadegrense.maalemetode =
                    when (val maalemetode = sosiLovvirkeomraadegrense.getKvalitet()?.getMalemetode()) {
                        null -> throw RuntimeException("Maalemetode er påkrevd")
                        else -> maalemetodeRepository.findByIdOrNull(maalemetode) ?: throw RuntimeException("Ukjent maalemetode")
                    }

                lovvirkeomraadegrense.fastsettingstype =
                    when (val fastsettingstype = sosiLovvirkeomraadegrense.getFastsettingstype()) {
                        null -> null
                        else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype) ?: throw RuntimeException("Ukjent fastsettingstype")
                    }

                lovvirkeomraadegrense.grense = SosiGeometryFunctions.sosiKoordinaterToLineString(sosiLovvirkeomraadegrense.getAlleSosiNordOest(), sosiLovvirkeomraadegrense.koordsys)
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