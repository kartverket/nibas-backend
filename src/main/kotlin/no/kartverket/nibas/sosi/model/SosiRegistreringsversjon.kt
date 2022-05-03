package no.kartverket.nibas.sosi.model

import java.time.ZonedDateTime

class SosiRegistreringsversjon(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "REGISTRERINGSVERSJON", niva, verdier = verdier) {
    fun getProdukt(): String? = verdier.getOrNull(0)?.verdi
    fun getVersjon(): String? = verdier.getOrNull(1)?.verdi
}