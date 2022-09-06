package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.sosi.model.SosiKoordinat
import no.kartverket.nibas.sosi.model.SosiNordOest
import no.kartverket.nibas.sosi.model.SosiReferanse
import no.kartverket.nibas.sosi.model.SosiVerdi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import kotlin.test.assertNull

internal class GrenseMatcherTest {

    @Test
    fun thatFinnGrensesegmentMedKoordinatReturnererKorrektSegment() {
        val seg1 = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(20.0, 0.0))
        val seg2 = lagSosiGeometri(Coordinate(20.0, 0.0), Coordinate(40.0, 0.0))
        val seg3 = lagSosiGeometri(Coordinate(40.0, 0.0), Coordinate(60.0, 0.0))
        val seg4 = lagSosiGeometri(Coordinate(60.0, 0.0), Coordinate(80.0, 0.0))
        val segmenter = listOf(seg1, seg2, seg3, seg4)

        assertEquals(seg2, GrenseMatcher.finnGrenseSegmentMedKoordinat(Coordinate(30.0, 0.0), segmenter), "Expected seg2")
        assertEquals(seg4, GrenseMatcher.finnGrenseSegmentMedKoordinat(Coordinate(70.0, 0.0), segmenter), "Expected seg4")
        assertNull(GrenseMatcher.finnGrenseSegmentMedKoordinat(Coordinate(100.0, 0.0), segmenter), "Expected no segment")
    }

    @Test
    fun thatGrenserSomDekkerNyGrense_IngenTreff(){
        val grense = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(300.0, 0.0))
        val skalSjekkesMot = listOf(
            lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(100.0, 0.0)),
            lagSosiGeometri(Coordinate(100.0, 0.0), Coordinate(200.0, 0.0)),
            lagSosiGeometri(Coordinate(200.0, 0.0), Coordinate(250.0, 0.0))
        )
        val matchendeGrenser = GrenseMatcher.grenserSomDekkerNyGrense(skalSjekkesMot, grense)
        assertThat(matchendeGrenser).isEmpty()
    }

    @Test
    fun thatGrenserSomDekkerNyGrense_TreffInnenEtSegment(){
        val grense = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(300.0, 0.0))

        val seg1 = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(400.0, 0.0))
        val seg2 = lagSosiGeometri(Coordinate(400.0, 0.0), Coordinate(500.0, 0.0))
        val skalSjekkesMot = listOf(seg1, seg2)

        val matchendeGrenser = GrenseMatcher.grenserSomDekkerNyGrense(skalSjekkesMot, grense)
        assertThat(matchendeGrenser).size().isEqualTo(1)
        assertThat(matchendeGrenser.first()).isEqualTo(seg1)
    }

    @Test
    fun thatGrenserSomDekkerNyGrense_TreffOverFlereSegmenter(){
        val grense = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(300.0, 0.0))

        val seg1 = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(200.0, 0.0))
        val seg2 = lagSosiGeometri(Coordinate(200.0, 0.0), Coordinate(300.0, 0.0))
        val skalSjekkesMot = listOf(seg1, seg2)

        val matchendeGrenser = GrenseMatcher.grenserSomDekkerNyGrense(skalSjekkesMot, grense)
        assertThat(matchendeGrenser).size().isEqualTo(2)
        assertThat(matchendeGrenser[0]).isEqualTo(seg1)
        assertThat(matchendeGrenser[1]).isEqualTo(seg2)
    }

    @Test
    fun thatGrenserSomDekkerNyGrense_TreffPaSisteOgForsteOverlapping(){
        val grense = lagSosiGeometri(Coordinate(50.0, 0.0), Coordinate(0.0, 0.0), Coordinate(0.0, 50.0))

        val seg1 = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(100.0, 0.0))
        val seg2 = lagSosiGeometri(Coordinate(100.0, 0.0), Coordinate(100.0, 100.0))
        val seg3 = lagSosiGeometri(Coordinate(100.0, 100.0), Coordinate(0.0, 100.0))
        val seg4 = lagSosiGeometri(Coordinate(0.0, 100.0), Coordinate(0.0, 0.0))
        val skalSjekkesMot = listOf(seg1, seg2, seg3, seg4)

        val matchendeGrenser = GrenseMatcher.grenserSomDekkerNyGrense(skalSjekkesMot, grense)
        assertThat(matchendeGrenser).size().isEqualTo(2)
        assertThat(matchendeGrenser[0]).isEqualTo(seg4)
        assertThat(matchendeGrenser[1]).isEqualTo(seg1)
    }

    @Test
    fun thatMaSplittes_False_SegmentStartsAtStartAndEndsAtEnd() {
        val grense = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        assertThat(GrenseMatcher.maSplittes(grense, Coordinate(0.0, 0.0), Coordinate(20.0, 0.0))).isFalse
    }
    @Test
    fun thatMaSplittes_True_OnlyStartInCorrectLocation(){
        val grense = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        assertThat(GrenseMatcher.maSplittes(grense, Coordinate(0.0, 0.0), Coordinate(15.0, 0.0))).isTrue
    }

    @Test
    fun thatMaSplittes_True_OnlyEndInCorrectLocation() {
        val grense = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        assertThat(GrenseMatcher.maSplittes(grense, Coordinate(15.0, 0.0), Coordinate(20.0, 0.0))).isTrue
    }

    @Test
    fun thatMaSplittes_False_OnlyStartIsChecked(){
        val grense = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        assertThat(GrenseMatcher.maSplittes(grense, start = Coordinate(0.0, 0.0))).isFalse
    }

    @Test
    fun thatMaSplittes_False_OnlyEndIsChecked(){
        val grense = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        assertThat(GrenseMatcher.maSplittes(grense, start = null, slutt = Coordinate(20.0, 0.0))).isFalse
    }

    @Test
    fun thatSplittGrense_splitFromStartToCoordinate(){
        val coord0 = Coordinate(0.0, 0.0)
        val coord1 = Coordinate(20.0, 0.0)
        val coord2 = Coordinate(40.0, 0.0)
        val grenseSomSkalSplittes = lagSosiGeometri(coord0, coord1, coord2)

        val slutt = Coordinate(30.0, 0.0)
        val splittet = GrenseMatcher.splittGrense(grenseSomSkalSplittes, startCoord = null, endCoord = slutt)

        assertThat(splittet.basertPaGrense).isEqualTo(grenseSomSkalSplittes)
        assertThat(splittet.splittetGrense.coordinates.size).isEqualTo(3)
        assertThat(splittet.splittetGrense.coordinates[0]).isEqualTo(coord0)
        assertThat(splittet.splittetGrense.coordinates[1]).isEqualTo(coord1)
        assertThat(splittet.splittetGrense.coordinates[2]).isEqualTo(slutt)
    }

    @Test
    fun thatSplittGrense_splitFromCoordinateToEnd(){
        val coord0 = Coordinate(0.0, 0.0)
        val coord1 = Coordinate(20.0, 0.0)
        val coord2 = Coordinate(40.0, 0.0)
        val grenseSomSkalSplittes = lagSosiGeometri(coord0, coord1, coord2)

        val start = Coordinate(10.0, 0.0)
        val splittet = GrenseMatcher.splittGrense(grenseSomSkalSplittes, startCoord = start, endCoord = null)

        assertThat(splittet.basertPaGrense).isEqualTo(grenseSomSkalSplittes)
        assertThat(splittet.splittetGrense.coordinates.size).isEqualTo(3)
        assertThat(splittet.splittetGrense.coordinates[0]).isEqualTo(start)
        assertThat(splittet.splittetGrense.coordinates[1]).isEqualTo(coord1)
        assertThat(splittet.splittetGrense.coordinates[2]).isEqualTo(coord2)
    }

    @Test
    fun thatSplittGrense_splitFromCoordinateToCoordinate(){
        val coord0 = Coordinate(0.0, 0.0)
        val coord1 = Coordinate(20.0, 0.0)
        val coord2 = Coordinate(40.0, 0.0)
        val grenseSomSkalSplittes = lagSosiGeometri(coord0, coord1, coord2)

        val start = Coordinate(10.0, 0.0)
        val slutt = Coordinate(30.0, 0.0)
        val splittet = GrenseMatcher.splittGrense(grenseSomSkalSplittes, startCoord = start, endCoord = slutt)

        assertThat(splittet.basertPaGrense).isEqualTo(grenseSomSkalSplittes)
        assertThat(splittet.splittetGrense.coordinates.size).isEqualTo(3)
        assertThat(splittet.splittetGrense.coordinates[0]).isEqualTo(start)
        assertThat(splittet.splittetGrense.coordinates[1]).isEqualTo(coord1)
        assertThat(splittet.splittetGrense.coordinates[2]).isEqualTo(slutt)
    }

    @Test
    fun thatSplittGrense_endIsBeforeStart(){
        val coord0 = Coordinate(0.0, 0.0)
        val coord1 = Coordinate(20.0, 0.0)
        val coord2 = Coordinate(40.0, 0.0)
        val grenseSomSkalSplittes = lagSosiGeometri(coord0, coord1, coord2)

        val start = Coordinate(30.0, 0.0)
        val slutt = Coordinate(10.0, 0.0)
        val splittet = GrenseMatcher.splittGrense(grenseSomSkalSplittes, startCoord = slutt, endCoord = start)

        assertThat(splittet.basertPaGrense).isEqualTo(grenseSomSkalSplittes)
        assertThat(splittet.splittetGrense.coordinates.size).isEqualTo(3)
        assertThat(splittet.splittetGrense.coordinates[0]).isEqualTo(slutt)
        assertThat(splittet.splittetGrense.coordinates[1]).isEqualTo(coord1)
        assertThat(splittet.splittetGrense.coordinates[2]).isEqualTo(start)
    }

    @Test
    fun thatSplittGrenser_coveredByMultipleSegments(){
        val sosiRef = SosiReferanse(1, mutableListOf())
        val grense = lagSosiGeometri(Coordinate(20.0, 0.0), Coordinate(100.0, 0.0))

        val seg1 = lagSosiGeometri(Coordinate(0.0, 0.0), Coordinate(35.0, 0.0))
        val seg2 = lagSosiGeometri(Coordinate(35.0, 0.0), Coordinate(50.0, 0.0))
        val seg3 = lagSosiGeometri(Coordinate(50.0, 0.0), Coordinate(75.0, 0.0))
        val seg4 = lagSosiGeometri(Coordinate(75.0, 0.0), Coordinate(125.0, 0.0))

        val splitTilImport = GrenseMatcher.splittGrenser(sosiRef, grense, grenserSomDekker = listOf(seg1, seg2, seg3, seg4))
        assertThat(splitTilImport.size).isEqualTo(4)
        assertThat(splitTilImport[0].splitRef.basertPaGrense).isEqualTo(seg1)
        assertThat(splitTilImport[0].splitRef.splittetGrense.startPoint.x).isEqualTo(grense.asLineString().startPoint.x)
        assertThat(splitTilImport[0].splitRef.splittetGrense.endPoint.x).isEqualTo(seg1.asLineString().endPoint.x)

        assertThat(splitTilImport[1].splitRef.basertPaGrense).isEqualTo(seg2)
        assertThat(splitTilImport[1].splitRef.splittetGrense).isEqualTo(seg2.asLineString())

        assertThat(splitTilImport[2].splitRef.basertPaGrense).isEqualTo(seg3)
        assertThat(splitTilImport[2].splitRef.splittetGrense).isEqualTo(seg3.asLineString())

        assertThat(splitTilImport[3].splitRef.basertPaGrense).isEqualTo(seg4)
        assertThat(splitTilImport[3].splitRef.splittetGrense.startPoint.x).isEqualTo(seg4.asLineString().startPoint.x)
        assertThat(splitTilImport[3].splitRef.splittetGrense.endPoint.x).isEqualTo(grense.asLineString().endPoint.x)
    }

    private fun lagSosiGeometri(vararg koordinater: Coordinate): SosiGeometri {
        val sosiKoordinater = koordinater.map { koordinat ->
            SosiKoordinat(koordinat.y.toString(), koordinat.x.toString()) as SosiVerdi
        }.toMutableList()

        val nordOest = SosiNordOest(niva = 1, verdier = sosiKoordinater)
        val geometri = SosiGeometri(type = "KURVE", 1, mutableListOf())
        geometri.addBarn(nordOest)
        return geometri
    }
}
