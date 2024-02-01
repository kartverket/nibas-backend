package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import org.locationtech.jts.geom.MultiPolygon
import java.time.LocalDate

interface Flate : CommonMetadataProperties {
    val id: String
    var omraade: MultiPolygon?
    val type: Flatetype

    fun getGrensekoblinger(gyldighetsdato: LocalDate = LocalDate.now()): Set<GrenseKobling>
}

interface KobletTilGrense {
    val lokalid: String
    val version: Int

    fun oppdaterEllerLeggTilGrenseKobling(grenseMedAttributter: GrenseMedAttributter)
    fun antallGrenser(): Int
    fun addGrensekobling(grense: Grense, grenseKoblingAttributter: GrenseKoblingAttributter)
    fun removeGrensekobling(gammelGrense: Grense)
    fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?, grenseKoblingAttributter: GrenseKoblingAttributter)
    fun oppdaterOmraadeOgPosisjon()
}
