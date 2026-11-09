package no.kartverket.nibas.vedtak2027

import com.ninjasquad.springmockk.MockkBean
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertFailsWith

/**
 * Tester flyttingen på et lite rutenett med samme oppbygning som Liabygda:
 *
 * ```
 *  y=3 +--------+--------+--------+
 *      | A1     | A2     | B1     |
 *      |15250102|15250103|15780101|
 *  y=2 +--------+--------+--------+
 *      | A3     | LIA    | B2     |
 *      |15250104|15250101|15780102|
 *  y=1 +--------+--------+--------+
 *      | A4     | B3     | B4     |
 *      |15250105|15780201|15780202|
 *  y=0 +--------+--------+--------+
 *     x=0      x=1      x=2      x=3
 * ```
 *
 * Kommune A er "Stranda" (1525) og B er "Fjord" (1578), i samme fylke. LIA flyttes fra A til B.
 * Den har to naboer i hver kommune, og naboene i B ligger i hvert sitt delområde (01 og 02).
 * Med nytt nummer 15780103 skal grensen mot B2 bli grunnkretsgrense og grensen mot B3 delområdegrense.
 *
 * Stemmekretser: i A er LIA egen stemmekrets 0001 og A1–A4 er 0002 (som Ringstad). I B er B1+B2
 * stemmekrets 0001 og B3+B4 stemmekrets 0002. Der grunnkretsene har grunnkrets- eller delområdegrense,
 * har stemmekretsene en egen stemmekretsgrense på samme linje, slik som i produksjonsdataene.
 */
@ActiveProfiles(profiles = ["localhost", "eventsDisabled"])
@Transactional
class Vedtak2027JobServiceIT : TestWithDbContainer() {

    @MockkBean
    private lateinit var kodelisteServiceMock: KodelisteService

    @Autowired
    private lateinit var jobService: Vedtak2027JobService

    private val opprettet = LocalDate.of(2020, 1, 1)
    private val virkningsdato = LocalDate.of(2027, 1, 1)
    private val dagenFoer = virkningsdato.minusDays(1)

    private val plan = Vedtak2027Plan(
        virkningsdato = virkningsdato,
        fraKommunenummer = "1525",
        tilKommunenummer = "1578",
        kretsnavn = "Liabygda",
        grunnkretsnummer = "15250101",
        nyttGrunnkretsnummer = "15780103",
        stemmekretsnummer = "0001",
        nyttStemmekretsnummer = "0003",
        nyttStemmekretsnavn = "LIABYGDA",
        arealToleranseM2 = 1e-9,
    )

