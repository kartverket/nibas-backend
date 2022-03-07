package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.sosi.model.SosiNordOest
import org.locationtech.jts.geom.*

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

            val geometryFactory = sridToGeometryFactory.getOrDefault(srid, GeometryFactory(PrecisionModel(), srid))
            if (!sridToGeometryFactory.containsKey(srid)) {
                sridToGeometryFactory[srid] = geometryFactory
            }

            return geometryFactory
        }

        fun sosiKoordinaterToLineString(sosiNordOest: List<SosiNordOest>, koordsys: String?): LineString {
            val koordinater: List<Coordinate> = sosiNordOest.flatMap { outer ->
                outer.getKoordinater().map {
                    Coordinate(it.oest.toDouble() * outer.enhet, it.nord.toDouble() * outer.enhet)
                }
            }

            return getGeometryFactory(koordsys).createLineString(koordinater.toTypedArray())
        }

        fun sosiKoordinatToPoint(sosiNordOest: List<SosiNordOest>, koordsys: String?): Point? {
            return sosiNordOest.firstOrNull()?.let { first ->
                first.getKoordinater().firstOrNull()?.let { coord ->
                    getGeometryFactory(koordsys).createPoint(
                        Coordinate(
                            coord.oest.toDouble() * first.enhet,
                            coord.nord.toDouble() * first.enhet
                        )
                    )
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
            val skall = GeometryFactory()
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
                    when (it.grenseKoblingAttributter.retningMedKlokken) {
                        true -> it.getGrense().grense!!
                        false -> it.getGrense().grense!!.reverse()
                    }
                }

                val hull: List<List<LineString>> = hullKoblinger.map { h ->
                    h.value.sortedBy { it.grenseKoblingAttributter.rekkefoelge }.map {
                        when (it.grenseKoblingAttributter.retningMedKlokken) {
                            true -> it.getGrense().grense!!
                            false -> it.getGrense().grense!!.reverse()
                        }
                    }
                }

                lagFlate(skall, hull)
            }

            return getGeometryFactory().createMultiPolygon(flater.toTypedArray())
        }
    }
}