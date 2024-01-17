package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.getGrenseJustertForRetning
import no.kartverket.nibas.repository.Grenserad
import no.kartverket.nibas.sosi.model.SosiKoordinat
import no.kartverket.nibas.sosi.model.SosiNordOest
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.operation.buffer.BufferParameters
import org.locationtech.jts.operation.valid.IsValidOp
import org.locationtech.jts.operation.valid.TopologyValidationError

object SosiGeometryFunctions {
    const val defaultSrid = 25833
    private val koordsysToSrid: Map<String, Int> = mapOf(
        Pair("23", defaultSrid)
    )

    private val sridToGeometryFactory = mutableMapOf<Int, GeometryFactory>()

    fun getGeometryFactory(koordsys: String? = null): GeometryFactory {
        val srid = when (koordsys) {
            null -> defaultSrid
            else -> koordsysToSrid.getOrDefault(koordsys, defaultSrid)
        }
        return createGeometryFactoryFromSrid(srid)
    }

    private fun createGeometryFactoryFromSrid(srid: Int): GeometryFactory {
        val geometryFactory = sridToGeometryFactory.getOrDefault(srid, GeometryFactory(PrecisionModel(100.0), srid))
        if (!sridToGeometryFactory.containsKey(srid)) {
            sridToGeometryFactory[srid] = geometryFactory
        }
        return geometryFactory
    }

    fun sosiKoordinaterToCoordinates(sosiNordOest: List<SosiNordOest>): List<Coordinate> {
        return sosiNordOest.flatMap { outer ->
            outer.getKoordinater().map {
                sosiKoordinatToCoordinate(it, outer.enhet)
            }
        }
    }

    fun sosiKoordinaterToLineString(sosiNordOest: List<SosiNordOest>, koordsys: String?): LineString {
        val koordinater = sosiKoordinaterToCoordinates(sosiNordOest)
        return getGeometryFactory(koordsys).createLineString(koordinater.toTypedArray())
    }

    fun koordinaterToLineString(coordinates: List<List<Double>>, srid: String = defaultSrid.toString()): LineString {
        return createGeometryFactoryFromSrid(srid.toInt()).createLineString(
            coordinates
                .map { Coordinate(it[0], it[1]) }
                .toTypedArray())
    }

    fun sosiKoordinatToPoint(sosiNordOest: List<SosiNordOest>, koordsys: String?): Point? {
        return sosiNordOest.firstOrNull()?.let { first ->
            first.getKoordinater().firstOrNull()?.let { coord ->
                getGeometryFactory(koordsys).createPoint(sosiKoordinatToCoordinate(coord, first.enhet))
            }
        }
    }

    fun dekker(geometriSomSkalDekke: Geometry, geometriSomSkalDekkes: Geometry, bufferStorrelse: Double): Boolean {
        //Når en MultiLineString bufres vil den kunne inneholde hull dersom vi bruker CAP_FLAT. Man må derfor gjøre det om til en LineString først.
        val geometryAsLineString = getGeometryFactory().createLineString(geometriSomSkalDekke.coordinates)

        //Forsøkte først å bufre med 5 meter og ingen cap style, dvs CAP_ROUND. Da kan vi få problemet med at grenser fra adm enheter "spiser" opp egne grenser som ikke blir med i matchingen.
        //Eks: Grense 13474 (Vegård grunnkrets) fra grunnkrets-fila: Denne matches opp i sin helhet av grense 447470. Da mister vi den lille grensa 445490.
        //Deretter prøvde vi å bufre 5 meter og CAP_FLAT. Problemet da er at dersom grensene fra både grunnkrets- og adm enheter-fila ender i samme punkt så må en ekstra grense tas med, dvs
        //grensene 447470, 445490 og 447177
        //Det virker ikke som covers godtar slutt i samme punkt.
        //Dersom vi bufrer to ganger for å få en liten avrundt buffer på enden får vi de to grensene vi ønsker (447470 og 445490)
        return geometryAsLineString.buffer(bufferStorrelse, 8, BufferParameters.CAP_FLAT).buffer(0.2).covers(geometriSomSkalDekkes)
    }

