package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder
import java.util.*

/**
 * Representasjon av en sosi ident.
 * Eksempel:
 * ..IDENT
 * ...LOKALID "123-abc"
 * ...NAVNEROM "https://data.geonorge.no/sosi/administrativeenheter"
 * ...VERSJONID "abc"
 *
 * Eksempelet over kan også kompaktifiseres til følgende:
 * ..IDENT 123-abc "https://data.geonorge.no/sosi/administrativeenheter" abc
 */
class SosiIdent(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "IDENT", niva, verdier = verdier) {
    fun getLokalId(): String? = when (harBarn()) {
        true -> getVerdiAvBarn(SosiKoder.LOKALID)
        false -> getVerdiMedIndex(0)
    }

    fun getNavnerom(): String? = when (harBarn()) {
        true -> getVerdiAvBarn(SosiKoder.NAVNEROM)
        false -> getVerdiMedIndex(1)
    }

    fun getVersjonId(): String? = when(harBarn()) {
        true -> getVerdiAvBarn(SosiKoder.VERSJONID)
        false -> getVerdiMedIndex(2)
    }
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
