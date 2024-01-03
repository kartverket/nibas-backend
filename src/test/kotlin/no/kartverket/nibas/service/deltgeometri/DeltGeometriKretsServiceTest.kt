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
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import java.time.LocalDate

internal class DeltGeometriKretsServiceTest {
    val KOMMUNENNR_ID = "ID123"
    val KOMMUNENR = "3020"

    val kommuneServiceMock = mockk<KommuneService>()
    val kodelisteServiceMock = mockk<KodelisteService>()
    val deltGeometriKretsService = DeltGeometriKretsService(kommuneServiceMock, kodelisteServiceMock)

    val nyKommune = Kommune().apply {
        kommunenummerUuid = KOMMUNENNR_ID
        kommunenummer = KOMMUNENR
        kommuneLokalid = UnikLokalid(flatetype = Flatetype.KOMMUNE)
    }

    val grunnkretsikommunen = Grunnkrets().apply { this.kommuneLokalid = nyKommune.kommuneLokalid }
    val grunnkretsUtenfor1 = Grunnkrets().apply { this.kommuneLokalid = nyKommune.kommuneLokalid }
    val grunnkretsUtenfor2 = Grunnkrets().apply { this.kommuneLokalid = nyKommune.kommuneLokalid }

    val stemmekretsikommunen = Stemmekrets().apply { this.kommuneLokalid = nyKommune.kommuneLokalid }
    val stemmekretsUtenfor1 = Stemmekrets().apply { this.kommuneLokalid = nyKommune.kommuneLokalid }
    val stemmekretsUtenfor2 = Stemmekrets().apply { this.kommuneLokalid = nyKommune.kommuneLokalid }

    val punktA = Coordinate(0.0, 0.0).toPoint()
    val punktB = Coordinate(0.0, 100.0).toPoint()
    val punktC = Coordinate(0.0, 200.0).toPoint()
    val punktD = Coordinate(200.0, 200.0).toPoint()
    val punktE = Coordinate(0.0, 50.0).toPoint()

    val grenseAB = lagKommuneGrense(punktA, punktB)
    val grenseBC = lagKommuneGrense(punktB, punktC)
    val grenseCD = lagKommuneGrense(punktC, punktD)
    val gGrenseDA = lagGrunnkretsGrense(punktD, punktA)
    val sGrenseDA = lagStemmekretsGrense(punktD, punktA)

    // Grenser for ny geometri
    val grenseAE = lagKommuneGrense(punktA, punktE)
    val grenseEB = lagKommuneGrense(punktE, punktB)
    val nyeAdministrativeGrenser = listOf(grenseAE, grenseEB, grenseBC, grenseCD)

    init {
        nyKommune.kommuneLokalid.kommuner = mutableSetOf(nyKommune)

        // Setter opp grunnkretser
        grunnkretsikommunen.grenser = mutableSetOf(
            lagGrunnkretsGrenseKobling(grunnkretsikommunen, grenseAB, 0, false),
            lagGrunnkretsGrenseKobling(grunnkretsikommunen, grenseBC, 1, false),
            lagGrunnkretsGrenseKobling(grunnkretsikommunen, grenseCD, 2, false),
            lagGrunnkretsGrenseKobling(grunnkretsikommunen, gGrenseDA, 3, false),
        )

        grenseAB.grunnkretser = mutableSetOf(
            lagGrunnkretsGrenseKobling(grunnkretsikommunen, grenseAB, 0, false),
            lagGrunnkretsGrenseKobling(grunnkretsUtenfor1, grenseAB, 0, false),
        )

        grenseBC.grunnkretser = mutableSetOf(
            lagGrunnkretsGrenseKobling(grunnkretsikommunen, grenseBC, 0, false),
            lagGrunnkretsGrenseKobling(grunnkretsUtenfor1, grenseBC, 0, false),
        )

        grenseCD.grunnkretser = mutableSetOf(
            lagGrunnkretsGrenseKobling(grunnkretsikommunen, grenseCD, 0, false),
            lagGrunnkretsGrenseKobling(grunnkretsUtenfor2, grenseCD, 0, false),
        )

        gGrenseDA.grunnkretser = mutableSetOf(
            lagGrunnkretsGrenseKobling(grunnkretsikommunen, gGrenseDA, 0, false),
            lagGrunnkretsGrenseKobling(grunnkretsUtenfor2, gGrenseDA, 0, false),
        )

        // Setter opp stemmekretser
        stemmekretsikommunen.grenser = mutableSetOf(
            lagStemmekretsGrenseKobling(stemmekretsikommunen, grenseAB, 0, false),
            lagStemmekretsGrenseKobling(stemmekretsikommunen, grenseBC, 1, false),
            lagStemmekretsGrenseKobling(stemmekretsikommunen, grenseCD, 2, false),
            lagStemmekretsGrenseKobling(stemmekretsikommunen, sGrenseDA, 3, false),
        )

        grenseAB.stemmekretser = mutableSetOf(
            lagStemmekretsGrenseKobling(stemmekretsikommunen, grenseAB, 0, false),
            lagStemmekretsGrenseKobling(stemmekretsUtenfor1, grenseAB, 0, false),
        )

        grenseBC.stemmekretser = mutableSetOf(
            lagStemmekretsGrenseKobling(stemmekretsikommunen, grenseBC, 0, false),
            lagStemmekretsGrenseKobling(stemmekretsUtenfor1, grenseBC, 0, false),
        )

        grenseCD.stemmekretser = mutableSetOf(
            lagStemmekretsGrenseKobling(stemmekretsikommunen, grenseCD, 0, false),
            lagStemmekretsGrenseKobling(stemmekretsUtenfor2, grenseCD, 0, false),
        )

        gGrenseDA.stemmekretser = mutableSetOf(
            lagStemmekretsGrenseKobling(stemmekretsikommunen, gGrenseDA, 0, false),
            lagStemmekretsGrenseKobling(stemmekretsUtenfor2, gGrenseDA, 0, false),
        )
    }

