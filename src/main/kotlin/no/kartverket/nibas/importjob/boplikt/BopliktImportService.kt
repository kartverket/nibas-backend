package no.kartverket.nibas.importjob.boplikt

import com.fasterxml.jackson.databind.ObjectMapper
import no.kartverket.nibas.NibasBackendApplication
import no.kartverket.nibas.importjob.geojson.GeoJsonFeature
import no.kartverket.nibas.importjob.geojson.GeoJsonReader
import no.kartverket.nibas.service.KommuneService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
@Profile("bopliktimport")
class BopliktImportService(
    private val geoJsonReader: GeoJsonReader,
    private val objectMapper: ObjectMapper,
    private val repository: BopliktRepository,
    private val kommuneService: KommuneService
) {
    private val logger = LoggerFactory.getLogger(NibasBackendApplication::class.java)
    private val importDate = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourcePath = "import/bopliktsomraade.geojson"

    @EventListener(ApplicationReadyEvent::class)
    fun runImportOnStartup() {
        logger.info("Starting boplikt import job from resource '{}'", resourcePath)

        if (!repository.isEmpty()) {
            logger.info("Table 'bopliktomraade' not empty, skipping import")
            return
        }

        val imported = importFromLocalResource()
        logger.info("Completed boplikt import job — {} rows imported", imported)
    }

    fun importFromLocalResource(): Int {
        val featureCollection = geoJsonReader.readFeatureCollectionFromClasspath(resourcePath)

        var count = 0
        featureCollection.features.forEach { feature ->
            val row = mapFeatureToRow(feature)
            repository.insert(row)
            count++
        }

        return count
    }

    private fun mapFeatureToRow(feature: GeoJsonFeature): BopliktOmraadeRow {
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

        val kommunenummer = prop("kommunenummer")?.toString()
        val kommunelokalid = kommunenummer?.let {
            kommuneService
                .hentKommuneByKommunenummer(it, LocalDate.now())
                ?.kommuneLokalid
                ?.lokalid
        }

        val geometryJson = objectMapper.writeValueAsString(
            mapOf(
                "type" to feature.geometry.type,
                "coordinates" to feature.geometry.coordinates
            )
        )

        return BopliktOmraadeRow(
            objid = objid,
            lokalid = lokalid,
            gyldigfra = gyldigfra,
            gyldigtil = gyldigtil,
            datafangstdato = datafangstdato,
            foerstedigitaliseringsdato = foerstedigitaliseringsdato,
            opphav = opphav,
            endretdato = endretdato,
            endretav = endretav,
            typeendring = typeendring,
            omraadeGeoJson = geometryJson,
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
}
