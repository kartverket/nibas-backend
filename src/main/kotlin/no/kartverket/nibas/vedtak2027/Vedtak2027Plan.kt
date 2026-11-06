package no.kartverket.nibas.vedtak2027

import java.time.LocalDate

/**
 * Plan for engangsjobben som flytter én grunnkrets, og stemmekretsen som dekker nøyaktig samme
 * område, til en annen kommune i samme fylke. Jobben finner objektene selv ut fra numrene.
 */
data class Vedtak2027Plan(
    /** Datoen endringen gjelder fra. Gamle versjoner får dette som gyldigTil, nye som gyldigFra. */
    val virkningsdato: LocalDate,
    val fraKommunenummer: String,
    val tilKommunenummer: String,
    val kretsnavn: String,
    val grunnkretsnummer: String,
    val nyttGrunnkretsnummer: String,
    val stemmekretsnummer: String,
    val nyttStemmekretsnummer: String,
    val nyttStemmekretsnavn: String,
    val arealToleranseM2: Double,
) {
    companion object {
        val LIABYGDA = Vedtak2027Plan(
            virkningsdato = LocalDate.of(2027, 1, 1),
            fraKommunenummer = "1525",
            tilKommunenummer = "1578",
            kretsnavn = "Liabygda",
            grunnkretsnummer = "15250101",
            nyttGrunnkretsnummer = "15780212",
            stemmekretsnummer = "01",
            nyttStemmekretsnummer = "4",
            nyttStemmekretsnavn = "LIABYGDA",
            arealToleranseM2 = 1.0,
        )
    }
}
