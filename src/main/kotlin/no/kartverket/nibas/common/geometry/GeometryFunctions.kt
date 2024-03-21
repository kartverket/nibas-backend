package no.kartverket.nibas.common.geometry

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.operation.valid.IsValidOp
import org.locationtech.jts.operation.valid.TopologyValidationError

const val defaultSrid = 25833

private val koordsysToSrid: Map<String, Int> = mapOf(
    Pair("23", defaultSrid)
)

fun getGeometryFactory(koordsys: String? = null): GeometryFactory {
    val srid = koordsys?.let { koordsysToSrid[koordsys] } ?: defaultSrid
    return createGeometryFactory(srid)
}

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
    var nyeGrenser = mutableListOf<LineString>()

    while (grenserSomGjenstaar.size > 0) {
        if (nyeGrenser.isEmpty()) {
            nyeGrenser.add(grenserSomGjenstaar.first())
            grenserSomGjenstaar.remove(nyeGrenser.last())
        }

        val forrigeGrense = nyeGrenser.last()

        // Hvis en grense biter seg selv i halen så er dette et eget polygon
        if (forrigeGrense.startPoint == forrigeGrense.endPoint) {
            nyeGrenser.removeLast()
            nyeGrenseRinger.add(listOf(forrigeGrense))
            continue
        }

        val punkterSomErBesoekt = listOf(nyeGrenser.first().startPoint) + nyeGrenser.map { it.endPoint }
        val nesteGrense = grenserSomGjenstaar.firstOrNull { it.startPoint == forrigeGrense.endPoint || it.endPoint == forrigeGrense.endPoint }
            ?: throw UgyldigGeometriException(TopologyValidationError(TopologyValidationError.RING_NOT_CLOSED, forrigeGrense.endPoint.coordinate))

        grenserSomGjenstaar.remove(nesteGrense)

        if (nesteGrense.startPoint == forrigeGrense.endPoint) {
            nyeGrenser.add(nesteGrense)
        } else {
            nyeGrenser.add(nesteGrense.reverse())
        }

        // Om endepunktet på grensen vi akkurat la til er et punkt vi allerede har besøkt betyr det at disse grensene som er mellom her dannet et polygon
        // Vi fjerner derfor grensene som dannet dette polygonet. Ved tilfeller hvor man har enklaver som også overlapper med en annen flate i ett punkt
        // som f.eks. frogn så har man en enklave som berører kommunen i ett punkt. Det da da viktig at vi sørger for at denne enklaven blir ett eget polygon.
        if (punkterSomErBesoekt.contains(nyeGrenser.last().endPoint)) {
            val indexOfFoerstGrenseIPolygon = nyeGrenser.indexOfFirst { it.startPoint == nyeGrenser.last().endPoint }
            if (indexOfFoerstGrenseIPolygon == -1) {
                throw RuntimeException("Det oppstod en feil ved sortering av grensene. Klarte ikke finne hvilke grenser som tilhørte et komplett polygon.")
            }

            val grenserForPolygon = nyeGrenser.subList(indexOfFoerstGrenseIPolygon, nyeGrenser.size)
            nyeGrenseRinger.add(grenserForPolygon)
            nyeGrenser = nyeGrenser.dropLast(grenserForPolygon.size).toMutableList()
        }
    }

    if (nyeGrenser.isNotEmpty()) {
        throw RuntimeException("Ikke alle grensene ble brukt når vi bygde opp polygonet. Det er ${nyeGrenser.size} som ikke ble brukt i danning av polygonene.")
    }

    return nyeGrenseRinger.toList()
}

/**
 * Transformerer en liste med linestrings til et multipolygon
 */
fun List<List<LineString>>.toMultiPolygon(): MultiPolygon {
    val geometryFactory = createGeometryFactory()
    data class PolygonHelper(
        val lineStrings: List<LineString>,
        val linearRing: LinearRing,
        val polygon: Polygon
    )

    val polygonHelpers = this.map {
        PolygonHelper(
            lineStrings = it,
            linearRing = konverterLineStringsTilLinearRing(it),
            polygon = konverterLineStringsTilPolygon(it)
        )
    }

    val polygoner = polygonHelpers.map { it.polygon }

    val (hull, skall) = polygonHelpers.partition { erFlateHullIEtPolygon(it.polygon, polygoner) }

    val flater = skall.map {
        val hullIFlaten = hull.filter { h -> h.polygon.within(it.polygon) }
            .map { h-> h.linearRing }
            .toTypedArray()

        geometryFactory.createPolygon(it.linearRing, hullIFlaten)
    }

    return geometryFactory.createMultiPolygon(flater.toTypedArray())
}

/**
 * Konverterer en liste av linestrings til et polygon
 */
fun konverterLineStringsTilPolygon(lineStrings: List<LineString>): Polygon {
    val geometryFactory = getGeometryFactory()
    val linearRing = konverterLineStringsTilLinearRing(lineStrings)
    val polygon = geometryFactory.createPolygon(linearRing)
    validateGeometry(polygon)
    return polygon
}

/**
 * Konverterer en liste av linestrings til in linear ring
 */
