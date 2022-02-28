package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.AdministrativGrense
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.updateSosiIdentifikasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiGeometri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class AdministrativGrenseImporter @Autowired constructor(
    private val grensestatusRepository: GrensestatusRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
    private val noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
    private val kodelisteService: KodelisteService
) {

    fun importer(sosiGeometri: SosiGeometri): AdministrativGrense {
        val grense = when (sosiGeometri.getObjType()) {
            SosiKoder.FYLKESGRENSE -> Fylkesgrense()
            SosiKoder.KOMMUNEGRENSE -> Kommunegrense()
            else -> throw RuntimeException("Ukjent grensetype for import av administrativ grense")
        }

        grense.updateSosiIdentifikasjon(sosiGeometri.getIdent())
        grense.oppdateringsdato = sosiGeometri.getOppdateringsdato()!!
        grense.datauttaksdato = sosiGeometri.getDataUttaksdato()
        grense.kvalitet?.noeyaktighet = sosiGeometri.getKvalitet()?.getNoyaktighet()
        grense.opphav = sosiGeometri.getOpphav()
        grense.gyldigFra = sosiGeometri.getGyldigFra()
        grense.gyldigTil = sosiGeometri.getGyldigTil()
        grense.datafangstdato = sosiGeometri.getDatafangstDato()
        grense.omtvistet = sosiGeometri.getOmtvistet()

        grense.kvalitet?.maalemetode = when (val maalemetode = sosiGeometri.getKvalitet()?.getMalemetode()) {
            null -> null
            else -> kodelisteService.getMalemetodeIdByCodeValue(maalemetode)
        }
        grense.grensestatus = when (val grenseStatus = sosiGeometri.getGrensestatus()) {
            null -> null
            else -> grensestatusRepository.findByIdOrNull(grenseStatus) ?: throw RuntimeException("Ukjent grensestatus")
        }

        grense.fastsettingstype =
            when (val fastsettingstype = sosiGeometri.getFastsettingstype()) {
                null -> null
                else -> fastsettingstypeRepository.findByIdOrNull(fastsettingstype)
                    ?: throw RuntimeException("Ukjent fastsettingstype")
            }

        grense.foelgerterrengdetalj =
            when (val foelgerterrengdetalj = sosiGeometri.getFolgerTerrengdetalj()) {
                null -> null
                else -> terrengdetaljkodeRepository.findByIdOrNull(foelgerterrengdetalj)
                    ?: throw RuntimeException("Ukjent terrengdetalj")
            }

        grense.noeyaktighetsklasse =
            when (val noeyaktighetsklasse = sosiGeometri.getNoyaktighetsklasse()) {
                null -> null
                else -> noeyaktighetsklassekodeRepository.findByIdOrNull(noeyaktighetsklasse) ?: throw RuntimeException(
                    "Ukjent nøyaktighetsklasse"
                )
            }

        grense.grense =
            SosiGeometryFunctions.sosiKoordinaterToLineString(sosiGeometri.getAlleSosiNordOest(), sosiGeometri.koordsys)
        return grense
    }
}
