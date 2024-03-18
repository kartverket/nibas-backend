package no.kartverket.nibas.sosi.geometry

import no.kartverket.nibas.common.geometry.*
import no.kartverket.nibas.sosi.model.SosiKoordinat
import org.locationtech.jts.geom.Coordinate

    fun sosiKoordinatToCoordinate(sosiKoordinat: SosiKoordinat, enhet: Double): Coordinate {
        val precisionModel = getGeometryFactory().precisionModel
        val x = precisionModel.makePrecise(sosiKoordinat.oest.toDouble() * enhet)
        val y = precisionModel.makePrecise(sosiKoordinat.nord.toDouble() * enhet)
        return Coordinate(x, y)
    }
