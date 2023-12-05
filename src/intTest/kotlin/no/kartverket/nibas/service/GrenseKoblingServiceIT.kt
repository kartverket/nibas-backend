package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.response.ObjektIdentifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.locationtech.jts.geom.Coordinate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month
import jakarta.persistence.EntityManager

@ActiveProfiles(profiles = ["security-off", "eventsDisabled"])
@Transactional
class GrenseKoblingServiceIT : TestWithDbContainer() {

    // Dato til grunnkrets og stemmekrets
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)

    @Autowired
    lateinit var grenseKoblingService: GrenseKoblingService

    @Test
    fun `fjernGrenseFraKretser klarer å fjerne grense fra krets`(){
        // lager en grunnkrets
        val grunnkrets = saveGrunnkrets(id = "test", lokalid = Lokalid("test")).apply {
            kommuneLokalid = UnikLokalid("1", Flatetype.KOMMUNE).apply {
                kommuner = mutableSetOf(Kommune().apply {
                    kommuneLokalid = UnikLokalid("2", Flatetype.KOMMUNE) }) } }
        // lager grense som danner et polygon med en ekstra kant
        val grenserForGrunnkrets = mockedPolygon(LocalDate.now(), Grensetype.Grunnkretsgrense, true)
        val kantGrense = grenserForGrunnkrets.last()

        // lager grunnkretsgrensekoblinger mellom grensene og grunnkretsen og legger de på grunnkretsen
        val grunnkretsGrenseKoblinger = grenserForGrunnkrets
            .map {
                GrunnkretsGrenseKobling(grunnkrets = grunnkrets, grense = it, GrenseKoblingAttributter())
            }
        grunnkrets.apply {
            grenser = grunnkretsGrenseKoblinger.toMutableSet()
        }

        val grenseKoblingMellomKantOgGrunnkrets = grunnkretsGrenseKoblinger.first { it.grense.lokalid == kantGrense.lokalid }

        val flaterSomHarFaattNyVersjon = mutableMapOf<Lokalid, Flate>()

        // fjern den frie kanten
        assertDoesNotThrow {
            grenseKoblingService.fjernGrenseFraKretserViaGrenseKobling(kantGrense, listOf(grenseKoblingMellomKantOgGrunnkrets), LocalDate.now(), flaterSomHarFaattNyVersjon, Endringstype.VEDTATT_GRENSEJUSTERING)
        }
        // Hent grunnkretsen etter at metoden har kjørt og se om kantgrensen er fjernet
        val oppdatertGrunnkrets = flaterSomHarFaattNyVersjon[Lokalid("test")] as Grunnkrets
        assertThat(oppdatertGrunnkrets.version).isEqualTo(grunnkrets.version+1)
        assertThat(oppdatertGrunnkrets.grenser.size).isEqualTo(4)
        assertThat(oppdatertGrunnkrets.grenser.map { it.grense }).doesNotContain(kantGrense)
    }

    @Test
    fun `lagGrunnkretsGrenseKoblingerForKretser lager korrekte GrunnkretsGrenseKoblinger`(){
        val mockedPolygon = mockedPolygon(november28In2022, Grensetype.Grunnkretsgrense)

        // Grunnkrets instansen av kretsen grensa skal få tilhørighet til
        val savedGrunnkrets = saveGrunnkrets(id = "test", lokalid = Lokalid("test")).apply {
            grenser = mutableSetOf(
                GrunnkretsGrenseKobling(this, mockedPolygon[0], GrenseKoblingAttributter()),
                GrunnkretsGrenseKobling(this, mockedPolygon[1], GrenseKoblingAttributter()),
                GrunnkretsGrenseKobling(this, mockedPolygon[2], GrenseKoblingAttributter()),
            )
            kommuneLokalid = UnikLokalid("1", Flatetype.KOMMUNE).apply {
                kommuner = mutableSetOf(Kommune().apply {
                    setGyldigFra(november28In2022)
                    kommuneLokalid = UnikLokalid("2", Flatetype.KOMMUNE) }) }
        }

        // Grense som skal få tilhørighet til grunnkretsen
        val grense = Grense("nyGrense", "nyGrense", Grensetype.Grunnkretsgrense).apply {
            grense = mockedPolygon[3].grense
        }

        // KontekstEgenskaper som refererer til grunnkrets som grensa skal få tilhørighet til
        val kontekstEgenskaper = KontekstEgenskaper(ObjektIdentifikator(Lokalid("test"), LocalDate.now()), KontekstEnhetType.GRUNNKRETS, 0)

        // Map som skal bli oppdatert med oppdatert grunnkrets
        val flaterSomHarFaattNyVersjon = mutableMapOf<Lokalid, Flate>()

        // Lager nye grunnkretsgrensekoblinger til kretsen for grensa
        val nyeGrunnkretsGrenseKoblinger = grenseKoblingService.lagGrunnkretsGrenseKoblingerForKretserViaKontekstEgenskaper(
            listOf(kontekstEgenskaper), grense, november28In2022, flaterSomHarFaattNyVersjon, Endringstype.VEDTATT_GRENSEJUSTERING
        )

        // henter siste versjon av grunnkretsen fra map
        val oppdatertGrunnkrets = flaterSomHarFaattNyVersjon[Lokalid("test")] as Grunnkrets

        // Sjekker det har blitt laget en ny versjon, at det er én ny grensekobling, og at det er fire grensekoblinger på den oppdaterte grunnkretsen
        assertThat(oppdatertGrunnkrets.version).isEqualTo(savedGrunnkrets.version+1)
        assertThat(nyeGrunnkretsGrenseKoblinger.size).isEqualTo(1)
        assertThat(oppdatertGrunnkrets.grenser.size).isEqualTo(4)
    }

    private fun mockPolygon(coordinatePairs: List<Pair<Coordinate, Coordinate>>, gyldigFra: LocalDate, grensetype: Grensetype): List<Grense> {
        return coordinatePairs.mapIndexed { index, pair ->
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
    }

    /**
     * Gir en liste med grenser (Grense) som danner én eller to polygoner.
     *
     * @param gyldigFra Gyldig fra-dato for de genererte grensene.
     * @param grensetype Typen grense (for eksempel Grensetype.Grunnkretsgrense).
     * @param medFeil Indikerer om en ekstra linje inkluderes for å danne et polygon med feil.
     *
     * @return En liste over Grense-objekter som representerer grensene som danner et polygon.
     *
     * @see Grense
     * @see Coordinate
     * @see LocalDate
     *
     * Visuell representasjon av mulige polygoner:
     *
     * Hovedpolygon:
     * (1.0, 3.0) ----- (3.0, 3.0)
     *     |                 |
     *     |                 |
     * (1.0, 1.0) ----- (3.0, 1.0)
     *
     * Andre Polygon (hvis medFeil er true):
     *
     * (1.0, 3.0) ----- (3.0, 3.0) ----- (4.0, 3.0)
     *   |                 |
     *   |                 |
     * (1.0, 1.0) ----- (3.0, 1.0)
     */
    private fun mockedPolygon(gyldigFra: LocalDate, grensetype: Grensetype, medFeil: Boolean = false): List<Grense> {
        var koordinater = listOf(
            Pair(Coordinate(1.0, 1.0), Coordinate(1.0, 3.0)),
            Pair(Coordinate(1.0, 3.0), Coordinate(3.0, 3.0)),
            Pair(Coordinate(3.0, 3.0), Coordinate(3.0, 1.0)),
            Pair(Coordinate(3.0, 1.0), Coordinate(1.0, 1.0)),
        )
        if (medFeil){
            koordinater = koordinater+listOf(Pair(Coordinate(3.0, 3.0), Coordinate(4.0, 3.0)))
        }
        return mockPolygon(koordinater, gyldigFra, grensetype)
    }
}
