package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiStemmekretsId(type: String, niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = type, niva, verdier = verdier) {
    fun getStemmekretsnummer(): String? = getVerdiAvBarn(SosiKoder.STEMMEKRETSNUMMER)
    fun getStemmekretsnavn(): String? = getVerdiAvBarn(SosiKoder.STEMMEKRETSNAVN)
    fun getTellekretsnummer(): String? = getVerdiAvBarn(SosiKoder.TELLEKRETSNUMMER)
    fun getTellekretsnavn(): String? = getVerdiAvBarn(SosiKoder.TELLEKRETSNAVN)
    fun getValgdistriktsnummer(): String? = getVerdiAvBarn(SosiKoder.VALGDISTRIKTSNUMMER)
}

