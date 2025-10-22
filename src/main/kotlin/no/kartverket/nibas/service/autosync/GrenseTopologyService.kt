package no.kartverket.nibas.service.autosync

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class GrenseTopologyService(
    private val coordinateService: CoordinateService,
    private val grenseArchiveService: GrenseArchiveService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val geometryFactory = GeometryFactory()

    fun findHjelpepunkt(
        toArchive: List<GrenseArchiveService.GrenseToArchive>,
        gapStart: Point,
        gapEnd: Point
    ): List<Point> {
        val allEndpoints = toArchive.flatMap { listOf(it.coords.first(), it.coords.last()) }
        val endpointCounts = mutableMapOf<Point, Int>()

        for (endpoint in allEndpoints) {
            val existingKey = endpointCounts.keys.find { coordinateService.pointsMatch(it, endpoint) }
            if (existingKey != null) {
                endpointCounts[existingKey] = endpointCounts[existingKey]!! + 1
            } else {
                endpointCounts[endpoint] = 1
            }
        }

        val hjelpepunkt = mutableListOf<Point>()
        for ((endpoint, count) in endpointCounts) {
            val isGapEndpoint = coordinateService.pointsMatch(endpoint, gapStart) ||
                                coordinateService.pointsMatch(endpoint, gapEnd)

            // Skip hjelpepunkt very close to gap endpoints - these would project onto gap endpoint, breaking topology
            val tooCloseToGap = endpoint.distanceTo(gapStart) < SpatialConstants.KNUTEPUNKT_MARGIN ||
                               endpoint.distanceTo(gapEnd) < SpatialConstants.KNUTEPUNKT_MARGIN

            if (count == 2 && !isGapEndpoint && !tooCloseToGap) {
                hjelpepunkt.add(endpoint)
            } else if (count == 2 && tooCloseToGap && !isGapEndpoint) {
                log.info("  Skipping hjelpepunkt [{}, {}] - too close to gap endpoint ({}m)",
                    endpoint.x, endpoint.y,
                    String.format(java.util.Locale.US, "%.2f",
                        kotlin.math.min(endpoint.distanceTo(gapStart), endpoint.distanceTo(gapEnd))))
            }
        }

        return hjelpepunkt
    }

    /**
     * First tries to use existing M22 vertex if within margin, falls back to projection.
     */
    fun projectPointOntoPath(
        point: Point,
        path: List<Point>
    ): GrenseArchiveService.HjelpepunktInfo {
        for (i in 1 until path.size - 1) {
            val vertex = path[i]
            val distance = vertex.distanceTo(point)

            if (distance < SpatialConstants.KNUTEPUNKT_MARGIN) {
                log.info("  Found M22 vertex at index {} within {}m of hjelpepunkt [{}, {}] - using as knutepunkt",
                    i, String.format(Locale.US, "%.2f", distance), point.x, point.y)
                return GrenseArchiveService.HjelpepunktInfo(
                    point,
                    coordinateService.roundPoint(vertex),
                    i
                )
            }
        }

        log.info("  No M22 vertex within {}m of hjelpepunkt [{}, {}], projecting onto path",
            SpatialConstants.KNUTEPUNKT_MARGIN, point.x, point.y)

        var minDistance = Double.MAX_VALUE
        var bestIndex = 0
        var bestProjected = path[0]

        val jtsPoint = point.toJtsPoint(geometryFactory)

        for (i in 0 until path.size - 1) {
            val segmentStart = path[i]
            val segmentEnd = path[i + 1]

            val coords = arrayOf(
                Coordinate(segmentStart.x, segmentStart.y),
                Coordinate(segmentEnd.x, segmentEnd.y)
            )
            val segment = geometryFactory.createLineString(coords)

            val closestPoint = org.locationtech.jts.operation.distance.DistanceOp
                .nearestPoints(segment, jtsPoint)[0]
            val distance = closestPoint.distance(jtsPoint.coordinate)

            if (distance < minDistance) {
                minDistance = distance
                bestProjected = coordinateService.roundPoint(Point(closestPoint.x, closestPoint.y))
                bestIndex = i + 1
            }
        }

        log.info("  Projected onto path at index {}, distance: {}m",
            bestIndex, String.format(Locale.US, "%.2f", minDistance))

        return GrenseArchiveService.HjelpepunktInfo(point, bestProjected, bestIndex)
    }

    fun deduplicateHjelpepunkt(
        hjelpepunktInfos: List<GrenseArchiveService.HjelpepunktInfo>
    ): Pair<List<GrenseArchiveService.HjelpepunktInfo>, List<List<Int>>> {
        if (hjelpepunktInfos.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }
        if (hjelpepunktInfos.size == 1) {
            return Pair(hjelpepunktInfos, listOf(listOf(0)))
        }

        val groups = mutableListOf<MutableList<Pair<GrenseArchiveService.HjelpepunktInfo, Int>>>()

        for ((index, info) in hjelpepunktInfos.withIndex()) {
            val existingGroup = groups.find { group ->
                val representative = group.first().first
                coordinateService.pointsMatch(representative.projectedPoint, info.projectedPoint)
            }

            if (existingGroup != null) {
                existingGroup.add(Pair(info, index))
            } else {
                groups.add(mutableListOf(Pair(info, index)))
            }
        }

        val deduplicated = mutableListOf<GrenseArchiveService.HjelpepunktInfo>()
        val mergeInfo = mutableListOf<List<Int>>()

        for (group in groups) {
            if (group.size > 1) {
                val oldPoints = group.map { it.first.oldPoint }
                log.warn("  MERGING {} hjelpepunkt at same position: old positions = {}",
                    group.size,
                    oldPoints.map { "[${it.x}, ${it.y}]" })
                log.info("    All snap to projected point: [{}, {}]",
                    group.first().first.projectedPoint.x,
                    group.first().first.projectedPoint.y)
            }


            val best = group.minBy { (info, _) ->
                info.oldPoint.distanceTo(info.projectedPoint)
            }

            deduplicated.add(best.first)
            mergeInfo.add(group.map { it.second })
        }

        if (deduplicated.size < hjelpepunktInfos.size) {
            log.info("  Deduplicated {} hjelpepunkt down to {} (merged duplicates at same position)",
                hjelpepunktInfos.size, deduplicated.size)
        }

        return Pair(deduplicated, mergeInfo)
    }

    fun splitPathAtHjelpepunkt(
        path: List<Point>,
        hjelpepunktInfos: List<GrenseArchiveService.HjelpepunktInfo>
    ): List<List<Point>> {
        if (hjelpepunktInfos.isEmpty()) {
            return listOf(path)
        }

        val sorted = hjelpepunktInfos.sortedBy { it.pathIndex }
        val segments = mutableListOf<List<Point>>()
        var startIndex = 0
        var previousHjelpepunkt: Point? = null

        for (info in sorted) {
            val segmentCoords = mutableListOf<Point>()

            if (previousHjelpepunkt != null) {
                segmentCoords.add(previousHjelpepunkt)
            }

            val coordsToAdd = if (previousHjelpepunkt != null) {
                path.subList(startIndex, info.pathIndex).drop(1)
            } else {
                path.subList(startIndex, info.pathIndex)
            }
            coordsToAdd.forEach { coord ->
                segmentCoords.add(coordinateService.roundPoint(coord))
            }

            segmentCoords.add(info.projectedPoint)
            segments.add(segmentCoords)

            previousHjelpepunkt = info.projectedPoint
            startIndex = info.pathIndex
        }


        val lastSegment = mutableListOf(sorted.last().projectedPoint)
        val skipFirst = 1
        val remainingCoords = path.subList(startIndex, path.size).drop(skipFirst)

        if (remainingCoords.isEmpty()) {
            log.warn("  Last hjelpepunkt at index {} leaves no coordinates - merging into previous segment",
                startIndex)

            if (segments.isNotEmpty()) {
                val previousSegment = segments.last().toMutableList()
                previousSegment.add(sorted.last().projectedPoint)
                segments[segments.size - 1] = previousSegment
                log.info("  Split path into {} segments at {} hjelpepunkt (merged last)",
                    segments.size, hjelpepunktInfos.size)
                return segments
            } else {
                log.warn("  Only one segment and hjelpepunkt at end - using full path")
                return listOf(path)
            }
        }

        remainingCoords.forEach { coord ->
            lastSegment.add(coordinateService.roundPoint(coord))
        }
        segments.add(lastSegment)

        log.info("  Split path into {} segments at {} hjelpepunkt",
            segments.size, hjelpepunktInfos.size)
        return segments
    }

    fun findGrenseSharingEndpoints(
        toArchive: List<GrenseArchiveService.GrenseToArchive>,
        kommune1: no.kartverket.nibas.domain.Kommune,
        kommune2: no.kartverket.nibas.domain.Kommune,
        gyldigFra: LocalDate
    ): List<Grense> {
        val archivedEndpoints = toArchive.flatMap {
            listOf(it.coords.first(), it.coords.last())
        }

        val allInternalGrenser = mutableSetOf<Grense>()
        collectInternalGrenser(kommune1, gyldigFra, allInternalGrenser)
        collectInternalGrenser(kommune2, gyldigFra, allInternalGrenser)

        val affectedGrenser = mutableListOf<Grense>()

        for (grense in allInternalGrenser) {
            val geom = grense.grense
            if (geom.coordinates.isEmpty()) continue

            val start = Point(geom.coordinates.first().x, geom.coordinates.first().y)
            val end = Point(geom.coordinates.last().x, geom.coordinates.last().y)

            for (archivedEndpoint in archivedEndpoints) {
                if (coordinateService.pointsMatch(start, archivedEndpoint) ||
                    coordinateService.pointsMatch(end, archivedEndpoint)) {
                    affectedGrenser.add(grense)
                    log.info("  Found affected grense: {} {}", grense.grensetype, grense.lokalid)
                    break
                }
            }
        }

        return affectedGrenser
    }

    fun buildEndpointMapping(
        hjelpepunktInfos: List<GrenseArchiveService.HjelpepunktInfo>
    ): Map<Point, Point> {
        return hjelpepunktInfos.associate { info ->
            info.oldPoint to info.projectedPoint
        }
    }

    private fun collectInternalGrenser(
        kommune: no.kartverket.nibas.domain.Kommune,
        gyldigFra: LocalDate,
        targetSet: MutableSet<Grense>
    ) {
        val grunnkretser = kommune.getGrunnkretser(gyldigFra)
        for (grunnkrets in grunnkretser) {
            val grensekoblinger = grunnkrets.getGrensekoblinger(gyldigFra)
            for (kobling in grensekoblinger) {
                if (kobling.grense.grensetype == Grensetype.Grunnkretsgrense ||
                    kobling.grense.grensetype == Grensetype.Delomraadegrense) {
                    targetSet.add(kobling.grense)
                }
            }
        }


        val stemmekretser = kommune.getStemmekretser(gyldigFra)
        for (stemmekrets in stemmekretser) {
            val grensekoblinger = stemmekrets.getGrensekoblinger(gyldigFra)
            for (kobling in grensekoblinger) {
                if (kobling.grense.grensetype == Grensetype.Stemmekretsgrense) {
                    targetSet.add(kobling.grense)
                }
            }
        }
    }
}
