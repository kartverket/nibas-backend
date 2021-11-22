package no.kartverket.nibas.sosi.common

import no.kartverket.nibas.sosi.model.SosiObjekt
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SosiObjektTest {

    @Test
    fun testAtfjernBarnUtenVerdierEllerBarnFjernerTommeNoder() {
        val root = SosiObjekt(niva = 0, type = "ROOT");
        val barn1 = SosiObjekt(type = "STUFF", niva = 1)
        val barn2 = SosiObjekt(type = "STUFF", niva = 2)

        root.addBarn(barn1)
        barn1.addBarn(barn2)

        assertEquals(0, root.fjernBarnUtenVerdierEllerBarn().barn.size, "Forventer at alle barn er fjernet")
    }
}