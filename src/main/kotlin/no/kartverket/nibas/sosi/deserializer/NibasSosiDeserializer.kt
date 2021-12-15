package no.kartverket.nibas.sosi.deserializer

import no.kartverket.nibas.sosi.common.SosiLinje
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.model.SosiObjektFactory
import java.io.*
import java.nio.charset.Charset
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class NibasSosiDeserializer {
    fun deserialize(stream: InputStream): SosiObjekt {
        skipUTF8BOM(stream)
        val contentAsBytes = stream.readAllBytes()
        val encoding = finnEncoding(contentAsBytes)
        val reformatted = reformat(contentAsBytes, encoding)

        return parseLinjer(reformatted)
    }

    fun parseLinjer(linjer: List<SosiLinje>): SosiObjekt {
        val root = SosiObjekt(niva = 0, type = "ROOT")
        var forrige: SosiObjekt? = root
        linjer.forEach { sosiLinje ->
            val denne = SosiObjektFactory.createSosiObjekt(sosiLinje)
            val forelder = finnForelder(forrige, denne)
            forelder?.addBarn(denne)

            forrige = denne
        }

        val kunMedVerdier = root.fjernBarnUtenVerdierEllerBarn()
        val koordsys = kunMedVerdier.getHode()?.getTranspar()?.getKoordSys()
        val enhet: Double = when (val enhet = kunMedVerdier.getHode()?.getTranspar()?.getEnhet()) {
            null -> 1.0
            else -> enhet
        }

        return kunMedVerdier.setKoordsysOgEnhetPaAlleBarn(koordsys, enhet)

    }

    fun finnForelder(forrige: SosiObjekt?, barn: SosiObjekt): SosiObjekt? {
        fun finnForelder(kandidat: SosiObjekt?, barn: SosiObjekt): SosiObjekt? {
            return when {
                kandidat?.niva == barn.niva - 1 -> kandidat
                kandidat?.forelder == null -> kandidat
                else -> finnForelder(kandidat.forelder, barn)
            }
        }
        return finnForelder(forrige, barn)
    }

    /**
     * Av-kompaktifiserer sosi-innholdet.
     *
     * Eksempel 1:
     * ..NØ
     * aaa bbb
     * ccc ddd
     *
     * blir til
     * ..NØ aaa bbb
     * ..NØ ccc ddd
     *
     * Eksempel 2:
     * ..REF :111 :222
     * :333 :444
     *
     * blir til
     * ..REF :111
     * ..REF :222
     * ..REF :333
     * ..REF :444
     *
     * Eksempel 3:
     * ..NØ 123 123 ...KP 1
     *
     * blir til
     * ..NØ 123 123
     * ...KP 1
     *
     * Forenkler parsing ved at hver linje i filen vil alltid ha både et sosi-nivå og en sosi-type.
     *
     * @param content Alt innholdet som skal reformatteres
     * @param encoding Hvilken encoding som skal brukes ved lesing
     */
    private fun reformat(content: ByteArray, encoding: Charset): ArrayList<SosiLinje> {
        val reformattedLines = ArrayList<SosiLinje>()
        var forrigeNivaOgType = ""
        BufferedReader(InputStreamReader(ByteArrayInputStream(content), encoding)).forEachLine { line ->
            val sosiLinje = SosiLinje(line)
            if(sosiLinje.skalLeses()) {
                if(sosiLinje.manglerNiva()) {
                    sosiLinje.settNivaOgType(forrigeNivaOgType)
                }
                forrigeNivaOgType = sosiLinje.nivaOgType()
                reformattedLines.addAll(
                    sosiLinje.splittSosiEgenskaper()
                        .flatMap { it.splittRef() })
            }
        }
        return reformattedLines
    }

    fun finnEncoding(content: ByteArray): Charset {
        val contentAsString = content.toString(Charset.defaultCharset())
        val matcher = Pattern.compile("(\\.\\.TEGNSETT)\\s+(\\S*)\\s.*").matcher(contentAsString)
        return when(matcher.find()) {
            true -> Charset.forName(matcher.group(2))
            false -> Charset.defaultCharset()
        }
    }

    @Throws(IOException::class)
    fun skipUTF8BOM(bis: InputStream) {
        val utf8bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        bis.mark(utf8bom.size)
        val firstFewBytes = ByteArray(utf8bom.size)
        bis.read(firstFewBytes)
        if (!firstFewBytes.contentEquals(utf8bom)) {
            bis.reset() //bom not detected
        }
    }
}
