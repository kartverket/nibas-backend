package no.kartverket.nibas.repository

import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.io.geojson.GeoJsonReader
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class EnhetsGrenseRepository(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
    private val rowMapper: RowMapper<Grenserad> =
        RowMapper<Grenserad> { rs: ResultSet, _: Int ->
            val geoJsonReader = GeoJsonReader()
            val geometry: Geometry = geoJsonReader.read(rs.getObject("grense", String::class.java))
            val lineString: LineString = geometry as LineString

            Grenserad(
                id = rs.getString("grense_id"),
                lokalid = Lokalid(rs.getString("grense_lokalid")),
                grense = lineString,
                grenseGyldighet = Gyldighet(
                    gyldigFra = rs.getDate("grensegyldigfra").toLocalDate(),
                    gyldigTil = rs.getDate("grensegyldigtil")?.toLocalDate(),
                    slettet = rs.getBoolean("grenseslettet")),
                grunnkretsGyldighet = Gyldighet(
                    gyldigFra = rs.getDate("grunnkretsgyldigfra").toLocalDate(),
                    gyldigTil = rs.getDate("grunnkretsgyldigtil")?.toLocalDate(),
                    slettet = rs.getBoolean("grunnkretsslettet")))
        }

}

data class Grenserad(
    val id: String,
    val lokalid: Lokalid,
    val grense: LineString,
    val grenseGyldighet: Gyldighet,
    val grunnkretsGyldighet: Gyldighet,
    val slettet: Boolean = false)
