package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.common.pop
import no.kartverket.nibas.domain.Grense
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Point

fun finnStiMellomPunkterMedBreddeFoerstSoek(startpunkt: Point, endepunkt: Point,
                                            grenser: List<Grense>, mellompunkter: List<Coordinate> = emptyList()): List<Grense> {
    val searchQueue = mutableListOf(SoekeSti(forrigeEndepunkt = startpunkt, grenserBrukt = emptyList()))
    while (searchQueue.isNotEmpty()) {
        val searchElement = searchQueue.pop()

        if (searchElement.forrigeEndepunkt == endepunkt && searchElement.grenserBrukt.isNotEmpty()) {
            //Søkestien har nådd endepunktet og det er noen brukte grenser (ønsker ikke den første dummysøkestien hvis hvis ønsket startpunkt er likt sluttpunkt)
            //Hvis grensa det søkes med ikke har noen kurvepunkter, bruk den funnede søkestien
            if (mellompunkter.isEmpty()) {
                return searchElement.grenserBrukt
            }

            if (searchElement.grenserBrukt.any { grense -> grense.grense.coordinates.any { punkt -> mellompunkter.any { erSammePunkt(punkt, it) } } }) {
                //Bruk den funnede søkestien hvis den inneholder minst ett punkt fra mellompunktene til grensa det søkes med
                return searchElement.grenserBrukt
            }
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

data class SoekeSti(
    val forrigeEndepunkt: Point,
    val grenserBrukt: List<Grense>
)
