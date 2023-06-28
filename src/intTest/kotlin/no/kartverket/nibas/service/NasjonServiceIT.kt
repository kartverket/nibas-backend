package no.kartverket.nibas.service

import no.kartverket.nibas.builders.*
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
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
class NasjonServiceIT : TestWithDbContainer() {

    @Autowired
    private lateinit var nasjonService: NasjonService

    @Autowired
    private lateinit var nasjonRepository: NasjonRepository

    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)

    @Test
    fun hentEksisterendeNasjonMedDagensDato() {
        val kommune = saveKommune()
        val fylke = saveFylke(kommuner = listOf(kommune))
        val lokalIdOfNasjon = Lokalid("ac7e05cd-fb65-4277-ad95-63c32c1370d0")
        saveNasjon(lokalid = lokalIdOfNasjon, fylker = listOf(fylke))
        assertThat(nasjonRepository.findAllByLokalid(lokalIdOfNasjon.value).size).isEqualTo(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(nasjonService.hentNasjon(Identifikator(lokalIdOfNasjon, november28In2022))).isNotNull
    }

    @Test
    fun hentVersjonAvNasjonFramITid() {
        val lokalIdOfNasjon = Lokalid("ac7e05cd-fb65-4277-ad95-63c32c1370d0")
        saveNasjonMed1FramtidigVersjon(lokalIdOfNasjon)
        assertThat(nasjonRepository.findAllByLokalid(lokalIdOfNasjon.value).size).isEqualTo(2)

        val framtidigVersjonAvNasjon = nasjonService.hentNasjon(Identifikator(lokalIdOfNasjon, jan1In2023))
        assertThat(framtidigVersjonAvNasjon).isNotNull
        assertThat(framtidigVersjonAvNasjon!!.opphav).isEqualTo("Sandefjord")
    }

    @Test
    fun hentAntallFramtidigeVersjoner() {
        val lokalIdOfNasjon = Lokalid("ac7e05cd-fb65-4277-ad95-63c32c1370d0")
        saveNasjonMed1FramtidigVersjon(lokalIdOfNasjon)
        assertThat(nasjonService.hentAntallGyldigeVersjonerFramITidFor(Identifikator(lokalIdOfNasjon, november28In2022))).isEqualTo(1)
    }

    @Test
    fun hentAlleNasjoner() {
        val jan1In2019 = LocalDate.of(2019, Month.JANUARY, 1)
        val july1In2019 = LocalDate.of(2019, Month.JULY, 1)
        val jan1In2020 = LocalDate.of(2020, Month.JANUARY, 1)

        val kommune = saveKommune()
        val fylke = saveFylke(kommuner = listOf(kommune))
        val fylkeListe = listOf(fylke)
        saveNasjon(id = "9000", lokalid = Lokalid("600"), gyldigFra = jan1In2019, fylker = fylkeListe)
        saveNasjon(id = "9001", lokalid = Lokalid("601"), gyldigFra = july1In2019, gyldigTil = jan1In2023, fylker = fylkeListe)
        saveNasjon(id = "9002", lokalid = Lokalid("601"), gyldigFra = jan1In2023, fylker = fylkeListe)
        saveNasjon(id = "9003", lokalid = Lokalid("602"), gyldigFra = jan1In2020, fylker = fylkeListe)

        assertThat(nasjonService.hentAlleNasjoner(july1In2019).map { it.id }).isEqualTo(listOf("9000", "9001"))
        assertThat(nasjonService.hentAlleNasjoner(jan1In2020).map { it.id }).isEqualTo(listOf("9000", "9001", "9003"))
        assertThat(nasjonService.hentAlleNasjoner(jan1In2023).map { it.id }).isEqualTo(listOf("9000", "9003", "9002")) //Sortert rekkefølge på gyldigFra
    }

    @Test
    fun hentGrenserForNasjonFramITid() {
        val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
        val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
        val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
        val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

        val grenseLokallid = Lokalid("888")
        val grenseLokallid2 = Lokalid("889")
        val grunnkretsLokalid = Lokalid("1001")
        val grunnkretsLokalid2 = Lokalid("1002")
        val kommuneLokalid = Lokalid("750")
        val fylkeLokalid = Lokalid("500")
        val fylke2Lokalid = Lokalid("501")
        val nasjonLokalid = Lokalid("400")

        val grense4 = saveGrenseAvTypeRiksgrense(id = "2003", lokalid = grenseLokallid2, gyldigFra = october1In2022, gyldigTil = jan1In2023)
        val grense3 = saveGrenseAvTypeRiksgrense(id = "2002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrenseAvTypeRiksgrense(id = "2001", lokalid = grenseLokallid, nesteVersjon = grense3, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrenseAvTypeRiksgrense(id = "2000", lokalid = grenseLokallid, nesteVersjon = grense2, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        val grunnkrets1 = saveGrunnkrets(id = "101", lokalid = grunnkretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val grunnkrets2 = saveGrunnkrets(id = "102", lokalid = grunnkretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1))
        val grunnkrets3 = saveGrunnkrets(id = "103", lokalid = grunnkretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense1))
        val grunnkrets4 = saveGrunnkrets(id = "104", lokalid = grunnkretsLokalid2, gyldigFra = october1In2022, grenser = listOf(grense4))

        val kommuneGyldigTilJan23 = saveKommune(id = "123", lokalid = kommuneLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023, grunnkretser = listOf(grunnkrets4))
        val kommuneGyldigFraJan23 = saveKommune(id = "124", lokalid = kommuneLokalid, gyldigFra = jan1In2023, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))

        val fylke1GyldigTilJan23 = saveFylke(id = "7000", lokalid = fylkeLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023, kommuner = listOf(kommuneGyldigTilJan23))
        val fylke1GyldigFraJan23 = saveFylke(id = "7001", lokalid = fylkeLokalid, gyldigFra = jan1In2023, kommuner = listOf(kommuneGyldigTilJan23))
        val fylke2 = saveFylke(id = "7002", lokalid = fylke2Lokalid, gyldigFra = october1In2022, kommuner = listOf(kommuneGyldigFraJan23))

        saveNasjon(id = "9000", lokalid = nasjonLokalid, gyldigFra = october1In2022, fylker = listOf(fylke1GyldigTilJan23, fylke1GyldigFraJan23, fylke2))

        val nasjonIdentifikatorFor1Jan23 = Identifikator(nasjonLokalid, jan1In2023)
        val grensekoblinger = nasjonService.hentGrenserForNasjon(nasjonIdentifikatorFor1Jan23)

        assertGrenser(
            grensekoblinger.toList(),
            gyldighetsdato = nasjonIdentifikatorFor1Jan23.gyldighetsdato,
            grenseIGrensekoblingId = "2000",
            relevantGrenseId = "2001",
            enhetKobletTilGrenseLokalid = grunnkretsLokalid
        )
    }

    private fun saveNasjonMed1FramtidigVersjon(lokalIdOfNasjon: Lokalid) {
        val kommune = saveKommune()
        val fylke = saveFylke(kommuner = listOf(kommune))
        saveNasjon(id = "9000", lokalid = lokalIdOfNasjon, gyldigTil = jan1In2023, fylker = listOf(fylke))
        saveNasjon(id = "9001", lokalid = lokalIdOfNasjon, gyldigFra = jan1In2023, fylker = listOf(fylke), opphav = "Sandefjord")
    }


}
