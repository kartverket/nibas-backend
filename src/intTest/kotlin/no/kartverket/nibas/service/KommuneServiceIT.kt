package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.builders.*
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.extensions.geometry.grenseKoblingToFeature
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.testcontainer.assertGrenser
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("localhost")
@Transactional
class KommuneServiceIT : TestWithDbContainer() {

    @MockkBean
    private lateinit var kodelisteServiceMock: KodelisteService

    private val lokalidOfKommuneInDataSet = "ac7e05cd-fb07-4270-ad95-e3c32c1370d0"
    private val lokalidOfKommuneMedGrunnkretsInDataSet = "8b023c74-4db4-48f9-8a5c-a3124c75da14"
    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28) // Dagens dato
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val grenseLokallid = Lokalid("888")
    private val kretsLokalid = Lokalid("1001")
    private val kommuneLokalid = Lokalid("750")

    @Test
    fun hentEksisterendeKommuneMedDagensDato() {
        val identifikatorKommuneLokalidMedDato = Identifikator(Lokalid(lokalidOfKommuneMedGrunnkretsInDataSet), november28In2022)
        val kommune = kommuneService.hentKommune(identifikatorKommuneLokalidMedDato)
        assertThat(kommune).isNotNull
        assertThat(kommuneService.hentKommune(Identifikator(Lokalid(lokalidOfKommuneMedGrunnkretsInDataSet)))).isNotNull

        val grensekoblinger = kommuneService.hentGrensekoblingerForKommune(identifikatorKommuneLokalidMedDato)
        assertThat(grensekoblinger).isNotEmpty

        val features = grensekoblinger.map { it.grenseKoblingToFeature(november28In2022) }
        assertThat(features).isNotEmpty
    }
    @Test
    fun hentGrensekoblingerForAlleGrunnkretserIKommuneHistoriske() {
        // Grense 1 er gyldig til 1.1.23
        // Grense 2 er gyldig 1.1.23-1.1.24
        // Grense 3 er gyldig fra 1.1.24
        val grense3 = saveGrense(id = "2002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrense(id = "2001", lokalid = grenseLokallid, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrense(id = "2000", lokalid = grenseLokallid, gyldigFra = october1In2022, gyldigTil = jan1In2023)
        // Alle grunnkretsene er koblet til grense 1 i koblingstabellen
        // Grunnkrets 2 er ikke gyldig 1.1.24 og det er den vi tester å få ut
        val grunnkrets1 = saveGrunnkrets(id = "101", lokalid = kretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val grunnkrets2 = saveGrunnkrets(id = "102", lokalid = kretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1, grense2))
        val grunnkrets3 = saveGrunnkrets(id = "103", lokalid = kretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense3))

        saveKommuneOgUnikLokalid(id = "123", lokalid = kommuneLokalid.value, gyldigFra = october1In2022, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))
        val identifikatorFor1Jan24 = Identifikator(kommuneLokalid, jan1In2024)
        val historiskeGrenser = kommuneService.hentGrensekoblingerForAlleGrunnkretserIKommuneHistoriske(identifikatorFor1Jan24)
        // Skal bare få ut den ene som ikke lenger er gyldig på datoen som sendes inn
        assertThat(historiskeGrenser).hasSize(1)
        assertGrenser(
            historiskeGrenser.toList(),
            grenseIGrensekoblingId = "2001",
            enhetKobletTilGrenseLokalid = kretsLokalid
        )
    }
    @Test
    fun hentGrensekoblingerForAlleStemmekretserIKommuneHistoriske() {
        // Grense 1 er gyldig til 1.1.23
        // Grense 2 er gyldig 1.1.23-1.1.24
        // Grense 3 er gyldig fra 1.1.24
        val grense3 = saveGrense(id = "2002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrense(id = "2001", lokalid = grenseLokallid, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrense(id = "2000", lokalid = grenseLokallid, gyldigFra = october1In2022, gyldigTil = jan1In2023)
        // Alle grunnkretsene er koblet til grense 1 i koblingstabellen
        // Grunnkrets 2 er ikke gyldig 1.1.24 og det er den vi tester å få ut
        val stemmekrets1 = saveStemmekrets(id = "101", lokalid = kretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val stemmekrets2 = saveStemmekrets(id = "102", lokalid = kretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1, grense2))
        val stemmekrets3 = saveStemmekrets(id = "103", lokalid = kretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense3))

        saveKommuneOgUnikLokalid(id = "123", lokalid = kommuneLokalid.value, gyldigFra = october1In2022, stemmekretser = listOf(stemmekrets1, stemmekrets2, stemmekrets3))
        val identifikatorFor1Jan24 = Identifikator(kommuneLokalid, jan1In2024)
        val historiskeGrenser = kommuneService.hentGrensekoblingerForAlleStemmekretserIKommuneHistoriske(identifikatorFor1Jan24)
        // Skal bare få ut den ene som ikke lenger er gyldig på datoen som sendes inn
        assertThat(historiskeGrenser).hasSize(1)
        assertGrenser(
            historiskeGrenser.toList(),
            grenseIGrensekoblingId = "2001",
            enhetKobletTilGrenseLokalid = kretsLokalid
        )
    }
    @Test
    fun hentVersjonAvKommuneFramITid() {
        val kommuneLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-e3c32c137055")
        saveKommuneMed1FramtidigVersjon(kommuneLokalid)
        assertThat(kommuneRepository.findAllByLokalid(kommuneLokalid.value).size).isEqualTo(2)

        val framtidigKommune = kommuneService.hentKommune(Identifikator((kommuneLokalid), jan1In2023))
        assertThat(framtidigKommune).isNotNull
        assertThat(framtidigKommune!!.kommunenummerUuid).isEqualTo("0302")
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

        saveKommuneOgUnikLokalid(id = "9000", lokalid = "1001", gyldigFra = july1In2019, gyldigTil = july2In2019)
        saveKommuneOgUnikLokalid(id = "9001", lokalid = "1002", gyldigFra = july2In2019, gyldigTil = july3In2019)
        saveKommuneOgUnikLokalid(id = "9002", lokalid = "1003", gyldigFra = july3In2019, gyldigTil = null)

        assertThat(kommuneService.hentAlleKommuner(july1In2019).map { it.id }).isEqualTo(listOf("9000"))
        assertThat(kommuneService.hentAlleKommuner(july2In2019).map { it.id }).isEqualTo(listOf("9001"))
        assertThat(kommuneService.hentAlleKommuner(july3In2019).map { it.id }).isEqualTo(listOf("9002"))
    }

    @Test
    fun hentKommunerForFylke() {
        val fylkeLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-73c32c137054")
        val kommuneLokalid = Lokalid("dc43454d-eb08-4270-ad95-73c32c137054")
        val unikLokalidFylke = saveUnikLokalid(fylkeLokalid.value, Flatetype.FYLKE)

        val kommune = saveKommuneOgUnikLokalid(id = "9000", lokalid = kommuneLokalid.value, gyldigFra = october1In2022, gyldigTil = jan1In2023)
        val kommuneFramITid = saveKommune(id = "9001", unikLokalid = kommune.kommuneLokalid, gyldigFra = jan1In2023)
        val kommuneMedAnnenLokalid = saveKommuneOgUnikLokalid(id = "9002", lokalid = "br7e05cd-eb08-4270-ad99-73c32c137054", gyldigFra = jan1In2023)

        saveFylke(id = "11000", unikLokalid = unikLokalidFylke, gyldigFra = october1In2022, gyldigTil = jan1In2023, kommuner = listOf(kommune, kommuneFramITid))
        saveFylke(id = "11001", unikLokalid = unikLokalidFylke, gyldigFra = jan1In2023, kommuner = listOf(kommune, kommuneFramITid, kommuneMedAnnenLokalid))

        val kommunerForFylke = kommuneService.hentKommunerForFylke(Identifikator(fylkeLokalid, november28In2022))
        assertThat(kommunerForFylke.map { it.id }).isEqualTo(listOf("9000"))
    }

    @Test
    fun hentKommunerForFylker() {
        val fylkeLokalid1 = Lokalid("ac7e05cd-fb07-4270-ad95-73c32c137054")
        val fylkeLokalid2 = Lokalid("e70c69ba-5bea-48a8-952c-b4860ada5afa")

        val unikLokalidFylke1 = saveUnikLokalid(fylkeLokalid1.value, Flatetype.FYLKE)
        val unikLokalidFylke2 = saveUnikLokalid(fylkeLokalid2.value, Flatetype.FYLKE)

        val kommuneLokalid1 = Lokalid("b495e6ec-f1c2-4b57-974d-b1cae1207c33")
        val kommuneLokalid2 = Lokalid("ab9f3402-23fb-48b4-a4fd-dbe4e3cc323b")
        val kommuneLokalid3 = Lokalid("c395e6ec-f1c2-4b57-974d-b1cae1207c3e")
        val kommuneLokalid4 = Lokalid("i39f3402-23fb-48b4-a4fd-dbe4e3cc329e")
        val kommuneLokalid5 = Lokalid("i39f3402-23fb-48b4-a4fd-dbe4e3cc329e")

        val kommune1 = saveKommuneOgUnikLokalid(id = "9000", lokalid = kommuneLokalid1.value, gyldigFra = october1In2022)
        val kommune2 = saveKommuneOgUnikLokalid(id = "9001", lokalid = kommuneLokalid2.value, gyldigFra = october1In2022)
        val kommune3 = saveKommuneOgUnikLokalid(id = "9002", lokalid = kommuneLokalid3.value, gyldigFra = october1In2022)
        val kommune4 = saveKommuneOgUnikLokalid(id = "9003", lokalid = kommuneLokalid4.value, gyldigFra = october1In2022)
        val kommune5utgaatt = saveKommuneOgUnikLokalid(id = "9004", lokalid = kommuneLokalid5.value, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        saveFylke(id = "11000", unikLokalid = unikLokalidFylke1, gyldigFra = october1In2022,  kommuner = listOf(kommune1,kommune2))
        saveFylke(id = "11001", unikLokalid = unikLokalidFylke2, gyldigFra = october1In2022, kommuner = listOf(kommune3,kommune4,kommune5utgaatt))

        val fylkeLokalids = listOf(Identifikator(fylkeLokalid1), Identifikator(fylkeLokalid2))
        val kommunerForFylker = kommuneService.hentKommunerForFylker(fylkeLokalids)
        val kommuneIds = kommunerForFylker.map { it.id }

        assertThat(kommuneIds.sorted()).isEqualTo(listOf("9000", "9001", "9002", "9003"))
    }
    @Test
    fun hentKommunesGrunnkretser() {
        val grunnkretsLokalid = Lokalid("ac7e05cd-eb08-4270-ab95-73c32c137057")
        val grunnkrets1 = saveGrunnkrets(id = "8000", lokalid = grunnkretsLokalid, gyldigFra = october1In2022, gyldigTil = november28In2022)
        val grunnkrets2 = saveGrunnkrets(id = "8001", lokalid = grunnkretsLokalid, gyldigFra = november28In2022, gyldigTil = jan1In2023)
        val grunnkrets3 = saveGrunnkrets(id = "8002", lokalid = grunnkretsLokalid, gyldigFra = jan1In2023)

        val kommuneLokalid = Lokalid("ac7e05cd-eb08-4270-ad95-73c32c137054")
        val unikLokalid = saveUnikLokalid(kommuneLokalid.value, Flatetype.KOMMUNE)

        saveKommune(id = "9000", unikLokalid = unikLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023, grunnkretser = listOf(grunnkrets1, grunnkrets2))
        saveKommune(id = "9001", unikLokalid = unikLokalid, gyldigFra = jan1In2023, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))

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
        val unikLokalid = saveUnikLokalid(kommuneLokalid.value, Flatetype.KOMMUNE)

        saveKommune(id = "9000", unikLokalid = unikLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023, stemmekretser = listOf(stemmekrets1, stemmekrets2))
        saveKommune(id = "9001", unikLokalid = unikLokalid, gyldigFra = jan1In2023, stemmekretser = listOf(stemmekrets1, stemmekrets2, stemmekrets3))

        val stemmekretserForKommune = kommuneService.hentKommunesStemmekretser(Identifikator(kommuneLokalid, november28In2022))
        assertThat(stemmekretserForKommune.map { it.id }).isEqualTo(listOf("8001"))
    }

    @Test
    fun hentGrenserForKommuneFramITid() {
        // Grense 1 er gyldig til 1.1.23
        // Grense 2 er gyldig 1.1.23-1.1.24
        // Grense 3 er gyldig fra 1.1.24
        val grense3 = saveGrense(id = "2002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrense(id = "2001", lokalid = grenseLokallid, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrense(id = "2000", lokalid = grenseLokallid, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        // Alle grunnkretsene er koblet til grense 1 i koblingstabellen
        // Grunnkrets 2 er gyldig 1.1.23 og det er dennes kobling som vil brukes
        val grunnkrets1 = saveGrunnkrets(id = "101", lokalid = kretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val grunnkrets2 = saveGrunnkrets(id = "102", lokalid = kretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1, grense2))
        val grunnkrets3 = saveGrunnkrets(id = "103", lokalid = kretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense3))

        val identifikatorFor1Jan23 = Identifikator(kommuneLokalid, jan1In2023)
        saveKommuneOgUnikLokalid(id = "123", lokalid = kommuneLokalid.value, gyldigFra = october1In2022, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))

        val grensekoblinger = kommuneService.hentGrensekoblingerForKommune(identifikatorFor1Jan23)

        assertGrenser(
            grensekoblinger.toList(),
            grenseIGrensekoblingId = "2001",
            enhetKobletTilGrenseLokalid = kretsLokalid
        )
    }

    private fun saveKommuneMed1FramtidigVersjon(kommuneLokalid: Lokalid) {
        val unikLokalid = saveUnikLokalid(kommuneLokalid.value, Flatetype.KOMMUNE)
        saveKommune(unikLokalid = unikLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023)
        saveKommune(id = "9001", unikLokalid = unikLokalid, kommunenummerUuid = "0302", gyldigFra = jan1In2023)
    }

    @Test
    fun `sjekk pekere mellom kommune og navn`() {
        val norskNavn = saveAdministrativenhetnavn(navn = "Karasjok", spraakid = "nor", rekkefoelge = 1)

        val lokalIdOfKommune = Lokalid("ac7e05cd-fb65-4277-ad95-63c32c1370d0")
        val kommune = saveKommuneOgUnikLokalid(lokalid = lokalIdOfKommune.value, administrativenhetnavn = listOf(norskNavn))

        var kommuneList = kommuneRepository.findAllByLokalid(lokalIdOfKommune.value)
        assertThat(kommuneList).hasSize(1)
        var lagretKommune = kommuneList.first()

        var admNavnSet = lagretKommune.kommuneLokalid.administrativenhetnavn
        assertThat(admNavnSet).hasSize(1)
        val administrativenhetnavn = admNavnSet.first()
        assertThat(administrativenhetnavn.spraak!!.id).isEqualTo("nor")
        assertThat(administrativenhetnavn.navn).isEqualTo(norskNavn.navn)
        assertThat(administrativenhetnavn.rekkefoelge).isEqualTo(1)
        assertThat(administrativenhetnavn.administrativenhetLokalid.kommuner).hasSize(1).extracting("id").contains(kommune.id)

        var lagretNorskNavn = administrativenhetnavnRepository.findById(norskNavn.id).get()
        assertThat(lagretNorskNavn.administrativenhetLokalid.kommuner).hasSize(1).extracting("id").contains(kommune.id)

        //Kommune har ikke noen navn ved dato før navnet oppsto, men har det for dagens dato
        assertThat(lagretKommune.getAdministrativenhetNavn(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretKommune.getAdministrativenhetNavn()).hasSize(1).extracting("id").contains(lagretNorskNavn.id)

        //Administrativenhetnavn har ikke noe kommune ved dato før kommunen oppsto, man har det for dagens dato
        assertThat(lagretNorskNavn.getKommune(LocalDate.of(2020, Month.NOVEMBER, 28))).isNull()
        assertThat(lagretNorskNavn.getKommune()!!.id).isEqualTo(lagretKommune.id)

        //Ett navn til for samme kommune
        val samiskNavn = saveAdministrativenhetnavn(navn = "Kárášjohka", spraakid = "sme", rekkefoelge = 2, gyldigFra = november28In2022)
        kommune.addAdministrativenhetNavn(samiskNavn)

        kommuneList = kommuneRepository.findAllByLokalid(lokalIdOfKommune.value)
        assertThat(kommuneList).hasSize(1)
        lagretKommune = kommuneList.first()

        admNavnSet = lagretKommune.kommuneLokalid.administrativenhetnavn
        assertThat(admNavnSet).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
        admNavnSet.forEach { k ->
            assertThat(k.administrativenhetLokalid.kommuner).hasSize(1).extracting("id").contains(lagretKommune.id)
            assertThat(k.getKommune()!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(november28In2022)!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(october1In2022)!!.id).isEqualTo(lagretKommune.id)
        }
        //Kommune har ikke noen navn ved dato før navnet oppsto, men har det for dagens dato
        assertThat(lagretKommune.getAdministrativenhetNavn(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretKommune.getAdministrativenhetNavn()).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
        assertThat(lagretKommune.getAdministrativenhetNavn(october1In2022)).hasSize(1).extracting("id").contains(norskNavn.id)
        assertThat(lagretKommune.getAdministrativenhetNavn(november28In2022)).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)

        //Ny versjon av kommunen
        lagretKommune.setGyldigTil(jan1In2023)
        kommuneRepository.save(lagretKommune)
        val kommuneVersjon2 = saveKommune(id = "kommuneVersjon2", unikLokalid = lagretKommune.kommuneLokalid, gyldigFra = jan1In2023,
            administrativenhetnavn = lagretKommune.kommuneLokalid.administrativenhetnavn.toList())

        kommuneList = kommuneRepository.findAllByLokalid(lokalIdOfKommune.value)
        assertThat(kommuneList).hasSize(2).extracting("id").contains(kommune.id, kommuneVersjon2.id)
        kommuneList.forEach { n ->
            assertThat(n.kommuneLokalid.administrativenhetnavn).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
            assertThat(n.getAdministrativenhetNavn()).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
            assertThat(n.getAdministrativenhetNavn(october1In2022)).hasSize(1).extracting("id").contains(norskNavn.id)
            assertThat(n.getAdministrativenhetNavn(november28In2022)).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
        }

        lagretNorskNavn = administrativenhetnavnRepository.findById(norskNavn.id).get()
        assertThat(lagretNorskNavn.administrativenhetLokalid.kommuner).hasSize(2).extracting("id").contains(kommune.id, kommuneVersjon2.id)
        assertThat(lagretNorskNavn.getKommune(october1In2022)!!.id).isEqualTo(lagretKommune.id)
        assertThat(lagretNorskNavn.getKommune(jan1In2023)!!.id).isEqualTo(kommuneVersjon2.id)

        val lagretSamiskNavn = administrativenhetnavnRepository.findById(samiskNavn.id).get()
        assertThat(lagretSamiskNavn.administrativenhetLokalid.kommuner).hasSize(2).extracting("id").contains(kommune.id, kommuneVersjon2.id)
        assertThat(lagretSamiskNavn.getKommune(october1In2022)!!.id).isEqualTo(lagretKommune.id)
        assertThat(lagretSamiskNavn.getKommune(jan1In2023)!!.id).isEqualTo(kommuneVersjon2.id)

        lagretSamiskNavn.setGyldigTil(jan1In2023)
        administrativenhetnavnRepository.save(lagretSamiskNavn)
        val samiskNavnVersjon2 = saveAdministrativenhetnavn(lokalid = samiskNavn.lokalid, navn = "Romsa 2", spraakid = "sme", rekkefoelge = 2, gyldigFra = jan1In2023)
        kommuneList.forEach {
            it.addAdministrativenhetNavn(samiskNavnVersjon2)
            kommuneRepository.save(it)}

        kommuneList = kommuneRepository.findAllByLokalid(lokalIdOfKommune.value)
        assertThat(kommuneList).hasSize(2).extracting("id").contains(kommune.id, kommuneVersjon2.id)
        kommuneList.forEach { n ->
            assertThat(n.kommuneLokalid.administrativenhetnavn).hasSize(3).extracting("id").contains(norskNavn.id, samiskNavn.id, samiskNavnVersjon2.id)
            assertThat(n.getAdministrativenhetNavn()).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavnVersjon2.id)
            assertThat(n.getAdministrativenhetNavn(october1In2022)).hasSize(1).extracting("id").contains(norskNavn.id)
            assertThat(n.getAdministrativenhetNavn(november28In2022)).hasSize(2).extracting("id").contains(norskNavn.id, samiskNavn.id)
        }

    }

    @Test
    fun `sjekk pekere mellom kommune og stemmekrets`() {
        val stemmekretsLokalid = Lokalid("stemmekretsLokalid")
        val stemmekrets = saveStemmekrets(lokalid = stemmekretsLokalid)
        val lokalIdOfKommune = Lokalid("lokalIdOfKommune")
        val kommuneUnikLokalid = saveUnikLokalid(lokalIdOfKommune.value, Flatetype.KOMMUNE)

        saveKommune(unikLokalid = kommuneUnikLokalid, stemmekretser = listOf(stemmekrets))
        val lagretStemmekrets = stemmekretsRepository.findLastByLokalid(stemmekrets.lokalid)!!

        var kommuneList = kommuneRepository.findAllByLokalid(lokalIdOfKommune.value)
        assertThat(kommuneList).hasSize(1)
        var lagretKommune = kommuneList.first()

        //Initielt én stemmekrets og en kommune - skal peke på hverandre
        assertThat(lagretKommune.kommuneLokalid.stemmekretser).hasSize(1).extracting("id").contains(lagretStemmekrets.id)
        assertThat(lagretStemmekrets.kommuneLokalid.kommuner).hasSize(1).extracting("id").contains(lagretKommune.id)

        //Kommune har ikke noen stemmekretser ved dato før stemmekretsen oppsto, men har det for dagens dato
        assertThat(lagretKommune.getStemmekretser(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretKommune.getStemmekretser()).hasSize(1).extracting("id").contains(lagretStemmekrets.id)

        //Stemmekrets har ikke noe kommune ved dato før kommunen oppsto, man har det for dagens dato
        assertThat(lagretStemmekrets.getKommune(LocalDate.of(2020, Month.NOVEMBER, 28))).isNull()
        assertThat(lagretStemmekrets.getKommune()!!.id).isEqualTo(lagretKommune.id)

        //Ny versjon av stemmekretsen, knyttet til samme kommune
        lagretStemmekrets.setGyldigTil(november28In2022)
        stemmekretsRepository.save(lagretStemmekrets)
        val stemmekretsVersjon2 = saveStemmekrets(id = "stemmekretsVersjon2", lokalid = stemmekretsLokalid, gyldigFra = november28In2022)
        lagretKommune.addStemmekrets(stemmekretsVersjon2)
        kommuneRepository.save(lagretKommune)

        lagretKommune = kommuneRepository.findLastByLokalid(lokalIdOfKommune.value)!!
        assertThat(lagretKommune.kommuneLokalid.stemmekretser).hasSize(2).extracting("id").contains(lagretStemmekrets.id, stemmekretsVersjon2.id)
        assertThat(lagretKommune.getStemmekretser(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretKommune.getStemmekretser(october1In2022)).hasSize(1).extracting("id").contains(lagretStemmekrets.id)
        assertThat(lagretKommune.getStemmekretser(november28In2022)).hasSize(1).extracting("id").contains(stemmekretsVersjon2.id)

        var stemmekretsList = stemmekretsRepository.findAllByLokalid(stemmekrets.lokalid)
        assertThat(stemmekretsList).hasSize(2)
        stemmekretsList.forEach { k ->
            assertThat(k.kommuneLokalid.kommuner).hasSize(1).extracting("id").contains(lagretKommune.id)
            assertThat(k.getKommune()!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(november28In2022)!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(october1In2022)!!.id).isEqualTo(lagretKommune.id)
        }

        //Ny versjon av kommunen, knyttet til samme stemmekretser
        lagretKommune.setGyldigTil(jan1In2023)
        kommuneRepository.save(lagretKommune)
        val kommuneVersjon2 = saveKommune(id = "kommuneVersjon2", unikLokalid = kommuneUnikLokalid, gyldigFra = jan1In2023, stemmekretser = stemmekretsList)

        kommuneList = kommuneRepository.findAllByLokalid(lokalIdOfKommune.value)
        assertThat(kommuneList).hasSize(2)
        kommuneList.forEach { n ->
            assertThat(n.kommuneLokalid.stemmekretser).hasSize(2).extracting("id").contains(lagretStemmekrets.id, stemmekretsVersjon2.id)
            assertThat(n.getStemmekretser(october1In2022)).hasSize(1).extracting("id").contains(lagretStemmekrets.id)
            assertThat(n.getStemmekretser(november28In2022)).hasSize(1).extracting("id").contains(stemmekretsVersjon2.id)
        }

        stemmekretsList = stemmekretsRepository.findAllByLokalid(stemmekrets.lokalid)
        assertThat(stemmekretsList).hasSize(2)
        stemmekretsList.forEach { k ->
            assertThat(k.kommuneLokalid.kommuner).hasSize(2).extracting("id").contains(lagretKommune.id, kommuneVersjon2.id)
            assertThat(k.getKommune(october1In2022)!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(november28In2022)!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(jan1In2023)!!.id).isEqualTo(kommuneVersjon2.id)
        }
    }
    
    @Test
    fun `sjekk pekere mellom kommune og grunnkrets`() {
        val grunnkretsLokalid = Lokalid("grunnkretsLokalid")
        val grunnkrets = saveGrunnkrets(lokalid = grunnkretsLokalid)
        val lokalIdOfKommune = Lokalid("lokalIdOfKommune")
        val kommuneUnikLokalid = saveUnikLokalid(lokalIdOfKommune.value, Flatetype.KOMMUNE)

        saveKommune(unikLokalid = kommuneUnikLokalid, grunnkretser = listOf(grunnkrets))
        val lagretGrunnkrets = grunnkretsRepository.findLastByLokalid(grunnkrets.lokalid)!!

        var kommuneList = kommuneRepository.findAllByLokalid(lokalIdOfKommune.value)
        assertThat(kommuneList).hasSize(1)
        var lagretKommune = kommuneList.first()

        //Initielt én grunnkrets og en kommune - skal peke på hverandre
        assertThat(lagretKommune.kommuneLokalid.grunnkretser).hasSize(1).extracting("id").contains(lagretGrunnkrets.id)
        assertThat(lagretGrunnkrets.kommuneLokalid.kommuner).hasSize(1).extracting("id").contains(lagretKommune.id)

        //Kommune har ikke noen grunnkretser ved dato før grunnkretsen oppsto, men har det for dagens dato
        assertThat(lagretKommune.getGrunnkretser(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretKommune.getGrunnkretser()).hasSize(1).extracting("id").contains(lagretGrunnkrets.id)

        //Grunnkrets har ikke noe kommune ved dato før kommunen oppsto, man har det for dagens dato
        assertThat(lagretGrunnkrets.getKommune(LocalDate.of(2020, Month.NOVEMBER, 28))).isNull()
        assertThat(lagretGrunnkrets.getKommune()!!.id).isEqualTo(lagretKommune.id)

        //Ny versjon av grunnkretsen, knyttet til samme kommune
        lagretGrunnkrets.setGyldigTil(november28In2022)
        grunnkretsRepository.save(lagretGrunnkrets)
        val grunnkretsVersjon2 = saveGrunnkrets(id = "grunnkretsVersjon2", lokalid = grunnkretsLokalid, gyldigFra = november28In2022)
        lagretKommune.addGrunnkrets(grunnkretsVersjon2)
        kommuneRepository.save(lagretKommune)

        lagretKommune = kommuneRepository.findLastByLokalid(lokalIdOfKommune.value)!!
        assertThat(lagretKommune.kommuneLokalid.grunnkretser).hasSize(2).extracting("id").contains(lagretGrunnkrets.id, grunnkretsVersjon2.id)
        assertThat(lagretKommune.getGrunnkretser(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretKommune.getGrunnkretser(october1In2022)).hasSize(1).extracting("id").contains(lagretGrunnkrets.id)
        assertThat(lagretKommune.getGrunnkretser(november28In2022)).hasSize(1).extracting("id").contains(grunnkretsVersjon2.id)

        var grunnkretsList = grunnkretsRepository.findAllByLokalid(grunnkrets.lokalid)
        assertThat(grunnkretsList).hasSize(2)
        grunnkretsList.forEach { k ->
            assertThat(k.kommuneLokalid.kommuner).hasSize(1).extracting("id").contains(lagretKommune.id)
            assertThat(k.getKommune()!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(november28In2022)!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(october1In2022)!!.id).isEqualTo(lagretKommune.id)
        }

        //Ny versjon av kommunen, knyttet til samme grunnkretser
        lagretKommune.setGyldigTil(jan1In2023)
        kommuneRepository.save(lagretKommune)
        val kommuneVersjon2 = saveKommune(id = "kommuneVersjon2", unikLokalid = kommuneUnikLokalid, gyldigFra = jan1In2023, grunnkretser = grunnkretsList)

        kommuneList = kommuneRepository.findAllByLokalid(lokalIdOfKommune.value)
        assertThat(kommuneList).hasSize(2)
        kommuneList.forEach { n ->
            assertThat(n.kommuneLokalid.grunnkretser).hasSize(2).extracting("id").contains(lagretGrunnkrets.id, grunnkretsVersjon2.id)
            assertThat(n.getGrunnkretser(october1In2022)).hasSize(1).extracting("id").contains(lagretGrunnkrets.id)
            assertThat(n.getGrunnkretser(november28In2022)).hasSize(1).extracting("id").contains(grunnkretsVersjon2.id)
        }

        grunnkretsList = grunnkretsRepository.findAllByLokalid(grunnkrets.lokalid)
        assertThat(grunnkretsList).hasSize(2)
        grunnkretsList.forEach { k ->
            assertThat(k.kommuneLokalid.kommuner).hasSize(2).extracting("id").contains(lagretKommune.id, kommuneVersjon2.id)
            assertThat(k.getKommune(october1In2022)!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(november28In2022)!!.id).isEqualTo(lagretKommune.id)
            assertThat(k.getKommune(jan1In2023)!!.id).isEqualTo(kommuneVersjon2.id)
        }
    }

    @Test
    fun `oppdaterKommune skal lage ny kommune og sette gyldigtil for gammel kommune`() {
        val kommuneRequest = KommuneRequestBuilder().withVersion(2).withSamiskforvaltningsomraade(true).build()
        val identifikator = Identifikator(Lokalid(kommuneRequest.lokalid), october1In2022)

        val kommune = saveKommuneOgUnikLokalid(
            id = "2",
            lokalid = identifikator.lokalid.value,
            gyldigFra = october1In2022,
        )
        saveFylkeOgUnikLokalid(kommuner = listOf(kommune))
        val versjonFoerOppdatering = kommune.version

        val now = LocalDate.now()
        val opprinneligKommune = kommuneService.hentKommune(identifikator)!!

        kommuneService.oppdaterKommune(
            lokalid = identifikator.lokalid,
            oppdatertKommune = kommuneRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligKommuneEtter = kommuneService.hentKommune(identifikator)!!
        val kommuneEtter = kommuneService.hentKommune(Identifikator(identifikator.lokalid))!!

        assertThat(kommuneEtter.samiskforvaltningsomraade).isNotEqualTo(kommune.samiskforvaltningsomraade)
            .isEqualTo(kommuneRequest.samiskforvaltningsomraade)
        assertThat(kommuneEtter.kommunenummerUuid).isEqualTo(kommune.kommunenummerUuid)
        assertThat(kommuneEtter.kommunenummer).isEqualTo(kommune.kommunenummer)
        assertThat(kommuneEtter.omraade).isEqualTo(kommune.omraade)
        assertThat(kommuneEtter.posisjon).isEqualTo(kommune.posisjon)
        assertThat(kommuneEtter.gyldighet.gyldigFra).isEqualTo(now)
        assertThat(kommuneEtter.gyldighet.gyldigTil).isNull()

        assertThat(opprinneligKommuneEtter.gyldighet.gyldigFra).isEqualTo(october1In2022).isEqualTo(opprinneligKommune.gyldighet.gyldigFra)
        assertThat(opprinneligKommuneEtter.gyldighet.gyldigTil).isEqualTo(now)
        assertThat(opprinneligKommuneEtter.version).isGreaterThan(versjonFoerOppdatering)

        assertThat(kommuneEtter.kommuneLokalid.lokalid).isEqualTo(opprinneligKommuneEtter.kommuneLokalid.lokalid).isEqualTo(kommune.kommuneLokalid.lokalid)
        assertThat(kommuneEtter.fylkeLokalid.lokalid).isEqualTo(opprinneligKommuneEtter.fylkeLokalid.lokalid).isEqualTo(kommune.fylkeLokalid.lokalid)
        assertThat(kommuneEtter.kommuneLokalid).isEqualTo(opprinneligKommuneEtter.kommuneLokalid).isEqualTo(kommune.kommuneLokalid)
        assertThat(kommuneEtter.fylkeLokalid).isEqualTo(opprinneligKommuneEtter.fylkeLokalid).isEqualTo(kommune.fylkeLokalid)
    }

    @Test
    fun `sjekk createNyKommune i nytt fylke`() {
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue("5622") } returns "b0f1dfb4-2ba3-44a8-9b86-37bea79f6a60"

        val norskNavn = saveAdministrativenhetnavn(navn = "Porsanger", spraakid = "nor", rekkefoelge = 1)
        val samiskNavn = saveAdministrativenhetnavn(navn = "Porsáŋgu", spraakid = "sme", rekkefoelge = 2)
        val kvenskNavn = saveAdministrativenhetnavn(navn = "Porsanki", spraakid = "fkv", rekkefoelge = 3)

        val kommune = saveKommuneOgUnikLokalid(lokalid = "5436Porsanger", kommunenummerUuid = "f2c438ec-4551-4245-b6a0-153d53a39dde",
            kommunenummer = "5436", administrativenhetnavn = listOf(norskNavn, samiskNavn, kvenskNavn))
        saveFylkeOgUnikLokalid(lokalid = "54TromsOgFinnmark", fylkesnummerUuid = "efe425f5-fc7f-4a29-a70d-31363ec6a306",
            fylkesnummer = "54", kommuner = listOf(kommune))

        val nyFinnmark = saveFylkeOgUnikLokalid(id = "56Finnmark1", lokalid = "56Finnmark",
            fylkesnummerUuid = "ee97a593-2f9d-49c7-b251-c0f0cfa8769b", fylkesnummer = "56", gyldigFra = jan1In2024)

        assertThat(kommune.utledetFra).isNull()
        val nyKommune = kommuneService.createNyKommuneMedNyUnikLokalid(
            kopierFraKommune = kommune,
            nyttKommunenummer = "5622",
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )

        assertThat(nyKommune.id).isNotEqualTo(kommune.id)
        assertThat(nyKommune.lokalid).isNotEqualTo(kommune.lokalid)
        assertThat(nyKommune.kommunenummerUuid).isEqualTo("b0f1dfb4-2ba3-44a8-9b86-37bea79f6a60")
        assertThat(nyKommune.kommunenummer).isEqualTo("5622")
        assertThat(nyKommune.utledetFra!!.id).isEqualTo(kommune.id)

        assertThat(nyKommune.getFylke(jan1In2024)!!.lokalid).isEqualTo(nyFinnmark.lokalid)
        assertThat(nyFinnmark.fylkeLokalid.kommunerForFylke).hasSize(1).extracting("id").contains(nyKommune.id)

        assertThat(nyKommune.kommuneLokalid.administrativenhetnavn).hasSize(3).extracting("navn", "rekkefoelge", "spraak.id")
            .contains(tuple("Porsanger", 1, "nor"), tuple("Porsáŋgu", 2, "sme"), tuple("Porsanki", 3, "fkv"))
        assertThat(nyKommune.kommuneLokalid.administrativenhetnavn).extracting("id").doesNotContain(norskNavn.id, samiskNavn.id, kvenskNavn.id)
    }

    @Test
    fun `sjekk createNyKommune i gammelt fylke`() {
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue("1508") } returns "9720eabc-0bff-4c93-b05c-95bae39ef2ea"
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue("1580") } returns "687f3337-899f-42f8-849d-6f2d5b603dd3"

        val norskNavn = saveAdministrativenhetnavn(navn = "Ålesund", spraakid = "nor")
        val gamleAalesund = saveKommuneOgUnikLokalid(lokalid = "1507Ålesund", kommunenummerUuid = "afc65fde-8a62-497d-b35d-77345efddee3",
            kommunenummer = "1507", administrativenhetnavn = listOf(norskNavn))
        val moereOgRomsdal = saveFylkeOgUnikLokalid(lokalid = "15MøreOgRomsdal", fylkesnummerUuid = "c499a315-0cd5-4e99-8acf-bd6f4803799a",
            fylkesnummer = "15", kommuner = listOf(gamleAalesund))

        assertThat(gamleAalesund.utledetFra).isNull()
        val nyeAalesund = kommuneService.createNyKommuneMedNyUnikLokalid(
            kopierFraKommune = gamleAalesund,
            nyttKommunenummer = "1508",
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )

        assertThat(nyeAalesund.id).isNotEqualTo(gamleAalesund.id)
        assertThat(nyeAalesund.lokalid).isNotEqualTo(gamleAalesund.lokalid)
        assertThat(nyeAalesund.kommunenummerUuid).isEqualTo("9720eabc-0bff-4c93-b05c-95bae39ef2ea")
        assertThat(nyeAalesund.kommunenummer).isEqualTo("1508")
        assertThat(nyeAalesund.utledetFra!!.id).isEqualTo(gamleAalesund.id)

        assertThat(nyeAalesund.getFylke(jan1In2024)!!.lokalid).isEqualTo(moereOgRomsdal.lokalid)
        assertThat(moereOgRomsdal.fylkeLokalid.kommunerForFylke).hasSize(2).extracting("id").contains(gamleAalesund.id, nyeAalesund.id)

        assertThat(nyeAalesund.kommuneLokalid.administrativenhetnavn).hasSize(1).extracting("navn", "rekkefoelge", "spraak.id")
            .contains(tuple("Ålesund", null, "nor"))
        assertThat(nyeAalesund.kommuneLokalid.administrativenhetnavn).extracting("id").doesNotContain(norskNavn.id)

        val haramNavn = administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Haram", spraakForkortelse = "nor")
        val haram = kommuneService.createNyKommuneMedNyUnikLokalid(
            kopierFraKommune = gamleAalesund,
            nyttKommunenummer = "1580",
            admenhetnavnList = listOf(haramNavn),
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )

        assertThat(haram.lokalid).isNotEqualTo(gamleAalesund.lokalid)
        assertThat(haram.kommunenummerUuid).isEqualTo("687f3337-899f-42f8-849d-6f2d5b603dd3")
        assertThat(haram.kommunenummer).isEqualTo("1580")
        assertThat(haram.utledetFra!!.id).isEqualTo(gamleAalesund.id)

        assertThat(haram.getFylke(jan1In2024)!!.lokalid).isEqualTo(moereOgRomsdal.lokalid)
        assertThat(moereOgRomsdal.fylkeLokalid.kommunerForFylke).hasSize(3).extracting("id").contains(gamleAalesund.id, nyeAalesund.id, haram.id)

        assertThat(haram.kommuneLokalid.administrativenhetnavn).hasSize(1)
        val navn = haram.kommuneLokalid.administrativenhetnavn.first()
        assertThat(navn.id).isNotEqualTo(norskNavn.id)
        assertThat(navn.navn).isEqualTo("Haram")
        assertThat(navn.spraak!!.id).isEqualTo("nor")
        assertThat(navn.rekkefoelge).isNull()
        assertThat(navn.gyldighet.gyldigFra).isEqualTo(jan1In2024)
        assertThat(navn.gyldighet.gyldigTil).isNull()
        assertThat(navn.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)
    }

    @Test
    fun `settKommuneSomHistorisk skal sette gyldigTil paa kommunen`() {
        val lagretKommune = saveKommuneOgUnikLokalid(lokalid = lokalidOfKommuneInDataSet)
        kommuneService.settKommuneSomHistorisk(lagretKommune, jan1In2024)
        assertThat(lagretKommune.gyldighet.gyldigTil).isEqualTo(jan1In2024)
    }

    @Test
    fun `settKommuneSomHistorisk skal sette gyldigTil paa alle administrative navn forkommunen`() {
        val norskNavn = saveAdministrativenhetnavn(navn = "Ålesund", spraakid = "nor")
        val lagretKommune = saveKommuneOgUnikLokalid(lokalid = lokalidOfKommuneInDataSet, administrativenhetnavn = listOf(norskNavn))
        kommuneService.settKommuneSomHistorisk(lagretKommune, jan1In2024)
        lagretKommune.getAdministrativenhetNavn().forEach { assertThat(it.gyldighet.gyldigTil).isEqualTo(jan1In2024) }
    }

    @Test
    fun `settKommuneSomHistorisk skal sjekke at alle grunnkretser ikke har gyldigTil dato paa eller etter den gitte settHistoriskFra datoen`() {
        val grunnkrets = saveGrunnkrets(id = "8001", lokalid = kretsLokalid, gyldigFra = november28In2022)
        val lagretKommune = saveKommuneOgUnikLokalid(lokalid = lokalidOfKommuneInDataSet, grunnkretser = listOf(grunnkrets))

        Assertions.assertThatThrownBy { kommuneService.settKommuneSomHistorisk(lagretKommune, jan1In2024) }
            .isInstanceOf(UgyldigTilstandException::class.java)
            .hasMessageContaining("Kan ikke sette kommunen med lokalId: ${lokalidOfKommuneInDataSet} til historisk da den har grunnkretser som er gyldige etter 2024-01-01")
    }

    @Test
    fun `settKommuneSomHistorisk skal sjekke at alle stemmekretser ikke har gyldigTil dato paa eller etter den gitte settHistoriskFra datoen`(){
        val stemmekrets = saveStemmekrets(id = "8001", lokalid = kretsLokalid, gyldigFra = november28In2022)
        val lagretKommune = saveKommuneOgUnikLokalid(lokalid = lokalidOfKommuneInDataSet, stemmekretser = listOf(stemmekrets))

        Assertions.assertThatThrownBy { kommuneService.settKommuneSomHistorisk(lagretKommune, jan1In2024) }
            .isInstanceOf(UgyldigTilstandException::class.java)
            .hasMessageContaining("Kan ikke sette kommunen med lokalId: ${lokalidOfKommuneInDataSet} til historisk da den har stemmekretser som er gyldige etter 2024-01-01")
    }
}