    /**
     * Sorterer innkommende grenser fra potensielt flere flater, danner flater og pakker den/dem inn i et multipolygon som returneres
     * Ikke i bruk pr nå, men kan brukes dersom man skal flatedanne nye kretser som har allerede definerte hull.
     * Vurder om de også bør/kan benytte dannFlaterMedHullBasertPaaGeometri i stedet.
     */
    fun dannFlaterFraGrenserader(grenserMedKorrektRetning: List<Grenserad>): MultiPolygon {
        val flater: List<Polygon> = grenserMedKorrektRetning
            .groupBy { it.flateIndeks }
            .map { entry ->
                val grenseraderForFlate = entry.value
                val skallGrenser: List<Grenserad> = grenseraderForFlate.filter { it.hullIndeks == null }
                val hullGrenser: Map<Int, List<Grenserad>> = (grenseraderForFlate - skallGrenser).groupBy { it.hullIndeks!! }

                val skall: FlatedanningResult = finnGrenserSomDannerEnFlate(skallGrenser.map { GrenseMedLokalid(it.grense, it.lokalid) })
                val hull: List<FlatedanningResult> = hullGrenser.map { hullGrenserForHull: Map.Entry<Int, List<Grenserad>> ->
                    finnGrenserSomDannerEnFlate(hullGrenserForHull.value.map { GrenseMedLokalid(it.grense, it.lokalid) })
                }

                val ukompletteHull = hull.filter { it is FlatedanningResult.UkomplettFlate }

                if (skall is FlatedanningResult.KomplettFlate && ukompletteHull.isEmpty()) {
                    val hullFlateSegmenter: List<List<LineString>> = hull.map {
                        val hullKomplett = it as FlatedanningResult.KomplettFlate // Vet at den er komplett ettersom det ikke finnes ukomplette hull
                        hullKomplett.sorterteGrenser.map { grenseMedLokalid -> grenseMedLokalid.grense }
                    }
                    val flate = lagFlate(skall.sorterteGrenser.map { grenseMedLokalid -> grenseMedLokalid.grense }, hullFlateSegmenter)
                    validateGeometry(flate)
                    flate
                } else {
                    if (skall is FlatedanningResult.UkomplettFlate) {
                        throw RuntimeException("Klarte ikke å danne flate med grenser: ${skall.forsoekteGrenser}")
                    } else {
                        throw RuntimeException("Klarte ikke å danne flate grunnet feil med hull, antall hull som feiler: ${ukompletteHull.size}")
                    }
                }
            }

        val multiPolygon = getGeometryFactory().createMultiPolygon(flater.toTypedArray())
        validateGeometry(multiPolygon)
        return multiPolygon
    }


    /**
     * Danner et multipolygon basert på en eller flere flater med potensielt et eller flere hull
     */
    fun dannFlaterMedHullBasertPaaGeometri(grenserMedKorrektRetning: List<GrenseMedLokalid>, enhetsLokalid: String? = null): MultiPolygon {
        val geometryFactory = GeometryFactory()
        val kompletteFlater = finnFlaterForGrenser(grenserMedKorrektRetning, enhetsLokalid)
            .map { FlatedanningResult.KomplettFlate(it) }

        val multiPolygon = dannMultipolygon(kompletteFlater, geometryFactory)
        validateGeometry(multiPolygon)
        return multiPolygon
    }

    fun finnFlaterForGrenser(grenserMedKorrektRetning: List<GrenseMedLokalid>, enhetsLokalid: String? = null): List<List<GrenseMedLokalid>> {
        var gjenstaaendeGrenser = grenserMedKorrektRetning

        val kompletteFlater: MutableList<FlatedanningResult.KomplettFlate> = mutableListOf()
        val ukompletteFlater: MutableList<FlatedanningResult.UkomplettFlate> = mutableListOf()

        while (gjenstaaendeGrenser.isNotEmpty()) {
            when (val result = finnGrenserSomDannerEnFlate(gjenstaaendeGrenser, enhetsLokalid)) {
                is FlatedanningResult.KomplettFlate -> {
                    kompletteFlater.add(result)
                    gjenstaaendeGrenser = gjenstaaendeGrenser - result.sorterteGrenser
                }
                is FlatedanningResult.UkomplettFlate -> {
                    ukompletteFlater.add(result)
                    gjenstaaendeGrenser = gjenstaaendeGrenser - result.forsoekteGrenser
                }
            }
        }

        if (ukompletteFlater.isNotEmpty()) {
            throw RuntimeException("Klarte ikke å danne flate grunnet ukomplette flater, antall som feiler: ${ukompletteFlater.size}")
        }

        return kompletteFlater.map { it.sorterteGrenser }
    }

