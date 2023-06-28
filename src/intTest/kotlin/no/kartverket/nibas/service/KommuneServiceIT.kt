package no.kartverket.nibas.service

import no.kartverket.nibas.builders.*
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("security-off")
@Transactional
class KommuneServiceIT : TestWithDbContainer() {

    @Autowired
    private lateinit var kommuneService: KommuneService

    @Autowired
    private lateinit var kommuneRepository: KommuneRepository

    private val lokalidOfKommuneInDataSet = "ac7e05cd-fb07-4270-ad95-e3c32c1370d0"
    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28) // Dagens dato
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val grenseLokallid = Lokalid("888")
    private val grunnkretsLokalid = Lokalid("1001")
    private val kommuneLokalid = Lokalid("750")

    @Test
    fun hentEksisterendeKommuneMedDagensDato() {
        assertThat(kommuneService.hentKommune(Identifikator(Lokalid((lokalidOfKommuneInDataSet)), november28In2022))).isNotNull
        assertThat(kommuneService.hentKommune(Identifikator(Lokalid((lokalidOfKommuneInDataSet))))).isNotNull
    }

    @Test
    fun hentVersjonAvKommuneFramITid() {
        val kommuneLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-e3c32c137055")
        saveKommuneMed1FramtidigVersjon(kommuneLokalid)
        assertThat(kommuneRepository.findAllByLokalid(kommuneLokalid.value).size).isEqualTo(2)

        val framtidigKommune = kommuneService.hentKommune(Identifikator((kommuneLokalid), jan1In2023))
        assertThat(framtidigKommune).isNotNull
        assertThat(framtidigKommune!!.kommunenummer).isEqualTo("0302")
    }

    @Test
    fun hentAntallFramtidigeVersjoner() {
        val kommuneLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-e3c32c137055")
        saveKommuneMed1FramtidigVersjon(kommuneLokalid)
        assertThat(kommuneService.hentAntallGyldigeVersjonerFramITidFor(Identifikator(kommuneLokalid, november28In2022))).isEqualTo(1)
    }

    @Test
    fun hentAlleKommunerPaaDato() {
        val july1In2019 = LocalDate.of(2019, Month.JULY, 1)
        val july2In2019 = LocalDate.of(2019, Month.JULY, 2)
        val july3In2019 = LocalDate.of(2019, Month.JULY, 3)

        saveKommune(id = "9000", lokalid = Lokalid("1001"), gyldigFra = july1In2019, gyldigTil = july2In2019)
        saveKommune(id = "9001", lokalid = Lokalid("1002"), gyldigFra = july2In2019, gyldigTil = july3In2019)
        saveKommune(id = "9002", lokalid = Lokalid("1003"), gyldigFra = july3In2019, gyldigTil = null)

        assertThat(kommuneService.hentAlleKommuner(july1In2019).map { it.id }).isEqualTo(listOf("9000"))
        assertThat(kommuneService.hentAlleKommuner(july2In2019).map { it.id }).isEqualTo(listOf("9001"))
        assertThat(kommuneService.hentAlleKommuner(july3In2019).map { it.id }).isEqualTo(listOf("9002"))
    }

    @Test
    fun hentKommunerForFylke() {
        val fylkeLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-73c32c137054")
        val kommuneLokalid = Lokalid("ac7e05cd-eb08-4270-ad95-73c32c137054")

        val kommune = saveKommune(id = "9000", lokalid = kommuneLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023)
        val kommuneFramITid = saveKommune(id = "9001", lokalid = kommuneLokalid, gyldigFra = jan1In2023)
        val kommuneMedAnnenLokalid = saveKommune(id = "9002", lokalid = Lokalid("ac7e05cd-eb08-4270-ad99-73c32c137054"), gyldigFra = jan1In2023)

        saveFylke(id = "11000", lokalid = fylkeLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023, kommuner = listOf(kommune, kommuneFramITid))
        saveFylke(id = "11001", lokalid = fylkeLokalid, gyldigFra = jan1In2023, kommuner = listOf(kommune, kommuneFramITid, kommuneMedAnnenLokalid))

        val kommunerForFylke = kommuneService.hentKommunerForFylke(Identifikator(fylkeLokalid, november28In2022))
        assertThat(kommunerForFylke.map { it.id }).isEqualTo(listOf("9000"))
    }

    @Test
    fun hentKommunesGrunnkretser() {
        val grunnkretsLokalid = Lokalid("ac7e05cd-eb08-4270-ab95-73c32c137057")
        val grunnkrets1 = saveGrunnkrets(id = "8000", lokalid = grunnkretsLokalid, gyldigFra = october1In2022, gyldigTil = november28In2022)
        val grunnkrets2 = saveGrunnkrets(id = "8001", lokalid = grunnkretsLokalid, gyldigFra = november28In2022, gyldigTil = jan1In2023)
        val grunnkrets3 = saveGrunnkrets(id = "8002", lokalid = grunnkretsLokalid, gyldigFra = jan1In2023)

        val kommuneLokalid = Lokalid("ac7e05cd-eb08-4270-ad95-73c32c137054")
        saveKommune(id = "9000", lokalid = kommuneLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023, grunnkretser = listOf(grunnkrets1, grunnkrets2))
        saveKommune(id = "9001", lokalid = kommuneLokalid, gyldigFra = jan1In2023, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))

        val grunnkretserForKommune = kommuneService.hentKommunesGrunnkretser(Identifikator(kommuneLokalid, november28In2022))
        assertThat(grunnkretserForKommune.map { it.id }).isEqualTo(listOf("8001"))
    }

    @Test
    fun hentKommunesStemmekretser() {
        val stemmekretsLokalid = Lokalid("ac7e05cd-eb08-4271-ab95-73c32c137057")
        val stemmekrets1 = saveStemmekrets(id = "8000", lokalid = stemmekretsLokalid, gyldigFra = october1In2022, gyldigTil = november28In2022)
        val stemmekrets2 = saveStemmekrets(id = "8001", lokalid = stemmekretsLokalid, gyldigFra = november28In2022, gyldigTil = jan1In2023)
        val stemmekrets3 = saveStemmekrets(id = "8002", lokalid = stemmekretsLokalid, gyldigFra = jan1In2023)

        val kommuneLokalid = Lokalid("ac7e05cd-eb08-4270-ad95-73c32c137054")
        saveKommune(id = "9000", lokalid = kommuneLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023, stemmekretser = listOf(stemmekrets1, stemmekrets2))
        saveKommune(id = "9001", lokalid = kommuneLokalid, gyldigFra = jan1In2023, stemmekretser = listOf(stemmekrets1, stemmekrets2, stemmekrets3))

        val stemmekretserForKommune = kommuneService.hentKommunesStemmekretser(Identifikator(kommuneLokalid, november28In2022))
        assertThat(stemmekretserForKommune.map { it.id }).isEqualTo(listOf("8001"))
    }

    @Test
    fun hentGrenserForKommuneFramITid() {
        // Grense 1 er gyldig til 1.1.23, peker til grense 2
        // Grense 2 er gyldig 1.1.23-1.1.24, peker til grense 3
        // Grense 3 er gyldig fra 1.1.24
        val grense3 = saveGrenseAvTypeKommunegrense(id = "2002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrenseAvTypeKommunegrense(id = "2001", lokalid = grenseLokallid, nesteVersjon = grense3, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrenseAvTypeKommunegrense(id = "2000", lokalid = grenseLokallid, nesteVersjon = grense2, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        // Alle grunnkretsene er koblet til grense 1 i koblingstabellen
        // Grunnkrets 2 er gyldig 1.1.23 og det er dennes kobling som vil brukes
        val grunnkrets1 = saveGrunnkrets(id = "101", lokalid = grunnkretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val grunnkrets2 = saveGrunnkrets(id = "102", lokalid = grunnkretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1))
        val grunnkrets3 = saveGrunnkrets(id = "103", lokalid = grunnkretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense1))

        val identifikatorFor1Jan23 = Identifikator(kommuneLokalid, jan1In2023)
        saveKommune(id = "123", lokalid = kommuneLokalid, gyldigFra = october1In2022, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))

        val grensekoblinger = kommuneService.hentGrenserForKommune(identifikatorFor1Jan23)

        assertGrenser(
            grensekoblinger.toList(),
            gyldighetsdato = identifikatorFor1Jan23.gyldighetsdato,
            grenseIGrensekoblingId = "2000",
            relevantGrenseId = "2001",
            enhetKobletTilGrenseLokalid = grunnkretsLokalid
        )
    }

    private fun saveKommuneMed1FramtidigVersjon(kommuneLokalid: Lokalid) {
        saveKommune(lokalid = kommuneLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023)
        saveKommune(id = "9001", lokalid = kommuneLokalid, kommunenummer = "0302", gyldigFra = jan1In2023)
    }

}
