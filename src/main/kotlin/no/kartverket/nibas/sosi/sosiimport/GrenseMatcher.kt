package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.config.overlappedWindow
import no.kartverket.nibas.sosi.SosiImportException
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.sosi.model.SosiReferanse
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.linearref.LengthIndexedLine
import org.locationtech.jts.linearref.LengthLocationMap
import org.locationtech.jts.operation.distance.DistanceOp
import java.lang.Integer.min

object GrenseMatcher {
    fun grenserSomDekkerNyGrense(skalSjekkesMot: List<SosiGeometri>, grense: SosiGeometri): List<SosiGeometri> {
        val nyGrense = SosiGeometryFunctions.sosiKoordinaterToLineString(grense.getAlleSosiNordOest(), grense.koordsys)
        val initialWindowSize = 1
        val maxWindowSize = min(40, skalSjekkesMot.size)
        var currentWindowSize = initialWindowSize

        while (currentWindowSize <= maxWindowSize) {
            val windows = skalSjekkesMot.overlappedWindow(currentWindowSize)
            val grenserSomDekker: List<SosiGeometri>? = windows.find { grenserIVindu ->
                val grensesegmenter = grenserIVindu.fold(emptyList<LineString>()) { list, element ->
                    list.plus(SosiGeometryFunctions.sosiKoordinaterToLineString(element.getAlleSosiNordOest(), element.koordsys))
                }
                val segment = SosiGeometryFunctions.getGeometryFactory().createMultiLineString(grensesegmenter.toTypedArray())

                SosiGeometryFunctions.dekker(segment, nyGrense)
            }

            currentWindowSize += 1
            if (grenserSomDekker != null) {
                return grenserSomDekker
            }
        }
        return emptyList()
    }

    fun finnGrenseSegmentMedKoordinat(koordinat: Coordinate, grenser: List<SosiGeometri>): SosiGeometri? {
        return grenser.find { SosiGeometryFunctions.dekker(it.asLineString(), SosiGeometryFunctions.getGeometryFactory().createPoint(koordinat)) }
    }

    fun splittGrenser(ref: SosiReferanse, grense: SosiGeometri, grenserSomDekker: List<SosiGeometri>): List<SplitTilImport> {
        val startCoord = grense.startKoordinat(grense.erRetningMedKlokken)
        val sluttCoord = grense.sluttKoordinat(grense.erRetningMedKlokken)

        val splittetGrense = when (grenserSomDekker.size == 1) {
            true -> listOf(splittGrense(grenserSomDekker.first(), startCoord, sluttCoord)).map { SplitTilImport(it, ref, grense) }
            false -> {
                val grenseMedStartKoordinat = finnGrenseSegmentMedKoordinat(startCoord, grenserSomDekker) ?: grenserSomDekker.first()
                val grenseMedSluttKoordinat = finnGrenseSegmentMedKoordinat(sluttCoord, grenserSomDekker) ?: grenserSomDekker.last()
                val grenserSomSkalVareMedISinHelhet = grenserSomDekker
                    .minus(grenseMedStartKoordinat)
                    .minus(grenseMedSluttKoordinat)
                    .map { SplitRef(it.asLineString(), it) }

                val startSplittet = splittGrense(grenseMedStartKoordinat, startCoord = startCoord)
                val sluttSplittet = splittGrense(grenseMedSluttKoordinat, startCoord = null, endCoord = sluttCoord)

                listOf(startSplittet).plus(grenserSomSkalVareMedISinHelhet).plus(sluttSplittet).map { SplitTilImport(it, ref, grense) }
            }
        }
        return splittetGrense
    }