    private fun dannMultipolygon(
        kompletteFlater: List<FlatedanningResult.KomplettFlate>,
        geometryFactory: GeometryFactory
    ): MultiPolygon {
        val flater: MutableList<Polygon> = mutableListOf()
        kompletteFlater.forEach { komplettFlate ->
            val ytrePolygon = lagFlate(komplettFlate.sorterteGrenser.map { grenseMedLokalid -> grenseMedLokalid.grense }, emptyList())
            validateGeometry(ytrePolygon)
            val hullForPolygon: MutableList<List<LineString>> = mutableListOf()
            val resterendeKompletteFlater = kompletteFlater.minus(komplettFlate)

            var ytrePolygonErSkall = true
            resterendeKompletteFlater.forEach { resterendeKomplettFlate ->
                val indrePolygon = lagFlate(resterendeKomplettFlate.sorterteGrenser.map { grenseMedLokalid -> grenseMedLokalid.grense }, emptyList())
                validateGeometry(indrePolygon)
                if (ytrePolygon.contains(indrePolygon)) {
                    // Her har vi et ytre polygon (skall) med et indre hull. Hullet må da legges i hull-lista
                    hullForPolygon.add(resterendeKomplettFlate.sorterteGrenser.map { grenseMedLokalid -> grenseMedLokalid.grense })
                } else if (indrePolygon.contains(ytrePolygon)) {
                    // Her er det ytre polygonet et hull og skal derfor ikke legges til
                    ytrePolygonErSkall = false
                }
            }
            if (ytrePolygonErSkall) {
                val skallFlate = lagFlate(komplettFlate.sorterteGrenser.map { grenseMedLokalid -> grenseMedLokalid.grense }, hullForPolygon)
                validateGeometry(skallFlate)
                flater.add(skallFlate)
            }
        }
        return geometryFactory.createMultiPolygon(flater.toTypedArray())
    }

    private fun finnGrenserSomDannerEnFlate(grenser: List<GrenseMedLokalid>, enhetsLokalid: String? = null): FlatedanningResult {
        val forsteGrense = grenser.first()
        var gjeldendeGrense = forsteGrense

        val sorterteGrenserForFlate = mutableListOf<GrenseMedLokalid>()
        sorterteGrenserForFlate.add(gjeldendeGrense)

        do {
            var nesteGrense: GrenseMedLokalid = finnNesteGrense(gjeldendeGrense, grenser, enhetsLokalid)
                ?: return FlatedanningResult.UkomplettFlate(sorterteGrenserForFlate)

            if (nesteGrense.grense != forsteGrense.grense) {
                sorterteGrenserForFlate.add(nesteGrense)
                gjeldendeGrense = nesteGrense
            }
        } while (nesteGrense != forsteGrense)
        return FlatedanningResult.KomplettFlate(sorterteGrenserForFlate)
    }

