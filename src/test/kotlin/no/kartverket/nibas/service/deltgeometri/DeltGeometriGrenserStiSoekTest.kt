package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.domain.Grunnkrets
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

/**
 * Denne tester at breddeførst-søket for testene våre fungerer som forventet.
 * For disse testene ser vi for oss en krets som ser slik ut:
 *
 *     A -------- B ---------- C
 *     |                       |
 *     |                       |
 *     H                       D
 *     |                       |
 *     |                       |
 *     G -------- F ---------- E
 */
internal class DeltGeometriGrenserStiSoekTest {
    val punktA = Coordinate(0.0, 200.0).toPoint()
    val punktB = Coordinate(100.0, 200.0).toPoint()
    val punktC = Coordinate(200.0, 200.0).toPoint()
    val punktD = Coordinate(200.0, 100.0).toPoint()
    val punktE = Coordinate(200.0, 0.0).toPoint()
    val punktF = Coordinate(100.0, 0.0).toPoint()
    val punktG = Coordinate(0.0, 0.0).toPoint()
    val punktH = Coordinate(0.0, 100.0).toPoint()

    val grenseAB = lagKommuneGrense(punktA, punktB)
    val grenseBC = lagKommuneGrense(punktB, punktC)
    val grenseCD = lagKommuneGrense(punktC, punktD)
    val grenseDEF = lagKommuneGrense(punktD, punktE, punktF)
    val grenseFGH = lagKommuneGrense(punktF, punktG, punktH)
    val grenseHA = lagKommuneGrense(punktH, punktA)

    val grenser = listOf(grenseAB, grenseBC, grenseCD, grenseDEF, grenseFGH, grenseHA)

    private val grunnkrets = Grunnkrets()
        .apply {
            grenser = mutableSetOf(
                lagGrunnkretsGrenseKobling(this, grenseAB),
                lagGrunnkretsGrenseKobling(this, grenseBC),
                lagGrunnkretsGrenseKobling(this, grenseCD),
                lagGrunnkretsGrenseKobling(this, grenseDEF),
                lagGrunnkretsGrenseKobling(this, grenseFGH),
                lagGrunnkretsGrenseKobling(this, grenseHA)
            )
        }

