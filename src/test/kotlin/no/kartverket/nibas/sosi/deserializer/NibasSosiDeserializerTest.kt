package no.kartverket.nibas.sosi.deserializer

import no.kartverket.nibas.sosi.model.SosiObjekt
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class NibasSosiDeserializerTest {

    @Test
    fun testParsingAvEksempelfil() {
        when (val stream = this.javaClass.getResourceAsStream("/sosi-testfil.sos")) {
            null -> fail("Could not read file from resource")
            else -> {
                val root = NibasSosiDeserializer().deserialize(stream)
                root.debug(System.out)
                assertNotNull(root.getHode(), "Forventer et hode")
                assertTrue(root.getKurver().isNotEmpty(), "Forventer kurver")
                assertTrue(root.getFlater().isNotEmpty(), "Forventer flater")
            }
        }
    }

    @Test
    fun testParsingAvEksempelfilDerAltErPaEnLinje() {
        when (val stream = this.javaClass.getResourceAsStream("/sosi-testfil-one-line.sos")) {
            null -> fail("Could not read file from resource")
            else -> {
                val root = NibasSosiDeserializer().deserialize(stream)
                assertNotNull(root.getHode(), "Forventer et hode")
                assertTrue(root.getKurver().isNotEmpty(), "Forventer kurver")
                assertTrue(root.getFlater().isNotEmpty(), "Forventer flater")
            }
        }
    }

    @Test
    fun testAtFinnForelderFinnerKorrektForelder() {
        val root = SosiObjekt(niva = 0, type = "ROOT");
        val barn1 = SosiObjekt(type = "STUFF", niva = 1)
        val barn2 = SosiObjekt(type = "STUFF", niva = 2)
        val barn3 = SosiObjekt(type = "STUFF", niva = 3)
        val barn4 = SosiObjekt(type = "STUFF", niva = 4)

        root.addBarn(barn1)
        barn1.addBarn(barn2)
        barn2.addBarn(barn3)

        val nibasSosiDeserializer = NibasSosiDeserializer()
        assertEquals(0, nibasSosiDeserializer.finnForelder(barn3, barn1)?.niva, "Forventet annen forelder")
        assertEquals(1, nibasSosiDeserializer.finnForelder(barn3, barn2)?.niva, "Forventet annen forelder")
        assertEquals(2, nibasSosiDeserializer.finnForelder(barn3, barn3)?.niva, "Forventet annen forelder")
        assertEquals(3, nibasSosiDeserializer.finnForelder(barn3, barn4)?.niva, "Forventet annen forelder")
    }
}