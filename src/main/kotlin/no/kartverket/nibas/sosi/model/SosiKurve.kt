package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.*
import java.time.LocalDate

class SosiKurve(niva: Int, verdier: MutableList<SosiVerdi>) : SosiGeometri(type = "KURVE", niva, verdier = verdier)
