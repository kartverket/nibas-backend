package no.kartverket.nibas.api.v1.common.geojson.metadata

import no.kartverket.nibas.builders.DokumentasjonsreferanseBuilder
import no.kartverket.nibas.builders.FylkesgrenseBuilder
import no.kartverket.nibas.builders.INFORMASJONTEKST
import no.kartverket.nibas.builders.LINK_TEKST_1
import no.kartverket.nibas.builders.LINK_TEKST_2
import no.kartverket.nibas.builders.LOKALID
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.builders.REF_TEKST_1
import no.kartverket.nibas.builders.REF_TEKST_2
import no.kartverket.nibas.domain.DokumentasjonsreferanseDokumentlink
import no.kartverket.nibas.domain.DokumentasjonsreferanseInternReferanseKartverket
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.repository.LandKodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*


// Unit test av GrenseUpdater
internal class MetadataUpdaterTest {

    @Mock
    private lateinit var landKodeRepositoryMock: LandKodeRepository

    @Mock
    private lateinit var kodelisteHrefProviderMock: KodelisteHrefProviderI

    private lateinit var metadataUpdater: MetadataUpdater

    private val oppdDato: ZonedDateTime = ZonedDateTime.now()
    private val dataFanget = oppdDato.minusYears(1)
    private val gyldigFraTidspunkt = oppdDato.minusMonths(6).toLocalDate()
    private val fastsettingsdato = oppdDato.minusMonths(5).toLocalDate()

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

    private val territorialgrenseMetadata = TerritorialgrenseMetadata(
        common = expectedCommonMetadata(),
        commonGrense = expectedCommonGrensedata(),
    )

    private val fylkesgrense = FylkesgrenseBuilder()
        .withEndretDato(oppdDato)
        .datafangstdato(dataFanget)
        .gyldigFra(gyldigFraTidspunkt)
        .dokRef(dokumentasjonsreferanse)
        .build()

    private val administrativGrenseMetadata = AdministrativGrenseMetadata(
        common = expectedCommonMetadata(),
        commonGrense = expectedCommonGrensedata(),
    )

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        metadataUpdater = MetadataUpdater()

        `when`(kodelisteHrefProviderMock.terrengDetaljHref()).thenReturn(terrengDetaljHref)
        `when`(kodelisteHrefProviderMock.noeyaktighetsklasseHref()).thenReturn(noeyaktighetHref)
        `when`(kodelisteHrefProviderMock.maalemetodeKodeHref()).thenReturn(maalemetodeHref)
        `when`(kodelisteHrefProviderMock.landkodeHref()).thenReturn(landkodeHref)
        `when`(kodelisteHrefProviderMock.fastsettingstypeKodeHref()).thenReturn(fastsettingstypeHref)
    }

    @Test
    fun updateAdministrativGrense() {
        val newGyldigFraDato: LocalDate = gyldigFraTidspunkt.plusMonths(1)
        val updatedMetadata = administrativGrenseMetadata.copy(
            common = administrativGrenseMetadata.common.copy(
                gyldigFra = newGyldigFraDato,
            )
        )
        val updatedFylkesgrense: Grense = metadataUpdater.updateAdministrativGrense(fylkesgrense, updatedMetadata)
        assertThat(updatedFylkesgrense.gyldighet.gyldigFra).isEqualTo(newGyldigFraDato)
    }

    @Test
    fun updateTerritorialGrense() {
        val newGyldigFraDato: LocalDate = gyldigFraTidspunkt.plusMonths(1)
        val updatedMetadata = territorialgrenseMetadata.copy(
            common = territorialgrenseMetadata.common.copy(
                gyldigFra = newGyldigFraDato,
            )
        )
        val updatedFylkesgrense: Grense = metadataUpdater.updateTerritoritalgrense(fylkesgrense, updatedMetadata)
        assertThat(updatedFylkesgrense.gyldighet.gyldigFra).isEqualTo(newGyldigFraDato)
    }

    private fun expectedCommonMetadata() = CommonMetadata(
        identifikasjon = Identifikasjon(LOKALID),
        datafangstdato = dataFanget,
        gyldigFra = gyldigFraTidspunkt,
        gyldigTil = null,
        sporingsinformasjon = Sporingsinformasjon(oppdateringsdato = oppdDato),
        opphav = "opphav3",
        informasjon = INFORMASJONTEKST
    )

    private fun expectedCommonGrensedata() = CommonGrenseMetadata(
        posisjonskvalitet = Posisjonskvalitet(
            maalemetode = KodelisteEntry(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, href = maalemetodeHref),
            noeyaktighet = 200
        ),
    )
}

fun mockRussland(landKodeRepository: LandKodeRepository) {
    val landkodeRu = Landkode()
    landkodeRu.id = "ru"
    landkodeRu.description = "Russland"
    `when`(landKodeRepository.findById(anyString())).thenReturn(Optional.of(landkodeRu))
}

fun internReferanse(referanse: String): DokumentasjonsreferanseInternReferanseKartverket {
    return DokumentasjonsreferanseInternReferanseKartverket().apply { internReferanseKartverket = referanse }
}

fun dokLink(linkText: String): DokumentasjonsreferanseDokumentlink {
    return DokumentasjonsreferanseDokumentlink().apply { dokumentlink = linkText }
}
