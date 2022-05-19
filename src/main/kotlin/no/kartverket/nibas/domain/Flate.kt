package no.kartverket.nibas.domain

import org.locationtech.jts.geom.MultiPolygon

interface Flate : CommonMetadataProperties {
    val id: String
    var flate: MultiPolygon?

    fun oppdaterFlate()
    fun oppdaterEllerLeggTilGrenseKobling(grense: Grense, attributter: GrenseKoblingAttributter)

}

data class SamlingOgDefault<T : GrenseKobling>(val samling: MutableSet<T>, val default: GrenseKobling)

