package no.kartverket.nibas.repository

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.io.geojson.GeoJsonReader
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate

@Repository
class EnhetsGrenseRepository(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
    private val rowMapper: RowMapper<Grenserad> =
        RowMapper<Grenserad> { rs: ResultSet, _: Int ->
            val geoJsonReader = GeoJsonReader()
            val geometry: Geometry = geoJsonReader.read(rs.getObject("grense", String::class.java))
            val lineString: LineString =
                when (rs.getBoolean("retning_med_klokken")) {
                    true -> geometry as LineString
                    false -> geometry.reverse() as LineString
                }

            var hullIndeks: Int? = rs.getInt("hull_indeks")
            if (rs.wasNull()) {
                hullIndeks = null
            }

            Grenserad(
                id = rs.getString("grense_id"),
                lokalid = Lokalid(rs.getString("grense_lokalid")),
                grense = lineString,
                gyldighet = Gyldighet(
                    gyldigFra = rs.getDate("gyldigfra").toLocalDate(),
                    gyldigTil = rs.getDate("gyldigtil")?.toLocalDate(),
                    slettet = rs.getBoolean("slettet")),
                flateIndeks = rs.getInt("flate_indeks"),
                hullIndeks = hullIndeks,
                nesteVersjon = rs.getString("neste_versjon"))
        }

    fun hentGrenserMedRiktigRetning(enhetsnummer: String, tillatteGrensetyper: Set<Grensetype>, gyldighetsdato: LocalDate = LocalDate.now()): List<Grenserad> {
        val grensetyper = tillatteGrensetyper.map { it.value }
        val parameters = mapOf(
            "enhetsnummer" to "$enhetsnummer%",
            "grensetyper" to grensetyper
        )

        val customSql = """
            select st_asgeojson(g.grense) as grense, gg.retning_med_klokken, gg.flate_indeks, gg.hull_indeks, g.objid as grense_id, g.lokalid as grense_lokalid, g.gyldigfra, g.gyldigtil, 
            g.slettet, g.neste_versjon
            from grunnkrets gk
            left join grunnkrets_grense gg on gk.objid = gg.grunnkrets_fk
            left join grense g on gg.grense_fk = g.objid
            where gk.grunnkretsnummer like :enhetsnummer
            and g.avgrensningstype in (:grensetyper)
        """.trimIndent()
        val grunnkretsTilGrenseKoblinger = namedParameterJdbcTemplate.query(customSql, parameters, rowMapper)

        // Finne unike grense_fk i tuplene her
        // For hver av de: Hvis gyldig på dato, returnere denne
        // ELLERS: Følge neste_versjon til gyldig rad
        val grenseRader = grunnkretsTilGrenseKoblinger
            .distinctBy { it.id }
            .map {
                it.finnRelevantGrenseForDato(gyldighetsdato, this, enhetsnummer)
                    ?: throw RuntimeException("Fant ingen relevant grense for grenseid: $it.id og gyldighetsdato: $gyldighetsdato")
            }

        // For hver grenseendring man gjør på en grense vil man få en ny versjon av denne grensa.
        // Når man finner relevant grense for dato for de gamle grensene vil man få den nyeste grensa like mange ganger som man da har gamle grenser.
        // Denne kommer derfor like mange ganger i lista som man har versjoner av grensa og vi må gjøre en ny distinctBy her.
        return grenseRader.distinctBy { it.id }
    }

    fun fetchGrenseRow(grenseId: String, enhetsnummer: String): Grenserad {
        val grenseByIdQuery = """
            select g.objid as grense_id, g.lokalid as grense_lokalid, st_asgeojson(g.grense) as grense, gg.retning_med_klokken, gg.flate_indeks, gg.hull_indeks, g.gyldigfra, g.gyldigtil, 
            g.slettet, g.neste_versjon
            from grunnkrets gk 
            left join grunnkrets_grense gg on gk.objid = gg.grunnkrets_fk
            left join grense g on gg.grense_fk = g.objid
            where g.objid = :grenseId
            and gk.grunnkretsnummer like :enhetsnummer
        """.trimIndent()

        val grenserader = namedParameterJdbcTemplate.query(grenseByIdQuery, mapOf("grenseId" to grenseId, "enhetsnummer" to "$enhetsnummer%"), rowMapper)

        // Man får flere grenser når har endret en grense og det dannes flere versjoner av kretsen.
        // Siden koblingen er mellom krets og grense vil man med spørringen her få ut like mange grenser som det er versjoner av kretsen.
        val distinkteGrenser = grenserader.distinctBy { it.id }
        if (distinkteGrenser.size > 1) {
            throw GrenseKobling.RelevantGrenseNotFoundEx("Fant flere grenser for $grenseId")
        }
        return distinkteGrenser.first()
    }
}

data class Grenserad(
    val id: String,
    val lokalid: Lokalid,
    val grense: LineString,
    val gyldighet: Gyldighet,
    val flateIndeks: Int = 0,
    val hullIndeks: Int? = null,
    val slettet: Boolean = false,
    val nesteVersjon: String? = null) {
    fun finnRelevantGrenseForDato(gyldighetsdato: LocalDate = LocalDate.now(), enhetsGrenseRepository: EnhetsGrenseRepository, enhetsnummer: String): Grenserad? {
        var grenseradToCheck: Grenserad? = this
        var result: Grenserad? = null
        while (result == null && grenseradToCheck != null) {
            if (grenseradToCheck.gyldigPaaDato(gyldighetsdato)) {
                result = grenseradToCheck
            }
            val nesteVersjon = grenseradToCheck.nesteVersjon
            grenseradToCheck = nesteVersjon?.let { enhetsGrenseRepository.fetchGrenseRow(it, enhetsnummer) }
        }
        return result
    }

    private fun gyldigPaaDato(dato: LocalDate): Boolean {
        return if (!gyldighet.slettet) {
            val dagenEtterDato = dato.plusDays(1)
            val gyldigTil = gyldighet.gyldigTil
            gyldighet.gyldigFra.isBefore(dagenEtterDato) && (gyldigTil == null || gyldigTil.isAfter(dato))
        } else {
            false
        }
    }
}
