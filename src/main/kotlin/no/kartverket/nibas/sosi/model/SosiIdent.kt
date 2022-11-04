package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder
import java.util.*

/**
 * Representasjon av en sosi ident.
 * Eksempel:
 * ..IDENT
 * ...LOKALID "123-abc"
 *
 * Eksempelet over kan også kompaktifiseres til følgende:
 * ..IDENT 123-abc "https://data.geonorge.no/sosi/administrativeenheter" abc
 */
class SosiIdent(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "IDENT", niva, verdier = verdier) {
    fun getLokalId(): String? = when (harBarn()) {
        true -> getVerdiAvBarn(SosiKoder.LOKALID)
        false -> getVerdiMedIndex(0)
    }

    fun getValidLokalId(): String? {
        if (lokalIdIsValidUUID()) {
            return getLokalId()
        }
        return null
    }

    fun lokalIdIsValidUUID(): Boolean {
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
}


