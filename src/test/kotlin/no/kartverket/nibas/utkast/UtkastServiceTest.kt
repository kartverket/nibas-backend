package no.kartverket.nibas.utkast

import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.request.GrunnkretsRequest
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.api.v1.request.StemmekretsRequest
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.FylkeRequestBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.KommuneRequestBuilder
import no.kartverket.nibas.builders.NasjonRequestBuilder
import no.kartverket.nibas.builders.OpprettUtkastRequestBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.domain.utkast.Utkast
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.domain.utkast.UtkastStatus
import no.kartverket.nibas.inndelinger.FylkeService
import no.kartverket.nibas.inndelinger.GrenseService
import no.kartverket.nibas.inndelinger.GrunnkretsService
import no.kartverket.nibas.inndelinger.KommuneService
import no.kartverket.nibas.inndelinger.NasjonService
import no.kartverket.nibas.inndelinger.StemmekretsService
import no.kartverket.nibas.repository.UtkastRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.time.Month
import java.util.*
import org.mockito.Mockito.`when` as whenever

internal class UtkastServiceTest {

    @Mock
    private lateinit var utkastRepositoryMock: UtkastRepository

    @Mock
    private lateinit var nasjonServiceMock: NasjonService

    @Mock
    private lateinit var fylkeServiceMock: FylkeService

    @Mock
    private lateinit var kommuneServiceMock: KommuneService

    @Mock
    private lateinit var grunnkretsServiceMock: GrunnkretsService

    @Mock
    private lateinit var stemmekretsServiceMock: StemmekretsService

    @Mock
    private lateinit var grenseServiceMock: GrenseService
    private lateinit var utkastService: UtkastService

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        utkastService = UtkastService(utkastRepositoryMock, nasjonServiceMock, fylkeServiceMock, kommuneServiceMock, grunnkretsServiceMock,
            stemmekretsServiceMock, grenseServiceMock)
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

    @Test
    fun publiserUtkastMedAnnenStatusEnnOpprettetGirBadRequestEx() {
        val publisertUtkast = UtkastBuilder.builder("Utkast for publisering")
            .withStatus(UtkastStatus.PUBLISERT)
            .build()
        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(publisertUtkast))

        assertThatThrownBy {
            utkastService.publiserUtkast(publisertUtkast.id)
        }.isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun publiserUtkastUtenOperasjonerGirBadRequestEx() {
        val utkastUtenOperasjoner = UtkastBuilder.builder("Utkast for publisering")
            .build()
        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(utkastUtenOperasjoner))

        assertThatThrownBy {
            utkastService.publiserUtkast(utkastUtenOperasjoner.id)
        }.isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun publiserUtkast() {
        val utkast = UtkastBuilder.builder("Utkast for publisering")
            .withGyldigFra(LocalDate.of(2024, Month.JANUARY, 1))
            .withOperasjoner(Operasjoner(
                metadataendringer = Metadataendringer(
                    nasjonsendringer = mapOf("Nasjon-UUID-1" to NasjonRequestBuilder.DEFAULT_NASJON),
                    fylkesendringer = mapOf("Fylke-UUID-1" to FylkeRequestBuilder.DEFAULT_REQUEST),
                    kommuneendringer = mapOf("Kommune-UUID-1" to KommuneRequestBuilder.DEFAULT_REQUEST),
                    grunnkretsendringer = mapOf("Grunnkrets-UUID-1" to GrunnkretsRequestBuilder.DEFAULT_REQUEST),
                    stemmekretsendringer = mapOf("Stemmekrets-UUID-1" to StemmekretsRequestBuilder.DEFAULT_REQUEST)
                ),
                grenseendringer = Grenseendringer(
                    endredeFeatures = mapOf("56" to FeatureBuilder("56").build())
                )
            ))
            .build()
        whenever(utkastRepositoryMock.findById(anyString())).thenReturn(Optional.of(utkast))

        utkastService.publiserUtkast(utkast.id)

        assertThat(utkast.status).isEqualTo(UtkastStatus.PUBLISERT)

        verify(nasjonServiceMock).oppdaterNasjon(anyString(), any(NasjonRequest::class.java))
        verify(fylkeServiceMock).oppdaterFylke(anyString(), any(FylkeRequest::class.java))
        verify(kommuneServiceMock).oppdaterKommune(anyString(), any(KommuneRequest::class.java))
        verify(grunnkretsServiceMock).oppdaterGrunnkrets(anyString(), any(GrunnkretsRequest::class.java))
        verify(stemmekretsServiceMock).oppdaterStemmekrets(anyString(), any(StemmekretsRequest::class.java))
        verify(grenseServiceMock).lagreGrenser(any(FeatureCollection::class.java))
    }

    private fun <T> any(clazz: Class<T>): T = Mockito.any(clazz)

}
