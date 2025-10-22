package no.kartverket.nibas.service.autosync

import org.locationtech.jts.index.strtree.STRtree
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MatrikkelNetworkService(
    private val arbeidslisteClient: ArbeidslisteClient,
    private val coordinateService: CoordinateService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun fetchMatrikkelGrenslinjerForKommunePar(kommunePar: KommunePar): List<M22Feature> {
        val response = arbeidslisteClient.getMatrikkelGrenselinjer(
            kommunenummer = kommunePar.kommune1.kommunenummer
        )

        val k1 = kommunePar.kommune1.kommunenummer
        val k2 = kommunePar.kommune2.kommunenummer

        val filtered = response.features.filter { feature ->
            val m1 = feature.properties.kommunenr1
            val m2 = feature.properties.kommunenr2

            m1 != null && m2 != null && ((k1 == m1 && k2 == m2) || (k1 == m2 && k2 == m1))
        }

        log.info("  M22 data: {} total features, {} match kommune pair {}-{}",
            response.features.size, filtered.size, k1, k2)

        return filtered
    }

    /**
     * Uses greedy best-first search (works for M22 since there's usually one clear path).
     */
    fun walkMatrikkelNetwork(
        gapStart: Point,
        gapEnd: Point,
        matrikkelFeatures: List<M22Feature>
    ): MatrikkelPathResult? {
        log.info("  Starting M22 walk from [{}, {}] to [{}, {}]",
            gapStart.x, gapStart.y, gapEnd.x, gapEnd.y)
        log.info("  Distance: {}m", String.format(java.util.Locale.US, "%.2f", gapStart.distanceTo(gapEnd)))

        val spatialIndex = coordinateService.buildSpatialIndex(matrikkelFeatures)
        val allCoordinates = mutableListOf<Point>()
        val processedIds = mutableSetOf<String>()
        var currentEndpoint = gapStart
        var worstNoeyaktighet = 0
        var worstMaalemetodeId = 0
        var iterations = 0

        // Use larger tolerance for M22→NIBAS connection
        while (currentEndpoint.distanceTo(gapEnd) > SpatialConstants.NIBAS_M22_SNAP_TOLERANCE) {
            iterations++
            val isFirstSegment = iterations == 1
            val candidates = findCandidateSegments(
                currentEndpoint = currentEndpoint,
                goalPoint = gapEnd,
                spatialIndex = spatialIndex,
                processedIds = processedIds,
                isFirstSegment = isFirstSegment
            )

            if (candidates.isEmpty()) {
                log.warn("  Dead end after {} iterations at [{}, {}]!",
                    iterations, currentEndpoint.x, currentEndpoint.y)
                log.warn("  Distance to goal: {}m",
                    String.format(java.util.Locale.US, "%.2f", currentEndpoint.distanceTo(gapEnd)))

                val nearby = coordinateService.findFeaturesNearPoint(currentEndpoint, spatialIndex, searchRadius = 10.0)
                log.warn("  M22 features within 10m of current point: {}", nearby.size)

                return null
            }

            val best = candidates.minByOrNull { it.distanceToGoal }
                ?: throw IllegalStateException("minByOrNull returned null when candidates is not empty")

            processedIds.add(best.feature.id)

            val segmentNoeyaktighet = best.feature.properties.noyaktighet ?: 0
            val segmentMaalemetodeId = best.feature.properties.malemetodeId ?: 0
            if (segmentNoeyaktighet > worstNoeyaktighet) {
                worstNoeyaktighet = segmentNoeyaktighet
                worstMaalemetodeId = segmentMaalemetodeId
            }

            addCoordinatesToPath(allCoordinates, best.coords, best.startMatches)
            currentEndpoint = if (best.startMatches) best.coords.last() else best.coords.first()
        }

        log.info("  Path complete! {} coordinates, quality: {}cm",
            allCoordinates.size, worstNoeyaktighet)

        return MatrikkelPathResult(
            allCoordinates,
            GrenseArchiveService.WorstM22Posisjonskvalitet(worstMaalemetodeId, worstNoeyaktighet)
        )
    }

    fun snapEndpointsToNibas(
        coordinates: MutableList<Point>,
        nibasStart: Point,
        nibasEnd: Point
    ) {
        coordinates[0] = nibasStart
        coordinates[coordinates.size - 1] = nibasEnd
    }

    private fun findCandidateSegments(
        currentEndpoint: Point,
        goalPoint: Point,
        spatialIndex: STRtree,
        processedIds: Set<String>,
        isFirstSegment: Boolean = false
    ): List<Candidate> {
        // Use larger tolerance for NIBAS→M22 connection
        val tolerance = if (isFirstSegment) {
            SpatialConstants.NIBAS_M22_SNAP_TOLERANCE
        } else {
            SpatialConstants.COORDINATE_TOLERANCE
        }

        val searchRadius = if (isFirstSegment) {
            SpatialConstants.NIBAS_M22_SNAP_TOLERANCE
        } else {
            SpatialConstants.COORDINATE_TOLERANCE
        }

        val nearbyFeatures = coordinateService.findFeaturesNearPoint(currentEndpoint, spatialIndex, searchRadius)
        val candidates = mutableListOf<Candidate>()

        for (indexed in nearbyFeatures) {
            if (processedIds.contains(indexed.feature.id)) continue

            val coords = indexed.feature.geometry.coordinates.map { Point.fromList(it) }

            val startMatches = Point.fromJtsPoint(indexed.startPoint)
                .distanceTo(currentEndpoint) < tolerance
            val endMatches = Point.fromJtsPoint(indexed.endPoint)
                .distanceTo(currentEndpoint) < tolerance

            if (startMatches || endMatches) {
                val nextEndpoint = if (startMatches) {
                    Point.fromJtsPoint(indexed.endPoint)
                } else {
                    Point.fromJtsPoint(indexed.startPoint)
                }
                val distanceToGoal = nextEndpoint.distanceTo(goalPoint)
                candidates.add(Candidate(indexed.feature, coords, startMatches, distanceToGoal))
            }
        }
        return candidates
    }

    private fun addCoordinatesToPath(
        path: MutableList<Point>,
        coords: List<Point>,
        startMatches: Boolean
    ) {
        val coordsToAdd = if (startMatches) coords else coords.reversed()
        if (path.isEmpty()) {
            path.addAll(coordsToAdd)
        } else {
            path.addAll(coordsToAdd.drop(1))
        }
    }

    data class Candidate(
        val feature: M22Feature,
        val coords: List<Point>,
        val startMatches: Boolean,
        val distanceToGoal: Double
    )

    data class MatrikkelPathResult(
        val coordinates: MutableList<Point>,
        val worstQuality: GrenseArchiveService.WorstM22Posisjonskvalitet
    )
}
