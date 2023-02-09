package no.kartverket.nibas.repository

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class FlatedanningCustomSQLRepository(val jdbcTemplate: JdbcTemplate) {

    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun slettGrenserUtenUtstrekning() {
        val slettStemmekretsKoblingerForGrenserUtenUtstrekning = """
            delete from stemmekrets_grense where grense_fk in (
            	select objid from grense where st_length(grense) = 0
            )
        """.trimIndent()

        val slettGrunnkretsKoblingerForGrenserUtenUtstrekning = """
            delete from grunnkrets_grense where grense_fk in (
            	select objid from grense where st_length(grense) = 0
            )
        """.trimIndent()

        val slettGrenserUtenUtstrekning = "delete from grense where st_length(grense) = 0"

        val antallSlettedeStemmekretsKoblinger = jdbcTemplate.update(slettStemmekretsKoblingerForGrenserUtenUtstrekning)
        val antallSlettedeGrunnkretsKoblinger = jdbcTemplate.update(slettGrunnkretsKoblingerForGrenserUtenUtstrekning)
        val antallSlettedeGrenser = jdbcTemplate.update(slettGrenserUtenUtstrekning)

        logger.info("Slettet $antallSlettedeStemmekretsKoblinger stemmekretskoblinger, $antallSlettedeGrunnkretsKoblinger grunnkretskoblinger og $antallSlettedeGrenser grenser uten utstrekning")
    }

}
