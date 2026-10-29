package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiKvalitet(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "KVALITET", niva, verdier = verdier) {
    // Kvaliteten kan stå på samme linje (..KVALITET 82 50) eller som egne linjer under (...MÅLEMETODE 82, ...NØYAKTIGHET 50)
    // Står verdiene i hermetegn (..KVALITET "82 50") kommer de som én verdi fra parseren
    private fun verdierPaaLinjen(): List<String> = verdier.flatMap { it.verdi.trim().split(Regex("\\s+")) }
    fun getMalemetode(): String? = getVerdiAvBarn(SosiKoder.MAALEMETODE) ?: verdierPaaLinjen().getOrNull(0)
    fun getNoyaktighet(): Int? = (getVerdiAvBarn(SosiKoder.NOYAKTIGHET) ?: verdierPaaLinjen().getOrNull(1))?.toInt()
}