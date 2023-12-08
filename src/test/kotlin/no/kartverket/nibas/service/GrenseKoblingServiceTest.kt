package no.kartverket.nibas.service

import io.mockk.mockk
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.response.ObjektIdentifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.service.deltgeometri.DeltGeometriAdministrativeGrenserService
import no.kartverket.nibas.utils.GrenseHelper
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.locationtech.jts.geom.Coordinate
import java.time.LocalDate
import java.util.Random

class GrenseKoblingServiceTest {

    val grunnkretsServiceMock = mockk<GrunnkretsService>()
    val stemmekretsServiceMock = mockk<StemmekretsService>()
    val kommuneService = mockk<KommuneService>()
    val kodelisteService = mockk<KodelisteService>()
    val grunnkretsRepository = mockk<GrunnkretsRepository>()
    val stemmekretsRepository = mockk<StemmekretsRepository>()

    val gyldighetsdato = LocalDate.now()

    val grense1 = Grense("1", grensetype = Grensetype.Grunnkretsgrense)

    val grunnkrets1 = Grunnkrets(lokalid = "A").apply { grunnkretsnavn = "A"; grunnkretsnummer = "1" }
    val grunnkrets2 = Grunnkrets(lokalid = "B").apply { grunnkretsnavn = "B"; grunnkretsnummer = "2" }

    val kontekstEgenskaper1 = KontekstEgenskaper(ObjektIdentifikator(Lokalid("A"), gyldighetsdato), KontekstEnhetType.GRUNNKRETS, 0)
    val kontekstEgenskaper2 = KontekstEgenskaper(ObjektIdentifikator(Lokalid("C"), gyldighetsdato), KontekstEnhetType.GRUNNKRETS, 0)


    val stemmekrets = Stemmekrets(lokalid = "A").apply { stemmekretsnavn = "A"; stemmekretsnummer = "1" }

    val grunnkretsGrenseKobling1 = GrunnkretsGrenseKobling(grunnkrets1, grense1, GrenseKoblingAttributter())
    val grunnkretsGrenseKobling2 = GrunnkretsGrenseKobling(grunnkrets2, grense1, GrenseKoblingAttributter())

    val deltGeometriAdministrativeGrenserService = DeltGeometriAdministrativeGrenserService(kommuneService, kodelisteService)

    val grenseKoblingService = GrenseKoblingService(
        grunnkretsServiceMock, stemmekretsServiceMock, deltGeometriAdministrativeGrenserService, grunnkretsRepository, stemmekretsRepository)

    @Test
    fun `getTilhorighetEndringerForGrense gir korrekte partisjoner`(){
        val eksisterendeTilhorigheter = listOf(grunnkretsGrenseKobling1, grunnkretsGrenseKobling2)
        val innsendteKontekstEgenskaper = listOf(kontekstEgenskaper1, kontekstEgenskaper2)

        val tilhorighetEndringer =
            grenseKoblingService.getTilhorighetEndringer(eksisterendeTilhorigheter, innsendteKontekstEgenskaper, gyldighetsdato)

        assertThat(tilhorighetEndringer.nye).containsOnly(kontekstEgenskaper2)
        assertThat(tilhorighetEndringer.uendrede).containsOnly(grunnkretsGrenseKobling1)
        assertThat(tilhorighetEndringer.frafallende).containsOnly(grunnkretsGrenseKobling2)
    }

    @Test
    fun `dannGrenseKoblingAttributterForGrenser lager korrekte grensekoblingattributter når det ikke er hull`(){
        val komplettPolygon = mockedMultiPolygon(LocalDate.now(), Grensetype.Grunnkretsgrense, false, true)
        val gmas = grenseKoblingService.dannGrenseKoblingAttributterForGrenser(komplettPolygon)
        gmas.forEach{ gma ->
            assertThat(gma.attributter.flateIndeks).isIn(0, 1)
            assertThat(gma.attributter.rekkefoelge).isIn(0,1,2,3)
            assertThat(gma.attributter.hullIndeks).isNull()
        }
    }

    @Test
    fun `dannGrenseKoblingAttributterForGrenser lager korrekte grensekoblingattributter når det er hull`(){
        val komplettPolygonMedHull = mockedMultiPolygon(LocalDate.now(), Grensetype.Grunnkretsgrense, true, true)
        val gmas = grenseKoblingService.dannGrenseKoblingAttributterForGrenser(komplettPolygonMedHull)
        gmas.forEach { gma ->
            assertThat(gma.attributter.flateIndeks).isIn(0, 1)
            assertThat(gma.attributter.rekkefoelge).isIn(0,1,2,3)
            if (gma.attributter.hullIndeks != null){
                assertThat(gma.attributter.hullIndeks).isIn(0,1)
            }
        }
    }