fun konverterLineStringsTilLinearRing(lineStrings: List<LineString>): LinearRing {
    val geometryFactory = getGeometryFactory()
    validerSammenhengendeGrenser(lineStrings)

    // Startpunkt på lineString er samme koordinat some endepunkt på linestringen før
    // For å unngå duplikater dropper vi endepuktet på hver grense. Men for at grensen skal bli et komplett
    // polygon må vi ikke fjerne det absolutt siste endepunktet.
    val coordinates = lineStrings.flatMap { it.coordinates.toList().dropLast(1) } + lineStrings.last().endPoint.coordinate
    return geometryFactory.createLinearRing(coordinates.toTypedArray())
}

/**
 * Tar inn en flate og sjekker om denne flaten er et hull i den gitte listen med flater
 *
 * @param flate Flaten man ønsker å sjekke om er et hull
 * @param flater Flatene man sjekker om denne er et hull i
 * @return Om flaten er et hull
 */
fun erFlateHullIEtPolygon(flate: Geometry, flater: List<Geometry>): Boolean {
    return flater.any { currentFlate -> currentFlate != flate && flate.within(currentFlate) }
}

/**
 * Kjører en isValid-sjekk på geometri, se javadoc på IsValidOp for detaljer.
 */
fun validateGeometry(geometry: Geometry) {
    val validationError = IsValidOp(geometry).validationError
    if (validationError != null) {
        throw UgyldigGeometriException(validationError)
    }
}

/**
 * Tar inn en liste med grenser og validerer at de er sammenhengende, i riktig rekkefølge og
 * har riktig retning
 */
fun validerSammenhengendeGrenser(lineStrings: List<LineString>) {
    val forsteGrense = lineStrings[0]
    val sisteGrense = lineStrings[lineStrings.size - 1]
    if (sisteGrense.endPoint != forsteGrense.startPoint) {
        throw IkkeSammenhengendePolygonException(sisteGrense.endPoint, forsteGrense.startPoint, TopologyValidationError(TopologyValidationError.DISCONNECTED_INTERIOR))
    }
    for (i in 0..lineStrings.size - 2) {
        val gjeldendeGrense = lineStrings[i]
        val nesteGrense = lineStrings[i + 1]

        if (nesteGrense.startPoint != gjeldendeGrense.endPoint) {
            throw IkkeSammenhengendePolygonException(gjeldendeGrense.endPoint, nesteGrense.startPoint, TopologyValidationError(TopologyValidationError.DISCONNECTED_INTERIOR))
        }
    }
}

fun createGeometryFactory(srid: Int = 25833): GeometryFactory {
    return GeometryFactory(PrecisionModel(100.0), srid)
}

fun koordinaterToLineString(coordinates: List<List<Double>>, srid: Int = defaultSrid): LineString {
    return createGeometryFactory(srid).createLineString(
        coordinates
            .map { Coordinate(it[0], it[1]) }
            .toTypedArray())
}

/**
 * Denne funkksjonen tar inn en liste med LineStrings som kan representere en eller flere komplekse polygoner.
 *
 * Et komplekst polygon er et polygon som ikke kan bli brutt ned til mindre polygoner.
 * De kan derimot inneholde andre komplekse polygoner innenfor sitt plan.
 *
 * Returnerer en liste med komplekse polygoner i from av lister med LineStrings
 */
fun finnAlleKompleksePolygoner(grenser: List<LineString>): List<List<LineString>> {

    // Hvis vi på forhånd vet hvilke grenser som henger sammen kan vi med en gang vi finner et polygon legge alle grensene for dette polygoner i besøkt-listen uten å bekymre oss for om denne grensen er delt av to polygoner.
    // Rent teknisk dupliserer vi egentlig bare grensen for alle som bruker den.
    val adjacencyMap = finnSammengengendeGrenser(grenser)

    val polygons = mutableListOf<List<LineString>>()
    val observerteGrenser = mutableSetOf<LineString>()

    grenser.forEach {grense ->
        if (grense !in observerteGrenser) {
            val polygon = finnKomplekstPolygon(grense, adjacencyMap)
            polygons.add(polygon)
            observerteGrenser.addAll(polygon)
        }
    }
    return polygons
}

fun finnKomplekstPolygon(startGrense: LineString, adjacencyMap: Map<Coordinate, MutableList<LineString>>): List<LineString> {
    val polygon = mutableListOf(startGrense)
    var currentGrense = startGrense
    while (true) {
        val tilkobledeGrenser = adjacencyMap[currentGrense.endPoint.coordinate]
        val nesteGrense = tilkobledeGrenser?.firstOrNull { it != currentGrense }
        if (nesteGrense == null ) {
            break
        }
        // sjekker om vi har kommet oss tilbake til startpunket
        else if (nesteGrense.endPoint.coordinate == startGrense.startPoint.coordinate)    {
                polygon.add(nesteGrense)
                break
            }
        else {
            polygon.add(nesteGrense)
            currentGrense = nesteGrense
        }
    }
    return polygon
}

fun finnSammengengendeGrenser(grenser: List<LineString>): Map<Coordinate, MutableList<LineString>> {
    val adjacencyMap = mutableMapOf<Coordinate, MutableList<LineString>>()
    grenser.forEach {grense ->
        val start = grense.startPoint.coordinate
        val end = grense.endPoint.coordinate
        adjacencyMap.computeIfAbsent(start) { mutableListOf() }.add(grense)
        adjacencyMap.computeIfAbsent(end) { mutableListOf() }.add(grense)
    }
    return adjacencyMap
}
