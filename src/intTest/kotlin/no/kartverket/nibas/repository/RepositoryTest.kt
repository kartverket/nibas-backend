package no.kartverket.nibas.repository

import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.sporing.UserFacade
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.transaction.annotation.Transactional
import org.mockito.Mockito.`when` as whenever


class RepositoryTest : TestWithDbContainer() {

    @MockBean
    private lateinit var userFacade: UserFacade

    @BeforeEach
    fun beforeEach() {
        whenever(userFacade.getAuthenticatedUser()).thenReturn("Testbruker 123")
    }

    @Test
    fun saveUtkast() {
        val utkast = UtkastBuilder.builder("Utkast 1")
            .withKommuneRequests(kommuneRequests())
            .build()
        utkastRepository.save(utkast)
        assertThat(utkastRepository.findById(utkast.id).isPresent).isTrue
    }

    @Test
    fun saveUtkastWithOldVersionTriggersOptimisticLockException() {
        val utkast = UtkastBuilder.builder("Et foreløpig utkast")
            .withKommuneRequests(kommuneRequests())
            .build()

        val savedUtkast = utkastRepository.save(utkast)
        assertThat(savedUtkast.version).isEqualTo(2)
        savedUtkast.navn = "Navn 1"

        val fetchedUtkast = utkastRepository.findById(utkast.id).get()
        fetchedUtkast.navn = "Navn 2"

        //Versjon skal øke til 3
        val resavedUtkast = utkastRepository.save(savedUtkast)
        assertThat(resavedUtkast.version).isEqualTo(3)

        // Forsøker å lagre noe med versjon 1, skal feile
        assertThatThrownBy { utkastRepository.save(fetchedUtkast) }.isInstanceOf(ObjectOptimisticLockingFailureException::class.java)
    }

    // Kan ikke gjøre equals-sjekk på set av ids fordi tidligere tester lagrer også ned Utkast som blir plukket opp i denne testen.
    // Altså ikke atomiske tester.
    @Test
    fun findAllUtkastByStatus() {
        val utkast = UtkastBuilder.builder("Utkast 1").withStatus(UtkastStatus.OPPRETTET).build()
        val utkast2 = UtkastBuilder.builder("Utkast 2").withStatus(UtkastStatus.OPPRETTET).build()
        val utkast3 = UtkastBuilder.builder("Utkast 3").withStatus(UtkastStatus.FORKASTET).build()
        utkastRepository.saveAll(listOf(utkast, utkast2, utkast3))

        val matchingUtkastIdsForStatusOpprettet = utkastRepository.findAllByStatus(UtkastStatus.OPPRETTET).map { it.id }.toSet()
        assertThat(matchingUtkastIdsForStatusOpprettet).contains(utkast.id, utkast2.id)
        assertThat(matchingUtkastIdsForStatusOpprettet).doesNotContain(utkast3.id)
    }

    @Test
    fun findAllGrunnkretserByLokalid() {
        val grunnkrets1 = createGrunnkrets()
        val grunnkrets2MedSammeLokalid = createGrunnkrets(lokalid = grunnkrets1.lokalid)
        grunnkretsRepository.saveAll(listOf(grunnkrets1, grunnkrets2MedSammeLokalid))

        val grunnkretsIdsFunnet = grunnkretsRepository.findAllByLokalid(grunnkrets1.lokalid).map { it.id }.toSet()
        assertThat(grunnkretsIdsFunnet).isEqualTo(setOf(grunnkrets1.id, grunnkrets2MedSammeLokalid.id))
    }

    @Transactional
    @Test
    fun findGrense() {
        grenseRepository.save(GrenseBuilder(id = "300").buildGrunnkretsgrense())
        assertThat(grenseRepository.findById("300")).isNotNull
    }

    private fun createGrunnkrets(lokalid: String? = null): Grunnkrets {
        val grunnkrets = if (lokalid != null) Grunnkrets(lokalid = lokalid) else Grunnkrets()
        return grunnkrets.apply {
            grunnkretsnummer = "1234"
            grunnkretsnavn = "Basvatn"
        }
    }

    private fun kommuneRequests(): Map<String, KommuneRequest> {
        val navn = "Oslo"
        val spraak = "NOR"
        val lokalId = "123"
        val kommunenummer = "123"
        val samiskforvaltningsomraade = false
        return mapOf("UUID-1" to KommuneRequest(
            administrativenhetnavn = listOf(AdministrativEnhetNavn(navn, spraak, version = 1)),
            lokalid = lokalId,
            kommunenummerId = kommunenummer,
            samiskforvaltningsomraade = samiskforvaltningsomraade,
            version = 1
        ))
    }

    @Test
    fun saveSpraakkode() {
        val spraakkode = Spraakkode()
        spraakkode.id = "17"
        spraakkode.description = "norsk"
        spraakkodeRepository.save(spraakkode)
        assertThat(spraakkodeRepository.findById("17").isPresent).isTrue
    }
}