    private fun finnNesteGrense(gjeldendeGrense: GrenseMedLokalid, grenserMedKorrektRetning: List<GrenseMedLokalid>, enhetsLokalid: String? = null): GrenseMedLokalid? {
        var nesteGrense = grenserMedKorrektRetning.find { it.grense.startPoint.equals(gjeldendeGrense.grense.endPoint) }

        //Ås kommune
        if (enhetsLokalid == "74273193-b926-40e1-959e-e02c3942dc81" && gjeldendeGrense.lokalid.value == "9d2727e4-af52-34c1-b1b0-f43feac49bdb") {
            nesteGrense = grenserMedKorrektRetning.find { it.lokalid.value ==  "4fed07a6-04aa-3d6d-95cb-fcaf0794f91d"}
        } else if (enhetsLokalid == "74273193-b926-40e1-959e-e02c3942dc81" && gjeldendeGrense.lokalid.value == "10c0bda9-fad5-3130-9c91-84c58ebef2a6") {
            nesteGrense = grenserMedKorrektRetning.find { it.lokalid.value ==  "ba3b4bb0-8c0f-33f9-b8e3-7712b21afc1e"}
        }
        //Frogn kommune
        else if (enhetsLokalid == "37f9dcca-d06d-41a0-b8e3-13fdfd94c854" && gjeldendeGrense.lokalid.value == "4fed07a6-04aa-3d6d-95cb-fcaf0794f91d") {
            nesteGrense = grenserMedKorrektRetning.find { it.lokalid.value ==  "9d2727e4-af52-34c1-b1b0-f43feac49bdb"}
        } else if (enhetsLokalid == "37f9dcca-d06d-41a0-b8e3-13fdfd94c854" && gjeldendeGrense.lokalid.value == "ba3b4bb0-8c0f-33f9-b8e3-7712b21afc1e") {
            nesteGrense = grenserMedKorrektRetning.find { it.lokalid.value ==  "10c0bda9-fad5-3130-9c91-84c58ebef2a6"}
        }
        //Nome
        else if ( enhetsLokalid == "123e97ca-43df-4b98-9244-1bf9c98c8076" && gjeldendeGrense.lokalid.value == "abd01914-d0e3-3895-8797-9fc2f07525fa") {
            nesteGrense = grenserMedKorrektRetning.find { it.lokalid.value ==  "f8207ddb-0462-3600-bf7b-a755a7d647a3"}
        } else if (enhetsLokalid == "123e97ca-43df-4b98-9244-1bf9c98c8076" && gjeldendeGrense.lokalid.value == "6c6d66f3-bd30-3fb3-9b49-c398fe8e37eb") {
            nesteGrense = grenserMedKorrektRetning.find { it.lokalid.value ==  "5f2ea378-3d8d-364d-9569-e646477dcbdf"}
        }
        //Skien
        else if (enhetsLokalid == "886dd687-f7a3-448d-99b5-783b741646cb" && gjeldendeGrense.lokalid.value == "f8207ddb-0462-3600-bf7b-a755a7d647a3") {
            nesteGrense = grenserMedKorrektRetning.find { it.lokalid.value ==  "abd01914-d0e3-3895-8797-9fc2f07525fa"}
        } else if (enhetsLokalid == "886dd687-f7a3-448d-99b5-783b741646cb" && gjeldendeGrense.lokalid.value == "5f2ea378-3d8d-364d-9569-e646477dcbdf") {
            nesteGrense = grenserMedKorrektRetning.find { it.lokalid.value ==  "6c6d66f3-bd30-3fb3-9b49-c398fe8e37eb"}
        }

        return nesteGrense
    }

    fun beregnFlate(grenseKoblinger: Set<GrenseKobling>): MultiPolygon {
        val flater: List<Polygon> = grenseKoblinger
            .groupBy { it.grenseKoblingAttributter.flateIndeks }
            .map { entry ->
                val koblingerForFlate = entry.value

                val skallKoblinger = koblingerForFlate.filterTo(HashSet()) { it.grenseKoblingAttributter.hullIndeks == null }
                val hullKoblinger: Map<Int, List<GrenseKobling>> = (koblingerForFlate - skallKoblinger)
                    .groupBy { it.grenseKoblingAttributter.hullIndeks!! }

                val skall: List<LineString> = skallKoblinger
                    .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                    .map { it.getGrenseJustertForRetning() }

                val hull: List<List<LineString>> = hullKoblinger.map { hullkobling ->
                    hullkobling.value
                        .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                        .map { it.getGrenseJustertForRetning() }
                }

                val flate: Polygon = lagFlate(skall, hull)
                validateGeometry(flate) // Validerer hvert polygon for seg
                flate
            }

        val multiPolygon = getGeometryFactory().createMultiPolygon(flater.toTypedArray())
        validateGeometry(multiPolygon) // Validerer også multipolygonet
        return multiPolygon
    }

