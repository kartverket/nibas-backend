package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder

class SosiFlate(niva: Int, verdier: MutableList<SosiVerdi>) : SosiGeometri(type = "FLATE", niva, verdier = verdier) {
    fun isSamiskForvaltningsomrade(): Boolean = getVerdiAvBarn(SosiKoder.SAMISKFORVALTNINGSOMRADE) == "JA"
    fun getSkallRef(): List<SosiRef> {
        val skall = ArrayList<SosiRef>()
        var inniHull = false
        getReferanser().forEach { ref ->
            if(ref.getVerdi().startsWith("(")) {
                inniHull = true
            }
            if(!inniHull) {
                skall.add(ref)
            }

            if (ref.getVerdi().endsWith(")")) {
                inniHull = false
            }
        }
        return skall
    }

    fun getHullRef(): List<List<SosiRef>> {
        val hullListe = ArrayList<List<SosiRef>>()
        var inniHull = false
        var hull = ArrayList<SosiRef>()
        getReferanser().forEach { ref ->
            if(ref.getVerdi().startsWith("(")) {
                inniHull = true
            }
            if(inniHull) {
                hull.add(ref)
            }

            if (ref.getVerdi().endsWith(")")) {
                inniHull = false
                hullListe.add(hull)
                hull = ArrayList()
            }
        }

        return hullListe
    }
}
