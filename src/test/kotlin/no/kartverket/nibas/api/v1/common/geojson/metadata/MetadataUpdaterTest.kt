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
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.service.deltgeometri.copyWithNewUUID
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
        `when`(kodelisteHrefProviderMock.maalemetodeKodeHref()).thenReturn(maalemetodeHref)
    }

    @Test
    fun `Skal fjerne dokumentasjonsreferanse fra Grense når flagget isArchived`() {
        val grense = fylkesgrense.copyWithNewUUID()
        val originalDokrefDtos = grense.dokumentasjonsreferanser.map { it.toDokRef() }
        assertThat(originalDokrefDtos).isNotEmpty()

        val dokrefDTOflaggedArchived = originalDokrefDtos[0].copy(shouldArchive = true)
        val dokrefPreArchived = grense.dokumentasjonsreferanser.firstOrNull { it.id == dokrefDTOflaggedArchived.id }
        assertThat(dokrefPreArchived).isNotNull
        val dokrefDtosWithArchivedEntry = mutableListOf(dokrefDTOflaggedArchived)
        for (i in 1..<originalDokrefDtos.size) {
            dokrefDtosWithArchivedEntry.add(originalDokrefDtos[i])
        }

        grense.updateDokumentasjonsreferanser(dokrefDtosWithArchivedEntry, LocalDate.now().plusDays(1), Endringstype.KVALITETSHEVING)
        assertThat(dokrefPreArchived?.gyldighet?.gyldigTil).isNotNull()
        assertThat(originalDokrefDtos.size).isEqualTo(grense.dokumentasjonsreferanser.size + 1)
    }

    @Test
    fun `Skal lage ny versjon av dokumentasjonsreferanse ved oppdatering`() {
        val grense = fylkesgrense.copyWithNewUUID()
        val originalDokrefDtos = grense.dokumentasjonsreferanser.map { it.toDokRef() }
        assertThat(originalDokrefDtos).isNotEmpty()

        val changedDokrefDTO = originalDokrefDtos[0].copy(rettskildeTittel = "En helt annen tittel", vedtakGyldigFra = LocalDate.now())
        val dokrefToChange = grense.dokumentasjonsreferanser.firstOrNull { it.id == changedDokrefDTO.id }
        assertThat(dokrefToChange).isNotNull

        val dtosWithModifiedEntry = mutableListOf(changedDokrefDTO)
        for (i in 1..<originalDokrefDtos.size) {
            dtosWithModifiedEntry.add(originalDokrefDtos[i])
        }

        val gyldighetsdato = LocalDate.now().plusDays(1)
        grense.updateDokumentasjonsreferanser(dtosWithModifiedEntry, gyldighetsdato, Endringstype.KVALITETSHEVING)
        assertThat(dokrefToChange?.gyldighet?.gyldigTil).isEqualTo(gyldighetsdato)

        val changedDokref = grense.dokumentasjonsreferanser.first { it.lokalid == dokrefToChange?.lokalid }
        assertThat(changedDokref).isNotEqualTo(dokrefToChange)
        assertThat(changedDokref.gyldighet.gyldigFra).isEqualTo(gyldighetsdato)
        assertThat(changedDokref.auditInfo.typeEndring).isEqualTo(Endringstype.KVALITETSHEVING)
    }

    @Test
    fun `Skal oppdatere AdministrativGrense`() {
        val opprinneligInformasjonstekst = fylkesgrense.informasjonstekst
        val opprinneligKvalitet = fylkesgrense.kvalitet
        val opprinneligeDokumentasjonsreferanser = fylkesgrense.dokumentasjonsreferanser

        val newGyldigFraDato: LocalDate = gyldigFraTidspunkt.plusMonths(1)
        val updatedMetadata = administrativGrenseMetadata.copy(
            common = administrativGrenseMetadata.common.copy(
                gyldigFra = newGyldigFraDato,
            )
        )
        val updatedFylkesgrense: Grense = metadataUpdater.updateAdministrativGrense(fylkesgrense, updatedMetadata, LocalDate.now(), Endringstype.KVALITETSHEVING)
        assertThat(updatedFylkesgrense.informasjonstekst).isNotEqualTo(opprinneligInformasjonstekst).isEqualTo(updatedMetadata.common.informasjon)
        assertThat(updatedFylkesgrense.kvalitet!!.noeyaktighet).isNotEqualTo(opprinneligKvalitet!!.noeyaktighet)
            .isEqualTo(updatedMetadata.commonGrense.posisjonskvalitet!!.noeyaktighet)
        assertThat(updatedFylkesgrense.dokumentasjonsreferanser).isEqualTo(opprinneligeDokumentasjonsreferanser)
    }

    @Test
    fun `Skal oppdatere TerritorialGrense`() {
        val opprinneligInformasjonstekst = fylkesgrense.informasjonstekst
        val opprinneligKvalitet = fylkesgrense.kvalitet
        val opprinneligeDokumentasjonsreferanser = fylkesgrense.dokumentasjonsreferanser

        val newGyldigFraDato: LocalDate = gyldigFraTidspunkt.plusMonths(1)
        val updatedMetadata = territorialgrenseMetadata.copy(
            common = territorialgrenseMetadata.common.copy(
                gyldigFra = newGyldigFraDato,
            )
        )
        val updatedFylkesgrense: Grense = metadataUpdater.updateTerritoritalgrense(fylkesgrense, updatedMetadata, LocalDate.now(), Endringstype.KVALITETSHEVING)
        assertThat(updatedFylkesgrense.informasjonstekst).isNotEqualTo(opprinneligInformasjonstekst).isEqualTo(updatedMetadata.common.informasjon)
        assertThat(updatedFylkesgrense.kvalitet!!.noeyaktighet).isNotEqualTo(opprinneligKvalitet!!.noeyaktighet)
            .isEqualTo(updatedMetadata.commonGrense.posisjonskvalitet!!.noeyaktighet)
        assertThat(updatedFylkesgrense.dokumentasjonsreferanser).isEqualTo(opprinneligeDokumentasjonsreferanser)
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
