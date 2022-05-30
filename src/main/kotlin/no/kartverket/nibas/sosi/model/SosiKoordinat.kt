package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.locationtech.jts.geom.Coordinate

class SosiKoordinat(val nord: String, val oest: String, val hoyde: String? = null) : SosiVerdi("$nord $oest $hoyde") {
    fun toCoordinate(enhet: Double): Coordinate = SosiGeometryFunctions.sosiKoordinatToCoordinate(this, enhet)
}
