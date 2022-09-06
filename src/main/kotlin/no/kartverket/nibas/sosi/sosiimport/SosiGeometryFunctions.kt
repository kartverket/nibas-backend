package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.getGrenseJustertForRetning
import no.kartverket.nibas.sosi.model.SosiKoordinat
import no.kartverket.nibas.sosi.model.SosiNordOest
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.operation.valid.IsValidOp
import org.locationtech.jts.operation.valid.TopologyValidationError

class SosiGeometryFunctions {
    companion object {

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

        fun dekker(geometriSomSkalDekke: Geometry, geometriSomSkalDekkes: Geometry): Boolean {
            return geometriSomSkalDekke.buffer(5.0).covers(geometriSomSkalDekkes)
        }

        fun beregnFlate(grenseKoblinger: Set<GrenseKobling>): MultiPolygon {
            val flater: List<Polygon> = grenseKoblinger
                .groupBy { it.grenseKoblingAttributter.flateIndeks }
                .map { entry ->
                    val koblingerForFlate = entry.value

                    val skallKoblinger = koblingerForFlate.filterTo(HashSet()) { it.grenseKoblingAttributter.hullIndeks == null }
                    val hullKoblinger: Map<Int, List<GrenseKobling>> = (koblingerForFlate - skallKoblinger).groupBy { it.grenseKoblingAttributter.hullIndeks!! }

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
        private fun lagFlate(skallLineStrings: List<LineString>, hullLineStrings: List<List<LineString>>, koordsys: String? = null): Polygon {
            val geometryFactory = getGeometryFactory(koordsys)
            val skall: LinearRing = geometryFactory.createLinearRingFromLineStrings(skallLineStrings)
            val hull: Array<LinearRing> = hullLineStrings
                .map { liste -> geometryFactory.createLinearRingFromLineStrings(liste) }
                .toTypedArray()

            try {
                return geometryFactory.createPolygon(skall, hull)
            } catch (ex: Exception) { // Skall og hull bør ikke være ugyldige her, men for sikkerhets skyld håndteres også her
                throw GeometriFailedToBeCreatedException(coordinateToPoint(skall.coordinate), ex)
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
            try {
                return createLinearRing(coordinates)
            } catch (exception: Exception) {
                throw GeometriFailedToBeCreatedException(coordinateToPoint(coordinates[0]), exception)
            }
        }

    }
}

class GeometriFailedToBeCreatedException(val point: Point, rootEx: Exception? = null)
    : RuntimeException("Geometri-objekt kunne ikke opprettes. Feil-lokasjon: ${point.coordinate}", rootEx)

class UgyldigGeometriException(val geometry: Geometry,
                               val topologyValidationError: TopologyValidationError)
    : RuntimeException("Geometri-objekt er ikke gyldig: $geometry. Validation-feil: ${topologyValidationError.message}. Feil-lokasjon: ${topologyValidationError.coordinate}")