    @BeforeEach
    fun lagRutenett() {
        // Grunnkretsene, med cellens nedre venstre hjørne som map key
        val celler = mapOf(
            (0 to 2) to Celle("a1", "15250102", "A1"), (1 to 2) to Celle("a2", "15250103", "A2"), (2 to 2) to Celle("b1", "15780101", "B1"),
            (0 to 1) to Celle("a3", "15250104", "A3"), (1 to 1) to Celle("lia", "15250101", "Liabygda"), (2 to 1) to Celle("b2", "15780102", "B2"),
            (0 to 0) to Celle("a4", "15250105", "A4"), (1 to 0) to Celle("b3", "15780201", "B3"), (2 to 0) to Celle("b4", "15780202", "B4"),
        )

        // Hver kant i rutenettet blir én grense. Grensetypen følger av cellene på hver side.
        val grunnkretsgrenser = alleKanter().associateWith { kant ->
            val sider = kant.celler().mapNotNull { celler[it] }
            val type = when {
                sider.size == 1 -> Grensetype.Fylkesgrense
                sider[0].nummer.take(4) != sider[1].nummer.take(4) -> Grensetype.Kommunegrense
                sider[0].nummer.substring(4, 6) != sider[1].nummer.substring(4, 6) -> Grensetype.Delomraadegrense
                else -> Grensetype.Grunnkretsgrense
            }
            lagreGrense("gk-${kant.navn}", type, kant)
        }

        val grunnkretser = celler.mapValues { (posisjon, celle) ->
            saveGrunnkrets(
                id = "gk-${celle.lokalid}", lokalid = Lokalid(celle.lokalid), grunnkretsnummer = celle.nummer,
                grunnkretsnavn = celle.navn, gyldigFra = opprettet,
                grenser = grunnkretsgrenser.filterKeys { posisjon in it.celler() }.values.toList(),
            )
        }

        // Stemmekretsene bruker kommune- og fylkesgrensene direkte. Inne i kommunen får de egne
        // stemmekretsgrenser oppå grunnkretsgrensene, delt mellom de to stemmekretsene på hver side.
        val stemmekretsgrenser = mutableMapOf<Kant, Grense>()
        fun lagreStemmekrets(lokalid: String, nummer: String, navn: String, cellerIKretsen: Set<Pair<Int, Int>>): Stemmekrets {
            val grenser = alleKanter()
                .filter { kant -> kant.celler().count { it in cellerIKretsen } == 1 }
                .map { kant ->
                    val grunnkretsgrense = grunnkretsgrenser.getValue(kant)
                    if (grunnkretsgrense.grensetype in Stemmekrets.tillatteGrensetyper()) grunnkretsgrense
                    else stemmekretsgrenser.getOrPut(kant) { lagreGrense("sk-${kant.navn}", Grensetype.Stemmekretsgrense, kant) }
                }
            return saveStemmekrets(id = "sk-$lokalid", lokalid = Lokalid(lokalid), stemmekretsnummer = nummer,
                stemmekretsnavn = navn, gyldigFra = opprettet, grenser = grenser)
        }
        val liaStemmekrets = lagreStemmekrets("sk-lia", "0001", "Liabygda", setOf(1 to 1))
        val ringstad = lagreStemmekrets("sk-ringstad", "0002", "Ringstad", setOf(0 to 2, 1 to 2, 0 to 1, 0 to 0))
        val valldal = lagreStemmekrets("sk-valldal", "0001", "Valldal", setOf(2 to 2, 2 to 1))
        val stordal = lagreStemmekrets("sk-stordal", "0002", "Stordal", setOf(1 to 0, 2 to 0))

        val kommuneA = saveKommuneOgUnikLokalid(
            id = "kommune-a", lokalid = "kommune-1525", kommunenummerUuid = "1525", kommunenummer = "1525", gyldigFra = opprettet,
            grunnkretser = grunnkretser.values.filter { it.grunnkretsnummer!!.startsWith("1525") },
            stemmekretser = listOf(liaStemmekrets, ringstad),
        )
        val kommuneB = saveKommuneOgUnikLokalid(
            id = "kommune-b", lokalid = "kommune-1578", kommunenummerUuid = "1578", kommunenummer = "1578", gyldigFra = opprettet,
            grunnkretser = grunnkretser.values.filter { it.grunnkretsnummer!!.startsWith("1578") },
            stemmekretser = listOf(valldal, stordal),
        )
        saveFylkeOgUnikLokalid(id = "fylke-15", lokalid = "fylke-15", fylkesnummerUuid = "15", fylkesnummer = "15",
            gyldigFra = opprettet, kommuner = listOf(kommuneA, kommuneB))

        grenseRepository.flush()
        flateService.oppdaterFlaterFor(setOf(kommuneA, kommuneB))
    }

    @Test
    fun `flytter grunnkretsen til tilkommunen med nytt nummer, samme lokalid og samme omraade`() {
        val foer = grunnkrets("lia", dagenFoer)

        jobService.flyttGrunnkretsOgStemmekrets(plan)

        val etter = grunnkrets("lia", virkningsdato)
        assertThat(etter.grunnkretsnummer).isEqualTo("15780103")
        assertThat(etter.grunnkretsnavn).isEqualTo("Liabygda")
        assertThat(etter.getKommune(virkningsdato)?.kommunenummer).isEqualTo("1578")
        assertThat(etter.omraade!!.equalsTopo(foer.omraade)).isTrue()

        // Historikken viser fortsatt den gamle tilhørigheten
        val historisk = grunnkrets("lia", dagenFoer)
        assertThat(historisk.grunnkretsnummer).isEqualTo("15250101")
        assertThat(historisk.getKommune(dagenFoer)?.kommunenummer).isEqualTo("1525")
        assertThat(historisk.gyldighet.gyldigTil).isEqualTo(virkningsdato)
    }

