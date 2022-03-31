package no.kartverket.nibas.api.v1.common.geojson

import no.kartverket.nibas.builders.DokumentasjonsreferanseBuilder
import no.kartverket.nibas.builders.FastsettingstypeEnum
import no.kartverket.nibas.builders.FylkesgrenseBuilder
import no.kartverket.nibas.builders.GrensestatusEnum
import no.kartverket.nibas.builders.HJEMMEL_I_LOVEN
import no.kartverket.nibas.builders.INFORMASJONTEKST
import no.kartverket.nibas.builders.LINK_TEKST_1
import no.kartverket.nibas.builders.LINK_TEKST_2
import no.kartverket.nibas.builders.LOKALID
import no.kartverket.nibas.builders.MYNDIGHET
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.builders.NAVNEROM
import no.kartverket.nibas.builders.NoeyaktighetsklasseEnum
import no.kartverket.nibas.builders.REF_TEKST_1
import no.kartverket.nibas.builders.REF_TEKST_2
import no.kartverket.nibas.builders.RETTSKILDE_ID_1
import no.kartverket.nibas.builders.TITTEL
import no.kartverket.nibas.builders.TerrengdetaljEnum
import no.kartverket.nibas.builders.TerritorialgrenseBuilder
import no.kartverket.nibas.builders.VERSJON_33
import no.kartverket.nibas.domain.AnsvarligMyndighetHolder
import no.kartverket.nibas.domain.DokumentasjonsreferanseDokumentlink
import no.kartverket.nibas.domain.DokumentasjonsreferanseInternReferanseKartverket
import no.kartverket.nibas.domain.Territorialgrense
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

// Unit test av mappinglogikk
internal class MetaDataKtTest {

    private val oppdDato: ZonedDateTime = ZonedDateTime.now()
    private val dataFanget = oppdDato.minusYears(1)
    private val gyldigFraTidspunkt = oppdDato.minusMonths(6)
    private val fastsettingsdato: LocalDate = oppdDato.minusMonths(5).toLocalDate()
    private val beregningsdato: LocalDate = oppdDato.minusWeeks(17).toLocalDate()
    private val transformasjonsdato: LocalDate = oppdDato.minusDays(19).toLocalDate()

    private val link1 = dokLink(LINK_TEKST_1)
    private val link2 = dokLink(LINK_TEKST_2)
    private val referanse1 = internReferanse(REF_TEKST_1)
    private val referanse2 = internReferanse(REF_TEKST_2)

    private val dokumentasjonsreferanse = DokumentasjonsreferanseBuilder()
        .fastSettingAt(fastsettingsdato)
        .addLenker(link1, link2)
        .addInternRefs(referanse1, referanse2)
        .build()

    // Fra Fylkesgrense -> AdministrativGrenseMetadata
    @Test
    fun toAdministrativGrenseMetadata() {
        val fylkesgrense = FylkesgrenseBuilder()
            .withOppdateringsdato(oppdDato)
            .datafangstdato(dataFanget)
            .gyldigFra(gyldigFraTidspunkt)
            .dokRef(dokumentasjonsreferanse)
            .build()

        val administrativGrenseMetadata = AdministrativGrenseMetadata(
            common = expectedCommonMetadata(),
            commonGrense = expectedCommonGrensedata(),
            foelgerTerrengdetalj = TerrengdetaljEnum.AnnetGjerde.code,
            noeyaktighetsklasse = NoeyaktighetsklasseEnum.NK1.code,
            omtvistet = true
        )

        val metadata = fylkesgrense.toMetadata()
        assertThat(metadata).isEqualTo(administrativGrenseMetadata)

        val newGyldigFraDato = gyldigFraTidspunkt.plusMonths(1)
        val updatedMetadata = administrativGrenseMetadata.copy(
            common = administrativGrenseMetadata.common.copy(
                gyldigFra = newGyldigFraDato,
            )
        )
        fylkesgrense.updateMetadata(updatedMetadata)
        assertThat(fylkesgrense.gyldigFra).isEqualTo(newGyldigFraDato)
    }

