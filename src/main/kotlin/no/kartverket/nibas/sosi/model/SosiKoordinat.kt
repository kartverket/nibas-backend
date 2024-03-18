package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.geometry.sosiKoordinatToCoordinate
import org.locationtech.jts.geom.Coordinate

class SosiKoordinat(val nord: String, val oest: String, hoyde: String? = null) : SosiVerdi("$nord $oest $hoyde") {
    fun toCoordinate(enhet: Double): Coordinate = sosiKoordinatToCoordinate(this, enhet)
}
