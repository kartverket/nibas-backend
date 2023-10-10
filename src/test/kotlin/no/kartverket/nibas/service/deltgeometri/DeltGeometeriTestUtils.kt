package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point

fun lagGrense(grensetype: Grensetype, vararg punkter: Point) =
    Grense(grensetype = grensetype)
        .apply { grense = GeometryFactory().createLineString(punkter.map { punkt -> punkt.coordinate }.toTypedArray()) }

fun lagKommuneGrense(vararg punkter: Point) =
    lagGrense(Grensetype.Kommunegrense, *punkter)

fun lagStemmekretsGrense(vararg punkter: Point) =
    lagGrense(Grensetype.Stemmekretsgrense, *punkter)

fun lagGrunnkretsGrense(vararg punkter: Point) =
    lagGrense(Grensetype.Grunnkretsgrense, *punkter)


fun lagStemmekretsGrenseKobling(stemmekrets: Stemmekrets, grense: Grense, rekkefoelge: Int, reversed: Boolean): StemmekretsGrenseKobling =
    StemmekretsGrenseKobling(
        stemmekrets,
        grense,
        GrenseKoblingAttributter(flateIndeks = 0, rekkefoelge = rekkefoelge, retningMedKlokken = reversed)
    )

fun lagGrunnkretsGrenseKobling(grunnkrets: Grunnkrets, grense: Grense, rekkefoelge: Int, reversed: Boolean): GrunnkretsGrenseKobling =
    GrunnkretsGrenseKobling(
        grunnkrets,
        grense,
        GrenseKoblingAttributter(flateIndeks = 0, rekkefoelge = rekkefoelge, retningMedKlokken = reversed)
    )

fun Coordinate.toPoint(): Point =
    GeometryFactory().createPoint(this)
