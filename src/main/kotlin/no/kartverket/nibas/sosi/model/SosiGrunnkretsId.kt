package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiGrunnkretsId(type: String, niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = type, niva, verdier = verdier) {
    fun getGrunnkretsnummer(): String? = getVerdiAvBarn(SosiKoder.GRUNNKRETSNUMMER)
    fun getGrunnkretsnavn(): String? = getVerdiAvBarn(SosiKoder.GRUNNKRETSNAVN)
}