package no.kartverket.nibas.api.v1.common.geojson.metadata

import no.kartverket.nibas.builders.DokumentasjonsreferanseBuilder
import no.kartverket.nibas.builders.FylkesgrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.INFORMASJONTEKST
import no.kartverket.nibas.builders.LINK_TEKST_1
import no.kartverket.nibas.builders.LINK_TEKST_2
import no.kartverket.nibas.builders.LOKALID
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.builders.OPPHAV
import no.kartverket.nibas.builders.REF_TEKST_1
import no.kartverket.nibas.builders.REF_TEKST_2
import no.kartverket.nibas.builders.TerritorialgrenseBuilder
import no.kartverket.nibas.domain.AnsvarligMyndighetHolder
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.LandKodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Enhetstest av MetadataMapper.
 */
internal class MetadataMapperTest {

    @Mock
    private lateinit var kodelisteServiceMock: KodelisteService

    @Mock
    private lateinit var landKodeRepositoryMock: LandKodeRepository

    @Mock
    private lateinit var kodelisteHrefProviderMock: KodelisteHrefProviderI

    private lateinit var metadataMapper: MetadataMapper

    private val oppdateringsdato: ZonedDateTime = ZonedDateTime.now()
    private val dataFanget = oppdateringsdato.minusYears(1)
    private val gyldigFraTidspunkt = oppdateringsdato.minusMonths(6).toLocalDate()
    private val gyldigTilTidspunkt = oppdateringsdato.plusMonths(12).toLocalDate()
    private val fastsettingsdato: LocalDate = oppdateringsdato.minusMonths(5).toLocalDate()
    private val beregningsdato: LocalDate = oppdateringsdato.minusWeeks(17).toLocalDate()
    private val transformasjonsdato: LocalDate = oppdateringsdato.minusDays(19).toLocalDate()

    private val link1 = dokLink(LINK_TEKST_1)
    private val link2 = dokLink(LINK_TEKST_2)
    private val referanse1 = internReferanse(REF_TEKST_1)
    private val referanse2 = internReferanse(REF_TEKST_2)

    private val dokumentasjonsreferanse = DokumentasjonsreferanseBuilder()
        .fastSettingAt(fastsettingsdato)
        .addLenker(link1, link2)
        .addInternRefs(referanse1, referanse2)
        .build()

    private val maalemetodeHref = "maalemetodeHref"
    private val terrengDetaljHref = "terrengHref"
    private val noeyaktighetHref = "href2"
    private val landkodeHref = "landkodeHref"
    private val fastsettingstypeHref = "fastHref"
    private val grensestatusHref = "grensestatusHref"
    private val kv = "Kartverket"
    private val hav = "Havarikommisjonen"
    private val ansvarligMyndighet1 = AnsvarligMyndighetHolder(ansvarligmyndighet = kv)
    private val ansvarligMyndighet2 = AnsvarligMyndighetHolder(ansvarligmyndighet = hav)

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        metadataMapper = MetadataMapper(kodelisteServiceMock, kodelisteHrefProviderMock)

        `when`(kodelisteHrefProviderMock.terrengDetaljHref()).thenReturn(terrengDetaljHref)
        `when`(kodelisteHrefProviderMock.noeyaktighetsklasseHref()).thenReturn(noeyaktighetHref)
        `when`(kodelisteHrefProviderMock.maalemetodeKodeHref()).thenReturn(maalemetodeHref)
        `when`(kodelisteHrefProviderMock.landkodeHref()).thenReturn(landkodeHref)
        `when`(kodelisteHrefProviderMock.fastsettingstypeKodeHref()).thenReturn(fastsettingstypeHref)
        `when`(kodelisteHrefProviderMock.grensestatusKodeHref()).thenReturn(grensestatusHref)
    }

    // Fra Fylkesgrense -> AdministrativGrenseMetadata
    @Test
    fun toAdministrativGrenseMetadata() {
        val fylkesgrense = FylkesgrenseBuilder()
            .withEndretDato(oppdateringsdato)
            .datafangstdato(dataFanget)
            .gyldigFra(gyldigFraTidspunkt)
            .dokRef(dokumentasjonsreferanse)
            .build()

        val administrativGrenseMetadata = AdministrativGrenseMetadata(
            common = expectedCommonMetadata(),
            commonGrense = expectedCommonGrensedata(),
            dokumentasjonsreferanser = listOf(dokumentasjonsreferanse).map { it.toDokRef() }
        )

        val metadata: AdministrativGrenseMetadata = metadataMapper.toMetadataForAdministrativGrense(fylkesgrense)
        assertThat(metadata).isEqualTo(administrativGrenseMetadata)
    }

    // Får testet mapping for maritim grense
    // Territorialgrense -> TerritorialgrenseMetadata
    @Test
    fun toTerritorialgrenseMetadata() {
        mockRussland(landKodeRepositoryMock)

        val territorialgrense = TerritorialgrenseBuilder()
            .withEndretDato(oppdateringsdato)
            .datafangstdato(dataFanget)
            .gyldigFra(gyldigFraTidspunkt)
            .dokRef(dokumentasjonsreferanse)
            .transformasjonsdato(transformasjonsdato)
            .withAnsvarligeMyndigheter(ansvarligMyndighet1, ansvarligMyndighet2)
            .beregningsdato(beregningsdato)
            .build()

        val territorialgrenseMetadata = TerritorialgrenseMetadata(
            common = expectedCommonMetadata(),
            commonGrense = expectedCommonGrensedata(),
            dokumentasjonsreferanser = listOf(dokumentasjonsreferanse).map { it.toDokRef() },
        )

        val metadata: TerritorialgrenseMetadata = metadataMapper.toMetadataForTerritorialgrense(territorialgrense)
        assertThat(metadata).isEqualTo(territorialgrenseMetadata)
    }

    @Test
    fun toFlateMetadata() {
        val grunnkrets = GrunnkretsBuilder()
            .withDatafangstdato(dataFanget)
            .withGyldigFra(gyldigFraTidspunkt)
            .withGyldigTil(gyldigTilTidspunkt)
            .withEndretDato(oppdateringsdato)
            .build()

        val expectedFlateMetadata = FlateMetadata(common = expectedCommonMetadata().copy(gyldigTil = gyldigTilTidspunkt))

        val metadata = metadataMapper.toMetadata(grunnkrets)
        assertThat(metadata).isEqualTo(expectedFlateMetadata)
    }

    private fun expectedCommonMetadata() = CommonMetadata(
        identifikasjon = Identifikasjon(LOKALID),
        datafangstdato = dataFanget,
        gyldigFra = gyldigFraTidspunkt,
        gyldigTil = null,
        sporingsinformasjon = Sporingsinformasjon(oppdateringsdato = oppdateringsdato),
        opphav = OPPHAV,
        informasjon = INFORMASJONTEKST
    )

    private fun expectedCommonGrensedata() = CommonGrenseMetadata(
        posisjonskvalitet = Posisjonskvalitet(
            maalemetode = KodelisteEntry(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, href = maalemetodeHref),
            noeyaktighet = 200
        ),
    )

}
