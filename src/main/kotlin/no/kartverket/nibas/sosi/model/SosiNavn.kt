package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiNavn(type: String, niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = type, niva, verdier = verdier) {
    fun getNavn(): String? = getVerdiAvBarn(SosiKoder.NAVN)
    fun getSprak(): String? = getVerdiAvBarn(SosiKoder.SPRAK)
    fun getRekkefolge(): Int? = getVerdiAvBarn(SosiKoder.REKKEFOLGE)?.toInt()
}