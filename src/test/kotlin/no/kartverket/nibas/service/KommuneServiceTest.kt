package no.kartverket.nibas.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrunnkretsGrenseKoblingRepository
import no.kartverket.nibas.repository.KOMMUNEREPOSITORY_EXTENSIONS_FILEPATH
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.REPOSITORY_EXTENSIONS_FILEPATH
import no.kartverket.nibas.repository.StemmekretsGrenseKoblingRepository
import no.kartverket.nibas.repository.UnikLokalidRepository
import no.kartverket.nibas.repository.findGyldigKommuneForKommunenummer
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertEquals

class KommuneServiceTest {
    private val kommuneRepositoryMock = mockk<KommuneRepository>()
    private val fylkeRepositoryMock = mockk<FylkeRepository>()
    private val unikLokalidRepositoryMock = mockk<UnikLokalidRepository>()
    private val administrativenhetnavnRepositoryMock = mockk<AdministrativenhetnavnRepository>()
    private val administrativenhetnavnServiceMock = mockk<AdministrativenhetnavnService>()
    private val grunnkretsGrenseKoblingRepositoryMock = mockk<GrunnkretsGrenseKoblingRepository>()
    private val stemmekretsGrenseKoblingRepositoryMock = mockk<StemmekretsGrenseKoblingRepository>()
    val kodelisteServiceMock = mockk<KodelisteService>()

