package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.sosi.model.SosiKurve
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class GrenseImporter {

    @Autowired
    lateinit var avtaltAvgrensningslinjeImporter: AvtaltAvgrensningslinjeImporter

    @Autowired
    lateinit var lovVirkeomraadeGrenseImporter: LovVirkeomraadeGrenseImporter

    @Autowired
    lateinit var grunnlinjeImporter: GrunnlinjeImporter

    @Autowired
    lateinit var riksgrenseImporter: RiksgrenseImporter

    @Autowired
    lateinit var territorialgrenseImporter: TerritorialgrenseImporter

    @Autowired
    lateinit var kommunegrenseImporter: KommunegrenseImporter

    @Autowired
    lateinit var fylkesgrenseImporter: FylkesgrenseImporter

    fun importerGrense(grense: SosiGeometri): Grense {
        return when (grense.getObjType()) {
            SosiKoder.KOMMUNEGRENSE -> kommunegrenseImporter.importerKommunegrense(grense)
            SosiKoder.FYLKESGRENSE -> fylkesgrenseImporter.importerFylkesgrense(grense)
            SosiKoder.TERRITORIALGRENSE -> territorialgrenseImporter.importSosiTerritorialgrense(grense)
            SosiKoder.RIKSGRENSE -> riksgrenseImporter.importSosiRiksgrense(grense)
            SosiKoder.AVTALT_AVGRENSNINGSLINJE -> avtaltAvgrensningslinjeImporter.importAvtaltAvgrensningslinje(grense)
            SosiKoder.LOV_VIRKEOMRADEGRENSE -> lovVirkeomraadeGrenseImporter.importLovVirkeomradeGrense(grense as SosiKurve)
            SosiKoder.GRUNNLINJE -> grunnlinjeImporter.importGrunnlinje(grense)
            else -> throw RuntimeException("Found no importer for ${grense.getObjType()}")
        }
    }
}