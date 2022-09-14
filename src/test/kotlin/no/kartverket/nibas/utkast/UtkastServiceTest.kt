package no.kartverket.nibas.utkast

import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.domain.Utkast
import no.kartverket.nibas.domain.UtkastBuilder
import no.kartverket.nibas.repository.UtkastRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when` as whenever

internal class UtkastServiceTest {

    @Mock
    private lateinit var utkastRepositoryMock: UtkastRepository
    private lateinit var utkastService: UtkastService

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        utkastService = UtkastService(utkastRepositoryMock)
    }

    @Test
    fun opprettUtkast() {
        val navn = "Grenseendring i Oslo"
        val utkast = UtkastBuilder
            .builder(navn)
            .build()
        whenever(utkastRepositoryMock.save(any(Utkast::class.java))).thenReturn(utkast)

        val idForOpprettetUtkast = utkastService.opprettUtkast(
            UtkastRequestBuilder()
                .withNavn(navn)
                .build())
        assertThat(utkast.id).isEqualTo(idForOpprettetUtkast)
    }
}
