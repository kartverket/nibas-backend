package no.kartverket.nibas.domain

interface GrenseKobling {
    var grense: Grense
    fun getEnhetKobletTilGrense(): KobletTilGrense
    fun getLokalIdForKrets(): String
}