    fun sosiKoordinatToCoordinate(sosiKoordinat: SosiKoordinat, enhet: Double): Coordinate {
        val precisionModel = getGeometryFactory().precisionModel
        val x = precisionModel.makePrecise(sosiKoordinat.oest.toDouble() * enhet)
        val y = precisionModel.makePrecise(sosiKoordinat.nord.toDouble() * enhet)
        return Coordinate(x, y)
    }

    fun coordinateToPoint(coordinate: Coordinate): Point {
        return getGeometryFactory().createPoint(coordinate)
    }

    /**
     * Lager en flate (Polygon) basert på skall og hull.
     *
     * @param skallLineStrings Linjene som utgjør ytre del av et polygon. Disse linjene må forme et lukket Polygon
     * @param hullLineStrings Dobbel liste som inneholder alle hull i et polygon. Hver av listene inneholde linjer som utgjør et lukket polygon.
     */
    fun lagFlate(skallLineStrings: List<LineString>, hullLineStrings: List<List<LineString>>, koordsys: String? = null): Polygon {
        val geometryFactory = getGeometryFactory(koordsys)
        validerSammenhengendeGrenser(skallLineStrings)
        val skall: LinearRing = geometryFactory.createLinearRingFromLineStrings(skallLineStrings)
        val hull: Array<LinearRing> = hullLineStrings
            .map { liste ->
                validerSammenhengendeGrenser(liste)
                geometryFactory.createLinearRingFromLineStrings(liste)
            }
            .toTypedArray()

        try {
            return geometryFactory.createPolygon(skall, hull)
        } catch (ex: Exception) { // Skall og hull bør ikke være ugyldige her, men for sikkerhets skyld håndteres også her
            throw GeometriFailedToBeCreatedException(coordinateToPoint(skall.coordinate), ex)
        }
    }

    private fun validerSammenhengendeGrenser(
        lineStrings: List<LineString>
    ) {
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

    /**
     * Kjører en isValid-sjekk på geometri, se javadoc på IsValidOp for detaljer.
     */
    private fun validateGeometry(geometry: Geometry) {
        val validationError = IsValidOp(geometry).validationError
        if (validationError != null) {
            throw UgyldigGeometriException(geometry, validationError)
        }
    }

    /**
     *@throws GeometriFailedToBeCreatedException dersom linearRing ikke kunne dannes
     */
    private fun GeometryFactory.createLinearRingFromLineStrings(lineStrings: List<LineString>): LinearRing {
        val coordinates: Array<Coordinate> = createMultiLineString(lineStrings.toTypedArray()).coordinates
        val coordinateList = CoordinateList(coordinates, false)
        try {
            return createLinearRing(coordinateList.toCoordinateArray())
        } catch (exception: Exception) {
            throw GeometriFailedToBeCreatedException(coordinateToPoint(coordinates[0]), exception)
        }
    }

}

class GeometriFailedToBeCreatedException(val point: Point, rootEx: Exception? = null)
    : RuntimeException("Geometri-objekt kunne ikke opprettes. Feil-lokasjon: ${point.coordinate}", rootEx)

@Suppress("MaxLineLength")
class UgyldigGeometriException(val geometry: Geometry,
                               val topologyValidationError: TopologyValidationError)
    : RuntimeException("Geometri-objekt er ikke gyldig: $geometry. Validation-feil: ${topologyValidationError.message}. Feil-lokasjon: ${topologyValidationError.coordinate}")

class IkkeSammenhengendePolygonException(sluttPunkt: Point,
                                         startPunkt: Point,
                                         topologyValidationError: TopologyValidationError)
    : RuntimeException("Polygon som skal dannes har ikke-sammenhengende punkter, sluttpunkt: ${sluttPunkt.coordinate}, startPunkt: ${startPunkt.coordinate}. Validation-feil: ${topologyValidationError.message}.")

class GrenseMedLokalid (val grense: LineString, val lokalid: Lokalid)

sealed class FlatedanningResult {
    class KomplettFlate(val sorterteGrenser: List<GrenseMedLokalid>): FlatedanningResult()
    class UkomplettFlate(val forsoekteGrenser: List<GrenseMedLokalid>): FlatedanningResult()
}
