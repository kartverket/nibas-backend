package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiOmrade(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "OMRADE", niva, verdier = verdier) {
    fun getMinNO(): SosiNordOest? = barn.find { it is SosiNordOest && it.type == SosiKoder.MIN_NO } as? SosiNordOest
    fun getMaxNO(): SosiNordOest? = barn.find { it is SosiNordOest && it.type == SosiKoder.MAKS_NO } as? SosiNordOest
}
