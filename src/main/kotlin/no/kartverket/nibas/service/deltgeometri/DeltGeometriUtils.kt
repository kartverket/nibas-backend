package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.slf4j.LoggerFactory

fun Grense.antallLikePunkt(annenGrense: Grense): Int =
    this.grense.coordinates.count { punkt -> annenGrense.grense.coordinates.any { erSammePunkt(it, punkt) } }

fun Grense.antallLikePunkt(annenGrense: LineString): Int =
    this.grense.coordinates.count { punkt -> annenGrense.coordinates.any { erSammePunkt(it, punkt) } }

fun Collection<GrenseKobling>.finnGrensekoblingForGrense(lineString: LineString): GrenseKobling? {
    // Vi forsøker først å finne en grense som har 2 like punkt. Dette er en enkel sjekk og fungerer i nesten alle tilfeller
    val grenseMed2LikePunkt = this.find { it.grense.antallLikePunkt(lineString) >= 2 }
    if (grenseMed2LikePunkt != null) {
        return grenseMed2LikePunkt
    }

    /**
     * Noen ganger kan det være at grensen faktisk ikke har 2 ulike punkt som noen andre grenser. Dette oppstår i de tilfellene
     * hvor startpunktet til grensen f.eks. kun er ett grensepunkt i stemmekretsgrensene og endepunktet kun er i grunnkretsgrensene
     * Dette skjer i tilfeller hvor man har en kommunegrense som er en rett linje - men den blir splittet av grunnkrets og stemmekrets
     * på ulike steder. I disse tilfellene vil det ene grensepunktet være en del av en grense og det andre grensepunktet vil ligge på
     * linjen mellom dette grensepunktet og neste grensepunkt.
     *
     * Det man vet er at i slike tilfeller eksisterer det 2 grenser som begge kan brukes for å hente ut metadata, hvor den ene kommer
     * fra grunnkrets og den andre kommer fra stemmekrets. Den ene av disse vil ha `lineString.startPoint` som ett punkt i sin grense
     * og den andre vil ha `lineString.endPoint` som en del av sin grense. Siden det ikke har noe å si hvilken av grensene vi bruker
     * velger vi å bare bruke den som har `lineString.startPoint` som del av sin grense.
     */

    return this.find {
        val lastIndex = it.grense.grense.coordinates.lastIndex
        val linjeFraStartPoint = LineSegment(it.grense.grense.coordinates[0], it.grense.grense.coordinates[1])
        val linjeFraEndPoint = LineSegment(it.grense.grense.coordinates[lastIndex], it.grense.grense.coordinates[lastIndex-1])

        val matcherMedStartPoint = erSammePunkt(lineString.startPoint, it.grense.grense.startPoint) && erPunktPaaLinje(lineString.endPoint.coordinate, linjeFraStartPoint)
        val matcherMedEndPoint = erSammePunkt(lineString.startPoint, it.grense.grense.endPoint) && erPunktPaaLinje(lineString.endPoint.coordinate, linjeFraEndPoint)
        matcherMedStartPoint || matcherMedEndPoint
    }
}

fun erPunktPaaLinje(punkt: Coordinate, linje: LineSegment): Boolean {
    val distanseFraLinje = linje.distancePerpendicular(punkt)
    val innenforBoundaryBox = punkt.x >= linje.minX() && punkt.x <= linje.maxX() && punkt.y >= linje.minY() && punkt.y <= linje.maxY()
    return innenforBoundaryBox && distanseFraLinje < 0.1
}

fun erSammePunkt(punkt1: Coordinate, punkt2: Coordinate, toleranse: Double = 0.1): Boolean {
    if (punkt1 == punkt2) {
        return true
    }

    val distanseMellomPunkter = LineSegment(punkt1, punkt2).length
    return distanseMellomPunkter <= toleranse
}

fun erSammePunkt(punkt1: Point, punkt2: Point): Boolean {
    return punkt1.equalsExact(punkt2, 0.1)
}

fun LineString.erstattPunkt(nyttPunkt: Coordinate, index: Int): LineString {
    val coordinates = this.coordinates
    coordinates[index] = nyttPunkt
    return this.factory.createLineString(coordinates)
}

fun Grense.copyWithNewUUID(): Grense =
    Grense(grensetype = this.grensetype)
        .applyMappingFromExistingGrense(this)
        .applyCascadingOperationsBasedOnExistingGrense(this)

fun Grense.copyWithNewUUID(newLineString: LineString): Grense =
    this.copyWithNewUUID()
        .apply { this.grense = newLineString }