    private fun mockPolygon(coordinatePairs: List<Pair<Coordinate, Coordinate>>, gyldigFra: LocalDate, grensetype: Grensetype, medReversering: Boolean): List<Grense> {

        val grenser = coordinatePairs.mapIndexed { index, pair ->
            Grense(id = index.toString(), lokalid = index.toString(), grensetype = grensetype)
                .apply {
                    setGyldigFra(gyldigFra)
                    grense = GrenseHelper.geometryFactory.createLineString(
                        arrayOf(
                            pair.first,
                            pair.second
                        )
                    )
                }
        }
        if (medReversering){
            val randomIndexToReverse = Random().nextInt(0,coordinatePairs.size-1)
            grenser[randomIndexToReverse].grense = grenser[randomIndexToReverse].grense.reverse()
        }
        return grenser
    }

    /**
     * Genererer en liste med grenselinjer (Grense) som danner et multipolygon med eller uten hull.
     *
     * @param gyldigFra Gyldig fra-dato for de genererte grensene.
     * @param grensetype Typen grense (for eksempel Grensetype.Grunnkretsgrense).
     * @param medHull Indikerer om en indre hullgrense skal inkluderes for å danne et multipolygon.
     * @param medReversering Indikerer om en tilfeldig grense i multipolygonet skal reverseres.
     *
     * @return En liste over Grense-objekter som representerer grensene i multipolygonet.
     *
     * @see Grense
     * @see Coordinate
     * @see LocalDate
     *
     * Visuell representasjon av mulige polygoner (hvis medHull er true):
     *
     * Første polygon:
     *
     *   (1.0, 5.0) ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- (8.0, 5.0)
     *      |                                                                       |
     *      |    (2.0, 4.0) ----- (4.0, 4.0)   (5.0, 4.0) ----- (7.0, 4.0)          |
     *      |       |                  |            |               |               |
     *      |       |                  |            |               |               |
     *      |   (2.0, 2.0) ----- (4.0, 2.0)     (5.0, 2.0) ----- (7.0, 2.0)         |
     *      |                                                                       |
     *   (1.0, 1.0) ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- (8.0, 1.0)
     *
     * Andre Polygon:
     *
     *  (10.0, 5.0) ----- ----- ----- ----- ----- (14.0, 5.0)
     *      |                                         |
     *      |    (11.0, 4.0) ----- (13.0, 4.0)        |
     *      |       |                  |              |
     *      |       |                  |              |
     *      |   (11.0, 2.0) ----- (13.0, 2.0)         |
     *      |                                         |
     *   (10.0, 1.0) ----- ----- ----- ----- ----- (14.0, 1.0)
     */
    private fun mockedMultiPolygon(gyldigFra: LocalDate, grensetype: Grensetype, medHull: Boolean, medReversering: Boolean): List<Grense> {
        val koordinaterSkall = listOf(
            Pair(Coordinate(1.0, 1.0), Coordinate(1.0, 5.0)),
            Pair(Coordinate(1.0, 5.0), Coordinate(8.0, 5.0)),
            Pair(Coordinate(8.0, 5.0), Coordinate(8.0, 1.0)),
            Pair(Coordinate(8.0, 1.0), Coordinate(1.0, 1.0)),
            Pair(Coordinate(10.0, 1.0), Coordinate(10.0, 5.0)),
            Pair(Coordinate(10.0, 5.0), Coordinate(14.0, 5.0)),
            Pair(Coordinate(14.0, 5.0), Coordinate(14.0, 1.0)),
            Pair(Coordinate(14.0, 1.0), Coordinate(10.0, 1.0)),
        )
        if(medHull){
            koordinaterSkall+ listOf(
                Pair(Coordinate(2.0, 2.0), Coordinate(2.0, 4.0)),
                Pair(Coordinate(2.0, 4.0), Coordinate(4.0, 4.0)),
                Pair(Coordinate(4.0, 4.0), Coordinate(4.0, 2.0)),
                Pair(Coordinate(4.0, 2.0), Coordinate(2.0, 2.0)),
                Pair(Coordinate(5.0, 2.0), Coordinate(5.0, 4.0)),
                Pair(Coordinate(5.0, 4.0), Coordinate(7.0, 4.0)),
                Pair(Coordinate(7.0, 4.0), Coordinate(7.0, 2.0)),
                Pair(Coordinate(7.0, 2.0), Coordinate(5.0, 2.0)),
                Pair(Coordinate(11.0, 2.0), Coordinate(11.0, 4.0)),
                Pair(Coordinate(11.0, 4.0), Coordinate(13.0, 4.0)),
                Pair(Coordinate(13.0, 4.0), Coordinate(13.0, 2.0)),
                Pair(Coordinate(13.0, 2.0), Coordinate(11.0, 2.0))
            )
        }
        return mockPolygon(koordinaterSkall, gyldigFra, grensetype, medReversering)
    }
}
