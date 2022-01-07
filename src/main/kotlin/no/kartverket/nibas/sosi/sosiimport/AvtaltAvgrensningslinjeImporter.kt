package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.AvtaltavgrensningslinjeInformasjon
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AvtaltAvgrensningslinjeInformasjonRepository
import no.kartverket.nibas.repository.AvtaltAvgrensningslinjeRepository
import no.kartverket.nibas.repository.FastsettingstypeRepository
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
    private val avtaltAvgrensningslinjeRepository: AvtaltAvgrensningslinjeRepository,
    private val avtaltAvgrensningslinjeInformasjonRepository: AvtaltAvgrensningslinjeInformasjonRepository,
    private val landKodeRepository: LandKodeRepository,
    private val kodelisteService: KodelisteService,
    private val grensestatusRepository: GrensestatusRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val dokumentasjonsreferanseImporter: DokumentasjonsreferanseImporter

) {

    val logger: Logger = Logger.getLogger(this::class.java.name)

    fun importAvtaltAvgrensningslinje(sosiAvtaltAvgrensningslinje: SosiGeometri): Grense {
        return when (val existing =
            avtaltAvgrensningslinjeRepository.findByLokalid(sosiAvtaltAvgrensningslinje.getIdent()?.getLokalId()!!)) {
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
                        else -> kodelisteService.getMalemetodeIdByCodeValue(maalemetode)
                    }
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

                avtaltavgrensningslinje.grense = SosiGeometryFunctions.sosiKoordinaterToLineString(
                    sosiAvtaltAvgrensningslinje.getAlleSosiNordOest(),
                    sosiAvtaltAvgrensningslinje.koordsys
                )
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
