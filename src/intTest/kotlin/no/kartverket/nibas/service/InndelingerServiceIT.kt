package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ActiveProfiles(profiles = ["localhost", "eventsDisabled"])
@Transactional
class InndelingerServiceIT : TestWithDbContainer() {

    @Test
    fun `finnInndelinger gir korrekt soekeresultater`() {
        val gyldighetsdato = LocalDate.now()

        val gk1 = saveGrunnkrets(id = "1", lokalid = Lokalid("1"), grunnkretsnavn = "Oslo (løkka)")
        val gk2 = saveGrunnkrets(id = "2", lokalid = Lokalid("2"), grunnkretsnavn = "Singsaker")
        val sk1 = saveStemmekrets(id = "3", lokalid = Lokalid("3"), stemmekretsnavn = "Oslo rådhus")
        val sk2 = saveStemmekrets(id = "4", lokalid = Lokalid("4"), stemmekretsnavn = "Nidelva")
        val bp1 = saveBopliktomraade(id = "40", lokalid = Lokalid("40"), bopliktomraadenavn = "Ostekake")
        val k1 = saveKommune(
            unikLokalid = UnikLokalid("5", Flatetype.KOMMUNE),
            id = "5",
            kommunenummer = "3232",
            administrativenhetnavn = listOf(saveAdministrativenhetnavn(lokalid = "1", navn = "Trondheim", spraakid = "nor", rekkefoelge = 1)),
            grunnkretser = listOf(gk2),
            stemmekretser = listOf(sk2),
            bopliktomraader = listOf(bp1)
        )
        val k2 = saveKommune(
            unikLokalid = UnikLokalid("6", Flatetype.KOMMUNE),
            id = "6",
            administrativenhetnavn = listOf(saveAdministrativenhetnavn(lokalid = "2", navn = "Oslo", spraakid = "nor", rekkefoelge = 1)),
            grunnkretser = listOf(gk1),
            stemmekretser = listOf(sk1)
        )
        saveFylke(
            unikLokalid = UnikLokalid("7", Flatetype.FYLKE),
            id = "7",
            administrativenhetnavn = listOf(saveAdministrativenhetnavn(lokalid = "4", navn = "Oslo", spraakid = "nor", rekkefoelge = 1)),
            kommuner = listOf(k2)
        )
        saveFylke(
            unikLokalid = UnikLokalid("8", Flatetype.FYLKE),
            id = "8",
            fylkesnummer = "60",
            administrativenhetnavn = listOf(saveAdministrativenhetnavn(lokalid = "5", navn = "Os", spraakid = "nor", rekkefoelge = 1)),
            kommuner = listOf(k1)
        )

        grenseRepository.flush()

        val searchString = "os"
        val limit = 15

        val inndelinger = inndelingerService.finnInndelinger(searchString, gyldighetsdato, limit,
            listOf(Flatetype.GRUNNKRETS, Flatetype.KOMMUNE, Flatetype.STEMMEKRETS, Flatetype.FYLKE, Flatetype.BOPLIKTOMRAADE))

        assertThat(inndelinger.size).isEqualTo(6)
        inndelinger.forEach { inndelingResponse ->
            assertThat(inndelingResponse.navn.lowercase()).startsWith(searchString)
        }

        // Hvis dette feiler har det blitt lagt til noen nye kretser i testdatabasen i ettertid som matcher i søket
        assertThat(inndelinger.filter { it.type === Flatetype.GRUNNKRETS }).hasSize(1).extracting("navn", "nummer")
            .contains(Assertions.tuple("Oslo (løkka)", "42270304"))
        assertThat(inndelinger.filter { it.type === Flatetype.STEMMEKRETS }).hasSize(1).extracting("navn", "nummer")
            .contains(Assertions.tuple("Oslo rådhus", "03019"))
        assertThat(inndelinger.filter { it.type === Flatetype.KOMMUNE }).hasSize(1).extracting("navn", "nummer")
            .contains(Assertions.tuple("Oslo", "0301"))
        assertThat(inndelinger.filter { it.type === Flatetype.FYLKE }).hasSize(2)
            .extracting("navn", "nummer").contains(Assertions.tuple("Os", "60"), Assertions.tuple("Oslo", "89"))
        assertThat(inndelinger.filter { it.type === Flatetype.BOPLIKTOMRAADE }).hasSize(1).extracting("navn", "nummer")
            .contains(Assertions.tuple("Ostekake", "323201"))

        val inndelingerFiltrert = inndelingerService.finnInndelinger(searchString, gyldighetsdato, limit, listOf(Flatetype.STEMMEKRETS, Flatetype.FYLKE))

        // Hvis dette feiler har det blitt lagt til noen nye kretser i testdatabasen i ettertid som matcher i søket
        assertThat(inndelingerFiltrert.filter { it.type === Flatetype.GRUNNKRETS }).isEmpty()
        assertThat(inndelingerFiltrert.filter { it.type === Flatetype.STEMMEKRETS }).hasSize(1)
        assertThat(inndelingerFiltrert.filter { it.type === Flatetype.KOMMUNE }).isEmpty()
        assertThat(inndelingerFiltrert.filter { it.type === Flatetype.FYLKE }).hasSize(2)
    }

    @Test
    fun `finnInndelinger skal ikke returnere duplikate stemmekretser`() {
        val gyldighetsdato = LocalDate.now()

        val sk1 = saveStemmekrets(
            id = "1",
            lokalid = Lokalid("1"),
            stemmekretsnavn = "Egersund",
            stemmekretsnummer = "0001"
        )
        val sk2 = saveStemmekrets(
            id = "2",
            lokalid = Lokalid("2"),
            stemmekretsnavn = "Egersund",
            stemmekretsnummer = "0001"
        )

        saveKommune(
            unikLokalid = UnikLokalid("3", Flatetype.KOMMUNE),
            id = "3",
            administrativenhetnavn = listOf(saveAdministrativenhetnavn(lokalid = "3", navn = "Egersund", spraakid = "nor", rekkefoelge = 1)),
            stemmekretser = listOf(sk1, sk2)
        )

        grenseRepository.flush()

        val searchString = "Egersund"
        val limit = 10

        val inndelinger = inndelingerService.finnInndelinger(
            searchString,
            gyldighetsdato,
            limit,
            listOf(Flatetype.STEMMEKRETS)
        )

        assertThat(inndelinger).hasSize(1)
        assertThat(inndelinger[0].type).isEqualTo(Flatetype.STEMMEKRETS)
        assertThat(inndelinger[0].navn).isEqualTo("Egersund")
    }
}
