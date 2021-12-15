package no.kartverket.nibas.sosi.model

class SosiKoordinat(val nord: String, val oest: String, val hoyde: String? = null) : SosiVerdi("$nord $oest $hoyde")