package no.kartverket.nibas.repository

import no.kartverket.nibas.domain.Spraakkode
import no.kartverket.nibas.domain.Utkast
import no.kartverket.nibas.sporing.UserFacade
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.mockito.Mockito.`when` as whenever


class RepositoryTest : TestWithDbContainer() {

    @MockBean
    private lateinit var userFacade: UserFacade

    @Autowired
    private lateinit var utkastRepository: UtkastRepository

    @Autowired
    private lateinit var spraakkodeRepository: SpraakkodeRepository

    @BeforeEach
    fun beforeEach() {
        whenever(userFacade.getAuthenticatedUser()).thenReturn("Testbruker 123")
    }

    @Test
    fun saveUtkast() {
        utkastRepository.save(Utkast(id = "1"))
        assertThat(utkastRepository.findById("1").isPresent).isTrue
    }

    @Test
    fun saveUtkastWithOldVersionTriggersOptimisticLockException() {
        val utkastId2 = Utkast(id = "2")
        val savedUtkast = utkastRepository.save(utkastId2)
        assertThat(savedUtkast.version).isEqualTo(1)
        savedUtkast.navn = "Navn 1"

        val fetchedUtkast = utkastRepository.findById("2").get()
        fetchedUtkast.navn = "Navn 2"

        //Versjon skal øke til 2
        val resavedUtkast = utkastRepository.save(savedUtkast)
        assertThat(resavedUtkast.version).isEqualTo(2)

        // Forsøker å lagre noe med versjon 1, skal feile
        assertThatThrownBy { utkastRepository.save(fetchedUtkast) }.isInstanceOf(ObjectOptimisticLockingFailureException::class.java)
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

