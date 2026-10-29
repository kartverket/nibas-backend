package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiKvalitet(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "KVALITET", niva, verdier = verdier) {
    // Kvaliteten kan stå på samme linje (..KVALITET 82 50) eller som egne linjer under (...MÅLEMETODE 82, ...NØYAKTIGHET 50)
    fun getMalemetode(): String? = getVerdiAvBarn(SosiKoder.MAALEMETODE) ?: verdier.getOrNull(0)?.verdi
    fun getNoyaktighet(): Int? = (getVerdiAvBarn(SosiKoder.NOYAKTIGHET) ?: verdier.getOrNull(1)?.verdi)?.toInt()
}