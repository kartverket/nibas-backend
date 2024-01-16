package no.kartverket.nibas.repository

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.Lokalid
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
            if (rs.wasNull()) { //Siste kolonne var null
                hullIndeks = null
            }

            Grenserad(
                id = rs.getString("grense_id"),
                lokalid = Lokalid(rs.getString("grense_lokalid")),
                grense = lineString,
                grenseGyldighet = Gyldighet(
                    gyldigFra = rs.getDate("grensegyldigfra").toLocalDate(),
                    gyldigTil = rs.getDate("grensegyldigtil")?.toLocalDate(),
                    slettet = rs.getBoolean("grenseslettet")),
                flateIndeks = rs.getInt("flate_indeks"),
                hullIndeks = hullIndeks,
                grunnkretsGyldighet = Gyldighet(
                    gyldigFra = rs.getDate("grunnkretsgyldigfra").toLocalDate(),
                    gyldigTil = rs.getDate("grunnkretsgyldigtil")?.toLocalDate(),
                    slettet = rs.getBoolean("grunnkretsslettet")))
        }

    /**
     * Søker i basen etter grensekoblinger knyttet til grunnkrets med grunnkretsnummer som starter med enhetsnummer:
     * Dvs starter med kommunenummer for kommuner, starter med fylkesnummer for fylker og ingen begrensninger for nasjon
     */
    fun hentGrenserMedRiktigRetning(enhetsnummer: String, tillatteGrensetyper: Set<Grensetype>, gyldighetsdato: LocalDate = LocalDate.now()): List<Grenserad> {
        val grensetyper = tillatteGrensetyper.map { it.value }
        val parameters: Map<String, Any>

        var customSql = "select st_asgeojson(g.grense) as grense, gg.retning_med_klokken, gg.flate_indeks, gg.hull_indeks, g.objid as grense_id, " +
            "g.lokalid as grense_lokalid, g.gyldigfra as grensegyldigfra, g.gyldigtil as grensegyldigtil, g.slettet as grenseslettet, " +
            "gk.gyldigfra as grunnkretsgyldigfra, gk.gyldigtil as grunnkretsgyldigtil, gk.slettet as grunnkretsslettet " +
            "from grunnkrets gk " +
            "left join grunnkrets_grense gg on gk.objid = gg.grunnkrets_fk " +
            "left join grense g on gg.grense_fk = g.objid " +
            "where g.avgrensningstype in (:grensetyper) "
        if (enhetsnummer.isNotBlank()) {
            customSql += " and gk.grunnkretsnummer like :enhetsnummer"
            parameters = mapOf(
                "grensetyper" to grensetyper,
                "enhetsnummer" to "$enhetsnummer%")
        } else {
            parameters = mapOf(
                "grensetyper" to grensetyper)
        }

        val grunnkretsTilGrenseKoblinger = namedParameterJdbcTemplate.query(customSql, parameters, rowMapper)

        // For hver av de: Hvis gyldig på dato, returnere denne
        val grenseRader: List<Grenserad> = grunnkretsTilGrenseKoblinger
            .filter { it.grenseGyldighet.gyldigPaaDato(gyldighetsdato) }
            .filter { it.grunnkretsGyldighet.gyldigPaaDato(gyldighetsdato) }

        return grenseRader
    }

}

data class Grenserad(
    val id: String,
    val lokalid: Lokalid,
    val grense: LineString,
    val grenseGyldighet: Gyldighet,
    val grunnkretsGyldighet: Gyldighet,
    val flateIndeks: Int = 0,
    val hullIndeks: Int? = null,
    val slettet: Boolean = false)
