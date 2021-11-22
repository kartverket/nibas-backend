package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiIdent(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "IDENT", niva, verdier = verdier) {
    fun getLokalId(): String? = getVerdiAvBarn(SosiKoder.LOKALID)
    fun getNavnerom(): String? = getVerdiAvBarn(SosiKoder.NAVNEROM)
    fun getVersjonId(): String? = getVerdiAvBarn(SosiKoder.VERSJONID)
}
