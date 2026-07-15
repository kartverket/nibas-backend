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
            usikker_avgrensning,
            har_usikker_avgrensning,
            forskriftsreferanse, informasjon, url,
            bebygd_eiendom,
            gjelder_for_bolig_ikke_tatt_i_bruk,
            ikke_helaarsbolig_under_oppforing,
            gjelder_for_brukt_som_helarsbolig,
            ubebygd_tomt,
            gjelder_for_ubebygd_boligtomt,
            unntak_fra_slektskapsunntak,
            har_unntak_fra_slektskapsunntak,
            andre_avgrensninger,
            andre_lokale_avgrensninger
        ) VALUES (
            :objid, :lokalid, :navn, :nummer, :gyldigfra, :gyldigtil,
            :datafangstdato, :foerstedigitaliseringsdato,
            :opphav, :endretdato, :endretav, :typeendring,
            st_geomfromewkb(:omraade_wkb),
            st_geomfromewkb(:omraade_posisjon),
            :kommunelokalid, :version, :slettet,
            :usikkerAvgrensning,
            :harUsikkerAvgrensning,
            :forskriftsreferanse, :informasjon, :url,
            :bebygdEiendom,
            :gjelderForBoligIkkeTattIBruk,
            :ikkeHelaarsboligUnderOppforing,
            :gjelderForBruktSomHelarsbolig,
            :ubebygdTomt,
            :gjelderForUbebygdBoligtomt,
            :unntakFraSlektskapsunntak,
            :harUnntakFraSlektskapsunntak,
            :andreAvgrensninger,
            :andreLokaleAvgrensninger
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
            "usikkerAvgrensning" to row.usikkerAvgrensning,
            "harUsikkerAvgrensning" to row.harUsikkerAvgrensning,
            "forskriftsreferanse" to row.forskriftsreferanse,
            "informasjon" to row.informasjon,
            "url" to row.url,
            "bebygdEiendom" to row.bebygdEiendom,
            "gjelderForBoligIkkeTattIBruk" to row.gjelderForBoligIkkeTattIBruk,
            "ikkeHelaarsboligUnderOppforing" to row.ikkeHelaarsboligUnderOppforing,
            "gjelderForBruktSomHelarsbolig" to row.gjelderForBruktSomHelarsbolig,
            "ubebygdTomt" to row.ubebygdTomt,
            "gjelderForUbebygdBoligtomt" to row.gjelderForUbebygdBoligtomt,
            "unntakFraSlektskapsunntak" to row.unntakFraSlektskapsunntak,
            "harUnntakFraSlektskapsunntak" to row.harUnntakFraSlektskapsunntak,
            "andreAvgrensninger" to row.andreAvgrensninger,
            "andreLokaleAvgrensninger" to row.andreLokaleAvgrensninger
        )

        jdbc.update(sql, params)
    }
}
