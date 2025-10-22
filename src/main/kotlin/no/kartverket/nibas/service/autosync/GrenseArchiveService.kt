package no.kartverket.nibas.service.autosync

import no.kartverket.nibas.api.v1.common.*
import no.kartverket.nibas.api.v1.common.geojson.*
import no.kartverket.nibas.api.v1.common.geojson.metadata.*
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class GrenseArchiveService(
    private val coordinateService: CoordinateService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun buildArchiveFeatures(
        toArchive: List<GrenseToArchive>,
        gyldigFra: LocalDate
    ): List<Feature> {
        return toArchive.map { gta ->
            val grense = gta.grense
            val kontekstEgenskaper = buildKontekstEgenskaper(grense, gyldigFra)

            val roundedCoords = gta.coords.map { coordinateService.roundPoint(it).toList() }

            val feature = Feature(
                id = grense.lokalid,
                type = "Feature",
                geometry = LineString(coordinates = roundedCoords),
                properties = FeatureProperties(
                    type = grense.grensetype.toString(),
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

            logFeatureCoordinates(feature, "ARCHIVE ${grense.grensetype} ${grense.lokalid}")
            feature
        }
    }

    fun buildNewGrenseFeature(
        coordinates: List<Point>,
        kontekstEgenskaper: List<KontekstEgenskaper>,
        worstQuality: WorstM22Posisjonskvalitet,
        gyldigFra: LocalDate,
        grenseType: String
    ): Feature {
        log.info("Creating new grense: {} coords, quality: {}cm",
            coordinates.size, worstQuality.noeyaktighet)

        val roundedCoords = coordinates.map { coordinateService.roundPoint(it).toList() }

        val feature = Feature(
            id = null,
            type = "Feature",
            geometry = LineString(coordinates = roundedCoords),
            properties = FeatureProperties(
                type = grenseType,
                srid = 25833,
                version = 1,
                metadata = AdministrativGrenseMetadata(
                    common = CommonMetadata(
                        identifikasjon = Identifikasjon(lokalid = "NotARealID"),
                        gyldigFra = gyldigFra,
                        informasjon = "${gyldigFra.format(dateFormatter)} - Kvalitetsheving av $grenseType, jf. Arken 25/29245"
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

        logFeatureCoordinates(feature, "NEW $grenseType")
        return feature
    }

    fun buildKontekstEgenskaper(grense: Grense, gyldigFra: LocalDate): List<KontekstEgenskaper> {
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

    fun findMatchingArchiveKontekst(
        segmentStart: Point,
        segmentEnd: Point,
        gapStart: Point,
        gapEnd: Point,
        hjelpepunktInfos: List<HjelpepunktInfo>,
        archiveFeatures: List<Feature>
    ): List<KontekstEgenskaper> {
        val oldStartPoint = when {
            coordinateService.pointsMatch(segmentStart, gapStart) -> gapStart
            else -> hjelpepunktInfos.find { coordinateService.pointsMatch(it.projectedPoint, segmentStart) }?.oldPoint
        }

        val oldEndPoint = when {
            coordinateService.pointsMatch(segmentEnd, gapEnd) -> gapEnd
            else -> hjelpepunktInfos.find { coordinateService.pointsMatch(it.projectedPoint, segmentEnd) }?.oldPoint
        }


        val exactMatch = archiveFeatures.find { archiveFeature ->
            val archiveCoords = when (val geometry = archiveFeature.geometry) {
                is LineString -> geometry.coordinates.map { Point.fromList(it) }
                else -> emptyList()
            }

            if (archiveCoords.isEmpty()) return@find false

            val archiveStart = archiveCoords.first()
            val archiveEnd = archiveCoords.last()

            val sameDirection = oldStartPoint != null && oldEndPoint != null &&
                coordinateService.pointsMatch(archiveStart, oldStartPoint) &&
                coordinateService.pointsMatch(archiveEnd, oldEndPoint)

            val reversedDirection = oldStartPoint != null && oldEndPoint != null &&
                coordinateService.pointsMatch(archiveStart, oldEndPoint) &&
                coordinateService.pointsMatch(archiveEnd, oldStartPoint)

            sameDirection || reversedDirection
        }

        if (exactMatch != null) {
            return exactMatch.properties.kontekstEgenskaper
        }

        log.info("    No exact archive match - collecting from segments between endpoints (merged case)")

        val kontekst = archiveFeatures.flatMap { archiveFeature ->
            val archiveCoords = when (val geometry = archiveFeature.geometry) {
                is LineString -> geometry.coordinates.map { Point.fromList(it) }
                else -> emptyList()
            }

            if (archiveCoords.isEmpty()) return@flatMap emptyList()

            val archiveStart = archiveCoords.first()
            val archiveEnd = archiveCoords.last()

            val connectsToSegment = oldStartPoint != null && oldEndPoint != null && (
                coordinateService.pointsMatch(archiveStart, oldStartPoint) ||
                coordinateService.pointsMatch(archiveEnd, oldStartPoint) ||
                coordinateService.pointsMatch(archiveStart, oldEndPoint) ||
                coordinateService.pointsMatch(archiveEnd, oldEndPoint)
            )

            if (connectsToSegment) {
                archiveFeature.properties.kontekstEgenskaper
            } else {
                emptyList()
            }
        }.distinctBy { it.id?.lokalid }

        return kontekst
    }

    fun buildAffectedGrenseUpdateFeature(
        grense: Grense,
        coords: List<Point>,
        endpointMapping: Map<Point, Point>,
        gyldigFra: LocalDate
    ): Feature {
        val start = coords.first()
        val end = coords.last()


        val newStart = endpointMapping.entries.find { coordinateService.pointsMatch(it.key, start) }?.value
            ?: coordinateService.roundPoint(start)
        val newEnd = endpointMapping.entries.find { coordinateService.pointsMatch(it.key, end) }?.value
            ?: coordinateService.roundPoint(end)

        val kontekstEgenskaper = buildKontekstEgenskaper(grense, gyldigFra)
        val grensetype = when (grense.grensetype) {
            Grensetype.Grunnkretsgrense -> "Grunnkretsgrense"
            Grensetype.Delomraadegrense -> "Delområdegrense"
            Grensetype.Stemmekretsgrense -> "Stemmekretsgrense"
            else -> "Grunnkretsgrense"
        }

        val newCoords = coords.map { coordinateService.roundPoint(it).toList() }.toMutableList()
        newCoords[0] = newStart.toList()
        newCoords[newCoords.size - 1] = newEnd.toList()

        val feature = Feature(
            id = grense.lokalid,
            type = "Feature",
            geometry = LineString(coordinates = newCoords),
            properties = FeatureProperties(
                type = grensetype,
                srid = 25833,
                version = grense.version,
                metadata = AdministrativGrenseMetadata(
                    common = CommonMetadata(
                        identifikasjon = Identifikasjon(lokalid = grense.lokalid),
                        gyldigFra = gyldigFra,
                        informasjon = "${gyldigFra.format(dateFormatter)} - Endepunkt oppdatert for å matche ny kommunegrense, jf. Arken 25/29245"
                    ),
                    commonGrense = CommonGrenseMetadata(
                        posisjonskvalitet = Posisjonskvalitet(
                            maalemetode = KodelisteEntry(id = grense.kvalitet?.maalemetode ?: ""),
                            noeyaktighet = grense.kvalitet?.noeyaktighet ?: 0
                        )
                    )
                ),
                kontekstEgenskaper = kontekstEgenskaper,
                shouldArchive = false
            )
        )

        logFeatureCoordinates(feature, "UPDATE $grensetype ${grense.lokalid}")
        log.info("  Will update {} {}: endpoint moved to match knutepunkt", grensetype, grense.lokalid)
        return feature
    }

    private fun logFeatureCoordinates(feature: Feature, label: String) {
        val coords = (feature.geometry as LineString).coordinates.map { Point.fromList(it) }
        val badCoords = coords.filter { coordinateService.hasTooManyDecimals(it) }

        if (badCoords.isNotEmpty()) {
            log.warn("!!! {} has {} coordinates with too many decimals:", label, badCoords.size)
            badCoords.forEach { coord ->
                log.warn("    Nord: {} Øst: {}", coord.y, coord.x)
            }
        } else {
            log.info("  {} OK: all {} coordinates properly rounded", label, coords.size)
        }
    }

    data class GrenseToArchive(val grense: Grense, val coords: List<Point>)

    data class HjelpepunktInfo(
        val oldPoint: Point,
        val projectedPoint: Point,
        val pathIndex: Int
    )

    data class WorstM22Posisjonskvalitet(
        val maalemetodeId: Int,
        val noeyaktighet: Int
    )
}
