package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiHode(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "HODE", niva, verdier = verdier) {
    fun getTegnsett(): String? = getVerdiAvBarn(SosiKoder.TEGNSETT)
    fun getSosiVersjon(): String? = getVerdiAvBarn(SosiKoder.SOSI_VERSJON)
    fun getSosiNiva(): String? = getVerdiAvBarn(SosiKoder.SOSI_NIVA)
    fun getObjektkatalog(): String? = getVerdiAvBarn(SosiKoder.OBJEKTKATALOG)
    fun getTranspar(): SosiTranspar? = barn.find { it is SosiTranspar } as? SosiTranspar
    fun getOmrade(): SosiOmrade? = barn.find { it is SosiOmrade } as? SosiOmrade
}
