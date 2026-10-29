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
    val d = 2 * (start.x * (midt.y - slutt.y) + midt.x * (slutt.y - start.y) + slutt.x * (start.y - midt.y))
    // Tre punkter på linje er ingen bue
    if (abs(d) < 1e-9) return listOf(start, midt, slutt)

    val s2 = start.x * start.x + start.y * start.y
    val m2 = midt.x * midt.x + midt.y * midt.y
    val e2 = slutt.x * slutt.x + slutt.y * slutt.y
    val cx = (s2 * (midt.y - slutt.y) + m2 * (slutt.y - start.y) + e2 * (start.y - midt.y)) / d
    val cy = (s2 * (slutt.x - midt.x) + m2 * (start.x - slutt.x) + e2 * (midt.x - start.x)) / d
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
