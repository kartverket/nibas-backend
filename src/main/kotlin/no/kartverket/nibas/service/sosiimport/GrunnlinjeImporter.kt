package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class GrunnlinjeImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var maalemetodeRepository: MaalemetodeRepository

    @Autowired
    lateinit var grensestatusRepository: GrensestatusRepository

    @Autowired
    lateinit var fastsettingstypeRepository: FastsettingstypeRepository

    @Autowired
    lateinit var kommunenummerRepository: KommunenummerRepository

    @Autowired
    lateinit var grunnlinjeRepository: GrunnlinjeRepository

    @Autowired
    lateinit var grunnlinjeInformasjonRepository: GrunnlinjeInformasjonRepository

    @Autowired
    lateinit var dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter


    fun importGrunnlinje(sosiGrunnlinje: SosiGeometri): Grense {
        return when (val existing = grunnlinjeRepository.findByLokalid(sosiGrunnlinje.getIdent()?.getLokalId()!!)) {
            null -> {
                var grunnlinje = Grunnlinje()
                grunnlinje.lokalid = sosiGrunnlinje.getIdent()?.getLokalId()
                grunnlinje.navnerom = sosiGrunnlinje.getIdent()?.getNavnerom()
                grunnlinje.versjonid = sosiGrunnlinje.getIdent()?.getVersjonId()
                grunnlinje.datafangstdato = sosiGrunnlinje.getDatafangstDato()
                grunnlinje.oppdateringsdato = sosiGrunnlinje.getOppdateringsdato()
                grunnlinje.datauttaksdato = sosiGrunnlinje.getDataUttaksdato()

                grunnlinje.opphav = sosiGrunnlinje.getOpphav()
                grunnlinje.gyldigfra = sosiGrunnlinje.getGyldigFra()
                grunnlinje.gyldigtil = sosiGrunnlinje.getGyldigTil()

                grunnlinje.kommunenummer = sosiGrunnlinje.getKommunenummer()?.let { kommunenummerRepository.findByIdOrNull(it) }

                grunnlinje.noeyaktighet = sosiGrunnlinje.getKvalitet()?.getNoyaktighet()
                grunnlinje.maalemetode = when (val maalemetode = sosiGrunnlinje.getKvalitet()?.getMalemetode()) {
                    null -> throw RuntimeException("Maalemetode er påkrevd")
                    else -> maalemetodeRepository.findByIdOrNull(maalemetode) ?: throw RuntimeException("Ukjent maalemetode")
                }
                grunnlinje.grensestatus = when (val grenseStatus = sosiGrunnlinje.getGrensestatus()) {
                    null -> null
                    else -> grensestatusRepository.findByIdOrNull(grenseStatus) ?: throw RuntimeException("Ukjent grensestatus")
                }

                grunnlinje.fastsettingstype = when (val fastsettingstype = sosiGrunnlinje.getFastsettingstype()) {
                    null -> null
                    else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype) ?: throw RuntimeException("Ukjent fastsettingstype")
                }
                grunnlinje.grense = SosiGeometryFunctions.sosiKoordinaterToLineString(sosiGrunnlinje.getAlleSosiNordOest(), sosiGrunnlinje.koordsys)
                grunnlinje = grunnlinjeRepository.save(grunnlinje)

                sosiGrunnlinje.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, grunnlinje)
                }


                if (sosiGrunnlinje.getInformasjon() != null) {
                    val grunnlinjeInformasjon = GrunnlinjeInformasjon()
                    grunnlinjeInformasjon.informasjon = sosiGrunnlinje.getInformasjon()
                    grunnlinjeInformasjon.grunnlinjeFk = grunnlinje
                    grunnlinjeInformasjonRepository.save(grunnlinjeInformasjon)
                }
                return grunnlinje
            }
            else -> existing
        }
    }
}