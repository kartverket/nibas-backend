package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiFlate(niva: Int, verdier: MutableList<SosiVerdi>) : SosiGeometri(type = "FLATE", niva, verdier = verdier) {
    fun isSamiskForvaltningsomrade(): Boolean = getVerdiAvBarn(SosiKoder.SAMISKFORVALTNINGSOMRADE) == "JA"
}
