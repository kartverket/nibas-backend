package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.geojson.metadata.Dokref
import no.kartverket.nibas.api.v1.common.geojson.metadata.TekstHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class DokumentasjonsreferanseTest {

    @Test
    fun toDokRef() {
        val march1 = LocalDate.of(2022, Month.MARCH, 1)

        val dokumentasjonsreferanse = Dokumentasjonsreferanse()
        val link1 = dokLink("dok3")
        val link2 = dokLink()
        dokumentasjonsreferanse.dokumentlenker = mutableSetOf(link1, link2)
        dokumentasjonsreferanse.fastsettingsdato = march1
        dokumentasjonsreferanse.fastsettingsmyndighet = "mynd1"
        dokumentasjonsreferanse.hjemmel = "hjemmel2"
        val internRef1 = internRef("ref44")
        val internRef2 = internRef("ref22")
        val internRef3 = internRef()
        dokumentasjonsreferanse.internReferanserKartverket = mutableSetOf(internRef1, internRef2, internRef3)
        dokumentasjonsreferanse.rettskildeid = "id1"
        dokumentasjonsreferanse.rettskildetittel = "Titten Tei"

        val generatedUUID = dokumentasjonsreferanse.id
        val link1Id = link1.id
        val internRef1Id = internRef1.id
        val internRef2Id = internRef2.id

        val expectedDokRef = Dokref(
            id = generatedUUID,
            dokumentlenker = listOf(TekstHolder(link1Id, "dok3")),
            fastsettingsdato = march1,
            fastsettingsmyndighet = "mynd1",
            hjemmel = "hjemmel2",
            internReferanserKartverket = listOf(
                TekstHolder(internRef2Id, "ref22"),
                TekstHolder(internRef1Id, "ref44")
            ),
            rettskildeId = "id1",
            rettskildeTittel = "Titten Tei"
        )

        val dokRef = dokumentasjonsreferanse.toDokRef()
        assertThat(dokRef).isEqualTo(expectedDokRef)
    }

    private fun dokLink(link: String? = null): DokumentasjonsreferanseDokumentlink {
        val dokRefDokLink1 = DokumentasjonsreferanseDokumentlink()
        dokRefDokLink1.dokumentlink = link
        return dokRefDokLink1
    }

    private fun internRef(ref: String? = null): DokumentasjonsreferanseInternReferanseKartverket {
        val internRef1 = DokumentasjonsreferanseInternReferanseKartverket()
        internRef1.internReferanseKartverket = ref
        return internRef1
    }
}
