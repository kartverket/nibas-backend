package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.builders.*
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("security-off")
@Transactional
class NasjonServiceIT : TestWithDbContainer() {

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)

    @Test
    fun hentEksisterendeNasjonMedDagensDato() {
        val kommune = saveKommuneOgUnikLokalid()
        val fylke = saveFylkeOgUnikLokalid(kommuner = listOf(kommune))

        val lokalIdOfNasjon = Lokalid("ac7e05cd-fb65-4277-ad95-63c32c1370d0")
        saveNasjonOgUnikLokalid(lokalid = lokalIdOfNasjon.value, fylker = listOf(fylke))
        assertThat(nasjonRepository.findAllByLokalid(lokalIdOfNasjon.value).size).isEqualTo(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(nasjonService.hentNasjon(Identifikator(lokalIdOfNasjon, november28In2022))).isNotNull
    }

    @Test
    fun `sjekk pekere mellom nasjon og fylke`() {
        val kommune = saveKommuneOgUnikLokalid()

        val fylkeUnikLokalid = saveUnikLokalid("fylke-fb07-4277-ad95-63c32c1370d0", Flatetype.FYLKE)
        val fylke = saveFylke(unikLokalid = fylkeUnikLokalid, kommuner = listOf(kommune))

        val lokalIdOfNasjon = Lokalid("ac7e05cd-fb65-4277-ad95-63c32c1370d0")
        val nasjonUnikLokalid = saveUnikLokalid(lokalIdOfNasjon.value, Flatetype.NASJON)
        saveNasjon(unikLokalid = nasjonUnikLokalid, fylker = listOf(fylke))

        val lagretFylke = fylkeRepository.findLastByLokalid(fylke.lokalid)!!

        var nasjonList = nasjonRepository.findAllByLokalid(lokalIdOfNasjon.value)
        assertThat(nasjonList).hasSize(1)
        var lagretNasjon = nasjonList.first()

        //Initielt én nasjon og ett fylke - skal peke på hverandre
        assertThat(lagretNasjon.nasjonLokalid.fylkerForNasjon).hasSize(1).extracting("id").contains(lagretFylke.id)
        assertThat(lagretFylke.nasjonLokalid.nasjoner).hasSize(1).extracting("id").contains(lagretNasjon.id)

        //Nasjon har ikke noen fylker ved dato før fylket oppsto, men har det for dagens dato
        assertThat(lagretNasjon.getFylker(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretNasjon.getFylker()).hasSize(1).extracting("id").contains(lagretFylke.id)

        //Fylke har ikke noen nasjon ved dato før nasjonen oppsto, man har det for dagens dato
        assertThat(lagretFylke.getNasjon(LocalDate.of(2020, Month.NOVEMBER, 28))).isNull()
        assertThat(lagretFylke.getNasjon()!!.id).isEqualTo(lagretNasjon.id)

        //Ny versjon av fylket, knyttet til samme nasjon
        lagretFylke.setGyldigTil(november28In2022)
        fylkeRepository.save(lagretFylke)
        val fylkeVersjon2 = saveFylke(id = "fylkeVersjon2", unikLokalid = fylkeUnikLokalid, gyldigFra = november28In2022, kommuner = listOf(kommune))
        lagretNasjon.addFylke(fylkeVersjon2)
        nasjonRepository.save(lagretNasjon)

        lagretNasjon = nasjonRepository.findLastByLokalid(lokalIdOfNasjon.value)!!
        assertThat(lagretNasjon.nasjonLokalid.fylkerForNasjon).hasSize(2).extracting("id").contains(lagretFylke.id, fylkeVersjon2.id)
        assertThat(lagretNasjon.getFylker(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretNasjon.getFylker(october1In2022)).hasSize(1).extracting("id").contains(lagretFylke.id)
        assertThat(lagretNasjon.getFylker(november28In2022)).hasSize(1).extracting("id").contains(fylkeVersjon2.id)

        var fylkeList = fylkeRepository.findAllByLokalid(fylke.lokalid)
        assertThat(fylkeList).hasSize(2)
        fylkeList.forEach { f ->
            assertThat(f.nasjonLokalid.nasjoner).hasSize(1).extracting("id").contains(lagretNasjon.id)
            assertThat(f.getNasjon()!!.id).isEqualTo(lagretNasjon.id)
            assertThat(f.getNasjon(november28In2022)!!.id).isEqualTo(lagretNasjon.id)
            assertThat(f.getNasjon(october1In2022)!!.id).isEqualTo(lagretNasjon.id)
        }

        //Ny versjon av nasjonen, knyttet til samme fylker
        lagretNasjon.setGyldigTil(jan1In2023)
        nasjonRepository.save(lagretNasjon)
        val nasjonVersjon2 = saveNasjon(id = "nasjonVersjon2", unikLokalid = nasjonUnikLokalid, gyldigFra = jan1In2023, fylker = fylkeList)

        nasjonList = nasjonRepository.findAllByLokalid(lokalIdOfNasjon.value)
        assertThat(nasjonList).hasSize(2)
        nasjonList.forEach { n ->
            assertThat(n.nasjonLokalid.fylkerForNasjon).hasSize(2).extracting("id").contains(lagretFylke.id, fylkeVersjon2.id)
            assertThat(n.getFylker(october1In2022)).hasSize(1).extracting("id").contains(lagretFylke.id)
            assertThat(n.getFylker(november28In2022)).hasSize(1).extracting("id").contains(fylkeVersjon2.id)
        }

        fylkeList = fylkeRepository.findAllByLokalid(fylke.lokalid)
        assertThat(fylkeList).hasSize(2)
        fylkeList.forEach { f ->
            assertThat(f.nasjonLokalid.nasjoner).hasSize(2).extracting("id").contains(lagretNasjon.id, nasjonVersjon2.id)
            assertThat(f.getNasjon(october1In2022)!!.id).isEqualTo(lagretNasjon.id)
            assertThat(f.getNasjon(november28In2022)!!.id).isEqualTo(lagretNasjon.id)
            assertThat(f.getNasjon(jan1In2023)!!.id).isEqualTo(nasjonVersjon2.id)
        }
    }

    @Test
    fun `sjekk pekere mellom nasjon og navn`() {
        val kommune = saveKommuneOgUnikLokalid()
        val fylke = saveFylkeOgUnikLokalid(kommuner = listOf(kommune))

        val norskNavn = saveAdministrativenhetnavn(navn = "Norge", spraakid = "nor", rekkefoelge = 1)

        val lokalIdOfNasjon = Lokalid("ac7e05cd-fb65-4277-ad95-63c32c1370d0")
        val nasjonUnikLokalid = saveUnikLokalid(lokalIdOfNasjon.value, Flatetype.NASJON)
        val nasjon = saveNasjon(unikLokalid = nasjonUnikLokalid, fylker = listOf(fylke), administrativenhetnavn = listOf(norskNavn))

        var nasjonList = nasjonRepository.findAllByLokalid(lokalIdOfNasjon.value)
        assertThat(nasjonList).hasSize(1)
        var lagretNasjon = nasjonList.first()

        var admNavnSet = lagretNasjon.nasjonLokalid.administrativenhetnavn
        assertThat(admNavnSet).hasSize(1)
        val administrativenhetnavn = admNavnSet.first()
        assertThat(administrativenhetnavn.spraak!!.id).isEqualTo("nor")
        assertThat(administrativenhetnavn.navn).isEqualTo(norskNavn.navn)
        assertThat(administrativenhetnavn.rekkefoelge).isEqualTo(1)
        assertThat(administrativenhetnavn.administrativenhetLokalid.nasjoner).hasSize(1).extracting("id").contains(nasjon.id)

        var lagretNorskNavn = administrativenhetnavnRepository.findById(norskNavn.id).get()
        assertThat(lagretNorskNavn.administrativenhetLokalid.nasjoner).hasSize(1).extracting("id").contains(nasjon.id)

        //Nasjon har ikke noen navn ved dato før navnet oppsto, men har det for dagens dato
        assertThat(lagretNasjon.getAdministrativenhetNavn(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretNasjon.getAdministrativenhetNavn()).hasSize(1).extracting("id").contains(lagretNorskNavn.id)

        //Administrativenhetnavn har ikke noe nasjon ved dato før nasjonen oppsto, man har det for dagens dato
        assertThat(lagretNorskNavn.getNasjon(LocalDate.of(2020, Month.NOVEMBER, 28))).isNull()
        assertThat(lagretNorskNavn.getNasjon()!!.id).isEqualTo(lagretNasjon.id)

        //Ett navn til for samme nasjon
        val nynorskNavn = saveAdministrativenhetnavn(navn = "Noreg", spraakid = "nor", rekkefoelge = 2, gyldigFra = november28In2022)
        nasjon.addAdministrativenhetNavn(nynorskNavn)

        nasjonList = nasjonRepository.findAllByLokalid(lokalIdOfNasjon.value)
        assertThat(nasjonList).hasSize(1)
        lagretNasjon = nasjonList.first()

        admNavnSet = lagretNasjon.nasjonLokalid.administrativenhetnavn
        assertThat(admNavnSet).hasSize(2).extracting("id").contains(norskNavn.id, nynorskNavn.id)
        admNavnSet.forEach { k ->
            assertThat(k.administrativenhetLokalid.nasjoner).hasSize(1).extracting("id").contains(lagretNasjon.id)
            assertThat(k.getNasjon()!!.id).isEqualTo(lagretNasjon.id)
            assertThat(k.getNasjon(november28In2022)!!.id).isEqualTo(lagretNasjon.id)
            assertThat(k.getNasjon(october1In2022)!!.id).isEqualTo(lagretNasjon.id)
        }
        //Nasjon har ikke noen navn ved dato før navnet oppsto, men har det for dagens dato
        assertThat(lagretNasjon.getAdministrativenhetNavn(LocalDate.of(2020, Month.NOVEMBER, 28))).isEmpty()
        assertThat(lagretNasjon.getAdministrativenhetNavn()).hasSize(2).extracting("id").contains(norskNavn.id, nynorskNavn.id)
        assertThat(lagretNasjon.getAdministrativenhetNavn(october1In2022)).hasSize(1).extracting("id").contains(norskNavn.id)
        assertThat(lagretNasjon.getAdministrativenhetNavn(november28In2022)).hasSize(2).extracting("id").contains(norskNavn.id, nynorskNavn.id)

        //Ny versjon av nasjonen
        lagretNasjon.setGyldigTil(jan1In2023)
        nasjonRepository.save(lagretNasjon)
        val nasjonVersjon2 = saveNasjon(id = "nasjonVersjon2", unikLokalid = nasjonUnikLokalid, gyldigFra = jan1In2023,
            fylker = lagretNasjon.nasjonLokalid.fylkerForNasjon.toList(), administrativenhetnavn = lagretNasjon.nasjonLokalid.administrativenhetnavn.toList())

        nasjonList = nasjonRepository.findAllByLokalid(lokalIdOfNasjon.value)
        assertThat(nasjonList).hasSize(2).extracting("id").contains(nasjon.id, nasjonVersjon2.id)
        nasjonList.forEach { n ->
            assertThat(n.nasjonLokalid.administrativenhetnavn).hasSize(2).extracting("id").contains(norskNavn.id, nynorskNavn.id)
            assertThat(n.getAdministrativenhetNavn()).hasSize(2).extracting("id").contains(norskNavn.id, nynorskNavn.id)
            assertThat(n.getAdministrativenhetNavn(october1In2022)).hasSize(1).extracting("id").contains(norskNavn.id)
            assertThat(n.getAdministrativenhetNavn(november28In2022)).hasSize(2).extracting("id").contains(norskNavn.id, nynorskNavn.id)
        }

        lagretNorskNavn = administrativenhetnavnRepository.findById(norskNavn.id).get()
        assertThat(lagretNorskNavn.administrativenhetLokalid.nasjoner).hasSize(2).extracting("id").contains(nasjon.id, nasjonVersjon2.id)
        assertThat(lagretNorskNavn.getNasjon(october1In2022)!!.id).isEqualTo(lagretNasjon.id)
        assertThat(lagretNorskNavn.getNasjon(jan1In2023)!!.id).isEqualTo(nasjonVersjon2.id)

        val lagretNynorskNavn = administrativenhetnavnRepository.findById(nynorskNavn.id).get()
        assertThat(lagretNynorskNavn.administrativenhetLokalid.nasjoner).hasSize(2).extracting("id").contains(nasjon.id, nasjonVersjon2.id)
        assertThat(lagretNynorskNavn.getNasjon(october1In2022)!!.id).isEqualTo(lagretNasjon.id)
        assertThat(lagretNynorskNavn.getNasjon(jan1In2023)!!.id).isEqualTo(nasjonVersjon2.id)

        lagretNynorskNavn.setGyldigTil(jan1In2023)
        administrativenhetnavnRepository.save(lagretNynorskNavn)
        val nynorskNavnVersjon2 = saveAdministrativenhetnavn(lokalid = nynorskNavn.lokalid, navn = "Noreg", spraakid = "nor", rekkefoelge = 3, gyldigFra = jan1In2023)
        nasjonList.forEach {
            it.addAdministrativenhetNavn(nynorskNavnVersjon2)
            nasjonRepository.save(it)}

        nasjonList = nasjonRepository.findAllByLokalid(lokalIdOfNasjon.value)
        assertThat(nasjonList).hasSize(2).extracting("id").contains(nasjon.id, nasjonVersjon2.id)
        nasjonList.forEach { n ->
            assertThat(n.nasjonLokalid.administrativenhetnavn).hasSize(3).extracting("id").contains(norskNavn.id, nynorskNavn.id, nynorskNavnVersjon2.id)
            assertThat(n.getAdministrativenhetNavn()).hasSize(2).extracting("id").contains(norskNavn.id, nynorskNavnVersjon2.id)
            assertThat(n.getAdministrativenhetNavn(october1In2022)).hasSize(1).extracting("id").contains(norskNavn.id)
            assertThat(n.getAdministrativenhetNavn(november28In2022)).hasSize(2).extracting("id").contains(norskNavn.id, nynorskNavn.id)
        }
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

        val kommune = saveKommuneOgUnikLokalid()
        val fylke = saveFylkeOgUnikLokalid(kommuner = listOf(kommune))
        val fylkeListe = listOf(fylke)

        val unikLokalid600 = saveUnikLokalid("600", Flatetype.NASJON)
        val unikLokalid601 = saveUnikLokalid("601", Flatetype.NASJON)
        val unikLokalid602 = saveUnikLokalid("602", Flatetype.NASJON)
        saveNasjon(id = "9000", unikLokalid = unikLokalid600, gyldigFra = jan1In2019, fylker = fylkeListe)
        saveNasjon(id = "9001", unikLokalid = unikLokalid601, gyldigFra = july1In2019, gyldigTil = jan1In2023, fylker = fylkeListe)
        saveNasjon(id = "9002", unikLokalid = unikLokalid601, gyldigFra = jan1In2023, fylker = fylkeListe)
        saveNasjon(id = "9003", unikLokalid = unikLokalid602, gyldigFra = jan1In2020, fylker = fylkeListe)

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

        val kommuneUnikLokalid = saveUnikLokalid(kommuneLokalid.value, Flatetype.KOMMUNE)
        val fylkeUnikLokalid = saveUnikLokalid(fylkeLokalid.value, Flatetype.FYLKE)
        val fylke2UnikLokalid = saveUnikLokalid(fylke2Lokalid.value, Flatetype.FYLKE)
        val nasjonUnikLokalid = saveUnikLokalid(nasjonLokalid.value, Flatetype.NASJON)

        val grense4 = saveGrenseAvTypeRiksgrense(id = "2003", lokalid = grenseLokallid2, gyldigFra = october1In2022, gyldigTil = jan1In2023)
        val grense3 = saveGrenseAvTypeRiksgrense(id = "2002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrenseAvTypeRiksgrense(id = "2001", lokalid = grenseLokallid, nesteVersjon = grense3, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrenseAvTypeRiksgrense(id = "2000", lokalid = grenseLokallid, nesteVersjon = grense2, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        val grunnkrets1 = saveGrunnkrets(id = "101", lokalid = grunnkretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val grunnkrets2 = saveGrunnkrets(id = "102", lokalid = grunnkretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1))
        val grunnkrets3 = saveGrunnkrets(id = "103", lokalid = grunnkretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense1))
        val grunnkrets4 = saveGrunnkrets(id = "104", lokalid = grunnkretsLokalid2, gyldigFra = october1In2022, grenser = listOf(grense4))

        val kommuneGyldigTilJan23 = saveKommune(id = "123", unikLokalid = kommuneUnikLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023, grunnkretser = listOf(grunnkrets4))
        val kommuneGyldigFraJan23 = saveKommune(id = "124", unikLokalid = kommuneUnikLokalid, gyldigFra = jan1In2023, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))

        val fylke1GyldigTilJan23 = saveFylke(id = "7000", unikLokalid = fylkeUnikLokalid, gyldigFra = october1In2022, gyldigTil = jan1In2023, kommuner = listOf(kommuneGyldigTilJan23))
        val fylke1GyldigFraJan23 = saveFylke(id = "7001", unikLokalid = fylkeUnikLokalid, gyldigFra = jan1In2023, kommuner = listOf(kommuneGyldigTilJan23))
        val fylke2 = saveFylke(id = "7002", unikLokalid = fylke2UnikLokalid, gyldigFra = october1In2022, kommuner = listOf(kommuneGyldigFraJan23))

        saveNasjon(id = "9000", unikLokalid = nasjonUnikLokalid, gyldigFra = october1In2022, fylker = listOf(fylke1GyldigTilJan23, fylke1GyldigFraJan23, fylke2))

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
        val kommune = saveKommuneOgUnikLokalid()
        val fylke = saveFylkeOgUnikLokalid(kommuner = listOf(kommune))

        val nasjonUnikLokalid = saveUnikLokalid(lokalIdOfNasjon.value, Flatetype.NASJON)
        saveNasjon(id = "9000", unikLokalid = nasjonUnikLokalid, gyldigTil = jan1In2023, fylker = listOf(fylke))
        saveNasjon(id = "9001", unikLokalid = nasjonUnikLokalid, gyldigFra = jan1In2023, fylker = listOf(fylke), opphav = "Sandefjord")
    }


}
