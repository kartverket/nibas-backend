package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import no.kartverket.nibas.sosi.common.SosiKoder.FYLKESGRENSE
import no.kartverket.nibas.sosi.common.SosiKoder.KOMMUNEGRENSE
import no.kartverket.nibas.sporing.Gyldighet
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

    fun importer(skalImporteres: SplitTilImport): Grense {
        val sosiGeometri = skalImporteres.splitRef.basertPaGrense

        val grense = when (sosiGeometri.getObjType()) {
            FYLKESGRENSE -> createGrense(sosiGeometri.getIdent(), Grensetype.Fylkesgrense)
            KOMMUNEGRENSE -> createGrense(sosiGeometri.getIdent(), Grensetype.Kommunegrense)
            else -> throw RuntimeException("Ukjent grensetype for import av administrativ grense")
        }

        grense.datauttaksdato = sosiGeometri.getDataUttaksdato()
        grense.opphav = sosiGeometri.getOpphav()
        grense.gyldighet = Gyldighet(sosiGeometri.getGyldigFra(), sosiGeometri.getGyldigTil())
        grense.datafangstdato = sosiGeometri.getDatafangstDato()
        grense.omtvistet = sosiGeometri.getOmtvistet()

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

        grense.grense = skalImporteres.splitRef.splittetGrense

        return grense
    }

}
