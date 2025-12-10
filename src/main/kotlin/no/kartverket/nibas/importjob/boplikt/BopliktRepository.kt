package no.kartverket.nibas.importjob.boplikt

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class BopliktRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {

    fun isEmpty(): Boolean {
        val sql = "SELECT COUNT(*) = 0 FROM bopliktomraade"
        return jdbc.queryForObject(sql, emptyMap<String, Any>(), Boolean::class.java)!!
    }

    fun insert(row: BopliktOmraadeRow) {
        val sql = """
        INSERT INTO bopliktomraade (
            objid, lokalid, navn, nummer, gyldigfra, gyldigtil,
            datafangstdato, foerstedigitaliseringsdato,
            opphav, endretdato, endretav, typeendring,
            omraade, posisjon,
            kommunelokalid, delvis_boplikt, version, slettet,
            forskriftsreferanse, informasjon, url
        ) VALUES (
            :objid, :lokalid, :navn, :nummer, :gyldigfra, :gyldigtil,
            :datafangstdato, :foerstedigitaliseringsdato,
            :opphav, :endretdato, :endretav, :typeendring,
            ST_SetSRID(ST_GeomFromGeoJSON(:omraade_geojson), 25833),
            ST_SetSRID(
                ST_PointOnSurface(ST_GeomFromGeoJSON(:omraade_geojson)),
                25833
            ),
            :kommunelokalid, :delvisBoplikt, :version, :slettet,
            :forskriftsreferanse, :informasjon, :url
        )
    """.trimIndent()

        val params = mapOf(
            "objid" to row.objid,
            "lokalid" to row.lokalid,
            "navn" to row.navn,
            "nummer" to row.nummer,
            "gyldigfra" to row.gyldigfra,
            "gyldigtil" to row.gyldigtil,
            "datafangstdato" to row.datafangstdato,
            "foerstedigitaliseringsdato" to row.foerstedigitaliseringsdato,
            "opphav" to row.opphav,
            "endretdato" to row.endretdato,
            "endretav" to row.endretav,
            "typeendring" to row.typeendring,
            "omraade_geojson" to row.omraadeGeoJson,
            "kommunelokalid" to row.kommunelokalid,
            "delvisBoplikt" to row.delvisBoplikt,
            "version" to row.version,
            "slettet" to row.slettet,
            "forskriftsreferanse" to row.forskriftsreferanse,
            "informasjon" to row.informasjon,
            "url" to row.url
        )

        jdbc.update(sql, params)
    }

}
