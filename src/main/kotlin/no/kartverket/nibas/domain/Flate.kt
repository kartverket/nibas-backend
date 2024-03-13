package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import org.locationtech.jts.geom.MultiPolygon
import java.time.LocalDate

interface Flate : CommonMetadataProperties {
    val id: String
    var omraade: MultiPolygon?
    val type: Flatetype

    fun getGrensekoblinger(gyldighetsdato: LocalDate = LocalDate.now()): Set<GrenseKobling>

    fun getFlateInformasjon(): FlateInformasjon
}

interface KobletTilGrense {
    val lokalid: String
    val version: Int

    fun oppdaterEllerLeggTilGrenseKobling(grense: Grense)
    fun antallGrenser(): Int
    fun addGrensekobling(grense: Grense)
    fun removeGrensekobling(gammelGrense: Grense)
    fun replaceGrensekobling(gammelGrense: Grense, grense: Grense?)
    fun oppdaterOmraadeOgPosisjon()
}

data class FlateInformasjon (
    val flatetype: Flatetype,
    val lesbartNavn: String
)
