package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ActiveProfiles(profiles = ["security-off", "eventsDisabled"])
@Transactional
class InndelingerServiceIT: TestWithDbContainer() {

    @Test
    fun `finnInndelinger gir korrekt soekeresultater`(){
        val gyldighetsdato = LocalDate.now()

        val gk1 = saveGrunnkrets(id = "1", lokalid = Lokalid("1"), grunnkretsnavn = "Oslo (løkka)")
        val gk2 =saveGrunnkrets(id = "2", lokalid = Lokalid("2"), grunnkretsnavn = "Singsaker")
        val sk1 =saveStemmekrets(id = "3", lokalid = Lokalid("3"), stemmekretsnavn = "Oslo rådhus")
        val sk2 = saveStemmekrets(id = "4", lokalid = Lokalid("4"), stemmekretsnavn = "Nidelva")
        val k1 = saveKommune(
            unikLokalid = UnikLokalid("5", Flatetype.KOMMUNE),
            id = "5",
            administrativenhetnavn = listOf(saveAdministrativenhetnavn(lokalid = "1", navn = "Trondheim", spraakid = "nor", rekkefoelge = 1)),
            grunnkretser = listOf(gk2),
            stemmekretser = listOf(sk2)
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
            administrativenhetnavn = listOf(saveAdministrativenhetnavn(lokalid = "5", navn = "Os", spraakid = "nor", rekkefoelge = 1)),
            kommuner = listOf(k1)
        )

        grenseRepository.flush()

        val searchString = "os"
        val limit = 15

        val inndelinger = inndelingerService.finnInndelinger(searchString, gyldighetsdato, limit)

        assertThat(inndelinger.size).isEqualTo(5)
        inndelinger.forEach { inndelingResponse ->
            assertThat(inndelingResponse.navn.lowercase()).startsWith(searchString)
        }

        // Hvis dette feiler har det blitt lagt til noen nye kretser i testdatabasen i ettertid som matcher i søket
        assertThat(inndelinger.filter { it.type === Flatetype.GRUNNKRETS }.size).isEqualTo(1)
        assertThat(inndelinger.filter { it.type === Flatetype.STEMMEKRETS }.size).isEqualTo(1)
        assertThat(inndelinger.filter { it.type === Flatetype.KOMMUNE }.size).isEqualTo(1)
        assertThat(inndelinger.filter { it.type === Flatetype.FYLKE }.size).isEqualTo(2)
    }
}
