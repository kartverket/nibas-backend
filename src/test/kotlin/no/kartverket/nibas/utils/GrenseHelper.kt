package no.kartverket.nibas.utils

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.LocalDate

object GrenseHelper {
    val geometryFactory = GeometryFactory()

    fun getGrenserForVestligKrets(gyldighetsdato: LocalDate = LocalDate.now(), grensetype : Grensetype, medToDelteGrenser: Boolean = false) : List<Grense> {
        val grenselist = mutableListOf<Grense>()

        grenselist.add(Grense(id = "grenseVest", lokalid = "grenseVestLokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldighetsdato)
                grense = geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 10.0),
                        Coordinate(0.0, 5.0),
                        Coordinate(0.0, 0.0)
                    )
                )
            }
        )

        grenselist.add(Grense(id = "grenseNordVest", lokalid = "grenseNordVestLokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldighetsdato)
                grense = geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 10.0),
                        Coordinate(5.0, 10.0),
                        Coordinate(10.0, 10.0)
                    )
                )
            }
        )

        grenselist.add(Grense(id = "grenseDelt", lokalid = "grenseDeltLokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldighetsdato)
                grense = geometryFactory.createLineString(
                    if (medToDelteGrenser) {
                        arrayOf(
                            Coordinate(10.0, 10.0),
                            Coordinate(10.0, 5.0)
                        )
                    } else {
                        arrayOf(
                            Coordinate(10.0, 10.0),
                            Coordinate(10.0, 5.0),
                            Coordinate(10.0, 0.0)
                        )
                    }
                )
            }
        )

        if (medToDelteGrenser) {
            grenselist.add(Grense(id = "grenseDelt2", lokalid = "grenseDelt2Lokalid", grensetype = grensetype)
                .apply {
                    setGyldigFra(gyldighetsdato)
                    grense = geometryFactory.createLineString(
                        arrayOf(
                            Coordinate(10.0, 0.0),
                            Coordinate(10.0, 5.0)
                        )
                    )
                }
            )
        }

        grenselist.add(Grense(id = "grenseSørVest", lokalid = "grenseSørVestLokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldighetsdato)
                grense = geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(10.0, 0.0),
                        Coordinate(5.0, 0.0),
                        Coordinate(0.0, 0.0)
                    )
                )
            }
        )

        return grenselist.toList()
    }

    fun getGrenserForOestligKrets(gyldighetsdato: LocalDate = LocalDate.now(), grensetype : Grensetype, medToDelteGrenser: Boolean = false) : List<Grense> {
        val grenselist = mutableListOf<Grense>()

        grenselist.add(Grense(id = "grenseNordØst", lokalid = "grenseNordØstLokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldighetsdato)
                grense = geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(10.0, 10.0),
                        Coordinate(15.0, 10.0),
                        Coordinate(20.0, 10.0)
                    )
                )
            }
        )

        grenselist.add(Grense(id = "grenseØst", lokalid = "grenseØstLokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldighetsdato)
                grense = geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(20.0, 10.0),
                        Coordinate(20.0, 5.0),
                        Coordinate(20.0, 0.0)
                    )
                )
            }
        )

        grenselist.add(Grense(id = "grenseSørØst", lokalid = "grenseSørØstLokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldighetsdato)
                grense = geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(20.0, 0.0),
                        Coordinate(15.0, 0.0),
                        Coordinate(10.0, 0.0)
                    )
                )
            }
        )

        if (medToDelteGrenser) {
            grenselist.add(Grense(id = "grenseDelt2", lokalid = "grenseDelt2Lokalid", grensetype = grensetype)
                .apply {
                    setGyldigFra(gyldighetsdato)
                    grense = geometryFactory.createLineString(
                        arrayOf(
                            Coordinate(10.0, 0.0),
                            Coordinate(10.0, 5.0)
                        )
                    )
                }
            )
        }

        grenselist.add(Grense(id = "grenseDelt", lokalid = "grenseDeltLokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldighetsdato)
                grense = geometryFactory.createLineString(
                    if (medToDelteGrenser) {
                        arrayOf(
                            Coordinate(10.0, 10.0),
                            Coordinate(10.0, 5.0)
                        )
                    } else {
                        arrayOf(
                            Coordinate(10.0, 10.0),
                            Coordinate(10.0, 5.0),
                            Coordinate(10.0, 0.0)
                        )
                    }
                )
            }
        )

        return grenselist.toList()
    }

    fun getHullVestligKrets(gyldighetsdato: LocalDate = LocalDate.now(), grensetype : Grensetype): List<Grense> {
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
            Grense(id = "hullGrense$index", lokalid = "hullGrense$index" + "lokalid", grensetype = grensetype)
                .apply {
                    setGyldigFra(gyldighetsdato)
                    grense = geometri
                }
        }
    }

    fun getRetningMedKlokken(grense: Grense, forrigeGrense: Grense?): Boolean {
        if (forrigeGrense == null) {
            return true
        }
        //Noen tester setter ikke linestring på grensa
        try {
            grense.grense.startPoint
            forrigeGrense.grense.startPoint
        } catch (e: UninitializedPropertyAccessException) {
            return true
        }

        if (grense.grense.startPoint == forrigeGrense.grense.startPoint || grense.grense.startPoint == forrigeGrense.grense.endPoint) {
            return true
        }
        if (grense.grense.endPoint == forrigeGrense.grense.startPoint || grense.grense.endPoint == forrigeGrense.grense.endPoint) {
            return false
        }

        throw RuntimeException("Grensen toucher hverken startpunkt eller endepunktet til forrige grense. Er rekkefølgen riktig?")
    }

}
