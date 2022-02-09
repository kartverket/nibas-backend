package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder
import java.util.*

class SosiIdent(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "IDENT", niva, verdier = verdier) {
    fun getLokalId(): String? = getVerdiAvBarn(SosiKoder.LOKALID)
    fun getNavnerom(): String? = getVerdiAvBarn(SosiKoder.NAVNEROM)
    fun getVersjonId(): String? = getVerdiAvBarn(SosiKoder.VERSJONID)
}

fun SosiIdent.lokalIdIsValidUUID(): Boolean {
    val lokalId = getLokalId()
    return if (lokalId.isNullOrEmpty()) {
        false
    } else {
        try {
            UUID.fromString(lokalId)
            true
        } catch (e: Exception) {
            false
        }
    }
}
