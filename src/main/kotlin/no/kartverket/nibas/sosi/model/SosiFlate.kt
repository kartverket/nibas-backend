package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder
import java.lang.RuntimeException

class SosiFlate(niva: Int, verdier: MutableList<SosiVerdi>) : SosiGeometri(type = "FLATE", niva, verdier = verdier) {
    private lateinit var hullGeometri: List<List<SosiGeometri>>
    private lateinit var skallGeometri: List<SosiGeometri>

    fun isSamiskForvaltningsomrade(): Boolean = getVerdiAvBarn(SosiKoder.SAMISKFORVALTNINGSOMRADE) == "JA"
    fun getSkallReferanser(): List<SosiReferanse> {
        val skall = ArrayList<SosiReferanse>()
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

    fun getHullReferanser(): List<List<SosiReferanse>> {
        val hullListe = ArrayList<List<SosiReferanse>>()
        var inniHull = false
        var hull = ArrayList<SosiReferanse>()
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

    fun lastReferanser(admenhetRoot: SosiObjekt) {
        this.skallGeometri = getSkallReferanser().map { admenhetRoot.getSosiGeometriForReferanse(it)!! }
        this.hullGeometri = getHullReferanser().map { it.map { ref -> admenhetRoot.getSosiGeometriForReferanse(ref)!! } }
    }

    fun getSkallGeometri(): List<SosiGeometri> {
        if(!this::skallGeometri.isInitialized) {
            throw RuntimeException("Referanser er ikke lastet.")
        }

        return this.skallGeometri
    }

    fun getHullGeometri(): List<List<SosiGeometri>> {
        if(!this::hullGeometri.isInitialized) {
            throw RuntimeException("Referanser er ikke lastet")
        }

        return this.hullGeometri
    }
}
