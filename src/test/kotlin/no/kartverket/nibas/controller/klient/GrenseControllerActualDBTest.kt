package no.kartverket.nibas.controller.klient

import com.fasterxml.jackson.databind.ObjectMapper
import no.kartverket.nibas.TestConfiguration
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.AdministrativGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.Dokref
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataMapper
import no.kartverket.nibas.api.v1.common.geojson.metadata.TekstHolder
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.service.GrenseService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import javax.transaction.Transactional
import kotlin.test.Ignore

/**
 * For å kjøre, utkommenter Ignore-annotasjon.
 * Sett verdier til din lokale DB i user, pass, url i application.yml i test.
 * Sett en kommunegrense-uuid som du har i din DB.
 */
@ActiveProfiles("dev", "security-off")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfiguration::class)
class GrenseControllerActualDBTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: ObjectMapper

    @Autowired
    lateinit var grenseService: GrenseService

    val kommunegrenseIdentifikator = Identifikator(Lokalid("3e50135f-db16-426e-961e-6a0f9d464db7"))

    //TODO TS-696: Gjøre test til IT-test i int-pakke
    @Ignore
    @Transactional
    @Test
    fun oppdaterGrenserMedEndringerIMetadata() {
        // Henter en faktisk kommunegrense

        var kommunegrense = fetchKommunegrense()
        assertThat(kommunegrense.dokumentasjonsreferanser).isEmpty()

        // Konverterer til metadataObjekt og endrer noen egenskaper
        val metadataMapper = MetadataMapper()
        val metadata: AdministrativGrenseMetadata = metadataMapper.toMetadataForAdministrativGrense(kommunegrense)
        val gyldigFra = LocalDate.now().plusDays(1)
        val dokumentLink = "LinkTilMittDokument"
        val fastsettingsmyndighet = "Kartverket"
        val rettskildeId = "Id 33"
        val rettskildeTittel = "Min fine rettskilde!"
        val beskrivelse = "Min informasjonstekst"
        val updatedMetadata = metadata.copy(
            common = metadata.common.copy(
                gyldigFra = gyldigFra,
                informasjon = beskrivelse
            ),
            dokumentasjonsreferanser = listOf(
                Dokref(
                    dokumentlenker = listOf(TekstHolder(beskrivelse = dokumentLink)),
                    fastsettingsmyndighet = fastsettingsmyndighet,
                    rettskildeId = rettskildeId,
                    rettskildeTittel = rettskildeTittel
                )
            )
        )

        // POST mot /grenser
        val featureCollection = FeatureCollection(
            features = listOf(
                FeatureBuilder(id = kommunegrenseIdentifikator.lokalid.value)
                    .withGrensetype(Grensetype.Kommunegrense)
                    .withMetadata(updatedMetadata)
                    .build()
            )
        )

        val featureCollectionJsonAsString =
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(featureCollection)
        println(featureCollectionJsonAsString)
        mvc.perform(
            post("/v1/grenser/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(featureCollectionJsonAsString)
        ).andExpect(status().isOk)

        // Henter kommunegrense på nytt og asserter
        kommunegrense = fetchKommunegrense()
        assertThat(kommunegrense.gyldighet.gyldigFra).isEqualTo(gyldigFra)
        assertThat(kommunegrense.informasjonstekst).isEqualTo(beskrivelse)

        assertThat(kommunegrense.dokumentasjonsreferanser).hasSize(1)
        val dokumentasjonsreferanse = kommunegrense.dokumentasjonsreferanser.first()
        assertThat(dokumentasjonsreferanse.dokumentlenker).hasSize(1)
        assertThat(dokumentasjonsreferanse.dokumentlenker.first().dokumentlink).isEqualTo(dokumentLink)
        assertThat(dokumentasjonsreferanse.fastsettingsmyndighet).isEqualTo(fastsettingsmyndighet)
        assertThat(dokumentasjonsreferanse.rettskildeid).isEqualTo(rettskildeId)
        assertThat(dokumentasjonsreferanse.rettskildetittel).isEqualTo(rettskildeTittel)
    }

    private fun fetchKommunegrense(): Grense {
        val kommunegrense = grenseService.hentGrense(kommunegrenseIdentifikator)
        assertThat(kommunegrense).isNotNull
        return kommunegrense!!
    }

}