    @Test
    fun `grensene rundt grunnkretsen faar grensetype etter den nye kommunetilhoerigheten`() {
        jobService.flyttGrunnkretsOgStemmekrets(plan)

        // Mot A2 og A3 i frakommunen: grunnkretsgrense blir kommunegrense
        assertThat(grense("gk-h-1-2", virkningsdato).grensetype).isEqualTo(Grensetype.Kommunegrense)
        assertThat(grense("gk-v-1-1", virkningsdato).grensetype).isEqualTo(Grensetype.Kommunegrense)
        // Mot B2 (samme delområde 01 som det nye nummeret): kommunegrense blir grunnkretsgrense
        assertThat(grense("gk-v-2-1", virkningsdato).grensetype).isEqualTo(Grensetype.Grunnkretsgrense)
        // Mot B3 (delområde 02): kommunegrense blir delområdegrense
        assertThat(grense("gk-h-1-1", virkningsdato).grensetype).isEqualTo(Grensetype.Delomraadegrense)

        // Samme lokalid, ny versjon, og den gamle versjonen gjelder til virkningsdatoen
        assertThat(grense("gk-h-1-2", dagenFoer).grensetype).isEqualTo(Grensetype.Grunnkretsgrense)
        assertThat(grense("gk-h-1-1", dagenFoer).grensetype).isEqualTo(Grensetype.Kommunegrense)
        // version er også JPA-låsen og øker igjen når grensetypen settes etter lagring, som i
        // GrenseService.lagNyVersjonAvGrenseOgKnyttTilKretser. Derfor bare "nyere enn".
        assertThat(grense("gk-h-1-1", virkningsdato).version).isGreaterThan(grense("gk-h-1-1", dagenFoer).version)

        // Både Liabygda og naboene er koblet til de nye versjonene
        val naboB2 = grunnkrets("b2", virkningsdato)
        assertThat(naboB2.getGrensekoblinger(virkningsdato).map { it.grense }.single { it.lokalid == "gk-v-2-1" }.grensetype)
            .isEqualTo(Grensetype.Grunnkretsgrense)
        assertThat(grunnkrets("lia", virkningsdato).getGrensekoblinger(virkningsdato).map { it.grense.lokalid })
            .containsExactlyInAnyOrder("gk-h-1-1", "gk-h-1-2", "gk-v-1-1", "gk-v-2-1")
    }

    @Test
    fun `stemmekretsen flytter med og bruker kommunegrenser mot frakommunen og nye stemmekretsgrenser mot tilkommunen`() {
        jobService.flyttGrunnkretsOgStemmekrets(plan)

        val stemmekretser = kommuneService.hentAlleStemmekretserForKommune("1578", virkningsdato)
        val liabygda = stemmekretser.single { it.lokalid == "sk-lia" }
        assertThat(liabygda.stemmekretsnummer).isEqualTo("0003")
        assertThat(liabygda.stemmekretsnavn).isEqualTo("LIABYGDA")
        assertThat(kommuneService.hentAlleStemmekretserForKommune("1525", virkningsdato).map { it.stemmekretsnummer })
            .containsExactly("0002")

        val grenser = liabygda.getGrensekoblinger(virkningsdato).map { it.grense }
        // Mot Ringstad: de nye kommunegrensene, delt med grunnkretsene
        assertThat(grenser.filter { it.lokalid in setOf("gk-h-1-2", "gk-v-1-1") }.map { it.grensetype })
            .containsOnly(Grensetype.Kommunegrense).hasSize(2)
        // Mot Valldal og Stordal: to nye stemmekretsgrenser med nye lokalider
        val nyeStemmekretsgrenser = grenser.filter { !it.lokalid.startsWith("gk-") }
        assertThat(nyeStemmekretsgrenser).hasSize(2)
        assertThat(nyeStemmekretsgrenser.map { it.grensetype }).containsOnly(Grensetype.Stemmekretsgrense)
        assertThat(nyeStemmekretsgrenser.map { it.gyldighet.gyldigFra }).containsOnly(virkningsdato)
        assertThat(grenser).hasSize(4)

        // De gamle stemmekretsgrensene mot Ringstad er avsluttet
        val gammel = grenseRepository.findAllByLokalid("sk-h-1-2").single()
        assertThat(gammel.gyldighet.gyldigTil).isEqualTo(virkningsdato)
        assertThat(stemmekrets("sk-ringstad", virkningsdato).getGrensekoblinger(virkningsdato).map { it.grense.lokalid })
            .doesNotContain("sk-h-1-2", "sk-v-1-1").contains("gk-h-1-2", "gk-v-1-1")
    }

