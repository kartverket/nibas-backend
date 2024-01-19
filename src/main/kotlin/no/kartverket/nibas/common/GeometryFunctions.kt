package no.kartverket.nibas.common

import org.locationtech.jts.geom.LineString

/**
 * Denne funkksjonen tar inn en liste med LineStrings som representerer alle grensene for en krets, og returnerer
 * en liste med lister av grenser for hvert polygon.
 *
 * Den ytterse listen har ett element for hver flate i denne kretsen, og den indre listen er liste med alle grensene for denne flaten.
 * Den indre listen har grensene i sortert rekkefølge - slik at grensene kommer i sammenhengende rekkefølge. Med andre ord at sluttpunktet
 * på et grensestykke vil være startpoint for neste grensestykke i listen.
 */
fun getGrenserIRiktigRekkefoelge(grenser: List<LineString>): List<List<LineString>> {
    val grenserSomGjenstaar = grenser.toMutableList()
    val nyeGrenseRinger = mutableListOf<List<LineString>>()
    val nyeGrenser = mutableListOf<LineString>()

    while (grenserSomGjenstaar.size > 0) {
        if (nyeGrenser.isEmpty()) {
            nyeGrenser.add(grenserSomGjenstaar.first())
            grenserSomGjenstaar.remove(nyeGrenser.last())
        }

        val forrigeGrense = nyeGrenser.last()
        val nesteGrense = grenserSomGjenstaar.firstOrNull { it.startPoint == forrigeGrense.endPoint || it.endPoint == forrigeGrense.endPoint }
            ?: throw RuntimeException("Finner ikke neste LineString i grensen. Dette skal ikke skje og tyder på feil i implementasjonen eller dataene.")

        grenserSomGjenstaar.remove(nesteGrense)

        if (nesteGrense.startPoint == forrigeGrense.endPoint) {
            nyeGrenser.add(nesteGrense)
        } else {
            nyeGrenser.add(nesteGrense.reverse())
        }

        if (nyeGrenser.first().startPoint == nyeGrenser.last().endPoint) {
            nyeGrenseRinger.add(nyeGrenser.toList())
            nyeGrenser.clear()
        }
    }

    return nyeGrenseRinger.toList()
}
