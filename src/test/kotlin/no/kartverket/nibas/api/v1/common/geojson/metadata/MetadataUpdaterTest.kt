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
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.repository.DokrefGrenseKoblingRepository
import no.kartverket.nibas.repository.DokumentasjonsreferanseRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.service.DokumentasjonsreferanseService
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

internal class MetadataUpdaterTest {

    @Mock
    private lateinit var kodelisteHrefProviderMock: KodelisteHrefProviderI

    @Mock
    private lateinit var dokrefRepo: DokumentasjonsreferanseRepository

    @Mock
    private lateinit var dokrefGrenseKoblingRepo: DokrefGrenseKoblingRepository

    private lateinit var dokrefService: DokumentasjonsreferanseService

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

    private fun createFylkesgrense() = FylkesgrenseBuilder()
        .withEndretDato(oppdDato)
        .datafangstdato(dataFanget)
        .gyldigFra(gyldigFraTidspunkt)
        .dokRef(dokumentasjonsreferanse)
        .build()

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        `when`(kodelisteHrefProviderMock.maalemetodeKodeHref()).thenReturn(maalemetodeHref)
        dokrefService = DokumentasjonsreferanseService(dokrefRepo, dokrefGrenseKoblingRepo)
    }

    @Test
    fun `Skal sette GyldigTil når dokref er flagget isArchived`() {
        val gyldighetsdato = LocalDate.now().plusDays(1)
        val grense = FylkesgrenseBuilder()
            .withEndretDato(oppdDato)
            .datafangstdato(dataFanget)
            .gyldigFra(gyldigFraTidspunkt)
            .dokRef(dokumentasjonsreferanse)
            .build()
        val originalDokrefDtos = grense.dokrefGrensekobling.map { it.dokumentasjonsreferanse.toDokrefDTO() }
        originalDokrefDtos.forEach { it.vedtakGyldigFra = gyldigFraTidspunkt }
        assertThat(originalDokrefDtos).isNotEmpty()

        val dokrefDTOflaggedArchived = originalDokrefDtos[0].copy(shouldArchive = true)
        val dokrefPreArchived = grense.dokrefGrensekobling.firstOrNull { it.dokumentasjonsreferanse.id == dokrefDTOflaggedArchived.id }
        assertThat(dokrefPreArchived).isNotNull
        val dokrefDtosWithArchivedEntry = mutableListOf(dokrefDTOflaggedArchived)
        for (i in 1..<originalDokrefDtos.size) {
            dokrefDtosWithArchivedEntry.add(originalDokrefDtos[i])
        }

        dokrefService.updateDokumentasjonsreferanser(dokrefDtosWithArchivedEntry, grense, gyldighetsdato, Endringstype.KVALITETSHEVING)
        assertThat(dokrefPreArchived?.dokumentasjonsreferanse?.gyldighet?.gyldigTil).isNotNull()
    }

    @Test
    fun `Skal slette dokumentasjonsreferanse dersom den arkiveres samme dag`() {
        val grense = createFylkesgrense()
        val originalDokrefDtos = grense.dokrefGrensekobling.map { it.dokumentasjonsreferanse.toDokrefDTO() }
        val gyldighetsdato = LocalDate.now()
        assertThat(originalDokrefDtos).isNotEmpty()

        val dokrefDTOflaggedArchived = originalDokrefDtos[0].copy(shouldArchive = true)
        dokrefDTOflaggedArchived.vedtakGyldigFra = gyldighetsdato
        val dokrefPreArchived = grense.dokrefGrensekobling.firstOrNull { it.dokumentasjonsreferanse.id == dokrefDTOflaggedArchived.id }

        assertThat(dokrefPreArchived).isNotNull

        val dokrefDtosWithArchivedEntry = mutableListOf(dokrefDTOflaggedArchived)
        for (i in 1..<originalDokrefDtos.size) {
            dokrefDtosWithArchivedEntry.add(originalDokrefDtos[i])
        }

        dokrefService.updateDokumentasjonsreferanser(dokrefDtosWithArchivedEntry, grense, gyldighetsdato, Endringstype.KVALITETSHEVING)
        assertThat(dokrefPreArchived?.dokumentasjonsreferanse?.gyldighet?.slettet).isTrue()
    }

    @Test
    fun `Skal lage ny versjon av dokumentasjonsreferanse ved oppdatering`() {
        val grense = FylkesgrenseBuilder()
            .withEndretDato(oppdDato)
            .datafangstdato(dataFanget)
            .gyldigFra(gyldigFraTidspunkt)
            .dokRef(dokumentasjonsreferanse)
            .build()
        val originalDokrefDtos = grense.dokrefGrensekobling.map { it.dokumentasjonsreferanse.toDokrefDTO() }
        assertThat(originalDokrefDtos).isNotEmpty()

        val changedDokrefDTO = originalDokrefDtos[0].copy(rettskildeTittel = "En helt annen tittel", vedtakGyldigFra = LocalDate.now())
        val dokrefToChange = grense.dokrefGrensekobling.firstOrNull { it.dokumentasjonsreferanse.id == changedDokrefDTO.id }
        assertThat(dokrefToChange).isNotNull

        val dtosWithModifiedEntry = mutableListOf(changedDokrefDTO)
        for (i in 1..<originalDokrefDtos.size) {
            dtosWithModifiedEntry.add(originalDokrefDtos[i])
        }

        val gyldighetsdato = LocalDate.now().plusDays(1)
        val grensekoblinger = dokrefService.updateDokumentasjonsreferanser(dtosWithModifiedEntry, grense, gyldighetsdato, Endringstype.KVALITETSHEVING)
        assertThat(dokrefToChange?.dokumentasjonsreferanse?.gyldighet?.gyldigTil).isEqualTo(gyldighetsdato)

        assertThat(grensekoblinger.first().dokumentasjonsreferanse).isNotEqualTo(dokrefToChange?.dokumentasjonsreferanse)
        assertThat(grensekoblinger.first().dokumentasjonsreferanse.gyldighet.gyldigFra).isEqualTo(gyldighetsdato)
        assertThat(grensekoblinger.first().dokumentasjonsreferanse.auditInfo.typeEndring).isEqualTo(Endringstype.KVALITETSHEVING)
    }

    private fun expectedCommonMetadata() = CommonMetadata(
        identifikasjon = Identifikasjon(LOKALID),
        datafangstdato = dataFanget,
        gyldigFra = gyldigFraTidspunkt,
        gyldigTil = null,
        sporingsinformasjon = Sporingsinformasjon(oppdateringsdato = oppdDato),
        opphav = "opphav3",
        informasjon = INFORMASJONTEKST + " og det tredje"
    )

    private fun expectedCommonGrensedata() = CommonGrenseMetadata(
        posisjonskvalitet = Posisjonskvalitet(
            maalemetode = KodelisteEntry(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, href = maalemetodeHref),
            noeyaktighet = 205
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
