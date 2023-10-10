package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.common.pop
import no.kartverket.nibas.domain.Grense
import org.locationtech.jts.geom.Point

fun finnStiMellomPunkterMedBreddeFoerstSoek(startpunkt: Point, endepunkt: Point, grenser: List<Grense>): List<Grense> {
    val searchQueue = mutableListOf(SoekeSti(forrigeEndepunkt = startpunkt, grenserBrukt = emptyList()))
    while(searchQueue.isNotEmpty()) {
        val searchElement = searchQueue.pop()

        if(searchElement.forrigeEndepunkt == endepunkt) {
            return searchElement.grenserBrukt
        }

        val nesteGrenser = grenser
            .filter { it.grense.startPoint == searchElement.forrigeEndepunkt || it.grense.endPoint == searchElement.forrigeEndepunkt }
            .filter { !searchElement.grenserBrukt.contains(it) }

        val nyeStierSomSkalSoekes = nesteGrenser
            .map {
                SoekeSti(
                    forrigeEndepunkt = getEndepunktForGrense(it, searchElement.forrigeEndepunkt),
                    grenserBrukt = searchElement.grenserBrukt + it
                )
            }

        searchQueue.addAll(nyeStierSomSkalSoekes)
    }

    throw RuntimeException("Klarte ikke å finne en sti mellom $startpunkt og $endepunkt")
}

private fun getEndepunktForGrense(grense: Grense, startPunkt: Point) =
    if (grense.grense.startPoint == startPunkt) {
        grense.grense.endPoint
    } else {
        grense.grense.startPoint
    }

data class SoekeSti (
    val forrigeEndepunkt: Point,
    val grenserBrukt: List<Grense>
)