    private val kommuneService = KommuneService(
        kommuneRepositoryMock,
        fylkeRepositoryMock,
        administrativenhetnavnRepositoryMock,
        unikLokalidRepositoryMock,
        kodelisteServiceMock,
        administrativenhetnavnServiceMock,
        grunnkretsGrenseKoblingRepositoryMock,
        stemmekretsGrenseKoblingRepositoryMock,
    )

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
        mockkStatic(KOMMUNEREPOSITORY_EXTENSIONS_FILEPATH)
    }

    @Test
    fun `hent grenser for kommune returns null when kommune not nound`() {
        every { kommuneRepositoryMock.findAllByLokalid(any()) } returns emptyList()
        assertThat(kommuneService.hentGrensekoblingerForKommune(Identifikator(Lokalid("1")))).isEmpty()
    }

    @Test
    fun `hent grenser for kommune returns the correct borders`() {
        val kommuneIdentifikator = Identifikator(lokalid = Lokalid("1"))

        val kommune = KommuneBuilder()
            .withGrunnkrets(GrunnkretsBuilder()
                .withRandomGrenser(
                    Pair(Grensetype.Kommunegrense, 5),
                    Pair(Grensetype.Fylkesgrense, 4),
                    Pair(Grensetype.Riksgrense, 3),
                    Pair(Grensetype.Territorialgrense, 2),
                    Pair(Grensetype.AvtaltAvgrensningslinje, 1)
                )
                .build())
            .build()
        every { kommuneRepositoryMock.findGyldigObjektForIdentifikator(kommuneIdentifikator) } returns kommune

        val grensekoblingerForKommune = kommuneService.hentGrensekoblingerForKommune(kommuneIdentifikator)
        assertEquals(15, grensekoblingerForKommune.size, "Expected 15 borders")

        assertGrensekoblinger(grensekoblingerForKommune, Grensetype.AvtaltAvgrensningslinje, 1, "Expected 1 AvtaltAvgrensningslinje")
        assertGrensekoblinger(grensekoblingerForKommune, Grensetype.Territorialgrense, 2, "Expected 2 Territorialgrense")
        assertGrensekoblinger(grensekoblingerForKommune, Grensetype.Riksgrense, 3, "Expected 3 Riksgrense")
        assertGrensekoblinger(grensekoblingerForKommune, Grensetype.Fylkesgrense, 4, "Expected 4 Fylkesgrense")
        assertGrensekoblinger(grensekoblingerForKommune, Grensetype.Kommunegrense, 5, "5 Kommunegrense")
    }

    @Test
    fun `that oppdater kommune returns null when kommune not found`() {
        every { kommuneRepositoryMock.findAllByLokalid(any()) } returns emptyList()

        assertThatThrownBy {
            kommuneService.oppdaterKommune(Lokalid("1"), LocalDate.now(), Endringstype.FASTSETTING, KommuneRequestBuilder.DEFAULT_REQUEST)
        }
            .isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Kommune med Identifikator")
            .hasMessageContaining("ble ikke funnet")
    }

    /**
     * Kommune med 3 grunnkretser.
     * 1 Storvatn
     * 1 nåværende og 1 framtidig versjon av Moi.
     *
     * Later som dagens dato er 23.11.22 og da skal kommunes grunnkretser kun returnere
     * Storvatn og nåværende Moi.
     */
    @Test
    fun `hent kommunes grunnkretser returnerer bare gyldige naavaerende nersjoner`() {
        val august1 = LocalDate.of(2022, Month.AUGUST, 1)
        val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)
        val todayIsNovember22In2022 = LocalDate.of(2022, Month.NOVEMBER, 22)

        val kommuneId = "12"
        val lokalIdGrunnkretsStorvatn = Lokalid("98")
        val lokalIdGrunnkretsMoi = Lokalid("99")

        val grunnkretsStorvatn = GrunnkretsBuilder(
            id = "1",
            lokalId = lokalIdGrunnkretsStorvatn,
            gyldigFra = august1
        ).build()
        val grunnkretsMoi = GrunnkretsBuilder(
            id = "2",
            lokalId = lokalIdGrunnkretsMoi,
            gyldigFra = august1,
            gyldigTil = jan1In2024
        ).build()
        val grunnkretsMoiFremtidigVersjon = GrunnkretsBuilder(
            id = "3",
            lokalId = lokalIdGrunnkretsMoi,
            gyldigFra = jan1In2024
        ).build()

        val kommune = KommuneBuilder(
            id = kommuneId,
            navn = "Bergen",
            grunnkretser = mutableListOf(grunnkretsStorvatn, grunnkretsMoi, grunnkretsMoiFremtidigVersjon)
        ).build()

        val kommuneIdentifikator = Identifikator(
            lokalid = Lokalid(kommuneId),
            gyldighetsdato = todayIsNovember22In2022)
        every { kommuneRepositoryMock.findGyldigObjektForIdentifikator(kommuneIdentifikator) } returns kommune

        val kommunesGrunnkretser = kommuneService.hentKommunesGrunnkretser(kommuneIdentifikator)

        val expectedListOfGrunnkretsIds = listOf("1", "2")
        assertThat(kommunesGrunnkretser.map { it.id }).isEqualTo(expectedListOfGrunnkretsIds)
    }

    @Test
    fun `hentAlleStemmekretserForKommune returnerer stemmekretser for gyldighetsdato`() {
        val stemmekrets1 =
            StemmekretsBuilder(id = "1").withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1)).build()
        val stemmekrets2 =
            StemmekretsBuilder(id = "2").withGyldigFra(LocalDate.of(2019, Month.JANUARY, 1)).build()
        val stemmekrets3 =
            StemmekretsBuilder(id = "3")
                .withGyldigFra(LocalDate.of(2019, Month.JANUARY, 1))
                .withGyldigTil(LocalDate.of(2020, Month.JANUARY, 1))
                .build()
        val gyldighetsdato: LocalDate = LocalDate.of(2023, Month.JANUARY, 1)
        val kommunenummer = "0301"
        val kommune =
            KommuneBuilder()
                .withNavn("Oslo")
                .withStemmekrets(stemmekrets1)
                .withStemmekrets(stemmekrets2)
                .withStemmekrets(stemmekrets3)
                .build()

        every {
            kommuneRepositoryMock.findGyldigKommuneForKommunenummer(
                eq(kommunenummer),
                eq(gyldighetsdato)
            )
        } returns kommune

        val stemmekretser = kommuneService.hentAlleStemmekretserForKommune(kommunenummer, gyldighetsdato)
        assertEquals(stemmekretser, listOf(stemmekrets1, stemmekrets2))
    }

    @Test
    fun `hentAlleGrunnkretserForKommune returnerer grunnkretser for gyldighetsdato`() {
        val gk1 =
            GrunnkretsBuilder(id = "1", lokalId = Lokalid("87"))
                .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
                .build()
        val gk2 =
            GrunnkretsBuilder(id = "2", lokalId = Lokalid("88"))
                .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
                .build()
        val gk3 =
            GrunnkretsBuilder(id = "3", lokalId = Lokalid("88"))
                .withGyldigFra(LocalDate.of(2019, Month.JANUARY, 1))
                .withGyldigTil(LocalDate.of(2020, Month.JANUARY, 1))
                .build()
        val gyldighetsdato: LocalDate = LocalDate.of(2023, Month.JANUARY, 1)
        val kommunenummer = "0301"
        val kommune =
            KommuneBuilder()
                .withNavn("Oslo")
                .withGrunnkrets(gk1)
                .withGrunnkrets(gk2)
                .withGrunnkrets(gk3)
                .build()

        every {
            kommuneRepositoryMock.findGyldigKommuneForKommunenummer(
                eq(kommunenummer),
                eq(gyldighetsdato)
            )
        } returns kommune

        val grunnkretser = kommuneService.hentAlleGrunnkretserForKommune(kommunenummer, gyldighetsdato)
        assertEquals(grunnkretser, listOf(gk1, gk2))
    }

    @Test
    fun `hentGrensekoblingerForAlleStemmekretserIKommune skal returnere korrekte grenser`() {
        val sk1 =
            StemmekretsBuilder(id = "1", lokalId = Lokalid("87"))
                .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
                .build()
        val sk2 =
            StemmekretsBuilder(id = "2", lokalId = Lokalid("88"))
                .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
                .build()

        val gyldighetsdato: LocalDate = LocalDate.of(2023, Month.JANUARY, 1)
        val kommune =
            KommuneBuilder()
                .withNavn("Oslo")
                .build()

        val unikGrenseSK1 = GrenseBuilder().withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1)).withLokalId("unikGrenseSK1").build()
        val unikGrenseSK2 = GrenseBuilder().withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1)).withLokalId("unikGrenseSK2").build()
        val deltGrense = GrenseBuilder().withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1)).withLokalId("deltGrense").build()

        val grensekoblingerSK1 = listOf(
            StemmekretsGrenseKobling(stemmekrets = sk1, grense = unikGrenseSK1),
            StemmekretsGrenseKobling(stemmekrets = sk1, grense = deltGrense)
        )

        val grensekoblingerSK2 = listOf(
            StemmekretsGrenseKobling(stemmekrets = sk2, grense = unikGrenseSK2),
            StemmekretsGrenseKobling(stemmekrets = sk2, grense = deltGrense)
        )

        every { kommuneRepositoryMock.findGyldigObjektForIdentifikator(any()) } returns kommune
        every { stemmekretsGrenseKoblingRepositoryMock.findByStemmekretsKommuneLokalid(any()) } returns grensekoblingerSK1 + grensekoblingerSK2

        val grenser = kommuneService.hentGrensekoblingerForAlleStemmekretserIKommune(Identifikator(Lokalid(kommune.lokalid), gyldighetsdato))
            .sortedBy { it.grense.lokalid }

        assertThat(grenser).hasSize(3)
        assertThat(grenser[0].grense.lokalid).isEqualTo("deltGrense")
        assertThat(grenser[1].grense.lokalid).isEqualTo("unikGrenseSK1")
        assertThat(grenser[2].grense.lokalid).isEqualTo("unikGrenseSK2")
    }

    @Test
    fun `hentGrensekoblingerForAlleGrunnkretserIKommune skal returnere korrekte grenser`() {
        val gk1 =
            GrunnkretsBuilder(id = "1", lokalId = Lokalid("87"))
                .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
                .build()
        val gk2 =
            GrunnkretsBuilder(id = "2", lokalId = Lokalid("88"))
                .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
                .build()

        val gyldighetsdato: LocalDate = LocalDate.of(2023, Month.JANUARY, 1)
        val kommune =
            KommuneBuilder()
                .withNavn("Oslo")
                .build()

        val unikGrenseGK1 = GrenseBuilder().withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1)).withLokalId("unikGrenseGK1").build()
        val unikGrenseGK2 = GrenseBuilder().withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1)).withLokalId("unikGrenseGK2").build()
        val deltGrense = GrenseBuilder().withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1)).withLokalId("deltGrense").build()

        val grensekoblingerGK1 = listOf(
            GrunnkretsGrenseKobling(grunnkrets = gk1, grense = unikGrenseGK1),
            GrunnkretsGrenseKobling(grunnkrets = gk1, grense = deltGrense)
        )

        val grensekoblingerGK2 = listOf(
            GrunnkretsGrenseKobling(grunnkrets = gk2, grense = unikGrenseGK2),
            GrunnkretsGrenseKobling(grunnkrets = gk2, grense = deltGrense)
        )

        every { kommuneRepositoryMock.findGyldigObjektForIdentifikator(any()) } returns kommune
        every { grunnkretsGrenseKoblingRepositoryMock.findByGrunnkretsKommuneLokalid(any()) } returns grensekoblingerGK1 + grensekoblingerGK2

        val grenser = kommuneService.hentGrensekoblingerForAlleGrunnkretserIKommune(Identifikator(Lokalid(kommune.lokalid), gyldighetsdato))
            .sortedBy { it.grense.lokalid }

        assertThat(grenser).hasSize(3)
        assertThat(grenser[0].grense.lokalid).isEqualTo("deltGrense")
        assertThat(grenser[1].grense.lokalid).isEqualTo("unikGrenseGK1")
        assertThat(grenser[2].grense.lokalid).isEqualTo("unikGrenseGK2")
    }

    @Test
    fun `hent kommunes inndelinger med delt geometri grenser skal hente baade stemmekrets-, grunnkrets- og kommunegrenser for en kommune, uten duplikater`() {
        val gyldighetsdato: LocalDate = LocalDate.of(2020, Month.JANUARY, 1)

        val gk1 =
            GrunnkretsBuilder(id = "1", lokalId = Lokalid("87"))
                .withGyldigFra(gyldighetsdato)
                .build()
        val sk1 =
            StemmekretsBuilder(id = "2", lokalId = Lokalid("88"))
                .withGyldigFra(gyldighetsdato)
                .build()

        val gkGrense = GrenseBuilder().withLokalId("gk-grense").withGyldigFra(gyldighetsdato).withGrensetype(Grensetype.Grunnkretsgrense).build()
        val skGrense = GrenseBuilder().withLokalId("sk-grense").withGyldigFra(gyldighetsdato).withGrensetype(Grensetype.Stemmekretsgrense).build()
        val kommuneGrense = GrenseBuilder().withLokalId("kommune-grense").withGyldigFra(gyldighetsdato).withGrensetype(Grensetype.Kommunegrense).build()

        val skKoblinger = listOf(
            StemmekretsGrenseKobling(sk1, skGrense),
            StemmekretsGrenseKobling(sk1, kommuneGrense)
        )

        val gkKoblinger = listOf(
            GrunnkretsGrenseKobling(gk1, gkGrense),
            GrunnkretsGrenseKobling(gk1, kommuneGrense)
        )

        gk1.grenser.addAll(gkKoblinger)

        val kommune =
            KommuneBuilder()
                .withNavn("Oslo")
                .withGrunnkrets(gk1)
                .withStemmekrets(sk1)
                .build()

        every { kommuneRepositoryMock.findGyldigObjektForIdentifikator(any()) } returns kommune
        every { grunnkretsGrenseKoblingRepositoryMock.findByGrunnkretsKommuneLokalid(any()) } returns gkKoblinger
        every { stemmekretsGrenseKoblingRepositoryMock.findByStemmekretsKommuneLokalid(any()) } returns skKoblinger

        val grenser = kommuneService.hentKommunesInndelingerMedDeltGeometriGrenser(Identifikator(Lokalid(kommune.lokalid), gyldighetsdato))

        assertThat(grenser).hasSize(3)
        assertThat(grenser).flatExtracting({ it.grense.lokalid }).containsExactlyInAnyOrder("gk-grense", "sk-grense", "kommune-grense")
    }

    @Test
    fun `hent kommunes inndelinger med delt geometri grenser kan returnere enkle grensekoblinger`() {
        val gyldighetsdato: LocalDate = LocalDate.of(2023, Month.JANUARY, 1)

        val gk1 =
            GrunnkretsBuilder(id = "1", lokalId = Lokalid("87"))
                .withGyldigFra(gyldighetsdato)
                .build()

        val gkGrense = GrenseBuilder().withLokalId("gk-grense").withGyldigFra(gyldighetsdato).withGrensetype(Grensetype.Grunnkretsgrense).build()

        val gkKoblinger = listOf(
            GrunnkretsGrenseKobling(gk1, gkGrense),
        )

        gk1.grenser.addAll(gkKoblinger)

        val kommune =
            KommuneBuilder()
                .withNavn("Oslo")
                .withGrunnkrets(gk1)
                .build()


        val identifikator = Identifikator(Lokalid(kommune.lokalid), gyldighetsdato)

        every { kommuneRepositoryMock.findGyldigObjektForIdentifikator(any()) } returns kommune
        every { grunnkretsGrenseKoblingRepositoryMock.findByGrunnkretsKommuneLokalid(any()) } returns gkKoblinger
        every { stemmekretsGrenseKoblingRepositoryMock.findByStemmekretsKommuneLokalid(any()) } returns emptyList()

        val actualGrenser = kommuneService.hentKommunesInndelingerMedDeltGeometriGrenser(identifikator)

        assertThat(actualGrenser).hasSize(1)
        assertThat(actualGrenser[0].grense.lokalid).isEqualTo("gk-grense")

    }
}
