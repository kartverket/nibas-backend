package no.kartverket.nibas.service.deltgeometri

import io.mockk.every
import io.mockk.mockk
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.KommuneService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.locationtech.jts.geom.Coordinate
import java.time.LocalDate
import kotlin.test.Test


/**
 * For disse testene tar vi for oss en kommune som ser cirka slike ut:
 *
 *      A -------- B -------- C
 *      |          |          |
 *      D -------- E -------- F
 *      |          |          |
 *      G -------- H -------- I
 *
 * Det ytre skallet er her en kommungrense
 * B - E - H er kun en stemmekretsgrense, hvor punktene B og H ikke finnes på grunnkretsgrensene.
 * D - E - F er både en grunnkrets og stemmekrets grense.
 *
 * Totalt har denne kommunen da 2 grunnkretser og 4 stemmekretser. Enkelte tester kan
 * legge til ekstra punkter for å teste spesifikke ting.
 */
internal class DeltGeometriAdministrativeGrenserServiceTest {
    val punktA = Coordinate(0.0, 200.0).toPoint()
    val punktB = Coordinate(100.0, 200.0).toPoint()
    val punktC = Coordinate(200.0, 200.0).toPoint()
    val punktD = Coordinate(0.0, 100.0).toPoint()
    val punktE = Coordinate(100.0, 100.0).toPoint()
    val punktF = Coordinate(200.0, 100.0).toPoint()
    val punktG = Coordinate(0.0, 0.0).toPoint()
    val punktH = Coordinate(100.0, 0.0).toPoint()
    val punktI = Coordinate(200.0, 0.0).toPoint()

    val grunnkretsGrenseDACF = lagKommuneGrense(punktD, punktA, punktC, punktF)
        .also { it.opphav = "TEST OPPHAV" }
    val grunnkretsGrenseFIGD = lagKommuneGrense(punktF, punktI, punktG, punktD)
    val grunnkretsGrenseDEF = lagGrunnkretsGrense(punktD, punktE, punktF)

    val stemmekretsGrenseDAB = lagKommuneGrense(punktD, punktA, punktB)
    val stemmekretsGrenseBCF = lagKommuneGrense(punktB, punktC, punktF)
    val stemmekretsGrenseFIH = lagKommuneGrense(punktF, punktI, punktH)
    val stemmekretsGrenseHGD = lagKommuneGrense(punktH, punktG, punktD)
    val stemmekretsGrenseBE = lagStemmekretsGrense(punktB, punktE)
    val stemmekretsGrenseEH = lagStemmekretsGrense(punktE, punktH)
    val stemmekretsGrenseDE = lagStemmekretsGrense(punktD, punktE)
    val stemmekretsGrenseEF = lagStemmekretsGrense(punktE, punktF)

    val stemmekretsOppeVenstre = Stemmekrets()
        .apply {
            grenser = mutableSetOf(
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseDAB, 0, false),
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseBE, 1, false),
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseDE, 2, true)
            )
        }

    val stemmekretsOppeHoyre = Stemmekrets()
        .apply {
            grenser = mutableSetOf(
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseBCF, 0, false),
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseEF, 1, true),
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseBE, 2, true)
            )
        }

    val stemmekretsNedeVenstre = Stemmekrets()
        .apply {
            grenser = mutableSetOf(
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseHGD, 0, false),
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseDE, 1, false),
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseEH, 2, false)
            )
        }

    val stemmekretsNedeHoyre = Stemmekrets()
        .apply {
            grenser = mutableSetOf(
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseFIH, 0, false),
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseEH, 1, true),
                lagStemmekretsGrenseKobling(this, stemmekretsGrenseEF, 2, false)
            )
        }

    val grunnkretsOppe = Grunnkrets()
        .apply {
            grenser = mutableSetOf(
                lagGrunnkretsGrenseKobling(this, grunnkretsGrenseDACF, 0, false),
                lagGrunnkretsGrenseKobling(this, grunnkretsGrenseDEF, 1, true)
            )
        }

    val grunnkretsNede = Grunnkrets()
        .apply {
            grenser = mutableSetOf(
                lagGrunnkretsGrenseKobling(this, grunnkretsGrenseFIGD, 0, false),
                lagGrunnkretsGrenseKobling(this, grunnkretsGrenseDEF, 1, false)
            )
        }

    val alleGrunnkretser = mutableSetOf(grunnkretsNede, grunnkretsOppe)
    val alleStemmekretser = listOf(stemmekretsNedeHoyre, stemmekretsNedeVenstre, stemmekretsOppeHoyre, stemmekretsOppeVenstre)

    val kommuneServiceMock = mockk<KommuneService>()
    val kodelisteServiceMock = mockk<KodelisteService>()
    val kommune = Kommune().apply {
        kommuneLokalid = UnikLokalid(flatetype = Flatetype.KOMMUNE)
        alleGrunnkretser.forEach { this.addGrunnkrets(it) }
    }

    val deltGeometriAdministrativeGrenserService = DeltGeometriAdministrativeGrenserService(kommuneServiceMock, kodelisteServiceMock)

    @BeforeEach
    fun setup() {
        every { kodelisteServiceMock.getCodeValueByKommunenummerId(any()) } returns "123"
        every { kommuneServiceMock.hentAlleStemmekretserForKommune(any(), any()) } returns alleStemmekretser
    }

    @Test
    fun `skal ikke gi feil når den danner delt geometri for kommunen`() {
        assertDoesNotThrow {
            deltGeometriAdministrativeGrenserService.getAdministrativeGrenserMedDeltGeometriForKommune(kommune, LocalDate.now())
        }
    }

    @Test
    fun `grensene skal splittes på stemmekretsknutepunkter som ikke er en del av grunnkretgrensene (punkt B og H)`() {
        val nyeGrenser = deltGeometriAdministrativeGrenserService.getAdministrativeGrenserMedDeltGeometriForKommune(kommune, LocalDate.now())

        assertThat(nyeGrenser).hasSize(4)
        assertThat(nyeGrenser).anyMatch { it.grense.startPoint == punktB }
        assertThat(nyeGrenser).anyMatch { it.grense.startPoint == punktH }
    }

    @Test
    fun `nye grenser som oppstår via splitting skal beholde metadata fra grunnkretsen for samme punkter`() {
        val nyeGrenser = deltGeometriAdministrativeGrenserService.getAdministrativeGrenserMedDeltGeometriForKommune(kommune, LocalDate.now())

        val nyGrenseDAB = nyeGrenser.find { it.grense.coordinates.contains(punktD.coordinate) && it.grense.coordinates.contains(punktB.coordinate) }
        val nyGrenseBCF = nyeGrenser.find { it.grense.coordinates.contains(punktB.coordinate) && it.grense.coordinates.contains(punktF.coordinate) }

        assertThat(nyGrenseDAB?.opphav).isEqualTo("TEST OPPHAV")
        assertThat(nyGrenseBCF?.opphav).isEqualTo("TEST OPPHAV")
    }
}
