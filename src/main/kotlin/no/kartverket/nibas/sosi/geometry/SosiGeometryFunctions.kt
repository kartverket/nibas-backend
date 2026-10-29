package no.kartverket.nibas.sosi.geometry

import no.kartverket.nibas.common.geometry.getGeometryFactory
import no.kartverket.nibas.sosi.model.SosiKoordinat
import org.locationtech.jts.geom.Coordinate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun sosiKoordinatToCoordinate(sosiKoordinat: SosiKoordinat, enhet: Double): Coordinate {
    val precisionModel = getGeometryFactory().precisionModel
    val x = precisionModel.makePrecise(sosiKoordinat.oest.toDouble() * enhet)
    val y = precisionModel.makePrecise(sosiKoordinat.nord.toDouble() * enhet)
    return Coordinate(x, y)
}

/**
 * En BUEP er en sirkelbue gitt ved start-, midt- og sluttpunkt. NIBAS lagrer bare linjer, så buen deles opp i
 * linjestykker på høyst [maksVinkel] radianer hver, som standard 32 per kvartsirkel slik PostGIS gjør.
 * Start og slutt beholdes eksakt, så buen henger sammen med nabolinjene.
 */
fun bueTilKoordinater(start: Coordinate, midt: Coordinate, slutt: Coordinate, maksVinkel: Double = PI / 64): List<Coordinate> {
    // Regner relativt til startpunktet, ellers spiser kvadrerte UTM-koordinater opp presisjonen på små buer
    val mx = midt.x - start.x
    val my = midt.y - start.y
    val ex = slutt.x - start.x
    val ey = slutt.y - start.y
    val d = 2 * (mx * ey - my * ex)
    // Tre punkter på linje er ingen bue
    if (abs(d) < 1e-9) return listOf(start, midt, slutt)

    val m2 = mx * mx + my * my
    val e2 = ex * ex + ey * ey
    val cx = start.x + (ey * m2 - my * e2) / d
    val cy = start.y + (mx * e2 - ex * m2) / d
    val radius = hypot(start.x - cx, start.y - cy)

    val startvinkel = atan2(start.y - cy, start.x - cx)
    fun motKlokka(til: Coordinate): Double = (atan2(til.y - cy, til.x - cx) - startvinkel).mod(2 * PI)
    val sveip = if (motKlokka(midt) < motKlokka(slutt)) motKlokka(slutt) else motKlokka(slutt) - 2 * PI

    val antallSegmenter = maxOf(2, ceil(abs(sveip) / maksVinkel).toInt())
    val mellompunkter = (1 until antallSegmenter).map { i ->
        val vinkel = startvinkel + sveip * i / antallSegmenter
        Coordinate(cx + radius * cos(vinkel), cy + radius * sin(vinkel))
    }
    return listOf(start) + mellompunkter + listOf(slutt)
}