    private fun Territorialgrense.findIdForAnsvarligMyndighet(myndighet: String): String {
        return this.ansvarligeMyndigheter.first { it.ansvarligmyndighet == myndighet }.id
    }

    // Får testet mapping for maritim grense
    // Territorialgrense -> TerritorialgrenseMetadata
    @Test
    fun toTerritorialgrenseMetadata() {
        val kv = "Kartverket"
        val hav = "Havarikommisjonen"
        val ansvarligMyndighet1 = AnsvarligMyndighetHolder(ansvarligmyndighet = kv)
        val ansvarligMyndighet2 = AnsvarligMyndighetHolder(ansvarligmyndighet = hav)

        val territorialgrense = TerritorialgrenseBuilder()
            .withOppdateringsdato(oppdDato)
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
            maritimeGrenser = MaritimeGrenserMetadata(
                ansvarligeMyndigheter = listOf(
                    TekstHolder(territorialgrense.findIdForAnsvarligMyndighet(kv), kv),
                    TekstHolder(territorialgrense.findIdForAnsvarligMyndighet(hav), hav)
                ),
                gjelderOmraade = "Område over elva",
                transformasjonsdato = transformasjonsdato,
                virkeomraadenavn = "Virkeområde 33",
                land = listOf(
                    TekstHolder("ru", "Russland"),
                ),
            ),
            beregningsdato = beregningsdato
        )

        val metadata = territorialgrense.toMetadata()
        assertThat(metadata).isEqualTo(territorialgrenseMetadata)

        val updatedBeregningsdato = beregningsdato.minusDays(1)
        val updatedMetadata = territorialgrenseMetadata.copy(
            beregningsdato = updatedBeregningsdato
        )
        territorialgrense.updateMetadata(updatedMetadata)
        assertThat(territorialgrense.beregningsdato).isEqualTo(updatedBeregningsdato)
    }

    private fun expectedCommonMetadata() = CommonMetadata(
        identifikasjon = Identifikasjon(LOKALID, NAVNEROM, VERSJON_33),
        datafangstdato = dataFanget,
        gyldigFra = gyldigFraTidspunkt,
        gyldigTil = null,
        oppdateringsdato = oppdDato,
        opphav = "opphav3",
        informasjon = INFORMASJONTEKST
    )

    private fun expectedCommonGrensedata() = CommonGrenseMetadata(
        posisjonskvalitet = Posisjonskvalitet(
            MaalemetodeKodeEnum.TERRENGMAALT.uuid,
            200
        ),
        fastsettingstype = FastsettingstypeEnum.BestemteKoordinater.name,
        grensestatus = GrensestatusEnum.Traktat.name,
        dokumentasjonsreferanser = listOf(dokRef(dokumentasjonsreferanse.id))
    )

    private fun dokRef(id: String): Dokref {
        return Dokref(
            id = id,
            dokumentlenker = listOf(
                TekstHolder(link1.id, LINK_TEKST_1),
                TekstHolder(link2.id, LINK_TEKST_2)
            ),
            fastsettingsdato = fastsettingsdato,
            fastsettingsmyndighet = MYNDIGHET,
            hjemmel = HJEMMEL_I_LOVEN,
            internReferanserKartverket = listOf(
                TekstHolder(referanse2.id, REF_TEKST_2),
                TekstHolder(referanse1.id, REF_TEKST_1)
            ),
            rettskildeId = RETTSKILDE_ID_1,
            rettskildeTittel = TITTEL
        )
    }

    private fun internReferanse(referanse: String): DokumentasjonsreferanseInternReferanseKartverket {
        return DokumentasjonsreferanseInternReferanseKartverket().apply { internReferanseKartverket = referanse }
    }

    private fun dokLink(linkText: String): DokumentasjonsreferanseDokumentlink {
        return DokumentasjonsreferanseDokumentlink().apply { dokumentlink = linkText }
    }

}
