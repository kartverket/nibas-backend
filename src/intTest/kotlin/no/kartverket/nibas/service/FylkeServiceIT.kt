package no.kartverket.nibas.service

import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.repository.FylkeRepository
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
class FylkeServiceIT : TestWithDbContainer() {

    @Autowired
    private lateinit var fylkeService: FylkeService

    @Autowired
    private lateinit var fylkeRepository: FylkeRepository
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)

    @Test
    fun hentEksisterendeFylkeMedDagensDato() {
        val kommune = saveKommune()
        val lokalIdOfFylke = Lokalid("ac7e05cd-fb07-4277-ad95-63c32c1370d0")
        saveFylke(lokalid = lokalIdOfFylke, kommune = kommune)
        assertThat(fylkeRepository.findAllByLokalid(lokalIdOfFylke.value).size).isEqualTo(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(fylkeService.hentFylke(Identifikator(lokalIdOfFylke, november28In2022))).isNotNull
    }

    @Test
    fun hentVersjonAvFylkeFramITid() {
        val lokalIdOfFylke = Lokalid("ac7e05cd-fb07-4277-ad95-63c32c1370d0")
        saveFylkeMed1FramtidigVersjon(lokalIdOfFylke)
        assertThat(fylkeRepository.findAllByLokalid(lokalIdOfFylke.value).size).isEqualTo(2)

        val framtidigFylke = fylkeService.hentFylke(Identifikator(lokalIdOfFylke, jan1In2023))
        assertThat(framtidigFylke).isNotNull
        assertThat(framtidigFylke!!.fylkesnummer).isEqualTo("53")
    }

    @Test
    fun hentAntallFramtidigeVersjoner() {
        val lokalIdOfFylke = Lokalid("ac7e05cd-fb07-4277-ad95-63c32c1370d0")
        saveFylkeMed1FramtidigVersjon(lokalIdOfFylke)
        assertThat(fylkeService.hentAntallGyldigeVersjonerFramITidFor(Identifikator(lokalIdOfFylke, november28In2022))).isEqualTo(1)
    }

    @Test
    fun hentAlleFylker() {
        val jan1In2019 = LocalDate.of(2019, Month.JANUARY, 1)
        val july1In2019 = LocalDate.of(2019, Month.JULY, 1)
        val july2In2019 = LocalDate.of(2019, Month.JULY, 2)

        val kommune = saveKommune()
        saveFylke(id = "19000", lokalid = Lokalid("1002"), gyldigFra = jan1In2019, kommune = kommune)
        saveFylke(id = "9000", lokalid = Lokalid("1001"), gyldigFra = july1In2019, gyldigTil = july2In2019, kommune = kommune)
        saveFylke(id = "9001", lokalid = Lokalid("1001"), gyldigFra = july2In2019, kommune = kommune) // Framtidig versjon av 9000

        assertThat(fylkeService.hentAlleFylker(july1In2019).map { it.id }).isEqualTo(listOf("19000", "9000"))
        assertThat(fylkeService.hentAlleFylker(july2In2019).map { it.id }).isEqualTo(listOf("19000", "9001"))
    }

    private fun saveFylkeMed1FramtidigVersjon(lokalIdOfFylke: Lokalid) {
        val kommune = saveKommune()
        saveFylke(id = "9000", lokalid = lokalIdOfFylke, gyldigTil = jan1In2023, kommune = kommune)
        saveFylke(id = "9001", lokalid = lokalIdOfFylke, fylkesnummer = "53", gyldigFra = jan1In2023, kommune = kommune)
    }

}
