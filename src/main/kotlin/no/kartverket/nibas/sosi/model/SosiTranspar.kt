package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiTranspar(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "TRANSPAR", niva, verdier = verdier) {
    fun getKoordSys(): String? = getVerdiAvBarn(SosiKoder.KOORDSYS)
    fun getEnhet(): String? = getVerdiAvBarn(SosiKoder.ENHET)
    fun getOrigoNO(): SosiNordOest? = barn.find { it is SosiNordOest } as? SosiNordOest
}
