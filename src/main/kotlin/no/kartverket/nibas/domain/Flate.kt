package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Flatetype
import org.locationtech.jts.geom.MultiPolygon

interface Flate : CommonMetadataProperties {
    val id: String
    var flate: MultiPolygon?
    val type: Flatetype

    var registreringsversjon: Registreringsversjon?
    var kopidata: Kopidata?

    fun oppdaterFlate()
}

interface KobletTilGrense {
    val id: String

    fun oppdaterEllerLeggTilGrenseKobling(grenseMedAttributter: GrenseMedAttributter)
}
