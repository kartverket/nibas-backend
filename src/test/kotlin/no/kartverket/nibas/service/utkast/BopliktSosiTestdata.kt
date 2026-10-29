package no.kartverket.nibas.service.utkast

import no.kartverket.nibas.sosi.deserializer.NibasSosiDeserializer
import java.io.InputStream

/**
 * En liten SOSI-fil i UTM 32 med bopliktområdegrenser
 * yttergrensa av to kvadrater på 100 x 100 m som ligger inntil hverandre (A og B), og en halvsirkel med radius 50 m (C)
 * for seg selv. C er en rett linje og en BUEP. Som filene fra kommunene har den ingen skillelinje mellom A og B.
 */
object BopliktSosiTestdata {
    const val FILNAVN = "bopliktomraader.sos"

    private data class Linje(val geometritype: String, val lokalid: String, val nordOest: List<Pair<Int, Int>>)

    private val linjer = listOf(
        Linje("KURVE", "a-vest", listOf(6460000 to 372000, 6460100 to 372000)),
        Linje("KURVE", "a-nord", listOf(6460100 to 372000, 6460100 to 372100)),
        Linje("KURVE", "b-nord", listOf(6460100 to 372100, 6460100 to 372200)),
        Linje("KURVE", "b-oest", listOf(6460100 to 372200, 6460000 to 372200)),
        Linje("KURVE", "b-soer", listOf(6460000 to 372200, 6460000 to 372100)),
        Linje("KURVE", "a-soer", listOf(6460000 to 372100, 6460000 to 372000)),
        Linje("KURVE", "c-vest", listOf(6460000 to 372300, 6460100 to 372300)),
        Linje("BUEP", "c-bue", listOf(6460100 to 372300, 6460050 to 372350, 6460000 to 372300)),
    )

    /**
     * OBJTYPE kan byttes ut for å teste andre grensetyper. SOSI 4.5 har kvaliteten på samme linje, mens 5.0 har
     * målemetode og nøyaktighet som egne linjer under KVALITET, slik filene fra kommunene gjør. [kvalitet] overstyrer dette.
     */
    private fun tekst(objtype: String, sosiVersjon: String, kvalitet: List<String>?): String = buildString {
        appendLine(
            """
            .HODE
            ..TEGNSETT UTF-8
            ..TRANSPAR
            ...ENHET 0.01
            ...KOORDSYS 22
            ...ORIGO-NØ 0 0
            ..OMRÅDE
            ...MIN-NØ 6460000 372000
            ...MAX-NØ 6460100 372350
            ..SOSI-VERSJON $sosiVersjon
            ..SOSI-NIVÅ 4
            """.trimIndent()
        )
        linjer.forEachIndexed { indeks, linje ->
            appendLine(".${linje.geometritype} ${indeks + 1}:")
            appendLine("..OBJTYPE $objtype")
            appendLine("..IDENT")
            appendLine("...NAVNEROM http://data.geonorge.no/test")
            appendLine("...LOKALID ${linje.lokalid}")
            if (kvalitet != null) {
                kvalitet.forEach { appendLine(it) }
            } else if (sosiVersjon == "5.0") {
                appendLine("..KVALITET")
                appendLine("...MÅLEMETODE 82")
                appendLine("...NØYAKTIGHET 50")
            } else {
                appendLine("..KVALITET 82")
            }
            appendLine("..NØ")
            // ENHET 0.01 betyr koordinater i cm
            linje.nordOest.forEach { (nord, oest) -> appendLine("${nord * 100L} ${oest * 100L}") }
        }
        appendLine(".SLUTT")
    }

    fun inputStream(objtype: String = "Bopliktområdegrense", sosiVersjon: String = "4.5", kvalitet: List<String>? = null): InputStream =
        tekst(objtype, sosiVersjon, kvalitet).byteInputStream()

    fun les(objtype: String = "Bopliktområdegrense", sosiVersjon: String = "4.5", kvalitet: List<String>? = null) =
        NibasSosiDeserializer().deserialize(inputStream(objtype, sosiVersjon, kvalitet), FILNAVN)
}
