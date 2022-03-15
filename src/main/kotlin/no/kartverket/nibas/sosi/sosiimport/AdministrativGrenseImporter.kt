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
        grense.datauttaksdato = sosiGeometri.getDataUttaksdato()
        grense.opphav = sosiGeometri.getOpphav()
        grense.gyldigFra = sosiGeometri.getGyldigFra()
        grense.gyldigTil = sosiGeometri.getGyldigTil()
        grense.datafangstdato = sosiGeometri.getDatafangstDato()
        grense.omtvistet = sosiGeometri.getOmtvistet()

        grense.oppdateringsdato = sosiGeometri.getOppdateringsdato() ?: grense.oppdateringsdato
        grense.kvalitet = sosiGeometri.getPosisjonskvalitet(kodelisteService)
        grense.grensestatus = sosiGeometri.getGrensestatus()
            ?.let { grensestatusRepository.findByIdOrNull(it) ?: throw RuntimeException("Ukjent grensestatus") }
        grense.fastsettingstype = sosiGeometri.getFastsettingstype()
            ?.let { fastsettingstypeRepository.findByIdOrNull(it) ?: throw RuntimeException("Ukjent fastsettingstype") }
        grense.foelgerterrengdetalj = sosiGeometri.getFolgerTerrengdetalj()
            ?.let { terrengdetaljkodeRepository.findByIdOrNull(it) ?: throw RuntimeException("Ukjent terrengdetalj") }
        grense.noeyaktighetsklasse = sosiGeometri.getNoyaktighetsklasse()?.let {
            noeyaktighetsklassekodeRepository.findByIdOrNull(it) ?: throw RuntimeException("Ukjent nøyaktighetsklasse")
        }

        grense.grense =
            SosiGeometryFunctions.sosiKoordinaterToLineString(sosiGeometri.getAlleSosiNordOest(), sosiGeometri.koordsys)

        return grense
    }

}