    @BeforeEach
    fun setupMocks() {
        every { kommuneServiceMock.hentAlleGrunnkretserForKommune(any(), any()) } returns listOf(grunnkretsikommunen)
        every { kommuneServiceMock.hentAlleStemmekretserForKommune(any(), any()) } returns listOf(stemmekretsikommunen)
    }

    @Test
    fun `getGrunnkretserSomGrenserTilKommunegrense skal ikke gi duplikater om flere grenser grenser mot samme stemmekrets`() {
        val grunnkretser = deltGeometriKretsService.getGrunnkretserSomGrenserTilKommunegrense(KOMMUNENR, LocalDate.now())

        assertThat(grunnkretser).hasSize(3)
        assertThat(grunnkretser).contains(grunnkretsikommunen)
        assertThat(grunnkretser).contains(grunnkretsUtenfor1)
        assertThat(grunnkretser).contains(grunnkretsUtenfor2)
    }

    @Test
    fun `getStemmekretserSomGrenserTilKommunegrense skal ikke gi duplikater om flere grenser grenser mot samme stemmekrets`() {
        val stemmekretser = deltGeometriKretsService.getStemmekretserSomGrenserTilKommunegrense(KOMMUNENR, LocalDate.now())

        assertThat(stemmekretser).hasSize(3)
        assertThat(stemmekretser).contains(stemmekretsikommunen)
        assertThat(stemmekretser).contains(stemmekretsUtenfor1)
        assertThat(stemmekretser).contains(stemmekretsUtenfor2)
    }

    @Test
    fun `getDeltGeometriGrensekoblingerForGrunnkrets skal erstatte administrative grenser med med grenser for delt geometri`() {
        val nyeGrenser = deltGeometriKretsService.getDeltGeometriGrensekoblingerForGrunnkrets(grunnkretsikommunen, KOMMUNENR, LocalDate.now(), nyeAdministrativeGrenser)
        assertThat(nyeGrenser).hasSize(5)
        assertThat(nyeGrenser.map { it.grense }).contains(grenseAE)
        assertThat(nyeGrenser.map { it.grense }).contains(grenseEB)
    }

    @Test
    fun `getDeltGeometriGrensekoblingerForStemmekrets skal erstatte administrative grenser med med grenser for delt geometri`() {
        val nyeGrenser = deltGeometriKretsService.getDeltGeometriGrensekoblingerForStemmekrets(stemmekretsikommunen, KOMMUNENR, LocalDate.now(), nyeAdministrativeGrenser)
        assertThat(nyeGrenser).hasSize(5)
        assertThat(nyeGrenser.map { it.grense }).contains(grenseAE)
        assertThat(nyeGrenser.map { it.grense }).contains(grenseEB)
    }
}
