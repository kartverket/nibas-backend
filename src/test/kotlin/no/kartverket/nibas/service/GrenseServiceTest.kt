package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockkStatic
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.repository.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.Month

@SpringBootTest
class GrenseServiceTest {

    @Autowired
    lateinit var grenseService: GrenseService

    @MockkBean
    private lateinit var grenseRepositoryKotlinMock: GrenseRepository

    @MockkBean
    private lateinit var grunnkretsRepositoryKotlinMock: GrunnkretsRepository

    @MockkBean
    private lateinit var stemmekretsRepositoryKotlinMock: StemmekretsRepository

    @BeforeEach
    fun beforeEach() {
        // Bruker her SpringMockK for å kunne mocke ut kall til extension functions
        mockkStatic(REPOSITORY_EXTENSIONS_FILEPATH)
    }

    private fun assertRekkefoelge(grunnkretsGrenseKobling: GrunnkretsGrenseKobling, rekkefoelge: Int) {
        assertThat(grunnkretsGrenseKobling.grenseKoblingAttributter.rekkefoelge).isEqualTo(rekkefoelge)
    }

    private fun assertRetningMotKlokka(grunnkretsGrenseKobling: GrunnkretsGrenseKobling) {
        assertThat(grunnkretsGrenseKobling.grenseKoblingAttributter.retningMedKlokken).isEqualTo(false)
    }

    @Test
    fun thatIncomingFeatureWithoutIdThrowsException() {
        // Innkommende feature uten id
        val nyDelomraadegrensefeatureForGrunnkrets = FeatureBuilder(null)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId("1")
            .withGrensetype(Grensetype.Delomraadegrense)
            .build()

        assertThatThrownBy {
            grenseService.lagreGrenser(
                FeatureCollection(features = listOf(nyDelomraadegrensefeatureForGrunnkrets)),
                endringstype = Endringstype.RETTING
            )
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Missing feature id")
    }

    @Test
    fun thatIncomingFeatureWithUnknownIdThrowsException() {
        val gyldighetsdato = LocalDate.of(2022, Month.DECEMBER, 8)

        // Har en grunnkretsgrense med lokalid 34
        val grense = GrenseBuilder(id = "3400",
            lokalId = Lokalid("34"))
            .buildGrunnkretsgrense()
        val grenseIdentifikator = Identifikator(Lokalid("34"), gyldighetsdato)

        every { grenseRepositoryKotlinMock.findAllGyldigeObjekterForIdentifikatorer(allAny()) } returns mapOf(grenseIdentifikator to grense)

        // Innkommende feature på ukjent grenseId
        val featureWithUnknownId = FeatureBuilder("33")
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .build()

        assertThatThrownBy { grenseService.lagreGrenser(
            FeatureCollection(features = listOf(featureWithUnknownId)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.FASTSETTING
        ) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("Non-existing grenseidentifikatorer: [Identifikator(lokalid=Lokalid(value=33), gyldighetsdato=2022-12-08)])")
    }
    @Test
    fun `lagreGrenser skal kaste optimistiskLaasException hvis grensa har ulik versjon enn requesten`() {
        val gyldighetsdato = LocalDate.of(2022, Month.DECEMBER, 8)

        // Har en grunnkretsgrense med lokalid 34
        val grense = GrenseBuilder(id = "3400",
            lokalId = Lokalid("34"),
            gyldigFra = LocalDate.of(2020, Month.DECEMBER, 8))
            .withVersjon(4)
            .buildGrunnkretsgrense()
        val grenseIdentifikator = Identifikator(Lokalid("34"), gyldighetsdato)

        every { grenseRepositoryKotlinMock.findAllGyldigeObjekterForIdentifikatorer(allAny()) } returns mapOf(grenseIdentifikator to grense)

        val featureWithUnknownId = FeatureBuilder("34")
            .withGrenseversjon(1)
            .withKontekstType(KontekstEnhetType.GRUNNKRETS)
            .withKontekstId("1")
            .withGrensetype(Grensetype.Grunnkretsgrense)
            .build()

        assertThatThrownBy { grenseService.lagreGrenser(
            FeatureCollection(features = listOf(featureWithUnknownId)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.FASTSETTING
        ) }
            .isInstanceOf(OptimistiskLaasException::class.java)
            .hasMessageContaining("Det er ikke samsvar med versjon")
    }

    @Test
    fun hentGrenseReturnsNullInCaseOfNoMatch() {
        val grenseIdentifikator = Identifikator(Lokalid("1"))
        every { grenseRepositoryKotlinMock.findGyldigObjektForIdentifikator(grenseIdentifikator) } returns null

        assertThat(grenseService.hentGrense(grenseIdentifikator)).isNull()
    }

}
