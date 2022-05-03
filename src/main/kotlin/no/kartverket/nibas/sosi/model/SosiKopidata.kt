package no.kartverket.nibas.sosi.model

import java.time.LocalDate

class SosiKopidata(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "KOPIDATA", niva, verdier = verdier) {
    fun getOmraadeId(): Int? = verdier.getOrNull(0)?.verdi?.toInt()
    fun getOriginalDatavert(): String? = verdier.getOrNull(1)?.verdi
    fun getKopidato(): LocalDate? = stringAsDate(verdier.getOrNull(2)?.verdi)
}
