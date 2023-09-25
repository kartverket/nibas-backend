package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
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

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)

    @Test
    fun hentEksisterendeFylkeMedDagensDato() {
        val kommune = saveKommuneOgUnikLokalid()
        val lokalIdOfFylke = Lokalid("ac7e05cd-fb07-4277-ad95-63c32c1370d0")
        saveFylkeOgUnikLokalid(lokalid = lokalIdOfFylke.value, kommuner = listOf(kommune))

        assertThat(fylkeRepository.findAllByLokalid(lokalIdOfFylke.value).size).isEqualTo(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(fylkeService.hentFylke(Identifikator(lokalIdOfFylke, november28In2022))).isNotNull
    }

    @Test
    fun `sjekk pekere mellom fylke og kommune`() {
        val kommune = saveKommuneOgUnikLokalid()
        val lokalIdOfFylke = Lokalid("ac7e05cd-fb65-4277-ad95-63c32c1370d0")
        val fylkeUnikLokalid = saveUnikLokalid(lokalIdOfFylke.value, Flatetype.FYLKE)

        saveFylke(unikLokalid = fylkeUnikLokalid, kommuner = listOf(kommune))

        val lagretKommune = kommuneRepository.findLastByLokalid(kommune.lokalid)!!

        var fylkeList = fylkeRepository.findAllByLokalid(lokalIdOfFylke.value)
        assertThat(fylkeList).hasSize(1)
        var lagretFylke = fylkeList.first()

        //Initielt én kommune og ett fylke - skal peke på hverandre
        assertThat(lagretFylke.fylkeLokalid.kommunerForFylke).hasSize(1).extracting("id").contains(lagretKommune.id)
        assertThat(lagretKommune.fylkeLokalid.fylker).hasSize(1).extracting("id").contains(lagretFylke.id)

        //Fylke har ikke noen kommuner ved dato før kommunen oppsto, men har det for dagens dato
        assertThat(lagretFylke.getKommuner(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretFylke.getKommuner()).hasSize(1).extracting("id").contains(lagretKommune.id)

        //Kommune har ikke noe fylke ved dato før fylket oppsto, man har det for dagens dato
        assertThat(lagretKommune.getFylke(LocalDate.of(2020, Month.NOVEMBER, 28))).isNull()
        assertThat(lagretKommune.getFylke()!!.id).isEqualTo(lagretFylke.id)

        //Ny versjon av kommunen, knyttet til samme fylke
        lagretKommune.setGyldigTil(november28In2022)
        kommuneRepository.save(lagretKommune)
        val kommuneVersjon2 = saveKommune(id = "kommuneVersjon2", unikLokalid = lagretKommune.kommuneLokalid, gyldigFra = november28In2022)
        lagretFylke.addKommune(kommuneVersjon2)
        fylkeRepository.save(lagretFylke)

        lagretFylke = fylkeRepository.findLastByLokalid(lokalIdOfFylke.value)!!
        assertThat(lagretFylke.fylkeLokalid.kommunerForFylke).hasSize(2).extracting("id").contains(lagretKommune.id, kommuneVersjon2.id)
        assertThat(lagretFylke.getKommuner(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretFylke.getKommuner(october1In2022)).hasSize(1).extracting("id").contains(lagretKommune.id)
        assertThat(lagretFylke.getKommuner(november28In2022)).hasSize(1).extracting("id").contains(kommuneVersjon2.id)

        var kommuneList = kommuneRepository.findAllByLokalid(kommune.lokalid)
        assertThat(kommuneList).hasSize(2)
        kommuneList.forEach { k ->
            assertThat(k.fylkeLokalid.fylker).hasSize(1).extracting("id").contains(lagretFylke.id)
            assertThat(k.getFylke()!!.id).isEqualTo(lagretFylke.id)
            assertThat(k.getFylke(november28In2022)!!.id).isEqualTo(lagretFylke.id)
            assertThat(k.getFylke(october1In2022)!!.id).isEqualTo(lagretFylke.id)
        }

        //Ny versjon av fylket, knyttet til samme kommuner
        lagretFylke.setGyldigTil(jan1In2023)
        fylkeRepository.save(lagretFylke)
        val fylkeVersjon2 = saveFylke(id = "fylkeVersjon2", unikLokalid = fylkeUnikLokalid, gyldigFra = jan1In2023, kommuner = kommuneList)

        fylkeList = fylkeRepository.findAllByLokalid(lokalIdOfFylke.value)
        assertThat(fylkeList).hasSize(2)
        fylkeList.forEach { n ->
            assertThat(n.fylkeLokalid.kommunerForFylke).hasSize(2).extracting("id").contains(lagretKommune.id, kommuneVersjon2.id)
            assertThat(n.getKommuner(october1In2022)).hasSize(1).extracting("id").contains(lagretKommune.id)
            assertThat(n.getKommuner(november28In2022)).hasSize(1).extracting("id").contains(kommuneVersjon2.id)
        }

        kommuneList = kommuneRepository.findAllByLokalid(kommune.lokalid)
        assertThat(kommuneList).hasSize(2)
        kommuneList.forEach { k ->
            assertThat(k.fylkeLokalid.fylker).hasSize(2).extracting("id").contains(lagretFylke.id, fylkeVersjon2.id)
            assertThat(k.getFylke(october1In2022)!!.id).isEqualTo(lagretFylke.id)
            assertThat(k.getFylke(november28In2022)!!.id).isEqualTo(lagretFylke.id)
            assertThat(k.getFylke(jan1In2023)!!.id).isEqualTo(fylkeVersjon2.id)
        }
    }

    @Test
    fun `sjekk pekere mellom fylke og navn`() {
        val kommune = saveKommuneOgUnikLokalid()
        val lokalIdOfFylke = Lokalid("ac7e05cd-fb65-4277-ad95-63c32c1370d0")
        val fylkeUnikLokalid = saveUnikLokalid(lokalIdOfFylke.value, Flatetype.FYLKE)

        val norskNavn = saveAdministrativenhetnavn(navn = "Troms", spraakid = "nor", rekkefoelge = 1)

        val fylke = saveFylke(unikLokalid = fylkeUnikLokalid, kommuner = listOf(kommune), administrativenhetnavn = listOf(norskNavn))

        var fylkeList = fylkeRepository.findAllByLokalid(lokalIdOfFylke.value)
        assertThat(fylkeList).hasSize(1)
        var lagretFylke = fylkeList.first()

        var admNavnSet = lagretFylke.fylkeLokalid.administrativenhetnavn
        assertThat(admNavnSet).hasSize(1)
        val administrativenhetnavn = admNavnSet.first()
        assertThat(administrativenhetnavn.spraak!!.id).isEqualTo("nor")
        assertThat(administrativenhetnavn.navn).isEqualTo(norskNavn.navn)
        assertThat(administrativenhetnavn.rekkefoelge).isEqualTo(1)
        assertThat(administrativenhetnavn.administrativenhetLokalid.fylker).hasSize(1).extracting("id").contains(fylke.id)

        var lagretNorskNavn = administrativenhetnavnRepository.findById(norskNavn.id).get()
        assertThat(lagretNorskNavn.administrativenhetLokalid.fylker).hasSize(1).extracting("id").contains(fylke.id)

        //Fylke har ikke noen navn ved dato før navnet oppsto, men har det for dagens dato
        assertThat(lagretFylke.getAdministrativenhetNavn(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretFylke.getAdministrativenhetNavn()).hasSize(1).extracting("id").contains(lagretNorskNavn.id)

        //Administrativenhetnavn har ikke noe fylke ved dato før fylket oppsto, man har det for dagens dato
        assertThat(lagretNorskNavn.getFylke(LocalDate.of(2020, Month.NOVEMBER, 28))).isNull()
        assertThat(lagretNorskNavn.getFylke()!!.id).isEqualTo(lagretFylke.id)

        //Ett navn til for samme fylke
        val samiskNavn = saveAdministrativenhetnavn(navn = "Romsa", spraakid = "sme", rekkefoelge = 2, gyldigFra = november28In2022)
        fylke.addAdministrativenhetNavn(samiskNavn)

        fylkeList = fylkeRepository.findAllByLokalid(lokalIdOfFylke.value)
        assertThat(fylkeList).hasSize(1)
        lagretFylke = fylkeList.first()

        admNavnSet = lagretFylke.fylkeLokalid.administrativenhetnavn
        assertThat(admNavnSet).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
        admNavnSet.forEach { k ->
            assertThat(k.administrativenhetLokalid.fylker).hasSize(1).extracting("id").contains(lagretFylke.id)
            assertThat(k.getFylke()!!.id).isEqualTo(lagretFylke.id)
            assertThat(k.getFylke(november28In2022)!!.id).isEqualTo(lagretFylke.id)
            assertThat(k.getFylke(october1In2022)!!.id).isEqualTo(lagretFylke.id)
        }
        //Fylke har ikke noen navn ved dato før navnet oppsto, men har det for dagens dato
        assertThat(lagretFylke.getAdministrativenhetNavn(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretFylke.getAdministrativenhetNavn()).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
        assertThat(lagretFylke.getAdministrativenhetNavn(october1In2022)).hasSize(1).extracting("id").contains(norskNavn.id)
        assertThat(lagretFylke.getAdministrativenhetNavn(november28In2022)).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)

        //Ny versjon av fylket
        lagretFylke.setGyldigTil(jan1In2023)
        fylkeRepository.save(lagretFylke)
        val fylkeVersjon2 = saveFylke(id = "fylkeVersjon2", unikLokalid = fylkeUnikLokalid, gyldigFra = jan1In2023,
            kommuner = lagretFylke.fylkeLokalid.kommunerForFylke.toList(), administrativenhetnavn = lagretFylke.getAdministrativenhetNavn(jan1In2023))

        fylkeList = fylkeRepository.findAllByLokalid(lokalIdOfFylke.value)
        assertThat(fylkeList).hasSize(2).extracting("id").contains(fylke.id, fylkeVersjon2.id)
        fylkeList.forEach { n ->
            assertThat(n.fylkeLokalid.administrativenhetnavn).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
            assertThat(n.getAdministrativenhetNavn()).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
            assertThat(n.getAdministrativenhetNavn(october1In2022)).hasSize(1).extracting("id").contains(norskNavn.id)
            assertThat(n.getAdministrativenhetNavn(november28In2022)).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
        }

        lagretNorskNavn = administrativenhetnavnRepository.findById(norskNavn.id).get()
        assertThat(lagretNorskNavn.administrativenhetLokalid.fylker).hasSize(2).extracting("id").contains(fylke.id, fylkeVersjon2.id)
        assertThat(lagretNorskNavn.getFylke(october1In2022)!!.id).isEqualTo(lagretFylke.id)
        assertThat(lagretNorskNavn.getFylke(jan1In2023)!!.id).isEqualTo(fylkeVersjon2.id)

        val lagretSamiskNavn = administrativenhetnavnRepository.findById(samiskNavn.id).get()
        assertThat(lagretSamiskNavn.administrativenhetLokalid.fylker).hasSize(2).extracting("id").contains(fylke.id, fylkeVersjon2.id)
        assertThat(lagretSamiskNavn.getFylke(october1In2022)!!.id).isEqualTo(lagretFylke.id)
        assertThat(lagretSamiskNavn.getFylke(jan1In2023)!!.id).isEqualTo(fylkeVersjon2.id)

        lagretSamiskNavn.setGyldigTil(jan1In2023)
        administrativenhetnavnRepository.save(lagretSamiskNavn)
        val samiskNavnVersjon2 = saveAdministrativenhetnavn(lokalid = samiskNavn.lokalid, navn = "Romsa 2", spraakid = "sme", rekkefoelge = 2, gyldigFra = jan1In2023)
        fylkeList.forEach {
            it.addAdministrativenhetNavn(samiskNavnVersjon2)
            fylkeRepository.save(it)}

        fylkeList = fylkeRepository.findAllByLokalid(lokalIdOfFylke.value)
        assertThat(fylkeList).hasSize(2).extracting("id").contains(fylke.id, fylkeVersjon2.id)
        fylkeList.forEach { n ->
            assertThat(n.fylkeLokalid.administrativenhetnavn).hasSize(3).extracting("id").contains(norskNavn.id, samiskNavn.id, samiskNavnVersjon2.id)
            assertThat(n.getAdministrativenhetNavn()).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavnVersjon2.id)
            assertThat(n.getAdministrativenhetNavn(october1In2022)).hasSize(1).extracting("id").contains(norskNavn.id)
            assertThat(n.getAdministrativenhetNavn(november28In2022)).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
        }

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

        val kommune = saveKommuneOgUnikLokalid()

        val lokalIdOfFylke1001 = Lokalid("1001")
        val lokalIdOfFylke1002 = Lokalid("1002")
        val fylkeUnikLokalid1001 = saveUnikLokalid(lokalIdOfFylke1001.value, Flatetype.FYLKE)
        val fylkeUnikLokalid1002 = saveUnikLokalid(lokalIdOfFylke1002.value, Flatetype.FYLKE)

        saveFylke(id = "19000", unikLokalid = fylkeUnikLokalid1002, gyldigFra = jan1In2019, kommuner = listOf(kommune))
        saveFylke(id = "9000", unikLokalid = fylkeUnikLokalid1001, gyldigFra = july1In2019, gyldigTil = july2In2019, kommuner = listOf(kommune))
        saveFylke(id = "9001", unikLokalid = fylkeUnikLokalid1001, gyldigFra = july2In2019, kommuner = listOf(kommune)) // Framtidig versjon av 9000

        assertThat(fylkeService.hentAlleFylker(july1In2019).map { it.id }).isEqualTo(listOf("19000", "9000"))
        assertThat(fylkeService.hentAlleFylker(july2In2019).map { it.id }).isEqualTo(listOf("19000", "9001"))
    }

    private fun saveFylkeMed1FramtidigVersjon(lokalIdOfFylke: Lokalid) {
        val fylkeUnikLokalid = saveUnikLokalid(lokalIdOfFylke.value, Flatetype.FYLKE)

        val kommune = saveKommuneOgUnikLokalid()
        saveFylke(id = "9000", unikLokalid = fylkeUnikLokalid, gyldigTil = jan1In2023, kommuner = listOf(kommune))
        saveFylke(id = "9001", unikLokalid = fylkeUnikLokalid, fylkesnummer = "53", gyldigFra = jan1In2023, kommuner = listOf(kommune))
    }

}
