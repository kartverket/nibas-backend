package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.builders.FylkeRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("security-off")
@Transactional
class FylkeServiceIT : TestWithDbContainer() {

    @MockkBean
    private lateinit var kodelisteServiceMock: KodelisteService

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val lokalIdOfFylke = Lokalid("ac7e05cd-fb07-4277-ad95-63c32c1370d0")

    @Test
    fun hentEksisterendeFylkeMedDagensDato() {
        val kommune = saveKommuneOgUnikLokalid()

        saveFylkeOgUnikLokalid(lokalid = lokalIdOfFylke.value, kommuner = listOf(kommune))

        assertThat(fylkeRepository.findAllByLokalid(lokalIdOfFylke.value).size).isEqualTo(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(fylkeService.hentFylke(Identifikator(lokalIdOfFylke, november28In2022))).isNotNull
    }

    @Test
    fun `sjekk pekere mellom fylke og kommune`() {
        val kommune = saveKommuneOgUnikLokalid()

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
        saveFylkeMed1FramtidigVersjon(lokalIdOfFylke)
        assertThat(fylkeRepository.findAllByLokalid(lokalIdOfFylke.value).size).isEqualTo(2)

        val framtidigFylke = fylkeService.hentFylke(Identifikator(lokalIdOfFylke, jan1In2023))
        assertThat(framtidigFylke).isNotNull
        assertThat(framtidigFylke!!.fylkesnummer).isEqualTo("53")
    }

    @Test
    fun hentAntallFramtidigeVersjoner() {
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

    @Test
    fun `oppdaterFylke skal lage nytt fylke og sette gyldigtil for gammelt fylke`() {
        val fylkeRequest = FylkeRequestBuilder().withVersion(2).withSamiskforvaltningsomraade(true).build()

        val identifikator = Identifikator(Lokalid(fylkeRequest.lokalid), october1In2022)

        val fylke = saveFylkeOgUnikLokalid(
            id = "2",
            lokalid = identifikator.lokalid.value,
            gyldigFra = october1In2022,
            fylkesnummer = fylkeRequest.fylkesnummerId
        )
        saveNasjonOgUnikLokalid(fylker = listOf(fylke))
        val versjonFoerOppdatering = fylke.version

        val now = LocalDate.now()
        val opprinneligFylke = fylkeService.hentFylke(identifikator)!!

        fylkeService.oppdaterFylke(
            lokalid = identifikator.lokalid,
            oppdatertFylke = fylkeRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligFylkeEtter = fylkeService.hentFylke(identifikator)!!
        val fylkeEtter = fylkeService.hentFylke(Identifikator(identifikator.lokalid))!!

        assertThat(fylkeEtter.samiskforvaltningsomraade).isNotEqualTo(fylke.samiskforvaltningsomraade)
            .isEqualTo(fylkeRequest.samiskforvaltningsomraade)
        assertThat(fylkeEtter.fylkesnummer).isEqualTo(fylke.fylkesnummer)
        assertThat(fylkeEtter.omraade).isEqualTo(fylke.omraade)
        assertThat(fylkeEtter.posisjon).isEqualTo(fylke.posisjon)
        assertThat(fylkeEtter.gyldighet.gyldigFra).isEqualTo(now)
        assertThat(fylkeEtter.gyldighet.gyldigTil).isNull()

        assertThat(opprinneligFylkeEtter.gyldighet.gyldigFra).isEqualTo(october1In2022).isEqualTo(opprinneligFylke.gyldighet.gyldigFra)
        assertThat(opprinneligFylkeEtter.gyldighet.gyldigTil).isEqualTo(now)
        assertThat(opprinneligFylkeEtter.version).isGreaterThan(versjonFoerOppdatering)

        assertThat(fylkeEtter.fylkeLokalid.lokalid).isEqualTo(opprinneligFylkeEtter.fylkeLokalid.lokalid).isEqualTo(fylke.fylkeLokalid.lokalid)
        assertThat(fylkeEtter.nasjonLokalid.lokalid).isEqualTo(opprinneligFylkeEtter.nasjonLokalid.lokalid).isEqualTo(fylke.nasjonLokalid.lokalid)
        assertThat(fylkeEtter.fylkeLokalid).isEqualTo(opprinneligFylkeEtter.fylkeLokalid).isEqualTo(fylke.fylkeLokalid)
        assertThat(fylkeEtter.nasjonLokalid).isEqualTo(opprinneligFylkeEtter.nasjonLokalid).isEqualTo(fylke.nasjonLokalid)
    }

    @Test
    fun `sjekk createNyttFylke`() {
        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue("55") }  returns "d42b6763-0157-440a-a0eb-3dc24aa87c41"
        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue("56") } returns "ee97a593-2f9d-49c7-b251-c0f0cfa8769b"

        val norskNavn = saveAdministrativenhetnavn(navn = "Troms og Finnmark", spraakid = "nor", rekkefoelge = 1)
        val samiskNavn = saveAdministrativenhetnavn(navn = "Romsa ja Finnmárku", spraakid = "sme", rekkefoelge = 2)
        val kvenskNavn = saveAdministrativenhetnavn(navn = "Tromssa ja Finmarkku", spraakid = "fkv", rekkefoelge = 3)

        val tromsOgFinnmark = saveFylkeOgUnikLokalid(lokalid = "54TromsOgFinnmark", fylkesnummer = "efe425f5-fc7f-4a29-a70d-31363ec6a306",
            administrativenhetnavn = listOf(norskNavn, samiskNavn, kvenskNavn))
        assertThat(tromsOgFinnmark.utledetFra).isNull()

        val nasjon = saveNasjonOgUnikLokalid(fylker = listOf(tromsOgFinnmark))

        val tromsNavnList = listOf(
            administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Troms", spraakForkortelse = "nor", oppgittRekkefoelge = 1),
            administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Romsa", spraakForkortelse = "sme", oppgittRekkefoelge = 2),
            administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Tromssa", spraakForkortelse = "fkv", oppgittRekkefoelge = 3)
        )

        val nyTroms = fylkeService.createNyttFylkeMedNyUnikLokalid(
            kopierFraFylke = tromsOgFinnmark,
            nyttFylkesnummerIKlartekst = "55",
            admenhetnavnList = tromsNavnList,
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )

        assertThat(nyTroms.lokalid).isNotEqualTo(tromsOgFinnmark.lokalid)
        assertThat(nyTroms.fylkesnummer).isEqualTo("d42b6763-0157-440a-a0eb-3dc24aa87c41")
        assertThat(nyTroms.utledetFra!!.id).isEqualTo(tromsOgFinnmark.id)

        assertThat(nyTroms.getNasjon(jan1In2024)!!.lokalid).isEqualTo(nasjon.lokalid)
        assertThat(nasjon.nasjonLokalid.fylkerForNasjon).hasSize(2).extracting("id").contains(tromsOgFinnmark.id, nyTroms.id)

        assertThat(nyTroms.fylkeLokalid.administrativenhetnavn).hasSize(3).extracting("navn", "rekkefoelge", "spraak.id")
            .contains(Assertions.tuple("Troms", 1, "nor"), Assertions.tuple("Romsa", 2, "sme"), Assertions.tuple("Tromssa", 3, "fkv"))
        assertThat(nyTroms.fylkeLokalid.administrativenhetnavn).extracting("id").doesNotContain(norskNavn.id, samiskNavn.id, kvenskNavn.id)

        val finnmarkNavnList = listOf(
            administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Finnmark", spraakForkortelse = "nor", oppgittRekkefoelge = 2),
            administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Finnmárku", spraakForkortelse = "sme", oppgittRekkefoelge = 1),
            administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Finmarkku", spraakForkortelse = "fkv", oppgittRekkefoelge = 3)
        )

        val nyFinnmark = fylkeService.createNyttFylkeMedNyUnikLokalid(
            kopierFraFylke = tromsOgFinnmark,
            nyttFylkesnummerIKlartekst = "56",
            admenhetnavnList = finnmarkNavnList,
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )

        assertThat(nyFinnmark.lokalid).isNotEqualTo(tromsOgFinnmark.lokalid)
        assertThat(nyFinnmark.fylkesnummer).isEqualTo("ee97a593-2f9d-49c7-b251-c0f0cfa8769b")
        assertThat(nyFinnmark.utledetFra!!.id).isEqualTo(tromsOgFinnmark.id)

        assertThat(nyFinnmark.getNasjon(jan1In2024)!!.lokalid).isEqualTo(nasjon.lokalid)
        assertThat(nasjon.nasjonLokalid.fylkerForNasjon).hasSize(3).extracting("id").contains(tromsOgFinnmark.id, nyFinnmark.id, nyTroms.id)

        assertThat(nyFinnmark.fylkeLokalid.administrativenhetnavn).hasSize(3).extracting("navn", "rekkefoelge", "spraak.id")
            .contains(Assertions.tuple("Finnmark", 2, "nor"), Assertions.tuple("Finnmárku", 1, "sme"), Assertions.tuple("Finmarkku", 3, "fkv"))
        assertThat(nyFinnmark.fylkeLokalid.administrativenhetnavn).extracting("id").doesNotContain(norskNavn.id, samiskNavn.id, kvenskNavn.id)
    }

    @Test
    fun `settFylkeSomHistorisk skal sette gyldigTil på fylket`() {
        val lagretFylke = saveFylkeOgUnikLokalid(lokalid = lokalIdOfFylke.value)
        fylkeService.settFylkeSomHistorisk(lagretFylke, jan1In2024)
        assertThat(lagretFylke.gyldighet.gyldigTil).isEqualTo(jan1In2024)
    }

    @Test
    fun `settFylkeSomHistorisk skal sette gyldigTil på alle administrative navn for fylket`() {
        val norskNavn = saveAdministrativenhetnavn(navn = "Troms og Finnmark", spraakid = "nor", rekkefoelge = 1)
        val lagretFylke = saveFylkeOgUnikLokalid(lokalid = lokalIdOfFylke.value, administrativenhetnavn = listOf(norskNavn))
        fylkeService.settFylkeSomHistorisk(lagretFylke, jan1In2024)
        lagretFylke.getAdministrativenhetNavn().forEach { assertThat(it.gyldighet.gyldigTil).isEqualTo(jan1In2024) }
    }

    @Test
    fun `settFylkeSomHistorisk skal sjekke at alle kommuner ikke har gyldigTil dato på eller etter den gitte settHistoriskFra datoen`() {
        val kommune = saveKommuneOgUnikLokalid()
        val lagretFylke = saveFylkeOgUnikLokalid(lokalid = lokalIdOfFylke.value, kommuner = listOf(kommune))
        val exception = assertThrows<UgyldigTilstandException> {
            fylkeService.settFylkeSomHistorisk(lagretFylke, jan1In2024)
        }
        assertThat(exception.message).containsIgnoringCase("Kan ikke sette fylket med lokalId: ${lokalIdOfFylke.value} til historisk")
    }
}