    @Test
    fun `arealene til kommuner og kretser blir riktige`() {
        val rapport = jobService.flyttGrunnkretsOgStemmekrets(plan)

        assertThat(kommuneService.hentKommuneByKommunenummer("1525", dagenFoer)!!.omraade!!.area).isEqualTo(5.0)
        assertThat(kommuneService.hentKommuneByKommunenummer("1578", dagenFoer)!!.omraade!!.area).isEqualTo(4.0)
        assertThat(kommuneService.hentKommuneByKommunenummer("1525", virkningsdato)!!.omraade!!.area).isEqualTo(4.0)
        assertThat(kommuneService.hentKommuneByKommunenummer("1578", virkningsdato)!!.omraade!!.area).isEqualTo(5.0)

        assertThat(stemmekrets("sk-lia", virkningsdato).omraade!!.area).isEqualTo(1.0)
        assertThat(stemmekrets("sk-ringstad", virkningsdato).omraade!!.area).isEqualTo(4.0)
        assertThat(stemmekrets("sk-valldal", virkningsdato).omraade!!.area).isEqualTo(2.0)
        assertThat(stemmekrets("sk-stordal", virkningsdato).omraade!!.area).isEqualTo(2.0)

        // 2 kommuner, grunnkretsen, 4 nabogrunnkretser, stemmekretsen og 3 nabostemmekretser
        assertThat(rapport.endredeFlater).hasSize(11)
    }

    @Test
    fun `stopper hvis jobben allerede er kjoert`() {
        jobService.flyttGrunnkretsOgStemmekrets(plan)

        val feil = assertFailsWith<Vedtak2027GrunnlagException> { jobService.flyttGrunnkretsOgStemmekrets(plan) }
        assertThat(feil.message).contains("Er flyttingen allerede kjørt?")
    }

    @Test
    fun `stopper uten endringer hvis det nye grunnkretsnummeret ikke passer til kommunen`() {
        val feilKommune = assertFailsWith<Vedtak2027GrunnlagException> {
            jobService.flyttGrunnkretsOgStemmekrets(plan.copy(nyttGrunnkretsnummer = "15259901"))
        }
        assertThat(feilKommune.message).contains("starter ikke med kommunenummeret 1578")

        val uendret = grunnkrets("lia", virkningsdato)
        assertThat(uendret.grunnkretsnummer).isEqualTo("15250101")
        assertThat(uendret.version).isEqualTo(1)
    }

    @Test
    fun `stopper hvis det nye grunnkretsnummeret er opptatt`() {
        val feil = assertFailsWith<Vedtak2027GrunnlagException> {
            jobService.flyttGrunnkretsOgStemmekrets(plan.copy(nyttGrunnkretsnummer = "15780102"))
        }
        assertThat(feil.message).contains("Grunnkretsnummer 15780102 er brukt av en annen grunnkrets")
    }

    @Test
    fun `stopper hvis det nye stemmekretsnummeret er opptatt, ogsaa med andre ledende nuller`() {
        val feil = assertFailsWith<Vedtak2027GrunnlagException> {
            jobService.flyttGrunnkretsOgStemmekrets(plan.copy(nyttStemmekretsnummer = "2"))
        }
        assertThat(feil.message).contains("Stemmekretsnummer 2 er brukt av en annen stemmekrets i 1578")
    }

    @Test
    fun `stopper hvis det nye grunnkretsnummeret er brukt av en grunnkrets som starter etter virkningsdatoen`() {
        saveGrunnkrets(id = "gk-senere", lokalid = Lokalid("senere"), grunnkretsnummer = plan.nyttGrunnkretsnummer,
            grunnkretsnavn = "Senere", gyldigFra = virkningsdato.plusYears(1))

        val feil = assertFailsWith<Vedtak2027GrunnlagException> { jobService.flyttGrunnkretsOgStemmekrets(plan) }
        assertThat(feil.message).contains("Grunnkretsnummer 15780103 er brukt av en annen grunnkrets som gjelder fra $virkningsdato eller senere")
    }

    @Test
    fun `stopper hvis det nye stemmekretsnummeret er brukt av en stemmekrets som starter etter virkningsdatoen`() {
        // Ulike ledende nuller teller som samme nummer, også her
        val senere = saveStemmekrets(id = "sk-senere", lokalid = Lokalid("sk-senere"), stemmekretsnummer = "3",
            stemmekretsnavn = "Senere", gyldigFra = virkningsdato.plusYears(1))
        kommuneService.hentKommuneByKommunenummer("1578", virkningsdato)!!.addStemmekrets(senere)

        val feil = assertFailsWith<Vedtak2027GrunnlagException> { jobService.flyttGrunnkretsOgStemmekrets(plan) }
        assertThat(feil.message).contains("Stemmekretsnummer 0003 er brukt av en annen stemmekrets i 1578 som gjelder fra $virkningsdato eller senere")
    }

