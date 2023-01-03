package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KommunalKretsgrenseImporter @Autowired constructor(
    private val kodelisteService: KodelisteService,
) {

    fun importer(skalImporteres: SplitTilImport): Grense {
        val sosiGeometri = skalImporteres.splitRef.basertPaGrense

        val grense = when (sosiGeometri.getObjType()) {
            SosiKoder.STEMMEKRETSGRENSE -> createGrense(skalImporteres.lokalId(), Grensetype.Stemmekretsgrense)
            else -> throw RuntimeException("Ukjent grensetype (${sosiGeometri.getObjType()}) for kommunal kretsgrense")
        }

        grense.datafangstdato = sosiGeometri.getDatafangstDato()
        grense.gyldighet = Gyldighet(sosiGeometri.getGyldigFra(), sosiGeometri.getGyldigTil())
        grense.kvalitet = sosiGeometri.getPosisjonskvalitet(kodelisteService)
        grense.opphav = sosiGeometri.getOpphav()
        grense.grense = skalImporteres.splitRef.splittetGrense

        return grense

    }
}

