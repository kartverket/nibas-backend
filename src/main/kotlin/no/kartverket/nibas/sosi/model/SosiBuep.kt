package no.kartverket.nibas.sosi.model

class SosiBuep(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "BUEP", niva, verdier = verdier)