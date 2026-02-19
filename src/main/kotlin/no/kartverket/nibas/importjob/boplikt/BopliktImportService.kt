package no.kartverket.nibas.importjob.boplikt

import no.kartverket.nibas.common.TransactionScope
import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.common.round
import no.kartverket.nibas.domain.Bopliktomraade.Companion.KOMMUNE_BOUNDARY_BUFFER_METER
import no.kartverket.nibas.importjob.geojson.GeoJsonFeature
import no.kartverket.nibas.importjob.geojson.GeoJsonReader
import no.kartverket.nibas.importjob.geojson.toJTS
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.TransactionTimeService
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.operation.overlay.snap.GeometrySnapper
import org.locationtech.jts.precision.GeometryPrecisionReducer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*


@Service
@Transactional
@TransactionScope
class BopliktImportService(
    private val geoJsonReader: GeoJsonReader,
    private val repository: BopliktRepository,
    private val kommuneService: KommuneService,
    transactionTimeService: TransactionTimeService
) {
    private val resourcePath = "import/2026-02-06_boplikt_simple_example.geojson"
    private val now: Instant = transactionTimeService.transactionTime
    private val importDate = now.atOffset(ZoneOffset.UTC)
    private val today: LocalDate = now.atZone(ZoneId.of("Europe/Oslo")).toLocalDate()

    fun importFromLocalResource(): Int {
        if(!repository.isEmpty()) {
            return 0
        }
        repository.truncateCascade()
        val featureCollection = geoJsonReader.readFeatureCollectionFromClasspath(resourcePath)

        var count = 0
        val nextNrPerKommune = mutableMapOf<String, Int>()
        featureCollection.features.forEach { feature ->
            val kommunenummer = feature.properties["kommunenummer"]?.toString()?.trim()
                ?: error("Manglende 'kommunenummer' i : $feature")
            val next = (nextNrPerKommune[kommunenummer] ?: 0) + 1
            nextNrPerKommune[kommunenummer] = next
            val nummerStr = String.format("%02d", next)
            val row = mapFeatureToRow(feature, nummerStr)
            repository.insert(row)
            count++
        }
        return count
    }

    private fun mapFeatureToRow(feature: GeoJsonFeature, nummerStr: String): BopliktOmraadeRow {
        val p = feature.properties

        fun prop(name: String): Any? = p[name]

        val objid = UUID.randomUUID().toString()
        val lokalid = UUID.randomUUID().toString()

        val gyldigfra = parseDate(prop("gjelderFra"))
        val gyldigtil = parseDateOrNull(prop("gjelderTil"))

        val datafangstdato = gyldigfra
        val foerstedigitaliseringsdato = importDate

        val opphav = prop("opphav")?.toString()
        val endretdato = importDate
        val endretav = "boplikt-import"
        val typeendring = "import"

        val delvisBoplikt =
            (prop("delvis_boplikt") as? Number)?.toInt() == 1

        val forskriftsreferanse = prop("forskriftsreferanse")?.toString()
        val informasjon = prop("utfyllendeBeskrivelse")?.toString()
        val url = prop("url")?.toString()

        // Hvis gyldigFra < 2024-01-01, bruk 2024-01-01; ellers bruk gyldigFra
        val gyldigFraDefaultDate = LocalDate.of(2024, 1, 1)
        var gyldigFraDate = gyldigfra.toLocalDate()
        if (gyldigFraDate.isBefore(gyldigFraDefaultDate)) {
            gyldigFraDate = gyldigFraDefaultDate
        }
        val kommunenummer = prop("kommunenummer")?.toString()
        require(gyldigtil == null) {
            "Det er ikke støtte for å legge inn utgåtte bopliktkområder"
        }
        val kommune = kommunenummer
            ?.let { kommuneService.hentKommuneByKommunenummer(it, today) }
            ?: throw IllegalStateException("Kommune not found: $kommunenummer@$today")
        val kommunelokalid = kommune.lokalid
        val kommunenavn = kommune.getAdministrativenhetNavn(today).getInSpraak("nor")
        val navn = "Bopliktområde i $kommunenavn"
        val kommuneOmraade = kommune.omraade
            ?: throw IllegalStateException("Kommune $kommunenummer $kommunenavn har ikke geometri")

        if (gyldigFraDate.isBefore(kommune.gyldighet.gyldigFra)) {
            log.warn(
                "Bopliktområde for {} {} har gyldigFra {}, som er før kommunen sin gyldigFra. Setter gyldigFra til {}",
                kommunenummer, kommunenavn, gyldigFraDate, kommune.gyldighet.gyldigFra
            )
            gyldigFraDate = kommune.gyldighet.gyldigFra
        }

        val originalGeometry = feature.geometry.toJTS(UTM33_SRID)

        val fixedGeometry = originalGeometry
            .let { GeometryPrecisionReducer.reduce(it, halfCmModel) }
            .let { GeometrySnapper(it).snapTo(
                kommuneOmraade,
                MIN_DISTANCE_KOMMUNE_BOUNDARY_METER)
            }
            .intersection(kommuneOmraade)

        check(kommuneOmraade.buffer(KOMMUNE_BOUNDARY_BUFFER_METER).covers(fixedGeometry)) {
            "Kommune $kommunenummer $kommunenavn dekker ikke bopliktsområde"
        }

        // Etter geometrien er fikset på, risikerer vi å sitte igjen med både
        // polygoner (som ikke utgjør multipolygon) og linjer (veeeldig smale avvik),
        // men vi vil bare ha multipolygoner
        val gf = GeometryFactory(PrecisionModel(), UTM33_SRID)
        val bopliktomraade = when (fixedGeometry.geometryType) {
            Geometry.TYPENAME_POLYGON -> gf.createMultiPolygon(arrayOf(fixedGeometry as Polygon))
            Geometry.TYPENAME_MULTIPOLYGON -> fixedGeometry as MultiPolygon
            Geometry.TYPENAME_GEOMETRYCOLLECTION -> {
                (0 until fixedGeometry.numGeometries).map { fixedGeometry.getGeometryN(it) }.flatMap { when (it.geometryType) {
                    Geometry.TYPENAME_POLYGON -> listOf(it as Polygon)
                    Geometry.TYPENAME_MULTIPOLYGON -> {
                        (0 until it.numGeometries).map { n -> it.getGeometryN(n) as Polygon }
                    }
                    else -> {
                        log.warn("Ignorerer ${it.geometryType} i (justert) bopliktområdegeometri for $kommunenummer $kommunenavn: ${it.toText()}")
                        emptyList()
                    }
                }
                }.toTypedArray().let {
                    check(it.isNotEmpty()) {
                        "Ingen polygoner i (justert) bopliktområdegeometri for $kommunenummer $kommunenavn"
                    }
                    gf.createMultiPolygon(it)
                }
            }
            else -> throw IllegalStateException("Geometry not supported: ${fixedGeometry.geometryType}")
        }

        if (!originalGeometry.equalsNorm(bopliktomraade)) {
            val diff = originalGeometry.symDifference(bopliktomraade)
            val area = diff.area
            log.warn("Bopliktområde for $kommunenummer $kommunenavn måtte justeres med ${area.round(3)}m2 før import")
        }

        return BopliktOmraadeRow(
            objid = objid,
            lokalid = lokalid,
            navn = navn,
            nummer = nummerStr,
            gyldigfra = parseDate(gyldigFraDate),
            gyldigtil = gyldigtil,
            datafangstdato = datafangstdato,
            foerstedigitaliseringsdato = foerstedigitaliseringsdato,
            opphav = opphav,
            endretdato = endretdato,
            endretav = endretav,
            typeendring = typeendring,
            omraadeGeo = bopliktomraade,
            kommunelokalid = kommunelokalid,
            delvisBoplikt = delvisBoplikt,
            version = 1,
            slettet = false,
            forskriftsreferanse = forskriftsreferanse,
            informasjon = informasjon,
            url = url
        )
    }

    private fun parseDate(value: Any?): OffsetDateTime {
        val s = value?.toString() ?: error("dato mangler")
        val local = LocalDate.parse(s)
        return local.atStartOfDay().atOffset(ZoneOffset.UTC)
    }

    private fun parseDateOrNull(value: Any?): OffsetDateTime? {
        val s = value?.toString() ?: return null
        if (s.isBlank()) return null
        val local = LocalDate.parse(s)
        return local.atStartOfDay().atOffset(ZoneOffset.UTC)
    }

    companion object {
        private const val MIN_DISTANCE_KOMMUNE_BOUNDARY_METER = 0.1
        private const val UTM33_SRID = 25833
        private val halfCmModel = PrecisionModel(200.0)
        private val log = LoggerFactory.getLogger(BopliktImportService::class.java)
    }
}
