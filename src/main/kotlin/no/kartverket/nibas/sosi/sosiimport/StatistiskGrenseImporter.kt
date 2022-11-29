package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StatistiskGrenseImporter @Autowired constructor(
    private val kodelisteService: KodelisteService,
) {

    fun importer(skalImporteres: SplitTilImport): Grense {
        val sosiGeometri = skalImporteres.splitRef.basertPaGrense

        val grense = when (sosiGeometri.getObjType()) {
            SosiKoder.GRUNNKRETSGRENSE -> createGrense(sosiGeometri.getIdent(), Grensetype.Grunnkretsgrense)
            SosiKoder.DELOMRAADEGRENSE -> createGrense(sosiGeometri.getIdent(), Grensetype.Delomraadegrense)
            else -> throw RuntimeException("Ukjent grensetype (${sosiGeometri.getObjType()}) for statistisk grense")
        }

        grense.datafangstdato = sosiGeometri.getDatafangstDato()
        grense.gyldighet = Gyldighet(sosiGeometri.getGyldigFra(), sosiGeometri.getGyldigTil())
        grense.informasjonstekst = sosiGeometri.getInformasjon()
        grense.kvalitet = sosiGeometri.getPosisjonskvalitet(kodelisteService)
        grense.opphav = sosiGeometri.getOpphav()
        grense.grense = skalImporteres.splitRef.splittetGrense

        return grense

    }
}
