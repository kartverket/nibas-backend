package no.kartverket.nibas.service.utils

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

object StemmekretsHelper {
    val geometryFactory = GeometryFactory()

    fun getGrensterForVestligStemmekrets() =
        listOf(grenseVest, grenseNordVest, grenseDelt, grenseSorVest)

    fun getGrensterForOestligStemmekrets() =
        listOf(grenseNordOest, grenseOest, grenseSorOest, grenseDelt)

    fun getHullVestligStemmekrets(): List<Grense> {
        val geometriForHull = listOf(
            geometryFactory.createLineString(
                arrayOf(
                    Coordinate(2.0, 2.0),
                    Coordinate(2.0, 5.0),
                    Coordinate(2.0, 7.0)
                )
            ),
            geometryFactory.createLineString(
                arrayOf(
                    Coordinate(2.0, 7.0),
                    Coordinate(5.0, 7.0),
                    Coordinate(7.0, 7.0)
                )
            ),
            geometryFactory.createLineString(
                arrayOf(
                    Coordinate(7.0, 7.0),
                    Coordinate(7.0, 5.0),
                    Coordinate(7.0, 2.0)
                )
            ),
            geometryFactory.createLineString(
                arrayOf(
                    Coordinate(7.0, 2.0),
                    Coordinate(5.0, 2.0),
                    Coordinate(2.0, 2.0)
                )
            ),
        )

        return geometriForHull.mapIndexed { index, geometri ->
            Grense(id = "hullGrense$index", grensetype = Grensetype.Stemmekretsgrense)
                .apply { grense = geometri }
        }
    }

    val grenseVest = Grense(id = "grenseVest", grensetype = Grensetype.Stemmekretsgrense)
        .apply {
            grense = geometryFactory.createLineString(
                arrayOf(
                    Coordinate(0.0, 0.0),
                    Coordinate(0.0, 5.0),
                    Coordinate(0.0, 10.0)
                )
            )
        }

    val grenseNordVest = Grense(id = "grenseNordVest", grensetype = Grensetype.Stemmekretsgrense)
        .apply {
            grense = geometryFactory.createLineString(
                arrayOf(
                    Coordinate(0.0, 10.0),
                    Coordinate(5.0, 10.0),
                    Coordinate(10.0, 10.0)
                )
            )
        }

    val grenseNordOest = Grense(id = "grenseNordØst", grensetype = Grensetype.Stemmekretsgrense)
        .apply {
            grense = geometryFactory.createLineString(
                arrayOf(
                    Coordinate(10.0, 10.0),
                    Coordinate(15.0, 10.0),
                    Coordinate(20.0, 10.0)
                )
            )
        }

    val grenseOest = Grense(id = "grenseØst", grensetype = Grensetype.Stemmekretsgrense)
        .apply {
            grense = geometryFactory.createLineString(
                arrayOf(
                    Coordinate(20.0, 10.0),
                    Coordinate(20.0, 5.0),
                    Coordinate(20.0, 0.0)
                )
            )
        }

    val grenseSorOest = Grense(id = "grenseSørØst", grensetype = Grensetype.Stemmekretsgrense)
        .apply {
            grense = geometryFactory.createLineString(
                arrayOf(
                    Coordinate(20.0, 0.0),
                    Coordinate(15.0, 0.0),
                    Coordinate(10.0, 0.0)
                )
            )
        }

    val grenseSorVest = Grense(id = "grenseSørVest", grensetype = Grensetype.Stemmekretsgrense)
        .apply {
            grense = geometryFactory.createLineString(
                arrayOf(
                    Coordinate(10.0, 0.0),
                    Coordinate(5.0, 0.0),
                    Coordinate(0.0, 0.0)
                )
            )
        }

    val grenseDelt = Grense(id = "grenseDelt", grensetype = Grensetype.Stemmekretsgrense)
        .apply {
            grense = geometryFactory.createLineString(
                arrayOf(
                    Coordinate(10.0, 10.0),
                    Coordinate(10.0, 5.0),
                    Coordinate(10.0, 0.0)
                )
            )
        }
}
