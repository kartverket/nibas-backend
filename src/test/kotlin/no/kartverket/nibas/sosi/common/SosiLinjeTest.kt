package no.kartverket.nibas.sosi.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SosiLinjeTest {

    @Test
    fun testAtNivaReturnererForventetNiva() {
        assertEquals(1, SosiLinje(".OBJEKT").niva(), "Forventet nivå 1")
        assertEquals(2, SosiLinje("..OBJEKT").niva(), "Forventet nivå 2")
        assertEquals(3, SosiLinje("...OBJEKT").niva(), "Forventet nivå 3")
    }

    @Test
    fun testAtSettNivaOgTypeHvisManglerSetterKorrektNiva() {
        val sosiLinje = SosiLinje("MANGLER NIVÅ")
        sosiLinje.settNivaOgType(".NIVÅ1")
        assertEquals(".NIVÅ1", sosiLinje.nivaOgType(), "Forventet nivåOgType til å være .NIVÅ1")
        assertEquals(1, sosiLinje.niva(), "Forventet nivå 1")
    }

    @Test
    fun testAtTypeReturnererForventetType() {
        assertEquals("NØH", SosiLinje("...NØH").type(), "Forventet type til å være NØH")
    }

    @Test
    fun testAtVerdierReturnererEnListe() {
        val verdier = SosiLinje("..OBJEKT verdi1 verdi2 verdi3").verdier()
        assertEquals(3, verdier.size, "Forventet 3 verdier")
        assertEquals("verdi1", verdier[0].verdi, "Forventet at verdi skulle være verdi1")
        assertEquals("verdi2", verdier[1].verdi, "Forventet at verdi skulle være verdi2")
        assertEquals("verdi3", verdier[2].verdi, "Forventet at verdi skulle være verdi3")
    }

    @Test
    fun testAtVerdierReturnererEnVerdiHvisDetErEnStrengMedMellomrom() {
        val verdier = SosiLinje("..OBJEKT \"Dette er en streng med mellomrom\"").verdier()
        assertEquals(1, verdier.size, "Forventet 1 verdier")
        assertEquals("Dette er en streng med mellomrom", verdier[0].verdi, "Forventet at verdi skulle være Dette er en streng med mellomrom")
    }

    @Test
    fun testAtBarnSplittesFraEnSosiLinje() {
        val splittet = SosiLinje("..NØ 123 123 ...KP 1 .KURVE 123").splittSosiEgenskaper()
        assertEquals(3, splittet.size, "Forventet at antall sosiLinjer var 3")
        assertEquals("..NØ 123 123", splittet[0].linje, "Uventet linje")
        assertEquals("...KP 1", splittet[1].linje, "Uventet linje")
        assertEquals(".KURVE 123", splittet[2].linje, "Uventet linje")
    }

    @Test
    fun testAtSplittRefGirTilbakeKorrektAntallLinjerMedRiktigTypeOgNiva() {
        val splittetRef = SosiLinje("..REF :10 :11 :12 :13").splittRef()
        splittetRef.forEach { println(it.linje) }
        assertEquals(4, splittetRef.size, "Forventet 4 linjer etter splitting")
        assertEquals("..REF :10", splittetRef[0].linje, "Uventet REF")
        assertEquals("..REF :11", splittetRef[1].linje, "Uventet REF")
        assertEquals("..REF :12", splittetRef[2].linje, "Uventet REF")
        assertEquals("..REF :13", splittetRef[3].linje, "Uventet REF")
    }

    @Test
    fun testAtSkalLesesReturnererForventetSvar() {
        assertFalse(SosiLinje("").skalLeses(), "Blank linje skal ikke leses")
        assertFalse(SosiLinje("!Heisann").skalLeses(), "Linjer som begynner med ! skal ikke leses")
    }
}