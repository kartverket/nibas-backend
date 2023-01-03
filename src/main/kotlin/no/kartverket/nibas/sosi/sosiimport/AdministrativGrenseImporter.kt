package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.sosi.common.SosiKoder.FYLKESGRENSE
import no.kartverket.nibas.sosi.common.SosiKoder.KOMMUNEGRENSE
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AdministrativGrenseImporter @Autowired constructor(
    private val kodelisteService: KodelisteService
) {

    fun importer(skalImporteres: SplitTilImport): Grense {
        val sosiGeometri = skalImporteres.splitRef.basertPaGrense

        val grense = when (sosiGeometri.getObjType()) {
            FYLKESGRENSE -> createGrense(skalImporteres.lokalId(), Grensetype.Fylkesgrense)
            KOMMUNEGRENSE -> createGrense(skalImporteres.lokalId(), Grensetype.Kommunegrense)
            else -> throw RuntimeException("Ukjent grensetype for import av administrativ grense")
        }

        grense.opphav = sosiGeometri.getOpphav()
        grense.gyldighet = Gyldighet(sosiGeometri.getGyldigFra(), sosiGeometri.getGyldigTil())
        grense.datafangstdato = sosiGeometri.getDatafangstDato()
        grense.kvalitet = sosiGeometri.getPosisjonskvalitet(kodelisteService)
        grense.grense = skalImporteres.splitRef.splittetGrense

        return grense
    }

}
