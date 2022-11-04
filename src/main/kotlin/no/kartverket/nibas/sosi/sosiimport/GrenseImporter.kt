package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrenseMedAttributter
import no.kartverket.nibas.domain.KobletTilGrense
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiIdent
import no.kartverket.nibas.sosi.model.SosiKurve
import no.kartverket.nibas.sosi.model.SosiReferanse
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

    @Autowired
    lateinit var grunnkretsgrenseImporter: GrunnkretsgrenseImporter

    @Autowired
    lateinit var delomraadegrenseImporter: DelomraadegrenseImporter

    @Autowired
    lateinit var stemmekretsgrenseImporter: StemmekretsgrenseImporter

    fun importerGrenseRef(
        grunnkrets: KobletTilGrense,
        ref: SosiReferanse,
        kurver: List<SosiKurve>,
        kommuneFlater: List<SosiFlate>,
        skallIndex: Int,
        hullIndex: Int? = null
    ) {

        val referanse = ref.getVerdiUtenRetningOgParentes()
        when (val grense = kurver.find { it.getId() == referanse }) {
            null -> throw RuntimeException("Finner ikke grense som refereres til. Feil i datagrunnlaget. Ref: $referanse")
            else -> {
                grense.erRetningMedKlokken = ref.erRetningMedKlokken()
                val skalImporteres: List<SplitTilImport> = when(GrenseMatcher.skalMatches(grense)) {
                    true -> GrenseMatcher.match(ref, grense, kommuneFlater)
                    false -> listOf(SplitTilImport(SplitRef(grense.asLineString(), grense), ref, grense))
                }

                skalImporteres.forEach {
                    grunnkrets.oppdaterEllerLeggTilGrenseKobling(
                        GrenseMedAttributter(
                            grense = importerGrense(it),
                            attributter = GrenseKoblingAttributter(
                                retningMedKlokken = it.splitRef.basertPaGrense.erRetningMedKlokken,
                                rekkefoelge = grunnkrets.antallGrenser(),
                                flateIndeks = skallIndex,
                                hullIndeks = hullIndex
                            )
                        )
                    )
                }
            }
        }
    }

    fun importerGrense(skalImporteres: SplitTilImport): Grense {
        return when (skalImporteres.splitRef.basertPaGrense.getObjType()) {
            SosiKoder.KOMMUNEGRENSE -> kommunegrenseImporter.importerKommunegrense(skalImporteres)
            SosiKoder.FYLKESGRENSE -> fylkesgrenseImporter.importerFylkesgrense(skalImporteres)
            SosiKoder.TERRITORIALGRENSE -> territorialgrenseImporter.importSosiTerritorialgrense(skalImporteres)
            SosiKoder.RIKSGRENSE -> riksgrenseImporter.importSosiRiksgrense(skalImporteres)
            SosiKoder.AVTALT_AVGRENSNINGSLINJE -> avtaltAvgrensningslinjeImporter.importAvtaltAvgrensningslinje(skalImporteres)
            SosiKoder.LOV_VIRKEOMRADEGRENSE -> lovVirkeomraadeGrenseImporter.importLovVirkeomradeGrense(skalImporteres)
            SosiKoder.GRUNNLINJE -> grunnlinjeImporter.importGrunnlinje(skalImporteres)
            SosiKoder.GRUNNKRETSGRENSE -> grunnkretsgrenseImporter.importerGrunnkretsgrense(skalImporteres)
            SosiKoder.STEMMEKRETSGRENSE -> stemmekretsgrenseImporter.importerStemmekretsgrense(skalImporteres)
            SosiKoder.DELOMRAADEGRENSE -> delomraadegrenseImporter.importerDelomraadegrense(skalImporteres)
            else -> throw RuntimeException("Found no importer for ${skalImporteres.splitRef.basertPaGrense.getObjType()}")
        }
    }
}

/**
 * Oppretter Grense med lokalid fra eksternt system hvis den er gyldig.
 */
fun createGrense(sosiIdent: SosiIdent?, grensetype: Grensetype): Grense {
    val lokalid: String? = sosiIdent?.getValidLokalId()
    return if (lokalid != null) {
        Grense(lokalid = lokalid, grensetype = grensetype)
    } else {
        Grense(grensetype = grensetype)
    }
}