    @Test
    fun `rapporten viser alle objektene jobben endrer, med ny grensetype for hver grense`() {
        val rapport = jobService.flyttGrunnkretsOgStemmekrets(plan)

        val endringer = rapport.linjer.single { it.startsWith("Objektene jobben endrer:") }.lines().drop(1).map { it.trim() }
        // 2 kommuner, grunnkretsen, 4 nabogrunnkretser, stemmekretsen, 3 nabostemmekretser, 4 grenser og 2 stemmekretsgrenser
        assertThat(endringer).hasSize(17)
        assertThat(endringer.filter { it.startsWith("Grense:") }).containsExactlyInAnyOrder(
            "Grense: Grunnkretsgrense -> Kommunegrense mot 15250103 (gk-h-1-2)",
            "Grense: Grunnkretsgrense -> Kommunegrense mot 15250104 (gk-v-1-1)",
            "Grense: Kommunegrense -> Grunnkretsgrense mot 15780102 (gk-v-2-1)",
            "Grense: Kommunegrense -> Delomraadegrense mot 15780201 (gk-h-1-1)",
        )
        assertThat(endringer).contains(
            "Grunnkrets: 15250101 Liabygda -> 15780103 (lia)",
            "Stemmekrets: 1525 0001 Liabygda -> 1578 0003 LIABYGDA (sk-lia)",
        )
    }

    @Test
    fun `stopper hvis en av kommunene har bopliktomraader`() {
        val kommune = kommuneService.hentKommuneByKommunenummer("1525", virkningsdato)!!
        kommune.addBopliktomraade(saveBopliktomraade(lokalid = Lokalid("boplikt-1525"), gyldigFra = opprettet, gjelderKunDelAvKommunen = false))

        val feil = assertFailsWith<Vedtak2027GrunnlagException> { jobService.flyttGrunnkretsOgStemmekrets(plan) }
        assertThat(feil.message).contains("Jobben håndterer ikke boplikt")
    }

    private fun grunnkrets(lokalid: String, dato: LocalDate): Grunnkrets =
        grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(lokalid), dato))!!

    private fun stemmekrets(lokalid: String, dato: LocalDate): Stemmekrets =
        stemmekretsService.hentStemmekrets(Identifikator(Lokalid(lokalid), dato))!!

    private fun grense(lokalid: String, dato: LocalDate): Grense =
        grenseService.hentGrense(Identifikator(Lokalid(lokalid), dato))!!

    private fun lagreGrense(lokalid: String, grensetype: Grensetype, kant: Kant): Grense =
        grenseRepository.save(lagNyGrense(id = "objid-$lokalid", grensetype = grensetype, lokalId = lokalid, gyldigFra = opprettet,
            coordinates = listOf(Coordinate(kant.fra.first.toDouble(), kant.fra.second.toDouble()), Coordinate(kant.til.first.toDouble(), kant.til.second.toDouble()))))

    private data class Celle(val lokalid: String, val nummer: String, val navn: String)

    /** En kant med lengde 1 i rutenettet. `h-x-y` går fra (x,y) mot høyre, `v-x-y` fra (x,y) oppover. */
    private data class Kant(val navn: String, val fra: Pair<Int, Int>, val til: Pair<Int, Int>) {
        /** Cellene på hver side av kanten, med nedre venstre hjørne som map key. Utenfor rutenettet finnes ikke. */
        fun celler(): List<Pair<Int, Int>> {
            val (x, y) = fra
            val naboer = if (navn.startsWith("h")) listOf(x to y - 1, x to y) else listOf(x - 1 to y, x to y)
            return naboer.filter { (cx, cy) -> cx in 0..2 && cy in 0..2 }
        }
    }

    private fun alleKanter(): List<Kant> =
        (0..2).flatMap { x -> (0..3).map { y -> Kant("h-$x-$y", x to y, x + 1 to y) } } +
            (0..3).flatMap { x -> (0..2).map { y -> Kant("v-$x-$y", x to y, x to y + 1) } }
}
