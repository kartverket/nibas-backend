package no.kartverket.nibas.sosi.model

open class SosiNordOest(type: String = "NØ", niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = type, niva, verdier = verdier) {
    fun getKoordinater(): List<SosiKoordinat> {
        return verdier.filter { it is SosiKoordinat }.map { it as SosiKoordinat }
    }
}
