package no.kartverket.nibas.importjob.boplikt

import org.locationtech.jts.io.WKBWriter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class BopliktRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {

    fun truncateCascade() {
        jdbc.jdbcTemplate.execute("TRUNCATE TABLE bopliktomraade CASCADE")
    }

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
            kommunelokalid, version, slettet,
            forskriftsreferanse, informasjon, url,
            bebygd_eiendom,
            ikke_helaarsbolig_under_oppforing,
            ubebygd_tomt,
            unntak_fra_slektskapsunntak,
            andre_avgrensninger
        ) VALUES (
            :objid, :lokalid, :navn, :nummer, :gyldigfra, :gyldigtil,
            :datafangstdato, :foerstedigitaliseringsdato,
            :opphav, :endretdato, :endretav, :typeendring,
            st_geomfromewkb(:omraade_wkb),
            st_geomfromewkb(:omraade_posisjon),
            :kommunelokalid, :version, :slettet,
            :forskriftsreferanse, :informasjon, :url,
            :bebygdEiendom,
            :ikkeHelaarsboligUnderOppforing,
            :ubebygdTomt,
            :unntakFraSlektskapsunntak,
            :andreAvgrensninger
        )
    """.trimIndent()

        val wkbWriter = WKBWriter(2, true)
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
            "omraade_wkb" to row.omraadeGeo
                ?.let { wkbWriter.write(it) },
            "omraade_posisjon" to row.posisjonGeo
                .let { wkbWriter.write(it) },
            "kommunelokalid" to row.kommunelokalid,
            "version" to row.version,
            "slettet" to row.slettet,
            "forskriftsreferanse" to row.forskriftsreferanse,
            "informasjon" to row.informasjon,
            "url" to row.url,
            "bebygdEiendom" to row.bebygdEiendom,
            "ikkeHelaarsboligUnderOppforing" to row.ikkeHelaarsboligUnderOppforing,
            "ubebygdTomt" to row.ubebygdTomt,
            "unntakFraSlektskapsunntak" to row.unntakFraSlektskapsunntak,
            "andreAvgrensninger" to row.andreAvgrensninger
        )

        jdbc.update(sql, params)
    }
}
