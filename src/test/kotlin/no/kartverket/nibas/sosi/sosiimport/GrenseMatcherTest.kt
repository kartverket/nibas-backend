package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.sosi.model.SosiGeometri
import no.kartverket.nibas.sosi.model.SosiIdent
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
    fun testFlyttingAvKnutepunkt() {
        //Opprinnelig knutepunkt: (3.0, 2.0), flyttet til (3.0, 3.0)
        val opprinneligKnutepunkt = Coordinate(3.0, 2.0)
        val nyttKnutepunkt = Coordinate(3.0, 3.0)

        var segment1 = lagSosiGeometri("", Coordinate(1.0, 2.0), nyttKnutepunkt)
        var segment2 = lagSosiGeometri("", nyttKnutepunkt, Coordinate(4.0, 1.0))
        var segment3 = lagSosiGeometri("", nyttKnutepunkt, Coordinate(4.0, 5.0))

        val grenseMatcher = GrenseMatcher()
        val splittSegment1 = grenseMatcher.splittGrense("", segment1, Coordinate(1.0, 2.0), opprinneligKnutepunkt)
        assertEquals(2.6, splittSegment1.splittetGrense.endPoint.x)
        assertEquals(2.8, splittSegment1.splittetGrense.endPoint.y)

        val splittSegment2 = grenseMatcher.splittGrense("", segment2, opprinneligKnutepunkt, Coordinate(4.0, 1.0))
        assertEquals(3.4, splittSegment2.splittetGrense.startPoint.x)
        assertEquals(2.2, splittSegment2.splittetGrense.startPoint.y)

        val splittSegment3 = grenseMatcher.splittGrense("", segment3, opprinneligKnutepunkt, Coordinate(4.0, 5.0))
        assertEquals(3.0, splittSegment3.splittetGrense.startPoint.x)
        assertEquals(3.0, splittSegment3.splittetGrense.startPoint.y)

        val finnErstatningskoordinater = grenseMatcher.finnErstatningskoordinater()
        val erstatningskoordinat = finnErstatningskoordinater[opprinneligKnutepunkt]

        assertThat(erstatningskoordinat?.skalBrukeKnutepunkt).isTrue
        assertThat(erstatningskoordinat?.nyttKoordinat).isEqualTo(nyttKnutepunkt)
        assertThat(erstatningskoordinat?.koordinaterSomErstattes?.size).isEqualTo(3)
    }

    @Test
    fun thatFinnGrensesegmentMedKoordinatReturnererKorrektSegment() {
        val seg1 = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(20.0, 0.0))
        val seg2 = lagSosiGeometri("", Coordinate(20.0, 0.0), Coordinate(40.0, 0.0))
        val seg3 = lagSosiGeometri("", Coordinate(40.0, 0.0), Coordinate(60.0, 0.0))
        val seg4 = lagSosiGeometri("", Coordinate(60.0, 0.0), Coordinate(80.0, 0.0))
        val segmenter = listOf(seg1, seg2, seg3, seg4)

        val grenseMatcher = GrenseMatcher()
        assertEquals(seg2, grenseMatcher.finnGrenseSegmentMedKoordinat(Coordinate(30.0, 0.0), segmenter), "Expected seg2")
        assertEquals(seg4, grenseMatcher.finnGrenseSegmentMedKoordinat(Coordinate(70.0, 0.0), segmenter), "Expected seg4")
        assertNull(grenseMatcher.finnGrenseSegmentMedKoordinat(Coordinate(100.0, 0.0), segmenter), "Expected no segment")
    }

    @Test
    fun thatGrenserSomDekkerNyGrense_BrukerBesteTreff() {
        val grenseSomDekkesAvFlereGrenser = lagSosiGeometri("", Coordinate(216436.1, 6635966.51), Coordinate(216436.9, 6635968.27))
        val segmentSomDekkerBest = lagSosiGeometri("", Coordinate(216436.1, 6635966.51), Coordinate(216436.9, 6635968.27))
        val grenserSomDekker = listOf(
            lagSosiGeometri("", Coordinate(216394.04, 6635843.84), Coordinate(216404.22, 6635890.72), Coordinate(216430.56, 6635953.35), Coordinate(216436.10, 6635966.51)),
            segmentSomDekkerBest,
            lagSosiGeometri("", Coordinate(216436.9, 6635968.27), Coordinate(216437.91, 6635970.49)),
            lagSosiGeometri("", Coordinate(216437.91, 6635970.49), Coordinate(216434.11, 6636002.31), Coordinate(216484.55, 6636007.36))
        )

        val grenseMatcher = GrenseMatcher()
        val matchendeGrenser = grenseMatcher.grenserSomDekkerNyGrense(grenserSomDekker, grenseSomDekkesAvFlereGrenser)
        assertThat(matchendeGrenser).size().isEqualTo(1)
        assertThat(matchendeGrenser.first()).isEqualTo(segmentSomDekkerBest)
    }


    @Test
    fun thatGrenserSomDekkerNyGrense_IngenTreff(){
        val grense = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(300.0, 0.0))
        val skalSjekkesMot = listOf(
            lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(100.0, 0.0)),
            lagSosiGeometri("", Coordinate(100.0, 0.0), Coordinate(200.0, 0.0)),
            lagSosiGeometri("", Coordinate(200.0, 0.0), Coordinate(250.0, 0.0))
        )

        val grenseMatcher = GrenseMatcher()
        val matchendeGrenser = grenseMatcher.grenserSomDekkerNyGrense(skalSjekkesMot, grense)
        assertThat(matchendeGrenser).isEmpty()
    }

    @Test
    fun thatGrenserSomDekkerNyGrense_TreffInnenEtSegment(){
        val grense = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(300.0, 0.0))

        val seg1 = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(400.0, 0.0))
        val seg2 = lagSosiGeometri("", Coordinate(400.0, 0.0), Coordinate(500.0, 0.0))
        val skalSjekkesMot = listOf(seg1, seg2)

        val grenseMatcher = GrenseMatcher()
        val matchendeGrenser = grenseMatcher.grenserSomDekkerNyGrense(skalSjekkesMot, grense)
        assertThat(matchendeGrenser).size().isEqualTo(1)
        assertThat(matchendeGrenser.first()).isEqualTo(seg1)
    }

    @Test
    fun thatGrenserSomDekkerNyGrense_TreffOverFlereSegmenter(){
        val grense = lagSosiGeometri("G1", Coordinate(0.0, 0.0), Coordinate(300.0, 0.0))

        val seg1 = lagSosiGeometri("S1", Coordinate(0.0, 0.0), Coordinate(200.0, 0.0))
        val seg2 = lagSosiGeometri("S2", Coordinate(200.0, 0.0), Coordinate(300.0, 0.0))
        val skalSjekkesMot = listOf(seg1, seg2)

        val grenseMatcher = GrenseMatcher()
        val matchendeGrenser = grenseMatcher.grenserSomDekkerNyGrense(skalSjekkesMot, grense)
        assertThat(matchendeGrenser).size().isEqualTo(2)
        assertThat(matchendeGrenser[0]).isEqualTo(seg1)
        assertThat(matchendeGrenser[1]).isEqualTo(seg2)
    }

    @Test
    fun thatGrenserSomDekkerNyGrense_TreffPaSisteOgForsteOverlapping(){
        val grense = lagSosiGeometri("", Coordinate(50.0, 0.0), Coordinate(0.0, 0.0), Coordinate(0.0, 50.0))

        val seg1 = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(100.0, 0.0))
        val seg2 = lagSosiGeometri("", Coordinate(100.0, 0.0), Coordinate(100.0, 100.0))
        val seg3 = lagSosiGeometri("", Coordinate(100.0, 100.0), Coordinate(0.0, 100.0))
        val seg4 = lagSosiGeometri("", Coordinate(0.0, 100.0), Coordinate(0.0, 0.0))
        val skalSjekkesMot = listOf(seg1, seg2, seg3, seg4)

        val grenseMatcher = GrenseMatcher()
        val matchendeGrenser = grenseMatcher.grenserSomDekkerNyGrense(skalSjekkesMot, grense)
        assertThat(matchendeGrenser).size().isEqualTo(2)
        assertThat(matchendeGrenser[0]).isEqualTo(seg4)
        assertThat(matchendeGrenser[1]).isEqualTo(seg1)
    }

    @Test
    fun thatMaSplittes_False_SegmentStartsAtStartAndEndsAtEnd() {
        val grense = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        val grenseMatcher = GrenseMatcher()
        assertThat(grenseMatcher.maSplittes(grense, Coordinate(0.0, 0.0), Coordinate(20.0, 0.0))).isFalse
    }
    @Test
    fun thatMaSplittes_True_OnlyStartInCorrectLocation(){
        val grense = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        val grenseMatcher = GrenseMatcher()
        assertThat(grenseMatcher.maSplittes(grense, Coordinate(0.0, 0.0), Coordinate(15.0, 0.0))).isTrue
    }

    @Test
    fun thatMaSplittes_True_OnlyEndInCorrectLocation() {
        val grense = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        val grenseMatcher = GrenseMatcher()
        assertThat(grenseMatcher.maSplittes(grense, Coordinate(15.0, 0.0), Coordinate(20.0, 0.0))).isTrue
    }

    @Test
    fun thatMaSplittes_False_OnlyStartIsChecked(){
        val grense = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        val grenseMatcher = GrenseMatcher()
        assertThat(grenseMatcher.maSplittes(grense, start = Coordinate(0.0, 0.0))).isFalse
    }

    @Test
    fun thatMaSplittes_False_OnlyEndIsChecked(){
        val grense = lagSosiGeometri("", Coordinate(0.0, 0.0), Coordinate(10.0, 0.0), Coordinate(20.0, 0.0))
        val grenseMatcher = GrenseMatcher()
        assertThat(grenseMatcher.maSplittes(grense, start = null, slutt = Coordinate(20.0, 0.0))).isFalse
    }

    @Test
    fun thatSplittGrense_splitFromStartToCoordinate(){
        val coord0 = Coordinate(0.0, 0.0)
        val coord1 = Coordinate(20.0, 0.0)
        val coord2 = Coordinate(40.0, 0.0)
        val grenseSomSkalSplittes = lagSosiGeometri("", coord0, coord1, coord2)

        val slutt = Coordinate(30.0, 0.0)
        val grenseMatcher = GrenseMatcher()
        val splittet = grenseMatcher.splittGrense("REF123", grenseSomSkalSplittes, startCoord = null, endCoord = slutt)

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
        val grenseSomSkalSplittes = lagSosiGeometri("", coord0, coord1, coord2)

        val start = Coordinate(10.0, 0.0)
        val grenseMatcher = GrenseMatcher()
        val splittet = grenseMatcher.splittGrense("REF123", grenseSomSkalSplittes, startCoord = start, endCoord = null)

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
        val grenseSomSkalSplittes = lagSosiGeometri("", coord0, coord1, coord2)

        val start = Coordinate(10.0, 0.0)
        val slutt = Coordinate(30.0, 0.0)
        val grenseMatcher = GrenseMatcher()
        val splittet = grenseMatcher.splittGrense("REF123", grenseSomSkalSplittes, startCoord = start, endCoord = slutt)

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
        val grenseSomSkalSplittes = lagSosiGeometri("", coord0, coord1, coord2)

        val start = Coordinate(30.0, 0.0)
        val slutt = Coordinate(10.0, 0.0)
        val grenseMatcher = GrenseMatcher()
        val splittet = grenseMatcher.splittGrense("REF123", grenseSomSkalSplittes, startCoord = slutt, endCoord = start)

        assertThat(splittet.basertPaGrense).isEqualTo(grenseSomSkalSplittes)
        assertThat(splittet.splittetGrense.coordinates.size).isEqualTo(3)
        assertThat(splittet.splittetGrense.coordinates[0]).isEqualTo(slutt)
        assertThat(splittet.splittetGrense.coordinates[1]).isEqualTo(coord1)
        assertThat(splittet.splittetGrense.coordinates[2]).isEqualTo(start)
    }

    @Test
    fun thatSplittGrenser_coveredByMultipleSegments(){
        val sosiRef = SosiReferanse(2, mutableListOf(SosiVerdi("G1")))
        val grense = lagSosiGeometri("G1", Coordinate(20.0, 0.0), Coordinate(100.0, 0.0))

        val seg1 = lagSosiGeometri("S1", Coordinate(0.0, 0.0), Coordinate(35.0, 0.0))
        val seg2 = lagSosiGeometri("S2", Coordinate(35.0, 0.0), Coordinate(50.0, 0.0))
        val seg3 = lagSosiGeometri("S3", Coordinate(50.0, 0.0), Coordinate(75.0, 0.0))
        val seg4 = lagSosiGeometri("S4", Coordinate(75.0, 0.0), Coordinate(125.0, 0.0))

        val grenseMatcher = GrenseMatcher()
        val splitTilImport = grenseMatcher.splittGrenser(sosiRef, grense, grenserSomDekker = listOf(seg1, seg2, seg3, seg4))
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

    @Test
    fun splitTilImportLokalId_generertLokalIdSkalVeareDeterministisk() {
        val sosiRef = SosiReferanse(1, mutableListOf(SosiVerdi("g")))
        val grense1 = lagSosiGeometriMedUUID("grense", Coordinate(20.0, 0.0), Coordinate(100.0, 0.0))
        val seg1 = lagSosiGeometriMedUUID("seg", Coordinate(0.0, 0.0), Coordinate(35.0, 0.0))
        val grense2 = lagSosiGeometriMedUUID("grense", Coordinate(20.0, 0.0), Coordinate(100.0, 0.0))
        val seg2 = lagSosiGeometriMedUUID("seg", Coordinate(0.0, 0.0), Coordinate(35.0, 0.0))

        val grenseMatcher = GrenseMatcher()
        val splitTilImport1 = grenseMatcher.splittGrenser(sosiRef, grense1, grenserSomDekker = listOf(seg1))
        val splitTilImport2 = grenseMatcher.splittGrenser(sosiRef, grense2, grenserSomDekker = listOf(seg2))
        val lokalId1 = splitTilImport1.map { it.lokalId() }.first()
        val lokalId2 = splitTilImport2.map { it.lokalId() }.first()

        assertThat(lokalId1).isEqualTo(lokalId2)
    }

    @Test
    fun splitTilImportLokalId_alleSplittedeGrenserSkalHaUnikLokalId() {
        val sosiRef = SosiReferanse(1, mutableListOf(SosiVerdi("g")))
        val grense = lagSosiGeometriMedUUID("grense", Coordinate(20.0, 0.0), Coordinate(100.0, 0.0))

        val seg1 = lagSosiGeometriMedUUID("seg1", Coordinate(0.0, 0.0), Coordinate(35.0, 0.0))
        val seg2 = lagSosiGeometriMedUUID("seg2", Coordinate(35.0, 0.0), Coordinate(50.0, 0.0))
        val seg3 = lagSosiGeometriMedUUID("seg3", Coordinate(50.0, 0.0), Coordinate(75.0, 0.0))
        val seg4 = lagSosiGeometriMedUUID("seg4", Coordinate(75.0, 0.0), Coordinate(125.0, 0.0))

        val grenseMatcher = GrenseMatcher()
        val splitTilImport = grenseMatcher.splittGrenser(sosiRef, grense, grenserSomDekker = listOf(seg1, seg2, seg3, seg4))
        val lokalIder = splitTilImport.map { it.lokalId() }
        assertThat(lokalIder).doesNotHaveDuplicates()
    }

    private fun lagSosiGeometri(referanse: String, vararg koordinater: Coordinate): SosiGeometri {
        val sosiKoordinater = koordinater.map { koordinat ->
            SosiKoordinat(koordinat.y.toString(), koordinat.x.toString()) as SosiVerdi
        }.toMutableList()

        val nordOest = SosiNordOest(niva = 1, verdier = sosiKoordinater)
        val geometri = SosiGeometri(type = "KURVE", 1, mutableListOf(SosiVerdi(referanse)))
        geometri.addBarn(nordOest)
        return geometri
    }

    private fun lagSosiGeometriMedUUID(uuid: String, vararg koordinater: Coordinate): SosiGeometri {
        val geometri = lagSosiGeometri("ref", *koordinater)
        geometri.addBarn(SosiIdent(niva = 1, verdier = mutableListOf(SosiVerdi(uuid))))
        return geometri
    }
}
