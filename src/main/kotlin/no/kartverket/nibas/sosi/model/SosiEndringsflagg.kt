package no.kartverket.nibas.sosi.model

import java.time.LocalDate

class SosiEndringsflagg(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "ENDRINGSFLAGG", niva, verdier = verdier) {
    fun getTypeEndring(): String? = verdier.getOrNull(0)?.verdi
    fun getTidspunktEndring(): LocalDate? = stringAsDate(verdier.getOrNull(1)?.verdi)
}
