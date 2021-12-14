package no.kartverket.nibas.service.sosiimport

import no.kartverket.nibas.sosi.model.SosiNordOest
import org.locationtech.jts.geom.*

class SosiGeometryFunctions {
    companion object {

        val defaultSrid = 25833
        val koordsysToSrid = mapOf(
            Pair("23", 25833)
        )

        val sridToGeometryFactory = mutableMapOf<Int, GeometryFactory>()

        fun getGeometryFactory(koordsys: String?): GeometryFactory {
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
                    Coordinate(it.north.toDouble(), it.east.toDouble())
                }
            }

            return getGeometryFactory(koordsys).createLineString(koordinater.toTypedArray())
        }

        fun sosiKoordinatToPoint(sosiNordOest: List<SosiNordOest>, koordsys: String?): Point? {
            return sosiNordOest.firstOrNull()?.let { first ->
                first.getKoordinater().firstOrNull()?.let { coord ->
                    getGeometryFactory(koordsys).createPoint(Coordinate(coord.north.toDouble(), coord.east.toDouble()))
                }
            }
        }

        fun lagFlate(
            skallLineStrings: List<LineString>,
            hullLineStrings: List<List<LineString>>,
            koordsys: String?
        ): Polygon {
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
    }
}