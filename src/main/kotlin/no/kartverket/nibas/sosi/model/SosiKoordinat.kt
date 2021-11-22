package no.kartverket.nibas.sosi.model

class SosiKoordinat(val north: String, val east: String, val height: String? = null) : SosiVerdi("$north $east $height")