    fun splittGrense(grenseSomSkalSplittes: SosiGeometri, startCoord: Coordinate?, endCoord: Coordinate? = null): SplitRef {
        return when (maSplittes(grenseSomSkalSplittes, startCoord, endCoord)) {
            true -> {
                val toSplit = grenseSomSkalSplittes.asLineString()

                val start = startCoord ?: grenseSomSkalSplittes.startKoordinat(grenseSomSkalSplittes.erRetningMedKlokken)
                val end = endCoord ?: grenseSomSkalSplittes.sluttKoordinat(grenseSomSkalSplittes.erRetningMedKlokken)

                val nearestActualStartOnLine = DistanceOp(toSplit, SosiGeometryFunctions.getGeometryFactory().createPoint(start)).nearestPoints().first()
                val nearestActualEndOnLine = DistanceOp(toSplit, SosiGeometryFunctions.getGeometryFactory().createPoint(end)).nearestPoints().first()

                val indexedLineToSplit = LengthIndexedLine(toSplit)
                val splitPointStartIndex = indexedLineToSplit.indexOf(nearestActualStartOnLine)
                val splitPointEndIndex = indexedLineToSplit.indexOf(nearestActualEndOnLine)
                val startLoc = LengthLocationMap.getLocation(toSplit, splitPointStartIndex)
                val endLoc = LengthLocationMap.getLocation(toSplit, splitPointEndIndex)

                // Hvis end er før start, reverseres output av extractLine under.
                // Derfor snur vi koordinatene for å beholde korrekt retning
                val (splitStart, splitEnd) = when (endLoc < startLoc) {
                    true -> Pair(splitPointEndIndex, splitPointStartIndex)
                    false -> Pair(splitPointStartIndex, splitPointEndIndex)
                }

                val theSplitLine = indexedLineToSplit.extractLine(splitStart, splitEnd) as LineString
                return SplitRef(theSplitLine, grenseSomSkalSplittes)
            }

            false -> SplitRef(grenseSomSkalSplittes.asLineString(), grenseSomSkalSplittes)
        }
    }

    fun maSplittes(grense: SosiGeometri, start: Coordinate?, slutt: Coordinate? = null): Boolean {
        val koordinater = when (grense.erRetningMedKlokken) {
            true -> grense.koordinater()
            false -> grense.koordinater().reversed()
        }
        val koordinaterMatcher = when {
            start != null && slutt != null -> koordinater.first().equals2D(start) && koordinater.last().equals2D(slutt)
            start != null -> koordinater.first().equals2D(start)
            slutt != null -> koordinater.last().equals2D(slutt)
            else -> throw GrenseMatchingException(grense.asLineString(), "Trenger minimum ett koordinat for å kunne splitte grenser")
        }

        return !koordinaterMatcher
    }

    fun skalMatches(grense: SosiGeometri) = when (grense.getObjType()) {
        SosiKoder.KOMMUNEGRENSE -> true
        SosiKoder.FYLKESGRENSE -> true
        SosiKoder.TERRITORIALGRENSE -> true
        SosiKoder.RIKSGRENSE -> true
        SosiKoder.AVTALT_AVGRENSNINGSLINJE -> true
        SosiKoder.LOV_VIRKEOMRADEGRENSE -> true
        SosiKoder.GRUNNLINJE -> true
        else -> false
    }

    fun match(ref: SosiReferanse, grense: SosiGeometri, kommuneFlater: List<SosiFlate>): List<SplitTilImport> {
        var grenserSomDekker = emptyList<SosiGeometri>()
        kommuneFlater.forEach kommune@{ kommuneFlate ->
            val dekketAvSkall = grenserSomDekkerNyGrense(kommuneFlate.getSkallGeometri(), grense)
            if (dekketAvSkall.isNotEmpty()) {
                grenserSomDekker = dekketAvSkall
                return@kommune
            }

            kommuneFlate.getHullGeometri().forEach { hullGeometri ->
                val dekketAvHull = grenserSomDekkerNyGrense(hullGeometri, grense)
                if (dekketAvHull.isNotEmpty()) {
                    grenserSomDekker = dekketAvHull
                    return@kommune

                }
            }
        }

        if (grenserSomDekker.isEmpty()) {
            throw GrenseMatchingException(grense.asLineString(), "Klarte ikke å matche grense.")
        }

        return splittGrenser(ref, grense, grenserSomDekker)
    }
}

data class SplitRef(val splittetGrense: LineString, val basertPaGrense: SosiGeometri)
data class SplitTilImport(val splitRef: SplitRef, val originalRef: SosiReferanse, val originalGrense: SosiGeometri) {
    fun lokalId(): String {
        val lokalIdSplitTarget = splitRef.basertPaGrense.getIdent()?.getLokalId()!!
        val lokalIdOrigGrense = originalGrense.getIdent()?.getLokalId()!!
        return when (lokalIdSplitTarget == lokalIdOrigGrense) {
            true -> lokalIdSplitTarget
            false -> "${lokalIdSplitTarget}-${lokalIdOrigGrense}"
        }
    }
}

data class GrenseMatchingException(val geometri: Geometry, override val message: String, override val cause: Throwable? = null): RuntimeException(message, cause)
