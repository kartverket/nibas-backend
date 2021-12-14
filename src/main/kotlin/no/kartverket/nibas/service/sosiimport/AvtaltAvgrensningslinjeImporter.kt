package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class AvtaltAvgrensningslinjeImporter {

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var avtaltAvgrensningslinjeRepository: AvtaltAvgrensningslinjeRepository

    @Autowired
    lateinit var avtaltAvgrensningslinjeInformasjonRepository: AvtaltAvgrensningslinjeInformasjonRepository

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

    fun importAvtaltAvgrensningslinje(sosiAvtaltAvgrensningslinje: SosiGeometri): Grense {
        return when (val existing = avtaltAvgrensningslinjeRepository.findByLokalid(sosiAvtaltAvgrensningslinje.getIdent()?.getLokalId()!!)) {
            null -> {
                val landkoder = sosiAvtaltAvgrensningslinje.getLandkoder().map { landkode ->
                    when (val land = landKodeRepository.findById(landkode).unwrap()) {
                        null -> throw RuntimeException("Usupportert landkode") // TODO: Hvordan håndtere feil
                        else -> land
                    }
                }.toMutableSet()

                var avtaltavgrensningslinje = Avtaltavgrensningslinje()
                avtaltavgrensningslinje.lokalid = sosiAvtaltAvgrensningslinje.getIdent()?.getLokalId()
                avtaltavgrensningslinje.navnerom = sosiAvtaltAvgrensningslinje.getIdent()?.getNavnerom()
                avtaltavgrensningslinje.opphav = sosiAvtaltAvgrensningslinje.getOpphav()
                avtaltavgrensningslinje.datauttaksdato = sosiAvtaltAvgrensningslinje.getDataUttaksdato()
                avtaltavgrensningslinje.landkode = landkoder
                avtaltavgrensningslinje.oppdateringsdato = sosiAvtaltAvgrensningslinje.getOppdateringsdato()
                avtaltavgrensningslinje.gyldigfra = sosiAvtaltAvgrensningslinje.getGyldigFra()
                avtaltavgrensningslinje.noeyaktighet = sosiAvtaltAvgrensningslinje.getKvalitet()?.getNoyaktighet()
                avtaltavgrensningslinje.maalemetode =
                    when (val maalemetode = sosiAvtaltAvgrensningslinje.getKvalitet()?.getMalemetode()) {
                        null -> throw RuntimeException("Maalemetode er påkrevd")
                        else -> maalemetodeRepository.findById(maalemetode).orElseThrow()
                    }
                avtaltavgrensningslinje.grensestatus =
                    when (val grenseStatus = sosiAvtaltAvgrensningslinje.getGrensestatus()) {
                        null -> null
                        else -> grensestatusRepository.findByIdOrNull(grenseStatus)
                    }

                avtaltavgrensningslinje.fastsettingstype =
                    when (val fastsettingstype = sosiAvtaltAvgrensningslinje.getFastsettingstype()) {
                        null -> null
                        else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype)
                    }

                avtaltavgrensningslinje.grense = SosiGeometryFunctions.sosiKoordinaterToLineString(sosiAvtaltAvgrensningslinje.getAlleSosiNordOest(), sosiAvtaltAvgrensningslinje.koordsys)
                avtaltavgrensningslinje = avtaltAvgrensningslinjeRepository.save(avtaltavgrensningslinje)

                if (sosiAvtaltAvgrensningslinje.getInformasjon() != null) {
                    val avtaltavgrensningslinjeInformasjon = AvtaltavgrensningslinjeInformasjon()
                    avtaltavgrensningslinjeInformasjon.informasjon = sosiAvtaltAvgrensningslinje.getInformasjon()
                    avtaltavgrensningslinjeInformasjon.avtaltavgrensningslinjeFk = avtaltavgrensningslinje
                    avtaltAvgrensningslinjeInformasjonRepository.save(avtaltavgrensningslinjeInformasjon)
                }

                sosiAvtaltAvgrensningslinje.getDokumentasjonsReferanse().forEach { sosiDokRef ->
                    dokumentasjonsreferanseImporter.importerDokumentasjonsreferanse(sosiDokRef, avtaltavgrensningslinje)
                }

                return avtaltavgrensningslinje
            }
            else -> existing
        }
    }
}