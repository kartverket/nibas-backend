package no.kartverket.nibas.sosi.common

import no.kartverket.nibas.sosi.model.SosiVerdi
import org.apache.commons.lang3.StringUtils
import java.util.regex.Matcher
import java.util.regex.Pattern

class SosiLinje(var linje: String) {
    fun niva(): Int {
        return StringUtils.indexOfAnyBut(linje, ".")
    }

    fun manglerNiva(): Boolean = niva() == 0
    fun settNivaOgType(forrigeNiva: String) {
        linje = "$forrigeNiva $linje"
    }

    fun type(): String = linje.split(Regex("\\s"))[0].replace(".", "")

    fun verdier(): MutableList<SosiVerdi> {
        val kunVerdier = linjeUtenType().trim()
        val list: MutableList<String> = ArrayList()
        val m: Matcher = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(kunVerdier)
        while (m.find())
            list.add(m.group(1).replace("\"", ""))

        return list.map { s -> SosiVerdi(s) }.toMutableList()
    }

    fun splittSosiEgenskaper(): List<SosiLinje> {
        val stringList: MutableList<SosiLinje> = ArrayList()
        var startIndex: Int = 0
        var endIndex: Int = linje.indexOf(" .", startIndex)
        while (endIndex >= 0) {
            stringList.add(SosiLinje(linje.substring(startIndex, endIndex).trim()))
            startIndex = endIndex
            endIndex = linje.indexOf(" .", ++endIndex)
        }

        // Legger til linjen i sin helhet dersom ingen . blir funnet, ellers legger den til siste del av linjen.
        stringList.add(SosiLinje(linje.substring(startIndex).trim()))
        return stringList
    }

    fun splittRef(): List<SosiLinje> {
        val REF = "..REF"
        val sosiListe: MutableList<SosiLinje> = ArrayList()
        when(nivaOgType()) {
            REF -> {
                sosiListe.addAll(
                    this.linje
                        .replace(REF, "")
                        .split(Regex("\\s")).map {
                            SosiLinje("$REF ${it.trim()}")
                        }
                )
            }
            else -> sosiListe.add(this)
        }
        return sosiListe.filterNot { it.linje.trim() == REF }
    }

    fun skalLeses() = !ignorer()

    private fun harNiva(): Boolean {
        return niva() in 1..3;
    }
    fun nivaOgType(): String = linje.split(Regex("\\s"))[0]
    private fun linjeUtenType(): String = when {
        harNiva() -> linje.substring(linje.indexOf(type()) + type().length)
        else -> linje
    }.trim()
    private fun erBlankLinje(): Boolean = linje.trim().isEmpty()
    private fun erKommentar(): Boolean = linje.trim().startsWith("!")
    private fun ignorer(): Boolean = erBlankLinje() || erKommentar()
}