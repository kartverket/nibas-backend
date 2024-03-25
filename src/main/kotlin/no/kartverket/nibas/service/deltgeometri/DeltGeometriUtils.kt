package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point

fun Grense.antallLikePunkt(annenGrense: LineString): Int =
    this.grense.coordinates.count { punkt -> annenGrense.coordinates.any { erSammePunkt(it, punkt) } }

fun Collection<GrenseKobling>.finnGrensekoblingForGrense(lineString: LineString): GrenseKobling? {
    // Vi forsøker først å finne en grense som har 2 like punkt (endepunkt eller kurvepunkt) som linestringen.
    // Dette er en enkel sjekk og fungerer i nesten alle tilfeller
    val grenseMed2LikePunkt = this.find { it.grense.antallLikePunkt(lineString) >= 2 }
    if (grenseMed2LikePunkt != null) {
        return grenseMed2LikePunkt
    }

    /**
     * Noen ganger kan det være at linestringen faktisk ikke har 2 like punkt som kommunens grenser. Dette oppstår i de tilfellene
     * hvor startpunktet til grensen f.eks. kun er ett grensepunkt i stemmekretsgrensene og endepunktet kun er i grunnkretsgrensene.
     * Dette skjer i tilfeller hvor man har en kommunegrense som er en rett linje - men den blir splittet av grunnkrets og stemmekrets
     * på ulike steder. I disse tilfellene vil det ene grensepunktet være en del av en grense og det andre grensepunktet vil ligge på
     * linjen mellom dette grensepunktet og neste grensepunkt.
     */
    val grenseMedLiktStartpunktEllerSluttpunkt = this.find {
        val lastIndex = it.grense.grense.coordinates.lastIndex
        val linjeFraStartPoint = LineSegment(it.grense.grense.coordinates[0], it.grense.grense.coordinates[1])
        val linjeFraEndPoint = LineSegment(it.grense.grense.coordinates[lastIndex], it.grense.grense.coordinates[lastIndex - 1])

        // Hvis det finnes ei grense med samme startpunkt som linestringen og
        // hvor endepunktet til linestringen ligger på linja mellom grensa sitt startpunkt og neste punkt, trenger vi ikke å sjekke videre
        if (erSammePunkt(lineString.startPoint, it.grense.grense.startPoint) && erPunktPaaLinje(lineString.endPoint.coordinate, linjeFraStartPoint)) {
            return@find true
        }
        // Sjekk om det finnes ei grense med endpunkt som linestringens startpunkt og
        // hvor endepunktet til linestringen ligger på linja mellom grensa sitt endpunkt og neste siste punkt
        erSammePunkt(lineString.startPoint, it.grense.grense.endPoint) && erPunktPaaLinje(lineString.endPoint.coordinate, linjeFraEndPoint)
    }
    if (grenseMedLiktStartpunktEllerSluttpunkt != null) {
        return grenseMedLiktStartpunktEllerSluttpunkt
    }

    // Linestringens start- og endpunkt sammenFaller ikke med endepunkt eller kurvepunkt for noen av grunnkretsgrensene
    // Ser om linestringens startpunkt ligger mellom noen av kurvepunktene til grunnkretsgrensene
    // Trenger ikke sjekke linestringens endpunkt, fordi linestringen vil aldri gå over mer enn en grunnkretsgrense
    val grenseSomLinestringLiggerPaa = this.find {
        val koordinater = it.grense.grense.coordinates
        for (i in 0..koordinater.size - 2) {
            val startpunkt = koordinater[i]
            val sluttpunkt = koordinater[i + 1]
            val linjeFraStartPoint = LineSegment(startpunkt, sluttpunkt)
            if (erPunktPaaLinje(lineString.startPoint.coordinate, linjeFraStartPoint)) {
                return@find true
            }
        }

        false
    }

    return grenseSomLinestringLiggerPaa
}

fun erPunktPaaLinje(punkt: Coordinate, linje: LineSegment): Boolean {
    val distanseFraLinje = linje.distancePerpendicular(punkt)
    val innenforBoundaryBox = punkt.x >= linje.minX() && punkt.x <= linje.maxX() && punkt.y >= linje.minY() && punkt.y <= linje.maxY()
    return innenforBoundaryBox && distanseFraLinje <= 0.001
}

fun erSammePunkt(punkt1: Coordinate, punkt2: Coordinate, toleranse: Double = 0.001): Boolean {
    if (punkt1 == punkt2) {
        return true
    }

    val distanseMellomPunkter = LineSegment(punkt1, punkt2).length
    return distanseMellomPunkter <= toleranse
}

fun erSammePunkt(punkt1: Point, punkt2: Point): Boolean {
    return punkt1.equalsExact(punkt2, 0.001)
}



