package no.kartverket.nibas.sosi.model

class SosiKvalitet(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "KVALITET", niva, verdier = verdier) {
    fun getMalemetode(): String? = verdier.getOrNull(0)?.verdi
    fun getNoyaktighet(): Int? = verdier.getOrNull(1)?.verdi?.toInt()
}