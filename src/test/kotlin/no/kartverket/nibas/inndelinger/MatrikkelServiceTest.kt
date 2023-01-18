package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.kodelister.KodelisteService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.time.Month
import org.mockito.Mockito.`when` as whenever


internal class MatrikkelServiceTest {

    @Mock
    private lateinit var grunnkretsServiceMock: GrunnkretsService

    @Mock
    private lateinit var stemmekretsServiceMock: StemmekretsService

    @Mock
    private lateinit var kodelisteServiceMock: KodelisteService

    @Mock
    private lateinit var kommuneServiceMock: KommuneService

    private lateinit var matrikkelService: MatrikkelService

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        matrikkelService = MatrikkelService(grunnkretsServiceMock, stemmekretsServiceMock, kodelisteServiceMock, kommuneServiceMock)

        whenever(kodelisteServiceMock.getKommunenummerIdByCodeValue(anyString())).thenReturn("23b34165-bd8b-4eae-9356-4237ca68f287")
        whenever(kodelisteServiceMock.getCodeValueByKommunenummerId(anyString())).thenReturn("1787")
    }

    @Test
    fun hentAlleStemmekretserForKommune() {
        val stemmekrets1 = StemmekretsBuilder(id = "1")
            .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
            .build()
        val stemmekrets2 = StemmekretsBuilder(id = "2")
            .withGyldigFra(LocalDate.of(2019, Month.JANUARY, 1))
            .build()
        whenever(kommuneServiceMock.hentKommuneByKommunenummer(anyString())).thenReturn(
            KommuneBuilder()
                .withNavn("Oslo")
                .withStemmekrets(stemmekrets1)
                .withStemmekrets(stemmekrets2)
                .build())

        val stemmekretser = matrikkelService.hentAlleStemmekretserForKommune("0301")
        assertThat(stemmekretser).isEqualTo(listOf(stemmekrets1, stemmekrets2))
    }

    @Test
    fun hentAlleGrunnkretserForKommune() {
        val gk1 = GrunnkretsBuilder(
            id = "1",
            lokalId = Lokalid("87")
        )
            .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
            .build()
        val gk2 = GrunnkretsBuilder(
            id = "2",
            lokalId = Lokalid("88")
        )
            .withGyldigFra(LocalDate.of(2020, Month.JANUARY, 1))
            .build()
        whenever(kommuneServiceMock.hentKommuneByKommunenummer(anyString())).thenReturn(
            KommuneBuilder()
                .withNavn("Hole")
                .withGrunnkrets(gk1)
                .withGrunnkrets(gk2)
                .build())

        val grunnkretser = matrikkelService.hentAlleGrunnkretserForKommune("1787")
        assertThat(grunnkretser).isEqualTo(listOf(gk1, gk2))
    }

}