    @Test
    fun `punkt paa rett grense`() {
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(grenseAB.grense)!!.grense).isEqualTo(grenseAB)
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(grenseAB.grense.reverse())!!.grense).isEqualTo(grenseAB)

        val kortLinjeSomStarterIpunktA = GeometryFactory().createLineString(arrayOf(
            punktA.coordinate,
            punktB.coordinate.copy().also { it.setX(it.getX() - 10) }
        ))
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeSomStarterIpunktA)!!.grense).isEqualTo(grenseAB)
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeSomStarterIpunktA.reverse())!!.grense).isEqualTo(grenseAB)

        val kortLinjeSomSlutterIpunktB = GeometryFactory().createLineString(arrayOf(
            punktA.coordinate.copy().also { it.setX(it.getX() + 10) },
            punktB.coordinate
        ))
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeSomSlutterIpunktB)!!.grense).isEqualTo(grenseAB)
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeSomSlutterIpunktB.reverse())!!.grense).isEqualTo(grenseAB)

        val kortLinjeMellomPunktAogPunktB = GeometryFactory().createLineString(arrayOf(
            punktA.coordinate.copy().also { it.setX(it.getX() + 10) },
            punktB.coordinate.copy().also { it.setX(it.getX() - 10) }
        ))
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeMellomPunktAogPunktB)!!.grense).isEqualTo(grenseAB)
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeMellomPunktAogPunktB.reverse())!!.grense).isEqualTo(grenseAB)
    }

    @Test
    fun `punkt paa grense med vinkel`() {
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(grenseDEF.grense)!!.grense).isEqualTo(grenseDEF)
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(grenseDEF.grense.reverse())!!.grense).isEqualTo(grenseDEF)

        val grenseDE = GeometryFactory().createLineString(arrayOf(punktD.coordinate, punktE.coordinate))
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(grenseDE)!!.grense).isEqualTo(grenseDEF)
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(grenseDE.reverse())!!.grense).isEqualTo(grenseDEF)

        val kortLinjeSomStarterIpunktD = GeometryFactory().createLineString(arrayOf(
            punktD.coordinate,
            punktE.coordinate.copy().also { it.setY(it.getY() + 10) }
        ))
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeSomStarterIpunktD)!!.grense).isEqualTo(grenseDEF)
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeSomStarterIpunktD.reverse())!!.grense).isEqualTo(grenseDEF)

        val kortLinjeSomSlutterIpunktE = GeometryFactory().createLineString(arrayOf(
            punktD.coordinate.copy().also { it.setY(it.getY() - 10) },
            punktE.coordinate
        ))
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeSomSlutterIpunktE)!!.grense).isEqualTo(grenseDEF)
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeSomSlutterIpunktE.reverse())!!.grense).isEqualTo(grenseDEF)

        val kortLinjeMellomPunktDogPunktE = GeometryFactory().createLineString(arrayOf(
            punktD.coordinate.copy().also { it.setY(it.getY() - 10) },
            punktE.coordinate.copy().also { it.setY(it.getY() + 10) }
        ))
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeMellomPunktDogPunktE)!!.grense).isEqualTo(grenseDEF)
        assertThat(grunnkrets.grenser.finnGrensekoblingForGrense(kortLinjeMellomPunktDogPunktE.reverse())!!.grense).isEqualTo(grenseDEF)
    }

    @Test
    fun `korteste vei fra C til D skal være kun grenseCD`() {
        val kortesteVei = finnStiMellomPunkterMedBreddeFoerstSoek(punktC, punktD, grenser)

        assertThat(kortesteVei).hasSize(1)
        assertThat(kortesteVei.first()).isEqualTo(grenseCD)
    }

    @Test
    fun `korteste vei fra C til D skal være kun grenseCD selv om den går motsatt vei`() {
        val kortesteVei = finnStiMellomPunkterMedBreddeFoerstSoek(punktD, punktC, grenser)

        assertThat(kortesteVei).hasSize(1)
        assertThat(kortesteVei.first()).isEqualTo(grenseCD)
    }

    @Test
    fun `skal finne alle grenser hvis startpunkt er likt sluttpunkt`() {
        val kortesteVei = finnStiMellomPunkterMedBreddeFoerstSoek(punktC, punktC, grenser)

        assertThat(kortesteVei).hasSize(6)
        assertThat(kortesteVei).containsExactlyInAnyOrder(grenseAB, grenseBC, grenseCD, grenseDEF, grenseFGH, grenseHA)
    }

    @Test
    fun `finn sti hvis det er to muligheter`() {
        val kortesteVeiMedPunktD = finnStiMellomPunkterMedBreddeFoerstSoek(punktB, punktF, grenser, listOf(punktD.coordinate))

        assertThat(kortesteVeiMedPunktD).hasSize(3)
        assertThat(kortesteVeiMedPunktD).containsExactlyInAnyOrder(grenseBC, grenseCD, grenseDEF)

        val kortesteVeiMedPunktA = finnStiMellomPunkterMedBreddeFoerstSoek(punktB, punktF, grenser, listOf(punktA.coordinate))

        assertThat(kortesteVeiMedPunktA).hasSize(3)
        assertThat(kortesteVeiMedPunktA).containsExactlyInAnyOrder(grenseAB, grenseHA, grenseFGH)
    }

    @Test
    fun `skal kaste exception om den ikke finner en en sti til gitt punkt`() {
        val punktIkkePaaGrensen = Coordinate(50.0, 50.0).toPoint()

        assertThrows<RuntimeException> {
            finnStiMellomPunkterMedBreddeFoerstSoek(punktA, punktIkkePaaGrensen, grenser)
        }
    }

    @Test
    fun `korteste vei fra C til H skal være C B A H`() {
        val kortesteVei = finnStiMellomPunkterMedBreddeFoerstSoek(punktC, punktH, grenser)

        assertThat(kortesteVei).hasSize(3)
        assertThat(kortesteVei).isEqualTo(listOf(grenseBC, grenseAB, grenseHA))
    }
}
