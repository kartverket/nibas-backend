package no.kartverket.nibas.sosi.model

class SosiBuep(niva: Int, verdier: MutableList<SosiVerdi>) : SosiGeometri(type = "BUEP", niva, verdier = verdier)