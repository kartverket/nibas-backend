package no.kartverket.nibas.utkast

import no.kartverket.nibas.builders.FylkeRequestBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.builders.OpprettUtkastRequestBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.repository.UtkastRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.time.Month
import java.util.*
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
        val utkast = UtkastBuilder.builder(navn)
            .withFylkeRequests(mapOf("UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST))
            .build()
        whenever(utkastRepositoryMock.save(any(Utkast::class.java))).thenReturn(utkast)

        val idForOpprettetUtkast = utkastService.opprettUtkast(
            OpprettUtkastRequestBuilder()
                .withNavn(navn)
                .build())
        assertThat(utkast.id).isEqualTo(idForOpprettetUtkast)
    }

    @Test
    fun oppdaterUtkast() {
        val utkastBuilder = UtkastBuilder.builder("Utkast 3")
            .withFylkeRequests(mapOf("Fylke-UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST))
            .withGyldigFra(LocalDate.of(2024, Month.JANUARY, 1))
            .withKommuneRequests(mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST))
        val utkast = utkastBuilder.build()

        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(utkast))

        val feb3 = LocalDate.of(2025, Month.FEBRUARY, 3)
        val utkastEtterSave = utkastBuilder.withGyldigFra(feb3)
            .build()

        whenever(utkastRepositoryMock.save(any(Utkast::class.java))).thenReturn(utkastEtterSave)

        val oppdatertUtkast = utkastService.oppdaterUtkast(utkast.id, UtkastRequestBuilder()
            .withGyldigFra(feb3)
            .build())
        assertThat(oppdatertUtkast).isNotNull
        assertThat(oppdatertUtkast!!.gyldigFra).isEqualTo(feb3)
    }

}
