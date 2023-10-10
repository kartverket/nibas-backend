package no.kartverket.nibas.service.deltgeometri

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.locationtech.jts.geom.Coordinate

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
    val grenseDE = lagKommuneGrense(punktD, punktE)
    val grenseEF = lagKommuneGrense(punktE, punktF)
    val grenseFG = lagKommuneGrense(punktF, punktG)
    val grenseGH = lagKommuneGrense(punktG, punktH)
    val grenseHA = lagKommuneGrense(punktH, punktA)


    val grenser = listOf(
        grenseAB, grenseBC, grenseCD, grenseDE, grenseEF, grenseFG, grenseGH, grenseHA
    )

    @Test
    fun `korteste vei fra C til D skal være kun grenseCD`() {
        val kortesteVei = finnStiMellomPunkterMedBreddeFoerstSoek(punktC, punktD, grenser)

        assertThat(kortesteVei).hasSize(1)
        assertThat(kortesteVei.first()).isEqualTo(grenseCD)
    }

    @Test
    fun `korteste vei fra E til D skal være kun grenseDE selv om den går motsatt vei`() {
        val kortesteVei = finnStiMellomPunkterMedBreddeFoerstSoek(punktE, punktD, grenser)

        assertThat(kortesteVei).hasSize(1)
        assertThat(kortesteVei.first()).isEqualTo(grenseDE)
    }

    @Test
    fun `skal kaste exception om den ikke finner en en sti til gitt punkt`() {
        val punktIkkePaaGrensen = Coordinate(50.0, 50.0).toPoint()

        assertThrows<RuntimeException> {
            finnStiMellomPunkterMedBreddeFoerstSoek(punktA, punktIkkePaaGrensen, grenser)
        }
    }

    @Test
    fun `korteste vei fra B til G skal være B A H G`() {
        val kortesteVei = finnStiMellomPunkterMedBreddeFoerstSoek(punktB, punktG, grenser)

        assertThat(kortesteVei).hasSize(3)
        assertThat(kortesteVei).isEqualTo(listOf(grenseAB, grenseHA, grenseGH))
    }
}
