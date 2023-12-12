package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import org.locationtech.jts.geom.MultiPolygon

interface Flate : CommonMetadataProperties {
    val id: String
    var omraade: MultiPolygon?
    val type: Flatetype
}

interface KobletTilGrense {
    val lokalid: String

    fun oppdaterEllerLeggTilGrenseKobling(grenseMedAttributter: GrenseMedAttributter)
    fun antallGrenser(): Int
    fun addGrensekobling(grense: Grense, grenseKoblingAttributter: GrenseKoblingAttributter)
    fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?, grenseKoblingAttributter: GrenseKoblingAttributter)
    fun oppdaterOmraadeOgPosisjon()
}
