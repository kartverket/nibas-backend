package no.kartverket.nibas.repository

import no.kartverket.nibas.api.v1.common.Flatetype
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.io.ParseException
import org.locationtech.jts.io.WKBReader
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate


@Repository
class InndelingSearchRepository(val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun finnGrunnkretser(gyldighetsdato: LocalDate, searchString: String, limit: Int): List<Inndeling> {
        val query = """
                select lokalid, grunnkretsnavn as navn, grunnkretsnummer as nummer, posisjon as representasjonspunkt from grunnkrets g
                where gyldigfra <= (:gyldighetsdato) and (gyldigtil > (:gyldighetsdato) or gyldigtil is null) and slettet = false 
                and (grunnkretsnavn ilike CONCAT(:searchString, '%') or grunnkretsnummer ilike CONCAT(:searchString, '%'))
                limit :limit
            """.trimIndent()

        return jdbcTemplate.query(query, mapOf("gyldighetsdato" to gyldighetsdato, "searchString" to searchString, "limit" to limit), InndelingRowMapper(Flatetype.GRUNNKRETS))
    }

    fun finnStemmekretser(gyldighetsdato: LocalDate, searchString: String, limit: Int): List<Inndeling> {
        val query = """
                select lokalid, stemmekretsnavn as navn, stemmekretsnummer as nummer, posisjon as representasjonspunkt from stemmekrets s
                where gyldigfra <= (:gyldighetsdato) and (gyldigtil > (:gyldighetsdato) or gyldigtil is null) and slettet = false 
                and (stemmekretsnavn ilike CONCAT(:searchString, '%') or stemmekretsnummer ilike CONCAT(:searchString, '%'))
                limit :limit
            """.trimIndent()

        return jdbcTemplate.query(query, mapOf("gyldighetsdato" to gyldighetsdato, "searchString" to searchString, "limit" to limit), InndelingRowMapper(Flatetype.STEMMEKRETS))
    }

    fun finnKommuner(gyldighetsdato: LocalDate, searchString: String, limit: Int): List<Inndeling> {
        val query = """
                WITH gyldige_administrativenhetnavn AS (
                    SELECT STRING_AGG(a.navn, ', ') AS navn, administrativenhetlokalid AS fk
                    FROM administrativenhetnavn a
                    WHERE gyldigfra <= :gyldighetsdato
                      AND (gyldigtil > :gyldighetsdato OR gyldigtil IS NULL)
                      AND slettet = false
                    GROUP BY fk
                )
                SELECT k.lokalid, a.navn, k.kommunenummer as nummer, posisjon as representasjonspunkt
                FROM kommune k
                JOIN gyldige_administrativenhetnavn a ON k.lokalid = a.fk
                WHERE gyldigfra <= :gyldighetsdato
                  AND (gyldigtil > :gyldighetsdato OR gyldigtil IS NULL)
                  AND slettet = false
                  AND (a.navn ILIKE CONCAT(:searchString, '%') OR k.kommunenummer ILIKE CONCAT(:searchString, '%'))
                LIMIT :limit
            """.trimIndent()
        return jdbcTemplate.query(query, mapOf("gyldighetsdato" to gyldighetsdato, "searchString" to searchString, "limit" to limit), InndelingRowMapper(Flatetype.KOMMUNE))
    }

    fun finnFylker(gyldighetsdato: LocalDate, searchString: String, limit: Int): List<Inndeling> {
        val query = """
                WITH gyldige_administrativenhetnavn AS (
                    SELECT STRING_AGG(a.navn, ', ') AS navn, administrativenhetlokalid AS fk
                    FROM administrativenhetnavn a
                    WHERE gyldigfra <= :gyldighetsdato
                      AND (gyldigtil > :gyldighetsdato OR gyldigtil IS NULL)
                      AND slettet = false
                    GROUP BY fk
                )
                SELECT f.lokalid, a.navn, f.fylkesnummer as nummer, posisjon as representasjonspunkt
                FROM fylke f
                JOIN gyldige_administrativenhetnavn a ON f.lokalid = a.fk
                WHERE gyldigfra <= :gyldighetsdato
                  AND (gyldigtil > :gyldighetsdato OR gyldigtil IS NULL)
                  AND slettet = false
                  AND (a.navn ILIKE CONCAT(:searchString, '%') OR f.fylkesnummer ILIKE CONCAT(:searchString, '%'))
                LIMIT :limit
            """.trimIndent()
        return jdbcTemplate.query(query, mapOf("gyldighetsdato" to gyldighetsdato, "searchString" to searchString, "limit" to limit), InndelingRowMapper(Flatetype.FYLKE))
    }

    data class Inndeling (
        val id: String,
        val type: Flatetype,
        val navn: String,
        val nummer: String,
        val representasjonspunkt: Coordinate
    )

    private class InndelingRowMapper(val type: Flatetype) : RowMapper<Inndeling> {

        override fun mapRow(rs: ResultSet, rowNum: Int): Inndeling {
            val representasjonspunktWKBString = rs.getString("representasjonspunkt")
            return Inndeling(
                id = rs.getString("lokalid"),
                type = type,
                navn = rs.getString("navn"),
                nummer = rs.getString("nummer"),
                representasjonspunkt = parseJtsPointFromWKBString(representasjonspunktWKBString)?.coordinate
                    ?: throw ParseException("Klarte ikke parse $representasjonspunktWKBString til JTS Point")
            )
        }
        fun parseJtsPointFromWKBString(wkbString: String): Point? {
            return try {
                val wkbBytes = hexStringToByteArray(wkbString)
                val geometryFactory = GeometryFactory()
                val wkbReader = WKBReader(geometryFactory)
                val geometry = wkbReader.read(wkbBytes)
                if (geometry is Point) {
                    geometry
                } else null
            } catch (e: Exception) {
                null
            }
        }

        fun hexStringToByteArray(hexString: String): ByteArray {
            val len = hexString.length
            require(len % 2 == 0) { "Hex-stringen sin lengde må være jevn" }
            val byteArray = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                byteArray[i / 2] = ((Character.digit(hexString[i], 16) shl 4) +
                    Character.digit(hexString[i + 1], 16)).toByte()
                i += 2
            }
            return byteArray
        }
    }
}


