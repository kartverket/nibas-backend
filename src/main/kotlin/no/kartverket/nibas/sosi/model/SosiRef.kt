package no.kartverket.nibas.sosi.model

class SosiRef(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "REF", niva, verdier = verdier) {
    fun isRetningReversed(): Boolean = getVerdi().startsWith("-")
    fun getVerdi(): String = verdier[0].verdi.replace(":", "")
}