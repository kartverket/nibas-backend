package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiOmrade(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "OMRADE", niva, verdier = verdier) {
    fun getMinNO(): SosiNo? = barn.find { it is SosiNo && it.type == SosiKoder.MIN_NO } as? SosiNo
    fun getMaxNO(): SosiNo? = barn.find { it is SosiNo && it.type == SosiKoder.MAKS_NO } as? SosiNo
}
