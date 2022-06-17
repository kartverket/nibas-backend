package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.sosi.model.SosiKoordinat
import no.kartverket.nibas.sosi.model.SosiNordOest
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel

class SosiGeometryFunctions {
    companion object {

        val defaultSrid = 25833
        val koordsysToSrid = mapOf(
            Pair("23", 25833)
        )

        val sridToGeometryFactory = mutableMapOf<Int, GeometryFactory>()

        fun getGeometryFactory(koordsys: String? = null): GeometryFactory {
            val srid = when (koordsys) {
                null -> defaultSrid
                else -> koordsysToSrid.getOrDefault(koordsys, defaultSrid)
            }

            val geometryFactory = sridToGeometryFactory.getOrDefault(srid, GeometryFactory(PrecisionModel(100.0), srid))
            if (!sridToGeometryFactory.containsKey(srid)) {
                sridToGeometryFactory[srid] = geometryFactory
            }

            return geometryFactory
        }

        fun sosiKoordinaterToLineString(sosiNordOest: List<SosiNordOest>, koordsys: String?): LineString {
            val koordinater: List<Coordinate> = sosiNordOest.flatMap { outer ->
                outer.getKoordinater().map {
                    sosiKoordinatToCoordinate(it, outer.enhet)
                }
            }

            return getGeometryFactory(koordsys).createLineString(koordinater.toTypedArray())
        }

        fun sosiKoordinatToPoint(sosiNordOest: List<SosiNordOest>, koordsys: String?): Point? {
            return sosiNordOest.firstOrNull()?.let { first ->
                first.getKoordinater().firstOrNull()?.let { coord ->
                    getGeometryFactory(koordsys).createPoint(sosiKoordinatToCoordinate(coord, first.enhet))
                }
            }
        }

        /**
         * Lager en flate (Polygon) basert på skall og hull.
         *
         * @param skallLineStrings Linjene som utgjør ytre del av et polygon. Disse linjene må forme et lukket Polygon
         * @param hullLineStrings Dobbel liste som inneholder alle hull i et polygon. Hver av listene inneholde linjer som utgjør et lukket polygon.
         */
        fun lagFlate(skallLineStrings: List<LineString>, hullLineStrings: List<List<LineString>>, koordsys: String? = null): Polygon {
            val skall = getGeometryFactory(koordsys)
                .createLinearRing(
                    getGeometryFactory(koordsys)
                        .createMultiLineString(skallLineStrings.toTypedArray()).coordinates
                )

            val hull = hullLineStrings.map { liste ->
                getGeometryFactory(koordsys)
                    .createLinearRing(
                        getGeometryFactory(koordsys)
                            .createMultiLineString(liste.toTypedArray()).coordinates
                    )
            }.toTypedArray()

            return getGeometryFactory(koordsys).createPolygon(skall, hull)
        }

        fun beregnFlate(grenseKoblinger: Set<GrenseKobling>): MultiPolygon {
            val flater: List<Polygon> = grenseKoblinger.groupBy { it.grenseKoblingAttributter.flateIndeks }.map { entry ->
                val koblingerForFlate = entry.value

                val skallKoblinger = koblingerForFlate.filterTo(HashSet()) { it.grenseKoblingAttributter.hullIndeks == null }
                val hullKoblinger: Map<Int, List<GrenseKobling>> = (koblingerForFlate - skallKoblinger).groupBy { it.grenseKoblingAttributter.hullIndeks!! }

                val skall: List<LineString> = skallKoblinger.sortedBy { it.grenseKoblingAttributter.rekkefoelge }.map {
                    val grense: Grense = it.grense
                    when (it.grenseKoblingAttributter.retningMedKlokken) {
                        true -> grense.grense
                        false -> grense.grense.reverse()
                    }
                }

                val hull: List<List<LineString>> = hullKoblinger.map { h ->
                    h.value.sortedBy { it.grenseKoblingAttributter.rekkefoelge }.map {
                        val grense: Grense = it.grense
                        when (it.grenseKoblingAttributter.retningMedKlokken) {
                            true -> grense.grense
                            false -> grense.grense.reverse()
                        }
                    }
                }

                lagFlate(skall, hull)
            }

            return getGeometryFactory().createMultiPolygon(flater.toTypedArray())
        }

        fun sosiKoordinatToCoordinate(sosiKoordinat: SosiKoordinat, enhet: Double): Coordinate {
            val x = getGeometryFactory().precisionModel.makePrecise(sosiKoordinat.oest.toDouble() * enhet)
            val y = getGeometryFactory().precisionModel.makePrecise(sosiKoordinat.nord.toDouble() * enhet)
            return Coordinate(x, y)
        }

        fun makePrecise(lineString: LineString): LineString {
            val gf = getGeometryFactory()
            val pm = gf.precisionModel
            val preciseCoordinates = lineString.coordinates.map { coord ->
                Coordinate(pm.makePrecise(coord.x), pm.makePrecise(coord.y))
            }.toTypedArray()

            return gf.createLineString(preciseCoordinates)
        }
    }
}
