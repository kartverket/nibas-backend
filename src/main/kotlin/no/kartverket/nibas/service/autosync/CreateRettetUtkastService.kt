package no.kartverket.nibas.service.autosync

import no.kartverket.nibas.api.v1.common.*
import no.kartverket.nibas.api.v1.common.geojson.*
import no.kartverket.nibas.api.v1.common.geojson.metadata.*
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.repository.UtkastRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point as JtsPoint
import org.locationtech.jts.index.strtree.STRtree
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Main flow:
 * 1. Find grenselinjer with avvik
 * 2. Archive them
 * 3. Fill gap with Matrikkel data
 * 4. Create utkast
 */
@Service
class CreateRettetUtkastService(
    private val arbeidslisteClient: ArbeidslisteClient,
    private val utkastRepository: UtkastRepository,
    private val kommuneService: no.kartverket.nibas.service.KommuneService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val coordinateTolerance = 0.1 // meters
    private val geometryFactory = GeometryFactory()

    private data class GrenseToArchive(val grense: Grense, val coords: List<List<Double>>)
    private data class ConnectionPoint(val point: List<Double>, val grenseLokalid: String)
    private data class Candidate(
        val feature: M22Feature,
        val coords: List<List<Double>>,
        val startMatches: Boolean,
        val distanceToGoal: Double
    )
    private data class WorstM22Posisjonskvalitet(
        val maalemetodeId: Int,
        val noeyaktighet: Int
    )
    private data class MatrikkelPathResult(
        val coordinates: MutableList<List<Double>>,
        val worstQuality: WorstM22Posisjonskvalitet
    )

    private data class IndexedM22Feature(
        val feature: M22Feature,
        val startPoint: JtsPoint,
        val endPoint: JtsPoint
    )

    private enum class ProcessResult {
        CREATED,
        SKIPPED_HJELPEPUNKT,
        SKIPPED_OTHER
    }

    @Transactional
    fun autoFixAvvik(antall: Int = 5) {
        log.info("=== START: Generating {} utkasts ===", antall)

        val kommuneParPage = arbeidslisteClient.getKommunePar(
            grensetyper = listOf("Kommunegrense", "Fylkesgrense"),
            side = 0,
            // Currently 115 Kommunepar with avvik.
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
            if (createdCount >= antall) {
                log.info("Reached target of {} utkasts, stopping", antall)
                break
            }

            log.info("")
            log.info("=== Processing: {} - {} ===",
                kommunePar.kommune1.kommunenavn, kommunePar.kommune2.kommunenavn)

            val kommuneParName = "${kommunePar.kommune1.kommunenavn} - ${kommunePar.kommune2.kommunenavn}"
            val result = processKommunePar(kommunePar)
            when (result) {
                ProcessResult.CREATED -> createdCount++
                ProcessResult.SKIPPED_HJELPEPUNKT -> skippedHjelpepunkt.add(kommuneParName)
                ProcessResult.SKIPPED_OTHER -> skippedOther.add(kommuneParName)
            }
        }

        log.info("=== DONE! ===")
        log.info("  Created: {}", createdCount)
        log.info("  Skipped hjelpepunkt ({}): {}", skippedHjelpepunkt.size, skippedHjelpepunkt)
        log.info("  Skipped other ({}): {}", skippedOther.size, skippedOther)
    }


    /**
     * Process one kommune pair.
     *
     * Main flow - reads like a story:
     * 1. Get data avvik og grensekoblinger.
     * 2. Identify what to archive
     * 3. Check if contains  hjelpepunkt / knutepunkt
     * 4. Build archive features
     * 5. Find gap connection points
     * 6. Fill gap with Matrikkel data
     * 7. Create and save utkast
     */
    private fun processKommunePar(kommunePar: KommunePar): ProcessResult {
        if (shouldSkipKommunePar(kommunePar)) {
            log.warn("  Skipping (known problems, will handle later)")
            return ProcessResult.SKIPPED_OTHER
        }

        val gyldigFra = LocalDate.now()

        val avvikList = arbeidslisteClient.getAvvikForKommunePar(
            kommuneId1 = kommunePar.kommune1.kommuneLokalID,
            kommuneId2 = kommunePar.kommune2.kommuneLokalID,
            grensetyper = listOf("Kommunegrense", "Fylkesgrense")
        )

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
            listOf(it.geometri.coordinates.first(), it.geometri.coordinates.last())
        }

        val toArchive = identifyGrenslinjerToArchive(
            grensekoblinger = grensekoblinger,
            avvikList = avvikList,
            avvikEndpoints = avvikEndpoints,
            gyldigFra = gyldigFra
        )

        if (toArchive.isEmpty()) {
            log.info("  No grenselinjer to archive")
            return ProcessResult.SKIPPED_OTHER
        }

        if (hasHjelpepunkt(toArchive, kommunePar, gyldigFra)) {
            log.warn("  Skipping - has hjelpepunkt (too complex)")
            return ProcessResult.SKIPPED_HJELPEPUNKT
        }

        log.info("  Safe to proceed (no hjelpepunkt)")

        val archiveFeatures = buildArchiveFeatures(toArchive, gyldigFra)

        val connectionPoints = findGapConnectionPoints(
            toArchive = toArchive,
            grensekoblinger = grensekoblinger,
            archiveFeatures = archiveFeatures,
            gyldigFra = gyldigFra
        )

        if (connectionPoints.size != 2) {
            log.warn("  Expected 2 connection points, found: {}. Cannot fill gap", connectionPoints.size)
            return ProcessResult.SKIPPED_OTHER
        }

        val gapStart = connectionPoints[0].point
        val gapEnd = connectionPoints[1].point
        log.info("=== GAP: [{}, {}] to [{}, {}] ===",
            gapStart[0], gapStart[1], gapEnd[0], gapEnd[1])

        val matrikkelFeatures = fetchMatrikkelGrenslinjerForKommunePar(kommunePar)

        val pathResult = walkMatrikkelNetwork(
            gapStart = gapStart,
            gapEnd = gapEnd,
            matrikkelFeatures = matrikkelFeatures
        )

        if (pathResult == null) {
            log.error("  Failed to build complete path through Matrikkel network")
            return ProcessResult.SKIPPED_OTHER
        }

        snapEndpointsToNibas(pathResult.coordinates, gapStart, gapEnd)

        val sharedKontekstEgenskaper = archiveFeatures.firstOrNull()?.properties?.kontekstEgenskaper ?: emptyList()
        val newGrenseFeature = buildNewGrenseFeature(
            pathResult.coordinates,
            sharedKontekstEgenskaper,
            pathResult.worstQuality,
            gyldigFra
        )

        val utkast = createAndSaveUtkast(
            archiveFeatures = archiveFeatures,
            createFeatures = listOf(newGrenseFeature),
            kommunePar = kommunePar,
            avvikList = avvikList,
            gyldigFra = gyldigFra
        )

        log.info("  Created Utkast: {}", utkast.id)

        arbeidslisteClient.markAvvikAsFikset(avvikList.map { it.id })

        return ProcessResult.CREATED
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

        val kommune2LokalId = kommunePar.kommune2.kommuneLokalID
        return allGrensekoblinger.filter { grensekobling ->
            val kommuner = grensekobling.grense.kommuner(gyldigFra).map { it.lokalid }
            kommuner.contains(kommune2LokalId)
        }
    }

    /**
     * Identify which grenselinjer to archive.
     *
     * Archive if:
     * 1. Grense has avvik, OR
     * 2. Both endpoints are inside avvik region (connecting segments)
     */
    private fun identifyGrenslinjerToArchive(
        grensekoblinger: List<no.kartverket.nibas.domain.GrunnkretsGrenseKobling>,
        avvikList: List<AvvikDto>,
        avvikEndpoints: List<List<Double>>,
        gyldigFra: LocalDate
    ): List<GrenseToArchive> {
        val toArchive = mutableListOf<GrenseToArchive>()
        val avvikByGrenseId = avvikList.associateBy { it.grenseId }

        for (grensekobling in grensekoblinger) {
            val grense = grensekobling.grense
            val hasAvvik = avvikByGrenseId.containsKey(grense.id)

            val coords = extractCoordinates(grense)
            val finalCoords = avvikByGrenseId[grense.id]?.geometri?.coordinates ?: coords

            val start = finalCoords.first()
            val end = finalCoords.last()

            val startInRegion = avvikEndpoints.any { pointsMatch(it, start) }
            val endInRegion = avvikEndpoints.any { pointsMatch(it, end) }

            if (hasAvvik || (startInRegion && endInRegion)) {
                toArchive.add(GrenseToArchive(grense, finalCoords))
                val reason = if (hasAvvik) "has avvik" else "connects avvik segments"
                log.info("  Will archive {}: {}", grense.lokalid, reason)
            }
        }

        return toArchive
    }

    private fun hasHjelpepunkt(
        toArchive: List<GrenseToArchive>,
        kommunePar: KommunePar,
        gyldigFra: LocalDate
    ): Boolean {
        val archivedEndpoints = toArchive.flatMap {
            listOf(it.coords.first(), it.coords.last())
        }

        val kommune1 = kommuneService.hentKommune(
            Identifikator(Lokalid(kommunePar.kommune1.kommuneLokalID), gyldigFra)
        ) ?: return false

        val kommune2 = kommuneService.hentKommune(
            Identifikator(Lokalid(kommunePar.kommune2.kommuneLokalID), gyldigFra)
        ) ?: return false

        val problematicGrenser = mutableSetOf<Grense>()
        collectProblematicGrenser(kommune1, gyldigFra, problematicGrenser)
        collectProblematicGrenser(kommune2, gyldigFra, problematicGrenser)

        for (grense in problematicGrenser) {
            val geom = grense.grense
            if (geom.coordinates.isEmpty()) continue

            val start = listOf(geom.coordinates.first().x, geom.coordinates.first().y)
            val end = listOf(geom.coordinates.last().x, geom.coordinates.last().y)

            for (archivedEndpoint in archivedEndpoints) {
                if (pointsMatch(start, archivedEndpoint) || pointsMatch(end, archivedEndpoint)) {
                    log.warn("  HJELPEPUNKT! {} {} touches archived grenselinje",
                        grense.grensetype, grense.lokalid)
                    return true
                }
            }
        }

        return false
    }

    private fun buildArchiveFeatures(
        toArchive: List<GrenseToArchive>,
        gyldigFra: LocalDate
    ): List<Feature> {
        val features = toArchive.map { gta ->
            val grense = gta.grense
            val kontekstEgenskaper = buildKontekstEgenskaper(grense, gyldigFra)

            Feature(
                id = grense.lokalid,
                type = "Feature",
                geometry = LineString(coordinates = gta.coords),
                properties = FeatureProperties(
                    type = "Kommunegrense",
                    srid = 25833,
                    version = grense.version,
                    metadata = AdministrativGrenseMetadata(
                        common = CommonMetadata(
                            identifikasjon = Identifikasjon(lokalid = grense.lokalid),
                            gyldigFra = gyldigFra
                        ),
                        commonGrense = CommonGrenseMetadata(
                            posisjonskvalitet = Posisjonskvalitet(
                                maalemetode = KodelisteEntry(id = grense.kvalitet?.maalemetode ?: ""),
                                noeyaktighet = grense.kvalitet?.noeyaktighet ?: 0
                            )
                        )
                    ),
                    kontekstEgenskaper = kontekstEgenskaper,
                    shouldArchive = true
                )
            )
        }

        return features
    }

    /**
     * Find gap connection points.
     * Gap = where archived grenselinjer touch non-archived grenselinjer.
     * Need exactly 2 connection points, gap start and gap end.
     */
    private fun findGapConnectionPoints(
        toArchive: List<GrenseToArchive>,
        grensekoblinger: List<no.kartverket.nibas.domain.GrunnkretsGrenseKobling>,
        archiveFeatures: List<Feature>,
        gyldigFra: LocalDate
    ): List<ConnectionPoint> {
        val archivedEndpoints = toArchive.flatMap {
            listOf(it.coords.first(), it.coords.last())
        }

        val archivedLokalids = archiveFeatures.map {
            (it.properties.metadata as AdministrativGrenseMetadata).common.identifikasjon.lokalid
        }.toSet()

        val connectionPoints = mutableListOf<ConnectionPoint>()

        for (grensekobling in grensekoblinger) {
            val grense = grensekobling.grense

            if (archivedLokalids.contains(grense.lokalid)) continue

            val nibasCoords = extractCoordinates(grense)
            val start = nibasCoords.first()
            val end = nibasCoords.last()

            for (archivedEndpoint in archivedEndpoints) {
                if (pointsMatch(start, archivedEndpoint) && !connectionPoints.any { pointsMatch(it.point, start) }) {
                    connectionPoints.add(ConnectionPoint(start, grense.lokalid))
                }
                if (pointsMatch(end, archivedEndpoint) && !connectionPoints.any { pointsMatch(it.point, end) }) {
                    connectionPoints.add(ConnectionPoint(end, grense.lokalid))
                }
            }
        }

        return connectionPoints
    }

    private fun fetchMatrikkelGrenslinjerForKommunePar(
        kommunePar: KommunePar
    ): List<M22Feature> {
        val response = arbeidslisteClient.getMatrikkelGrenselinjer(
            kommunenummer = kommunePar.kommune1.kommunenummer
        )

        val filtered = response.features.filter { feature ->
            val k1 = kommunePar.kommune1.kommunenummer
            val k2 = kommunePar.kommune2.kommunenummer
            val m1 = feature.properties.kommunenr1
            val m2 = feature.properties.kommunenr2

            m1 != null && m2 != null && ((k1 == m1 && k2 == m2) || (k1 == m2 && k2 == m1))
        }

        return filtered
    }

    private fun walkMatrikkelNetwork(
        gapStart: List<Double>,
        gapEnd: List<Double>,
        matrikkelFeatures: List<M22Feature>
    ): MatrikkelPathResult? {
        val spatialIndex = buildSpatialIndex(matrikkelFeatures)
        val allCoordinates = mutableListOf<List<Double>>()
        val processedIds = mutableSetOf<String>()
        var currentEndpoint = gapStart
        var worstNoeyaktighet = 0
        var worstMaalemetodeId = 0
        val jtsGoal = toJtsPoint(gapEnd)

        while (!pointsMatch(currentEndpoint, gapEnd)) {
            val candidates = findCandidateSegmentsJTS(
                currentEndpoint = currentEndpoint,
                goalPoint = jtsGoal,
                spatialIndex = spatialIndex,
                processedIds = processedIds
            )

            if (candidates.isEmpty()) {
                log.warn("  Dead end! Could not reach goal")
                return null
            }

            val best = candidates.minByOrNull { it.distanceToGoal }!!
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
            WorstM22Posisjonskvalitet(worstMaalemetodeId, worstNoeyaktighet)
        )
    }

    private fun findCandidateSegmentsJTS(
        currentEndpoint: List<Double>,
        goalPoint: JtsPoint,
        spatialIndex: STRtree,
        processedIds: Set<String>
    ): List<Candidate> {
        val currentPoint = toJtsPoint(currentEndpoint)
        val nearbyFeatures = findFeaturesNearPoint(currentPoint, spatialIndex)
        val candidates = mutableListOf<Candidate>()

        for (indexed in nearbyFeatures) {
            if (processedIds.contains(indexed.feature.id)) continue

            val coords = indexed.feature.geometry.coordinates

            val startMatches = indexed.startPoint.distance(currentPoint) < coordinateTolerance
            val endMatches = indexed.endPoint.distance(currentPoint) < coordinateTolerance

            if (startMatches || endMatches) {
                val nextEndpoint = if (startMatches) indexed.endPoint else indexed.startPoint
                val distanceToGoal = nextEndpoint.distance(goalPoint)
                candidates.add(Candidate(indexed.feature, coords, startMatches, distanceToGoal))
            }
        }
        return candidates
    }

    private fun addCoordinatesToPath(
        path: MutableList<List<Double>>,
        coords: List<List<Double>>,
        startMatches: Boolean
    ) {
        val coordsToAdd = if (startMatches) coords else coords.reversed()
        if (path.isEmpty()) {
            path.addAll(coordsToAdd)
        } else {
            path.addAll(coordsToAdd.drop(1))
        }
    }

    private fun snapEndpointsToNibas(
        coordinates: MutableList<List<Double>>,
        nibasStart: List<Double>,
        nibasEnd: List<Double>
    ) {
        coordinates[0] = nibasStart
        coordinates[coordinates.size - 1] = nibasEnd
    }

    private fun buildNewGrenseFeature(
        coordinates: List<List<Double>>,
        kontekstEgenskaper: List<KontekstEgenskaper>,
        worstQuality: WorstM22Posisjonskvalitet,
        gyldigFra: LocalDate
    ): Feature {
        log.info("Creating new grense: {} coords, quality: {}cm",
            coordinates.size, worstQuality.noeyaktighet)

        return Feature(
            id = null,
            type = "Feature",
            geometry = LineString(coordinates = coordinates),
            properties = FeatureProperties(
                type = "Kommunegrense",
                srid = 25833,
                version = 1,
                metadata = AdministrativGrenseMetadata(
                    common = CommonMetadata(
                        identifikasjon = Identifikasjon(lokalid = "NotARealID"),
                        gyldigFra = gyldigFra,
                        informasjon = "13.10.2025 - Kvalitetsheving av kommunegrense, jf. Arken 25/29245"
                    ),
                    commonGrense = CommonGrenseMetadata(
                        posisjonskvalitet = Posisjonskvalitet(
                            maalemetode = KodelisteEntry(id = worstQuality.maalemetodeId.toString()),
                            noeyaktighet = worstQuality.noeyaktighet
                        )
                    )
                ),
                kontekstEgenskaper = kontekstEgenskaper,
                shouldArchive = false
            )
        )
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

    private fun pointsMatch(p1: List<Double>, p2: List<Double>): Boolean {
        val jtsP1 = toJtsPoint(p1)
        val jtsP2 = toJtsPoint(p2)
        return jtsP1.distance(jtsP2) < coordinateTolerance
    }

    private fun extractCoordinates(grense: Grense): List<List<Double>> {
        return grense.grense.coordinates.map { listOf(it.x, it.y) }
    }

    private fun collectProblematicGrenser(
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
    }

    private fun buildKontekstEgenskaper(grense: Grense, gyldigFra: LocalDate): List<KontekstEgenskaper> {
        return buildList {
            grense.grunnkretser(gyldigFra).forEach { grunnkrets ->
                add(KontekstEgenskaper(
                    id = ObjektIdentifikator(
                        lokalid = Lokalid(grunnkrets.lokalid),
                        gyldighetsdato = grunnkrets.gyldighet.gyldigFra
                    ),
                    type = KontekstEnhetType.GRUNNKRETS,
                    version = grunnkrets.version,
                    kommuneId = ObjektIdentifikator(
                        lokalid = Lokalid(grunnkrets.getKommune(gyldigFra)!!.lokalid),
                        gyldighetsdato = grunnkrets.getKommune(gyldigFra)!!.gyldighet.gyldigFra
                    ),
                    kretsNummer = grunnkrets.grunnkretsnummer!!
                ))
            }

            grense.stemmekretser(gyldigFra).forEach { stemmekrets ->
                add(KontekstEgenskaper(
                    id = ObjektIdentifikator(
                        lokalid = Lokalid(stemmekrets.lokalid),
                        gyldighetsdato = stemmekrets.gyldighet.gyldigFra
                    ),
                    type = KontekstEnhetType.STEMMEKRETS,
                    version = stemmekrets.version,
                    kommuneId = ObjektIdentifikator(
                        lokalid = Lokalid(stemmekrets.getKommune(gyldigFra)!!.lokalid),
                        gyldighetsdato = stemmekrets.getKommune(gyldigFra)!!.gyldighet.gyldigFra
                    ),
                    kretsNummer = stemmekrets.stemmekretsnummer!!
                ))
            }
        }
    }

    private fun toJtsPoint(coords: List<Double>): JtsPoint {
        return geometryFactory.createPoint(Coordinate(coords[0], coords[1]))
    }

    private fun buildSpatialIndex(matrikkelFeatures: List<M22Feature>): STRtree {
        val index = STRtree()

        matrikkelFeatures.forEach { feature ->
            val coords = feature.geometry.coordinates.map {
                Coordinate(it[0], it[1])
            }.toTypedArray()

            val jtsLine = geometryFactory.createLineString(coords)
            val startPoint = geometryFactory.createPoint(coords.first())
            val endPoint = geometryFactory.createPoint(coords.last())

            val indexed = IndexedM22Feature(feature, startPoint, endPoint)
            index.insert(jtsLine.envelopeInternal, indexed)
        }

        index.build()
        return index
    }

    @Suppress("UNCHECKED_CAST")
    private fun findFeaturesNearPoint(
        point: JtsPoint,
        spatialIndex: STRtree,
        searchRadius: Double = coordinateTolerance
    ): List<IndexedM22Feature> {
        val searchEnvelope = point.buffer(searchRadius).envelopeInternal
        return spatialIndex.query(searchEnvelope) as List<IndexedM22Feature>
    }

    /**
     * Temp skip kommunepar that have known problems.
     */
    private fun shouldSkipKommunePar(kommunePar: KommunePar): Boolean {
        val navn1 = kommunePar.kommune1.kommunenavn
        val navn2 = kommunePar.kommune2.kommunenavn

        // Saving some time then generating new utkast.
        // Will change this after we have fixed the easy avvik.
        val problematicPairs = listOf(
            // Fails for other reasons.
            "Surnadal" to "Rindal",
//            "Ullensaker" to "Eidsvoll",
            "Rennebu" to "Orkland",
            // Has knutepuntk
            "Nord-Aurdal" to "Vestre Slidre", "Moss" to "Vestby", "Nes" to "Sør-Odal",
            "Hamar" to "Løten", "Indre Østfold" to "Nordre Follo", "Ålesund" to "Haram",
            "Lindesnes" to "Lyngdal", "Kristiansand" to "Lillesand", "Oslo" to "Nittedal",
            "Bodø" to "Beiarn", "Kongsberg" to "Notodden", "Søndre Land" to "Nordre Land",
            "Vegårshei" to "Åmli", "Austevoll" to "Øygarden", "Smøla" to "Frøya",
            "Flekkefjord" to "Kvinesdal", "Horten" to "Holmestrand", "Larvik" to "Porsgrunn",
            "Farsund" to "Lyngdal", "Måsøy" to "Nordkapp", "Froland" to "Åmli",
            "Vestby" to "Ås", "Indre Østfold" to "Enebakk", "Kinn" to "Stad",
            "Solund" to "Askvoll", "Vanylven" to "Volda", "Nissedal" to "Åmli",
            "Sandefjord" to "Larvik", "Røros" to "Tydal", "Etnedal" to "Nord-Aurdal",
            "Os" to "Røros", "Sande" to "Herøy", "Kristiansund" to "Averøy",
            "Grimstad" to "Lillesand", "Ålesund" to "Aukra", "Gjerdrum" to "Nittedal",
            "Sandefjord" to "Færder", "Bærum" to "Hole", "Sokndal" to "Flekkefjord",
            "Eigersund" to "Bjerkreim", "Lierne" to "Røyrvik", "Engerdal" to "Røros",
            "Farsund" to "Flekkefjord", "Frøya" to "Osen", "Kristiansand" to "Lindesnes",
            "Krødsherad" to "Sigdal", "Sarpsborg" to "Våler", "Træna" to "Rødøy",
            "Kinn" to "Bremanger", "Gamvik" to "Berlevåg", "Ringerike" to "Hole",
            "Fredrikstad" to "Råde", "Snåsa" to "Grong", "Lillestrøm" to "Ullensaker",
            "Trondheim" to "Selbu", "Birkenes" to "Evje og Hornnes", "Lillestrøm" to "Nittedal",
            "Sør-Aurdal" to "Etnedal", "Krødsherad" to "Flå", "Flekkefjord" to "Sirdal",
            "Risør" to "Tvedestrand", "Båtsfjord" to "Vardø", "Halden" to "Aremark",
            "Bømlo" to "Austevoll", "Øygarden" to "Fedje",
            "Eigersund" to "Sokndal", "Osen" to "Flatanger", "Sør-Aurdal" to "Nord-Aurdal",
            "Kristiansund" to "Smøla", "Grimstad" to "Arendal",
            "Averøy" to "Hustadvika", "Kragerø" to "Risør", "Birkenes" to "Iveland",
            "Flå" to "Sigdal", "Hå" to "Klepp",
            "Lillestrøm" to "Gjerdrum", "Herøy" to "Træna", "Halden" to "Hvaler",
            "Iveland" to "Evje og Hornnes", "Berlevåg" to "Båtsfjord", "Bindal" to "Leka",
            "Karasjok" to "Kautokeino", "Andøy" to "Senja", "Herøy" to "Giske",
            "Sande" to "Stad"
        )

        return problematicPairs.any { (k1, k2) ->
            (navn1 == k1 && navn2 == k2) || (navn1 == k2 && navn2 == k1)
        }
    }
}
