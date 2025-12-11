package no.kartverket.nibas.importjob.boplikt

import com.fasterxml.jackson.databind.ObjectMapper
import no.kartverket.nibas.importjob.geojson.GeoJsonFeature
import no.kartverket.nibas.importjob.geojson.GeoJsonReader
import no.kartverket.nibas.service.KommuneService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class BopliktImportService(
    private val geoJsonReader: GeoJsonReader,
    private val objectMapper: ObjectMapper,
    private val repository: BopliktRepository,
    private val kommuneService: KommuneService
) {
    private val importDate = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourcePath = "import/bopliktsomraade.geojson"

    fun importFromLocalResource(): Int {
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

        val navn = "Bopliktområde"

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
            navn = navn,
            nummer = nummerStr,
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
