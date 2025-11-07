package no.kartverket.nibas.service.autosync

import no.kartverket.nibas.api.v1.common.*
import no.kartverket.nibas.api.v1.common.geojson.*
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.repository.UtkastRepository
import no.kartverket.nibas.service.KommuneService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate



@Service
class CreateRettetUtkastService(
    private val arbeidslisteClient: ArbeidslisteClient,
    private val utkastRepository: UtkastRepository,
    private val kommuneService: KommuneService,
    private val coordinateService: CoordinateService,
    private val grenseArchiveService: GrenseArchiveService,
    private val matrikkelNetworkService: MatrikkelNetworkService,
    private val grenseTopologyService: GrenseTopologyService,
    private val spatialNetworkPathfinder: SpatialNetworkPathfinder
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private enum class ProcessResult {
        CREATED,
        SKIPPED_HJELPEPUNKT,
        SKIPPED_OTHER
    }

    @Transactional
    fun autoFixAvvik(antall: Int = 5) {
        val actualAntall = 1
        log.info("=== START: Generating {} utkasts ===", actualAntall)

        val kommuneParPage = arbeidslisteClient.getKommunePar(
            grensetyper = listOf("Kommunegrense", "Fylkesgrense"),
            side = 0,
            antall = 150
        )

        if (kommuneParPage.content.isEmpty()) {
            log.info("No kommune pairs with avvik found!")
            return
        }

        log.info("Found {} kommunepar available", kommuneParPage.content.size)

        var createdCount = 0
        val skippedHjelpepunkt = mutableListOf<String>()
        val skippedOther = mutableListOf<String>()

        for (kommunePar in kommuneParPage.content) {
            if (createdCount >= actualAntall) {
                log.info("Reached target of {} utkasts, stopping", actualAntall)
                break
            }

            log.info("")
            log.info("=== Processing: {} - {} ===",
                kommunePar.kommune1.kommunenavn, kommunePar.kommune2.kommunenavn)

            val kommuneParName = "${kommunePar.kommune1.kommunenavn} - ${kommunePar.kommune2.kommunenavn}"

            if (shouldSkipKommunePar(kommunePar)) {
                skippedOther.add(kommuneParName)
                continue
            }

            try {
                val result = processKommunePar(kommunePar)
                when (result) {
                    ProcessResult.CREATED -> createdCount++
                    ProcessResult.SKIPPED_HJELPEPUNKT -> skippedHjelpepunkt.add(kommuneParName)
                    ProcessResult.SKIPPED_OTHER -> skippedOther.add(kommuneParName)
                }
            } catch (e: Exception) {
                log.error("  Failed to process $kommuneParName: ${e.message}")
                skippedOther.add(kommuneParName)
            }
        }

        log.info("=== DONE! ===")
        log.info("  Created: {}", createdCount)
        log.info("  Skipped hjelpepunkt ({}): {}", skippedHjelpepunkt.size, skippedHjelpepunkt)
        log.info("  Skipped other ({}): {}", skippedOther.size, skippedOther)
    }

    private fun processKommunePar(kommunePar: KommunePar): ProcessResult {
        val gyldigFra = LocalDate.now()

        val avvikList = arbeidslisteClient.getAvvikForKommunePar(
            kommuneId1 = kommunePar.kommune1.kommuneLokalID,
            kommuneId2 = kommunePar.kommune2.kommuneLokalID,
            grensetyper = listOf("Kommunegrense", "Fylkesgrense")
        ).filter { it.status == "NY" }

        if (avvikList.isEmpty()) {
            log.info("  No avvik found")
            return ProcessResult.SKIPPED_OTHER
        }

        val grensekoblinger = fetchGrensekoblingerForKommunePar(kommunePar, gyldigFra)
        if (grensekoblinger.isEmpty()) {
            log.warn("  No grensekoblinger found")
            return ProcessResult.SKIPPED_OTHER
        }

        val avvikEndpoints = avvikList.flatMap {
            listOf(
                Point.fromList(it.geometri.coordinates.first()),
                Point.fromList(it.geometri.coordinates.last())
            )
        }

        val toArchive = identifyGrenslinjerToArchive(
            grensekoblinger = grensekoblinger,
            avvikList = avvikList,
            avvikEndpoints = avvikEndpoints,
        )

        if (toArchive.isEmpty()) {
            log.info("  No grenselinjer to archive")
            return ProcessResult.SKIPPED_OTHER
        }

        val kommune1 = kommuneService.hentKommune(
            Identifikator(Lokalid(kommunePar.kommune1.kommuneLokalID), gyldigFra)
        ) ?: return ProcessResult.SKIPPED_OTHER

        val kommune2 = kommuneService.hentKommune(
            Identifikator(Lokalid(kommunePar.kommune2.kommuneLokalID), gyldigFra)
        ) ?: return ProcessResult.SKIPPED_OTHER

        val affectedGrenser = grenseTopologyService.findGrenseSharingEndpoints(
            toArchive, kommune1, kommune2, gyldigFra
        )

        if (affectedGrenser.isNotEmpty()) {
            log.info("  Has hjelpepunkt - will also fix {} affected grenser", affectedGrenser.size)
        }

        val allNeighborGrenser = fetchAllNeighborGrenser(kommunePar, gyldigFra)
        var connectionPoints = findGapConnectionPoints(toArchive, allNeighborGrenser)

        if (connectionPoints.size < 2) {
            log.warn("  Expected at least 2 connection points, found: {}. Cannot fill gap", connectionPoints.size)
            return ProcessResult.SKIPPED_OTHER
        }

        if (connectionPoints.size > 2) {
            log.warn("  Found {} connection points. Will attempt to find outermost points.", connectionPoints.size)
            connectionPoints = findOutermostPoints(connectionPoints)
        }

        val gapStart = connectionPoints[0].point
        val gapEnd = connectionPoints[1].point


        val finalToArchive = spatialNetworkPathfinder.ensureCompleteChain(
            toArchive = toArchive,
            gapStart = gapStart,
            gapEnd = gapEnd,
            allGrensekoblinger = allNeighborGrenser,
            gyldigFra = gyldigFra
        )

        val sortedToArchive = sortGrenserAlongBorder(finalToArchive, gapStart, gapEnd)
            ?: throw PathfindingException("Cannot sort grenser along border - chain is not continuous")

        val archiveFeatures = grenseArchiveService.buildArchiveFeatures(sortedToArchive, gyldigFra)

        log.info("=== GAP: [{}, {}] to [{}, {}] ===",
            gapStart.x, gapStart.y, gapEnd.x, gapEnd.y)
        log.info("  GAP START decimals: {}", if (coordinateService.hasTooManyDecimals(gapStart)) "BAD!" else "OK")
        log.info("  GAP END decimals: {}", if (coordinateService.hasTooManyDecimals(gapEnd)) "BAD!" else "OK")

        val matrikkelFeatures = matrikkelNetworkService.fetchMatrikkelGrenslinjerForKommunePar(kommunePar)

        val pathResult = matrikkelNetworkService.walkMatrikkelNetwork(
            gapStart = gapStart,
            gapEnd = gapEnd,
            matrikkelFeatures = matrikkelFeatures
        ) ?: throw PathfindingException("Failed to build complete path through Matrikkel network")

        matrikkelNetworkService.snapEndpointsToNibas(pathResult.coordinates, gapStart, gapEnd)

        val hjelpepunkt = grenseTopologyService.findHjelpepunkt(sortedToArchive, gapStart, gapEnd)
        val grenseType = sortedToArchive.firstOrNull()?.grense?.grensetype?.toString() ?: "Kommunegrense"

        val (archiveAndCreateFeatures, newGrenseFeatures) = if (hjelpepunkt.isNotEmpty()) {
            handleHjelpepunktCase(hjelpepunkt, pathResult, archiveFeatures, gapStart, gapEnd, gyldigFra, grenseType)
        } else {
            handleSimpleCase(pathResult, archiveFeatures, gyldigFra, grenseType)
        }

        val affectedUpdateFeatures = if (affectedGrenser.isNotEmpty()) {
            handleAffectedGrenser(affectedGrenser, hjelpepunkt, pathResult.coordinates, gyldigFra)
        } else {
            emptyList()
        }

        val allCreateFeatures = newGrenseFeatures + affectedUpdateFeatures

        val utkast = createAndSaveUtkast(
            archiveFeatures = archiveAndCreateFeatures,
            createFeatures = allCreateFeatures,
            kommunePar = kommunePar,
            avvikList = avvikList,
            gyldigFra = gyldigFra
        )

        log.info("  Created Utkast: {}", utkast.id)
        arbeidslisteClient.markAvvikAsFikset(avvikList.map { it.id })

        return ProcessResult.CREATED
    }

    private fun handleHjelpepunktCase(
        hjelpepunkt: List<Point>,
        pathResult: MatrikkelNetworkService.MatrikkelPathResult,
        archiveFeatures: List<Feature>,
        gapStart: Point,
        gapEnd: Point,
        gyldigFra: LocalDate,
        grenseType: String
    ): Pair<List<Feature>, List<Feature>> {
        log.info("  Found {} hjelpepunkt - will split new kommunegrense", hjelpepunkt.size)

        val hjelpepunktInfos = hjelpepunkt.map {
            grenseTopologyService.projectPointOntoPath(it, pathResult.coordinates)
        }

        val (deduplicatedHjelpepunkt, _) = grenseTopologyService.deduplicateHjelpepunkt(hjelpepunktInfos)
        val segments = grenseTopologyService.splitPathAtHjelpepunkt(pathResult.coordinates, deduplicatedHjelpepunkt)

        log.info("  Creating {} new segments (using archive kontekstEgenskaper)", segments.size)

        val newFeatures = segments.mapIndexed { index, segmentCoords ->
            val segmentStart = segmentCoords.first()
            val segmentEnd = segmentCoords.last()

            val kontekstEgenskaper = grenseArchiveService.findMatchingArchiveKontekst(
                segmentStart = segmentStart,
                segmentEnd = segmentEnd,
                gapStart = gapStart,
                gapEnd = gapEnd,
                hjelpepunktInfos = deduplicatedHjelpepunkt,
                archiveFeatures = archiveFeatures
            )

            log.info("  Segment {}: using {} kontekstEgenskaper from archive", index + 1, kontekstEgenskaper.size)

            grenseArchiveService.buildNewGrenseFeature(
                segmentCoords, kontekstEgenskaper, pathResult.worstQuality, gyldigFra, grenseType
            )
        }

        return Pair(archiveFeatures, newFeatures)
    }

    private fun handleSimpleCase(
        pathResult: MatrikkelNetworkService.MatrikkelPathResult,
        archiveFeatures: List<Feature>,
        gyldigFra: LocalDate,
        grenseType: String
    ): Pair<List<Feature>, List<Feature>> {
        log.info("  No hjelpepunkt - single segment from gapStart to gapEnd")

        val kontekstEgenskaper = archiveFeatures.flatMap { it.properties.kontekstEgenskaper }
            .distinctBy { it.id?.lokalid }

        log.info("  Using {} kontekstEgenskaper from archive", kontekstEgenskaper.size)

        val newFeature = listOf(
            grenseArchiveService.buildNewGrenseFeature(
                pathResult.coordinates, kontekstEgenskaper, pathResult.worstQuality, gyldigFra, grenseType
            )
        )

        return Pair(archiveFeatures, newFeature)
    }

    private fun handleAffectedGrenser(
        affectedGrenser: List<no.kartverket.nibas.domain.Grense>,
        hjelpepunkt: List<Point>,
        pathCoordinates: List<Point>,
        gyldigFra: LocalDate
    ): List<Feature> {
        val hjelpepunktInfos = hjelpepunkt.map { hp ->
            grenseTopologyService.projectPointOntoPath(hp, pathCoordinates)
        }

        val endpointMapping = grenseTopologyService.buildEndpointMapping(hjelpepunktInfos)
        log.info("  Built endpoint mapping with {} hjelpepunkt", endpointMapping.size)

        return affectedGrenser.map { grense ->
            val coords = coordinateService.extractCoordinates(grense)
            grenseArchiveService.buildAffectedGrenseUpdateFeature(grense, coords, endpointMapping, gyldigFra)
        }
    }

    private fun fetchGrensekoblingerForKommunePar(
        kommunePar: KommunePar,
        gyldigFra: LocalDate
    ): List<no.kartverket.nibas.domain.GrunnkretsGrenseKobling> {
        val kommune1Identifikator = Identifikator(
            Lokalid(kommunePar.kommune1.kommuneLokalID),
            gyldigFra
        )

        val allGrensekoblinger = kommuneService.hentGrensekoblingerForKommune(kommune1Identifikator)
        log.info("  Got {} total grensekoblinger for kommune1", allGrensekoblinger.size)

        val kommune2LokalId = kommunePar.kommune2.kommuneLokalID
        val filtered = allGrensekoblinger.filter { grensekobling ->
            val kommuner = grensekobling.grense.kommuner(gyldigFra).map { it.lokalid }
            kommuner.contains(kommune2LokalId)
        }
        log.info("  After filtering for kommune2 ({}): {} grensekoblinger", kommune2LokalId, filtered.size)
        return filtered
    }

    private fun fetchAllNeighborGrenser(
        kommunePar: KommunePar,
        gyldigFra: LocalDate
    ): List<no.kartverket.nibas.domain.GrunnkretsGrenseKobling> {
        val kommune1Identifikator = Identifikator(Lokalid(kommunePar.kommune1.kommuneLokalID), gyldigFra)
        val kommune2Identifikator = Identifikator(Lokalid(kommunePar.kommune2.kommuneLokalID), gyldigFra)

        val grenser1 = kommuneService.hentGrensekoblingerForKommune(kommune1Identifikator)
        val grenser2 = kommuneService.hentGrensekoblingerForKommune(kommune2Identifikator)

        return (grenser1 + grenser2).distinctBy { it.grense.id }
    }

    private fun identifyGrenslinjerToArchive(
        grensekoblinger: List<no.kartverket.nibas.domain.GrunnkretsGrenseKobling>,
        avvikList: List<AvvikDto>,
        avvikEndpoints: List<Point>,
    ): List<GrenseArchiveService.GrenseToArchive> {
        val toArchive = mutableListOf<GrenseArchiveService.GrenseToArchive>()
        val avvikByGrenseId = avvikList.associateBy { it.grenseId }

        for (grensekobling in grensekoblinger) {
            val grense = grensekobling.grense
            val hasAvvik = avvikByGrenseId.containsKey(grense.id)

            val coords = coordinateService.extractCoordinates(grense)
            val finalCoords = avvikByGrenseId[grense.id]?.geometri?.coordinates?.map { Point.fromList(it) }
                ?: coords

            val start = finalCoords.first()
            val end = finalCoords.last()

            val startInRegion = avvikEndpoints.any { coordinateService.pointsMatch(it, start) }
            val endInRegion = avvikEndpoints.any { coordinateService.pointsMatch(it, end) }

            if (hasAvvik || (startInRegion && endInRegion)) {
                toArchive.add(GrenseArchiveService.GrenseToArchive(grense, finalCoords))
                val reason = if (hasAvvik) "has avvik" else "connects avvik segments"
                log.info("  Will archive {}: {}", grense.lokalid, reason)
            }
        }

        return toArchive
    }

    private fun findGapConnectionPoints(
        toArchive: List<GrenseArchiveService.GrenseToArchive>,
        grensekoblinger: List<no.kartverket.nibas.domain.GrunnkretsGrenseKobling>
    ): List<ConnectionPoint> {
        val archivedEndpoints = toArchive.flatMap {
            listOf(it.coords.first(), it.coords.last())
        }.toSet()

        val archivedLokalids = toArchive.map { it.grense.lokalid }.toSet()

        val connectionPoints = mutableSetOf<ConnectionPoint>()

        for (grensekobling in grensekoblinger) {
            val grense = grensekobling.grense
            if (grense.lokalid in archivedLokalids) continue

            val nibasCoords = coordinateService.extractCoordinates(grense)
            val start = nibasCoords.first()
            val end = nibasCoords.last()

            if (archivedEndpoints.any { coordinateService.pointsMatch(it, start) }) {
                connectionPoints.add(ConnectionPoint(start, grense.lokalid))
            }
            if (archivedEndpoints.any { coordinateService.pointsMatch(it, end) }) {
                connectionPoints.add(ConnectionPoint(end, grense.lokalid))
            }
        }

        return connectionPoints.toList()
    }

    private fun findOutermostPoints(points: List<ConnectionPoint>): List<ConnectionPoint> {
        if (points.size <= 2) return points

        log.info("  All {} connection points:", points.size)
        points.forEachIndexed { index, cp ->
            log.info("    [{}] [{}, {}] from grense {}", index, cp.point.x, cp.point.y, cp.grenseLokalid)
        }

        // Deduplicate by coordinate
        val uniquePoints = points.groupBy { it.point }
            .map { (_, group) -> group.first() }

        if (uniquePoints.size < points.size) {
            log.warn("  Deduplicated {} connection points down to {} unique coordinates",
                points.size, uniquePoints.size)
            log.warn("  This indicates multiple grenser connect at the same coordinate (fork/branch in border)")
        }

        if (uniquePoints.size <= 2) {
            log.info("  After deduplication, only {} unique coordinates - using all of them", uniquePoints.size)
            return uniquePoints
        }

        var maxDistance = -1.0
        var bestPair = Pair(uniquePoints[0], uniquePoints[1])

        for (i in uniquePoints.indices) {
            for (j in i + 1 until uniquePoints.size) {
                val p1 = uniquePoints[i]
                val p2 = uniquePoints[j]
                val dist = p1.point.distanceTo(p2.point)
                if (dist > maxDistance) {
                    maxDistance = dist
                    bestPair = Pair(p1, p2)
                }
            }
        }

        log.info("  Selected outermost points: [{}, {}] to [{}, {}] (distance: {}m)",
            bestPair.first.point.x, bestPair.first.point.y,
            bestPair.second.point.x, bestPair.second.point.y,
            String.format(java.util.Locale.US, "%.2f", maxDistance))

        return listOf(bestPair.first, bestPair.second)
    }

    private fun sortGrenserAlongBorder(
        toArchive: List<GrenseArchiveService.GrenseToArchive>,
        gapStart: Point,
        gapEnd: Point
    ): List<GrenseArchiveService.GrenseToArchive>? {
        if (toArchive.isEmpty()) return emptyList()
        if (toArchive.size == 1) return toArchive

        val sorted = mutableListOf<GrenseArchiveService.GrenseToArchive>()
        val remaining = toArchive.toMutableList()
        var currentPoint = gapStart

        while (remaining.isNotEmpty()) {
            val nextSegment = remaining.find { segment ->
                coordinateService.pointsMatch(segment.coords.first(), currentPoint) ||
                coordinateService.pointsMatch(segment.coords.last(), currentPoint)
            }

            if (nextSegment == null) {
                log.warn("Could not find next segment in chain at [{}, {}]", currentPoint.x, currentPoint.y)
                log.warn("  Remaining segments: {}", remaining.map { it.grense.lokalid })

                for (segment in remaining) {
                    val start = segment.coords.first()
                    val end = segment.coords.last()
                    val distToStart = currentPoint.distanceTo(start)
                    val distToEnd = currentPoint.distanceTo(end)
                    log.warn("    Segment {}: start=[{}, {}] dist={}m, end=[{}, {}] dist={}m",
                        segment.grense.lokalid,
                        start.x, start.y, String.format(java.util.Locale.US, "%.2f", distToStart),
                        end.x, end.y, String.format(java.util.Locale.US, "%.2f", distToEnd))
                }

                return null
            }

            sorted.add(nextSegment)
            remaining.remove(nextSegment)

            currentPoint = if (coordinateService.pointsMatch(nextSegment.coords.first(), currentPoint)) {
                nextSegment.coords.last()
            } else {
                nextSegment.coords.first()
            }

            if (coordinateService.pointsMatch(currentPoint, gapEnd) && remaining.isNotEmpty()) {
                log.warn("Reached gapEnd but {} segments remain unsorted", remaining.size)
                return null
            }
        }

        log.info("  Sorted {} grenser along border from gapStart to gapEnd", sorted.size)
        return sorted
    }

    private fun createAndSaveUtkast(
        archiveFeatures: List<Feature>,
        createFeatures: List<Feature>,
        kommunePar: KommunePar,
        avvikList: List<AvvikDto>,
        gyldigFra: LocalDate
    ): no.kartverket.nibas.domain.utkast.Utkast {
        val operasjoner = Operasjoner(
            grenseendringer = Grenseendringer(
                endredeFeatures = archiveFeatures + createFeatures
            ),
            metadataendringer = Metadataendringer()
        )

        val avvikGrenselinjer = avvikList.size
        val avvikPunkter = avvikList.sumOf { it.antallKoordinaterMedAvvik }

        val utkast = UtkastBuilder.builder(
            navn = "Automatisk Kvalitetshevning: ${kommunePar.kommune1.kommunenummer} ${kommunePar.kommune1.kommunenavn} - ${kommunePar.kommune2.kommunenummer}" +
                " ${kommunePar.kommune2.kommunenavn} - $avvikGrenselinjer grenser med $avvikPunkter avvikspunkter",
            gyldigFra = gyldigFra
        )
            .withOperasjoner(operasjoner)
            .build()

        return utkastRepository.save(utkast)
    }

    private data class ConnectionPoint(val point: Point, val grenseLokalid: String)

    private fun shouldSkipKommunePar(kommunePar: KommunePar): Boolean {
        val navn1 = kommunePar.kommune1.kommunenavn
        val navn2 = kommunePar.kommune2.kommunenavn


        val problematicPairs = listOf(
            "Nes" to "Sør-Odal",
            "Indre Østfold" to "Nordre Follo",
            "Hamar" to "Løten",
            "Larvik" to "Porsgrunn",
            "Sande" to "Stad",
            "Ålesund" to "Haram",
            "Birkenes" to "Evje og Hornnes",
            "Austevoll" to "Øygarden",
            "Horten" to "Holmestrand",
            "Farsund" to "Lyngdal",
            "Kristiansand" to "Lillesand",
            "Solund" to "Askvoll",
            "Oslo" to "Nittedal",
            "Kinn" to "Stad",
            "Engerdal" to "Røros",
            "Flå" to "Sigdal",
            "Birkenes" to "Iveland",
            "Grimstad" to "Lillesand",
            "Iveland" to "Evje og Hornnes",
            "Farsund" to "Flekkefjord",
            "Sande" to "Herøy",
            "Kristiansund" to "Averøy",
            "Sandefjord" to "Færder",
            "Sokndal" to "Flekkefjord",
            "Bindal" to "Leka",
            "Herøy" to "Giske",
            "Smøla" to "Frøya",
            "Kristiansund" to "Smøla",
            "Ålesund" to "Aukra",
            "Averøy" to "Hustadvika",
            "Kragerø" to "Risør",
            "Hå" to "Klepp",
            "Grimstad" to "Arendal",
            "Kristiansand" to "Lindesnes",
            "Risør" to "Tvedestrand",
            "Halden" to "Hvaler",
            "Kinn" to "Bremanger",
            "Bømlo" to "Austevoll"
        )

        return problematicPairs.any { (k1, k2) ->
            (navn1 == k1 && navn2 == k2) || (navn1 == k2 && navn2 == k1)
        }
    }
}
object SpatialConstants {
    const val COORDINATE_TOLERANCE = 0.1
    const val NIBAS_M22_SNAP_TOLERANCE = 10.0
    const val KNUTEPUNKT_MARGIN = 15.0
    const val COORDINATE_SCALE = 200.0
    const val MAX_COORDINATE_DECIMALS = 3
}
