package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.dokLink
import no.kartverket.nibas.api.v1.common.geojson.metadata.internReferanse
import no.kartverket.nibas.builders.DokumentasjonsreferanseBuilder
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.KobletTilGrense
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.extensions.geometry.geometryToDto
import no.kartverket.nibas.extensions.geometry.grenseToFeature
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("security-off")
@Transactional
class GrenseServiceIT : TestWithDbContainer() {

    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)

    @Test
    fun hentEksisterendeGrenseMedDagensDato() {
        val lokalIdOfGrense = Lokalid("ac7e05ca-fb65-4277-ad95-63c32c1370d0")
        saveGrenseAvTypeKommunegrense(lokalid = lokalIdOfGrense)
        assertThat(grenseRepository.findAllByLokalid(lokalIdOfGrense.value).size).isEqualTo(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(grenseService.hentGrense(Identifikator(lokalIdOfGrense, november28In2022))).isNotNull
    }

    @Test
    fun hentVersjonAvKommunegrenseFramITid() {
        val lokalIdOfGrense = Lokalid("ac7e05ca-fb65-4277-ad95-63c32c1370d0")
        saveGrenseAvTypeKommunegrense(id = "9000", lokalid = lokalIdOfGrense, gyldigTil = jan1In2023)
        saveGrenseAvTypeKommunegrense(id = "9001", lokalid = lokalIdOfGrense, gyldigFra = jan1In2023, opphav = "Oslo")
        assertThat(grenseRepository.findAllByLokalid(lokalIdOfGrense.value).size).isEqualTo(2)

        val framtidigVersjonAvGrense = grenseService.hentGrense(Identifikator(lokalIdOfGrense, jan1In2023))
        assertThat(framtidigVersjonAvGrense).isNotNull
        assertThat(framtidigVersjonAvGrense!!.opphav).isEqualTo("Oslo")
    }

    @Test
    fun `opprett ny stemmekretsgrense som lukker stemmekretsene`() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val (kretsG1, kretsG2) = setupKretserForNyeGrenserTest(gyldigFra, Grensetype.Stemmekretsgrense, ::saveStemmekrets)
        saveKommuneOgUnikLokalid(stemmekretser = listOf(kretsG1, kretsG2), gyldigFra = gyldigFra)

        val grenseSomSkalEndres = kretsG1.grenser.first { it.grense.lokalid == "grenseDeltLokalid" }.grense
        val featureForGrenseSomSkalEndres = grenseSomSkalEndres.grenseToFeature(gyldighetsdato)
        val oppdatertGeometri = GrenseHelper.geometryFactory.createLineString(
            arrayOf(
                Coordinate(10.0, 0.0),
                Coordinate(10.0, 5.0)
            )
        )
        oppdatertGeometri.geometryToDto().also { featureForGrenseSomSkalEndres.geometry = it }

        val gyldigNyGrense = lagNyGrense(
            "grenseDelt2Id",
            Grensetype.Stemmekretsgrense,
            "grenseDelt2Lokalid",
            gyldigFra,
            listOf(Coordinate(10.0, 5.0), Coordinate(10.0, 10.0)))
        gyldigNyGrense.stemmekretser = grenseSomSkalEndres.stemmekretser
        val featureForNyGrense: Feature = gyldigNyGrense.grenseToFeature(gyldighetsdato, nyGrense = true)

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureForGrenseSomSkalEndres, featureForNyGrense)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        val stemmekretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(kretsG1.lokalid), gyldighetsdato))
        val stemmekrets2EtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(kretsG2.lokalid), gyldighetsdato))
        assertThat(stemmekretsEtterLagring?.grenser).hasSize(5).extracting("grense.lokalid")
        assertThat(stemmekrets2EtterLagring?.grenser).hasSize(5).extracting("grense.lokalid")

    }

    @Test
    fun `opprett ny stemmekretsgrense som ikke lukker stemmekretsene`() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val (kretsG1, kretsG2) = setupKretserForNyeGrenserTest(gyldigFra, Grensetype.Stemmekretsgrense, ::saveStemmekrets)
        saveKommuneOgUnikLokalid(stemmekretser = listOf(kretsG1, kretsG2), gyldigFra = gyldigFra)

        val grenseSomSkalEndres = kretsG1.grenser.first { it.grense.lokalid == "grenseDeltLokalid" }.grense
        val featureForGrenseSomSkalEndres = grenseSomSkalEndres.grenseToFeature(gyldighetsdato)
        val oppdatertGeometri = GrenseHelper.geometryFactory.createLineString(
            arrayOf(
                Coordinate(10.0, 0.0),
                Coordinate(10.0, 5.0)
            )
        )
        oppdatertGeometri.geometryToDto().also { featureForGrenseSomSkalEndres.geometry = it }

        val ugyldigNyGrense = lagNyGrense(
            "grenseDelt2Id",
            Grensetype.Stemmekretsgrense,
            "grenseDelt2Lokalid",
            gyldigFra,
            listOf(Coordinate(10.0, 8.0), Coordinate(10.0, 10.0)))
        ugyldigNyGrense.stemmekretser = grenseSomSkalEndres.stemmekretser
        val featureForNyGrense: Feature = ugyldigNyGrense.grenseToFeature(gyldighetsdato, nyGrense = true)

        assertThatThrownBy {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(featureForGrenseSomSkalEndres, featureForNyGrense)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )

        }.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Klarer ikke å sortere grensene slik at de danner et gyldig polygon.")
    }

    @Test
    fun `opprett ny grunnkretsgrense som lukker grunnkretsene`() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val (kretsG1, kretsG2) = setupKretserForNyeGrenserTest(gyldigFra, Grensetype.Grunnkretsgrense, ::saveGrunnkrets)
        saveKommuneOgUnikLokalid(grunnkretser = listOf(kretsG1, kretsG2), gyldigFra = gyldigFra)

        val grenseSomSkalEndres = kretsG1.grenser.first { it.grense.lokalid == "grenseDeltLokalid" }.grense
        val featureForGrenseSomSkalEndres = grenseSomSkalEndres.grenseToFeature(gyldighetsdato)
        val oppdatertGeometri = GrenseHelper.geometryFactory.createLineString(
            arrayOf(
                Coordinate(10.0, 0.0),
                Coordinate(10.0, 5.0)
            )
        )
        oppdatertGeometri.geometryToDto().also { featureForGrenseSomSkalEndres.geometry = it }

        val gyldigNyGrense = lagNyGrense(
            "grenseDelt2Id",
            Grensetype.Grunnkretsgrense,
            "grenseDelt2Lokalid",
            gyldigFra,
            listOf(Coordinate(10.0, 5.0), Coordinate(10.0, 10.0)))
        gyldigNyGrense.grunnkretser = grenseSomSkalEndres.grunnkretser
        val featureForNyGrense: Feature = gyldigNyGrense.grenseToFeature(gyldighetsdato, nyGrense = true)

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureForGrenseSomSkalEndres, featureForNyGrense)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(kretsG1.lokalid), gyldighetsdato))
        val grunnkrets2EtterLagring = grunnkretsService.hentGrunnkrets((Identifikator(Lokalid(kretsG2.lokalid), gyldighetsdato)))
        assertThat(grunnkretsEtterLagring?.grenser).hasSize(5).extracting("grense.lokalid")
        assertThat(grunnkrets2EtterLagring?.grenser).hasSize(5).extracting("grense.lokalid")
    }

    @Test
    fun `opprett ny grunnkretsgrense som ikke lukker grunnkretsene`() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val (kretsG1, kretsG2) = setupKretserForNyeGrenserTest(gyldigFra, Grensetype.Grunnkretsgrense, ::saveGrunnkrets)
        saveKommuneOgUnikLokalid(grunnkretser = listOf(kretsG1, kretsG2), gyldigFra = gyldigFra)

        val grenseSomSkalEndres = kretsG1.grenser.first { it.grense.lokalid == "grenseDeltLokalid" }.grense
        val featureForGrenseSomSkalEndres = grenseSomSkalEndres.grenseToFeature(gyldighetsdato)
        val oppdatertGeometri = GrenseHelper.geometryFactory.createLineString(
            arrayOf(
                Coordinate(10.0, 0.0),
                Coordinate(10.0, 5.0)
            )
        )
        oppdatertGeometri.geometryToDto().also { featureForGrenseSomSkalEndres.geometry = it }

        val ugyldigNyGrense = lagNyGrense(
            "grenseDelt2Id",
            Grensetype.Grunnkretsgrense,
            "grenseDelt2Lokalid",
            gyldigFra,
            listOf(Coordinate(10.0, 8.0), Coordinate(10.0, 10.0)))
        ugyldigNyGrense.grunnkretser = grenseSomSkalEndres.grunnkretser
        val featureForNyGrense: Feature = ugyldigNyGrense.grenseToFeature(gyldighetsdato, nyGrense = true)

        assertThatThrownBy {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(featureForGrenseSomSkalEndres, featureForNyGrense)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )

        }.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Klarer ikke å sortere grensene slik at de danner et gyldig polygon.")
    }


    @Test
    fun `arkiver grunnkretsgrense og opprett ny grunnkretsgrense som lukker grunnkretsene`() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val (kretsG1, kretsG2) = setupKretserForNyeGrenserTest(gyldigFra, Grensetype.Grunnkretsgrense, ::saveGrunnkrets)
        saveKommuneOgUnikLokalid(grunnkretser = listOf(kretsG1, kretsG2), gyldigFra = gyldigFra)

        val grenseSomSkalArkiveres = kretsG1.grenser.first { it.grense.lokalid == "grenseDeltLokalid" }.grense
        val featureForGrenseSomSkalArkiveres = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, shouldArchive = true)

        val gyldigNyGrense = lagNyGrense(
            "grenseDelt2Id",
            Grensetype.Grunnkretsgrense,
            "grenseDelt2Lokalid",
            gyldigFra,
            listOf(Coordinate(10.0, 0.0), Coordinate(8.0, 5.0), Coordinate(10.0, 10.0)))
        gyldigNyGrense.grunnkretser = grenseSomSkalArkiveres.grunnkretser
        val featureForNyGrense: Feature = gyldigNyGrense.grenseToFeature(gyldighetsdato, nyGrense = true)

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureForGrenseSomSkalArkiveres, featureForNyGrense)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(kretsG1.lokalid), gyldighetsdato))
        val grunnkrets2EtterLagring = grunnkretsService.hentGrunnkrets((Identifikator(Lokalid(kretsG2.lokalid), gyldighetsdato)))
        assertThat(grunnkretsEtterLagring?.grenser).hasSize(4).extracting("grense.lokalid")
        assertThat(grunnkrets2EtterLagring?.grenser).hasSize(4).extracting("grense.lokalid")
    }

    @Test
    fun arkiverStemmekretsgrenseUtenAaLukkeKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val stemmekretsRequest = StemmekretsRequestBuilder.DEFAULT_REQUEST
        val identifikator = Identifikator(Lokalid(stemmekretsRequest.identifikasjon.lokalid), gyldigFra)
        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(
            gyldigFra, gyldighetsdato = gyldighetsdato, Grensetype.Stemmekretsgrense)
        )
        grenseRepository.flush()

        val stemmekrets = saveStemmekrets(id = "1", lokalid = identifikator.lokalid, stemmekretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)
        val stemmekrets2 = saveStemmekrets(id = "2", lokalid = identifikator.lokalid.copy("lokal2"), stemmekretsnummer = "30", gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(stemmekretser = listOf(stemmekrets, stemmekrets2), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val grenseSomSkalArkiveres = stemmekrets.grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense

        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)

        assertThatThrownBy {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(arkivertFeature)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )

        }.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Klarer ikke å sortere grensene slik at de danner et gyldig polygon.")
    }

    @Test
    fun arkiverStemmekretsgrenseOgLukkKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val lokalid1 = Lokalid("1")
        val lokalid2 = Lokalid("2")

        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, gyldighetsdato = gyldighetsdato, Grensetype.Stemmekretsgrense))
        grenseRepository.flush()

        val stemmekrets = saveStemmekrets(id = "1", lokalid = lokalid1, stemmekretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)
        val stemmekrets2 = saveStemmekrets(id = "2", lokalid = lokalid2, stemmekretsnummer = "21", gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(stemmekretser = listOf(stemmekrets, stemmekrets2), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val grenseSomSkalArkiveres = stemmekrets.grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense
        val grenseSomSkalJusteres = stemmekrets.grenser.first { it.grense.lokalid == "grense2Lokalid" }.grense

        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)
        val featureMedEndretGeometri = utfoerStemmekretsgrensejusteringEtterArkivering(grenseSomSkalJusteres, gyldighetsdato)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(arkivertFeature, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
        assertThat(flateSet).hasSize(2)
        val flate = flateSet.firstOrNull { it.lokalid == lokalid1.value }
        assertThat(flate).isNotNull

        val stemmekretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(flate!!.lokalid), gyldighetsdato))
        assertThat(stemmekretsEtterLagring?.grenser).hasSize(3).extracting("grense.lokalid")
            .doesNotContain("grense1Lokalid")
    }

    @Test
    fun arkiverGrunnkretsgrenseUtenAaLukkeKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val grunnkretsRequest = GrunnkretsRequestBuilder.DEFAULT_REQUEST
        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), gyldigFra)
        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, gyldighetsdato = gyldighetsdato, Grensetype.Grunnkretsgrense))
        grenseRepository.flush()

        val grunnkrets = saveGrunnkrets(id = "1", lokalid = identifikator.lokalid, grunnkretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)
        val naboGrunnkrets = saveGrunnkrets(id = "2", lokalid = Lokalid("nabo"), grunnkretsnummer = "21", gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(grunnkretser = listOf(grunnkrets, naboGrunnkrets), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val grenseSomSkalArkiveres = grunnkrets.grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense

        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)

        assertThatThrownBy {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(arkivertFeature)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Klarer ikke å sortere grensene slik at de danner et gyldig polygon.")
    }

    @Test
    fun arkiverGrunnkretsgrenseOgLukkKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val lokalid1 = Lokalid("1")
        val lokalid2 = Lokalid("2")

        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, gyldighetsdato = gyldighetsdato, Grensetype.Grunnkretsgrense))
        grenseRepository.flush()

        val grunnkrets = saveGrunnkrets(id = "1", lokalid = lokalid1, grunnkretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)
        val grunnkrets2 = saveGrunnkrets(id = "2", lokalid = lokalid2, gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(grunnkretser = listOf(grunnkrets, grunnkrets2), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val grenseSomSkalArkiveres = grunnkrets.grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense
        val grenseSomSkalJusteres = grunnkrets.grenser.first { it.grense.lokalid == "grense2Lokalid" }.grense

        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)
        val featureMedEndretGeometri = utfoerGrunnkretsgrensejusteringEtterArkivering(grenseSomSkalJusteres, gyldighetsdato)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(arkivertFeature, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        assertThat(flateSet).hasSize(2)
        val flate = flateSet.firstOrNull { it.lokalid == lokalid1.value }
        assertThat(flate).isNotNull

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(flate!!.lokalid), gyldighetsdato))
        assertThat(grunnkretsEtterLagring?.grenser).hasSize(3).extracting("grense.lokalid")
            .doesNotContain("grense1Lokalid")
    }


    private fun utfoerStemmekretsgrensejusteringEtterArkivering(grenseSomSkalJusteres: Grense, gyldighetsdato: LocalDate): Feature {
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            Coordinate(5.0, 5.0),
            Coordinate(0.0, 0.0),
        ))
        val oppdatertGrensefeature = grenseSomSkalJusteres.grenseToFeature(gyldighetsdato)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }

        return oppdatertGrensefeature
    }

    private fun utfoerGrunnkretsgrensejusteringEtterArkivering(grenseSomSkalJusteres: Grense, gyldighetsdato: LocalDate): Feature {
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            Coordinate(5.0, 5.0),
            Coordinate(0.0, 0.0),
        ))
        val oppdatertGrensefeature = grenseSomSkalJusteres.grenseToFeature(gyldighetsdato)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }
        return oppdatertGrensefeature
    }

    /**
     * Oppretter en enkel kvadratisk krets:
     *
     *   (0.0, 5.0) ----- ----- -----  (5.0, 5.0)
     *      |            grense2           |
     *      |                              |
     *      | grense1                      | grense3
     *      |                              |
     *      |            grense4           |
     *   (0.0, 0.0) ----- ----- -----  (5.0, 0.0)
     */
    fun getGrenserForEnkelKrets(gyldigFra: LocalDate, gyldighetsdato: LocalDate, grensetype: Grensetype): List<Grense> {

        val grense1 = Grense(id = "grense1", lokalid = "grense1Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 5.0),
                        Coordinate(0.0, 0.0)
                    )
                )
            }

        val grense2 = Grense(id = "grense2", lokalid = "grense2Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 5.0),
                        Coordinate(0.0, 5.0),
                    )
                )
            }
        val grense3 = Grense(id = "grense3", lokalid = "grense3Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 0.0),
                        Coordinate(5.0, 5.0),
                    )
                )
            }
        val grense4 = Grense(id = "grense4", lokalid = "grense4Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 0.0),
                        Coordinate(0.0, 0.0)
                    )
                )
            }
        return listOf(grense1, grense2, grense3, grense4)
    }

    @Test
    fun `lagreGrenser klarer å endre tilhørighet på grunnkretsgrense`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        // oppretter tre kretser som deler grenser der én grense har feil grensekoblinger grunnet at en annen grense har fått endret geometrien sin.
        val (kretsG1, kretsG2, kretsG3) = setupTilhorighetTest(gyldighetsdato, Grensetype.Grunnkretsgrense, ::saveGrunnkrets)

        saveKommuneOgUnikLokalid(grunnkretser = listOf(kretsG1, kretsG2, kretsG3), gyldigFra = gyldighetsdato)

        // Fra krets finner vi fram til de to grensene som har endringer i dette fiktive utkastet
        val grenseSomSkalEndreTilhorighet = kretsG1.grenser.map { it.grense }.first { it.lokalid == "grenseSørVestLokalid" }
        val grenseSomEndreGeometrien = kretsG1.grenser.map { it.grense }.first { it.lokalid == "grenseDelt2Lokalid" }

        val nyeTilhorigheter = listOf(KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("G2"), gyldighetsdato),
            type = KontekstEnhetType.GRUNNKRETS,
            version = 1
        ), KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
            type = KontekstEnhetType.GRUNNKRETS,
            version = 1
        ))
        val featureMedEndretTilhorighet = utfoerGrenseTilhorighetEndring(grenseSomSkalEndreTilhorighet, nyeTilhorigheter)
        val featureMedEndretGeometri = utfoerGeometriEndring(grenseSomEndreGeometrien, grenseSomEndreGeometrien.grunnkretser.map { it.grunnkrets }, gyldighetsdato, KontekstEnhetType.GRUNNKRETS)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureMedEndretTilhorighet, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )

        assertGrunnkretsTilhorighetTest(flateSet, 3)
    }

    @Test
    fun `lagreGrenser klarer å endre tilhørighet på kommunegrense med grunnkretsgrensekoblinger`() {
        val gyldighetsdato = LocalDate.now()
        val (kretsG1, kretsG2, kretsG3) = setupTilhorighetTest(gyldighetsdato, Grensetype.Grunnkretsgrense, ::saveGrunnkrets, medDeltKommuneGrense = true)

        val kommuneNord = saveKommuneOgUnikLokalid(id = "kommuneNord", lokalid = "kommuneNord", kommunenummer = "0301", kommunenummerUuid = "0301", grunnkretser = listOf(kretsG1, kretsG2), gyldigFra = gyldighetsdato)
        val kommuneSoer = saveKommuneOgUnikLokalid(id = "kommuneSoer", lokalid = "kommuneSoer", kommunenummer = "0302", kommunenummerUuid = "0302", grunnkretser = listOf(kretsG3), gyldigFra = gyldighetsdato)
        saveFylke(kommuner = listOf(kommuneNord, kommuneSoer), gyldigFra = gyldighetsdato)

        grenseRepository.flush()
        flateService.oppdaterFlaterFor(setOf(kommuneSoer, kommuneNord))
        kommuneNord.posisjon = kommuneNord.omraade?.interiorPoint
        kommuneSoer.posisjon = kommuneSoer.omraade?.interiorPoint
        grenseRepository.flush()

        val grenseSomSkalEndreTilhorighet = kretsG1.grenser.map { it.grense }.first { it.lokalid == "grenseSørVestLokalid" }
        val grenseSomEndreGeometrien = kretsG1.grenser.map { it.grense }.first { it.lokalid == "grenseDelt2Lokalid" }

        val nyeTilhorigheter = listOf(KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("G2"), gyldighetsdato),
            type = KontekstEnhetType.GRUNNKRETS,
            version = 1
        ), KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
            type = KontekstEnhetType.GRUNNKRETS,
            version = 1
        ))

        val featureMedEndretTilhorighet = utfoerGrenseTilhorighetEndring(grenseSomSkalEndreTilhorighet, nyeTilhorigheter)
        val featureMedEndretGeometri = utfoerGeometriEndring(grenseSomEndreGeometrien, grenseSomEndreGeometrien.grunnkretser.map { it.grunnkrets }, gyldighetsdato, KontekstEnhetType.GRUNNKRETS)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureMedEndretTilhorighet, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )

        assertGrunnkretsTilhorighetTest(flateSet, 5)
    }

    private fun assertGrunnkretsTilhorighetTest(flateSet: Set<Flate>, size: Int) {
        assertThat(flateSet.size).isEqualTo(size)
        assertThat(flateSet.map { it.lokalid }).containsAll(listOf("G1", "G2", "G3"))
        (flateSet.first { it.lokalid == "G1" } as Grunnkrets).run {
            assertThat(this.grenser.size).isEqualTo(4)
            assertThat(this.grenser.find { it.grense.lokalid == "grenseSørVestLokalid" }).isEqualTo(null)
        }
        (flateSet.first { it.lokalid == "G2" } as Grunnkrets).run {
            assertThat(this.grenser.size).isEqualTo(6)
            val grenseSomFikkEndretTilhorighet = this.grenser.find { it.grense.lokalid == "grenseSørVestLokalid" }!!.grense
            assertThat(grenseSomFikkEndretTilhorighet).isNotNull
            assertThat(grenseSomFikkEndretTilhorighet.grunnkretser.size).isEqualTo(2)
            assertThat(grenseSomFikkEndretTilhorighet.grunnkretser.map { it.grunnkrets.lokalid }).containsAll(listOf("G2", "G3"))
        }
        (flateSet.first { it.lokalid == "G3" } as Grunnkrets).run {
            assertThat(this.grenser.size).isEqualTo(4)
        }
    }

    @Test
    fun `lagreGrenser klarer å endre tilhørighet på kommunegrense med stemmekretsgrensekoblinger`() {
        val gyldighetsdato = LocalDate.now()
        val (kretsG1, kretsG2, kretsG3) = setupTilhorighetTest(gyldighetsdato, Grensetype.Stemmekretsgrense, ::saveStemmekrets, medDeltKommuneGrense = true)

        val kommuneNord = saveKommuneOgUnikLokalid(id = "kommuneNord", lokalid = "kommuneNord", kommunenummer = "0301", kommunenummerUuid = "0301", stemmekretser = listOf(kretsG1, kretsG2), gyldigFra = gyldighetsdato)
        val kommuneSoer = saveKommuneOgUnikLokalid(id = "kommuneSoer", lokalid = "kommuneSoer", kommunenummer = "0302", kommunenummerUuid = "0302", stemmekretser = listOf(kretsG3), gyldigFra = gyldighetsdato)
        saveFylke(kommuner = listOf(kommuneNord, kommuneSoer), gyldigFra = gyldighetsdato)

        grenseRepository.flush()
        flateService.oppdaterFlaterFor(setOf(kommuneSoer, kommuneNord))
        kommuneNord.posisjon = kommuneNord.omraade?.interiorPoint
        kommuneSoer.posisjon = kommuneSoer.omraade?.interiorPoint
        grenseRepository.flush()

        val grenseSomSkalEndreTilhorighet = kretsG1.grenser.map { it.grense }.first { it.lokalid == "grenseSørVestLokalid" }
        val grenseSomEndreGeometrien = kretsG1.grenser.map { it.grense }.first { it.lokalid == "grenseDelt2Lokalid" }

        val nyeTilhorigheter = listOf(KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("G2"), gyldighetsdato),
            type = KontekstEnhetType.STEMMEKRETS,
            version = 1
        ), KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
            type = KontekstEnhetType.STEMMEKRETS,
            version = 1
        ))

        val featureMedEndretTilhorighet = utfoerGrenseTilhorighetEndring(grenseSomSkalEndreTilhorighet, nyeTilhorigheter)
        val featureMedEndretGeometri = utfoerGeometriEndring(grenseSomEndreGeometrien, grenseSomEndreGeometrien.stemmekretser.map { it.stemmekrets }, gyldighetsdato, KontekstEnhetType.STEMMEKRETS)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureMedEndretTilhorighet, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )

        assertStemmekretsTilhorighetTest(flateSet, 3)
    }

    private fun assertStemmekretsTilhorighetTest(flateSet: Set<Flate>, size: Int) {
        assertThat(flateSet.size).isEqualTo(size)
        assertThat(flateSet.map { it.lokalid }).containsAll(listOf("G1", "G2", "G3"))
        (flateSet.first { it.lokalid == "G1" } as Stemmekrets).run {
            assertThat(this.grenser.size).isEqualTo(4)
            assertThat(this.grenser.find { it.grense.lokalid == "grenseSørVestLokalid" }).isEqualTo(null)
        }
        (flateSet.first { it.lokalid == "G2" } as Stemmekrets).run {
            assertThat(this.grenser.size).isEqualTo(6)
            val grenseSomFikkEndretTilhorighet = this.grenser.find { it.grense.lokalid == "grenseSørVestLokalid" }!!.grense
            assertThat(grenseSomFikkEndretTilhorighet).isNotNull
            assertThat(grenseSomFikkEndretTilhorighet.stemmekretser.size).isEqualTo(2)
            assertThat(grenseSomFikkEndretTilhorighet.stemmekretser.map { it.stemmekrets.lokalid }).containsAll(listOf("G2", "G3"))
        }
        (flateSet.first { it.lokalid == "G3" } as Stemmekrets).run {
            assertThat(this.grenser.size).isEqualTo(4)
        }
    }

    @Test
    fun `lagreGrenser klarer å endre tilhørighet på grunnkretsgrense til nyopprettet grunnkrets`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        val (kretsOpprinnelig, kretsOest, kretsNordVest) =
            getTilhorigetTilNyopprettetKretsTestSetup(Grensetype.Grunnkretsgrense, gyldighetsdato, ::saveGrunnkrets)

        // når kretsen er ny vil den ligget i databasen på det tidspunktet lagreGrenser blir kjørt.
        val nyGrunnkrets = saveGrunnkrets("nyGrunnkrets", Lokalid("nyGrunnkrets"), grunnkretsnummer = "420")

        saveKommuneOgUnikLokalid(grunnkretser = listOf(kretsOpprinnelig, kretsOest, kretsNordVest, nyGrunnkrets), gyldigFra = gyldighetsdato)

        val grense3_2SomSkalEndreGeometri = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_2Lokalid" }!!.grense
        val grense3_1SomSkalEndreGeometriOgTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_1Lokalid" }!!.grense
        val grense2SomSkalEndreTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense2Lokalid" }!!.grense
        val grense1_2SomSkalEndreTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense1_2Lokalid" }!!.grense

        val tilhorighetTilKretsNordVestOgNyGrunnkrets = listOf(
            KontekstEgenskaper(
                id = null,
                kommuneId = null,
                kretsNummer = nyGrunnkrets.grunnkretsnummer,
                type = KontekstEnhetType.GRUNNKRETS,
                version = nyGrunnkrets.version
            ),
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(kretsNordVest.lokalid), gyldighetsdato),
                kommuneId = null,
                kretsNummer = null,
                type = KontekstEnhetType.GRUNNKRETS,
                version = kretsNordVest.version
            )
        )
        val tilhorighetTilKretsOestOgNyGrunnkrets = listOf(
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(kretsOest.lokalid), gyldighetsdato),
                kommuneId = null,
                kretsNummer = null,
                type = KontekstEnhetType.GRUNNKRETS,
                version = kretsOest.version
            ),
            KontekstEgenskaper(
                id = null,
                kommuneId = null,
                kretsNummer = nyGrunnkrets.grunnkretsnummer,
                type = KontekstEnhetType.GRUNNKRETS,
                version = nyGrunnkrets.version
            )
        )

        val grense3_2MedEndretGeometri = FeatureBuilder(id = grense3_2SomSkalEndreGeometri.lokalid).copy(grense3_2SomSkalEndreGeometri.grenseToFeature())
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(5.0, 0.0),
                    Coordinate(0.0, 2.5)
                )
            ))
            .withVersion(2)
            .build()

        val grense3_1MedEndretGeometriOgTilhorighet = FeatureBuilder(id = grense3_1SomSkalEndreGeometriOgTilhorighet.lokalid).copy(grense3_1SomSkalEndreGeometriOgTilhorighet.grenseToFeature())
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(5.0, 5.0),
                    Coordinate(0.0, 2.5)
                )
            ))
            .withKontekstEgenskaper(tilhorighetTilKretsOestOgNyGrunnkrets)
            .withVersion(2)
            .build()

        val grense2MedEndretTilhorighet = FeatureBuilder(id = grense2SomSkalEndreTilhorighet.lokalid).copy(grense2SomSkalEndreTilhorighet.grenseToFeature())
            .withKontekstEgenskaper(tilhorighetTilKretsNordVestOgNyGrunnkrets)
            .withVersion(2)
            .build()

        val grense1_2MedEndretTilhorighet = FeatureBuilder(id = grense1_2SomSkalEndreTilhorighet.lokalid).copy(grense1_2SomSkalEndreTilhorighet.grenseToFeature())
            .withKontekstEgenskaper(tilhorighetTilKretsNordVestOgNyGrunnkrets)
            .withVersion(2)
            .build()

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features =
            listOf(
                grense3_2MedEndretGeometri,
                grense3_1MedEndretGeometriOgTilhorighet,
                grense2MedEndretTilhorighet,
                grense1_2MedEndretTilhorighet
            )
            ),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        assertThat(flateSet.size).isEqualTo(4)
        assertThat(flateSet.map { it.lokalid }).containsAll(listOf("nyGrunnkrets", "Opprinnelig", "Oestre", "NordVest"))
        (flateSet.first { it.lokalid == "nyGrunnkrets" } as Grunnkrets).run {
            assertThat(this.grenser.size).isEqualTo(3)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(listOf("grense1_2Lokalid", "grense2Lokalid", "grense3_1Lokalid"))
        }
        (flateSet.first { it.lokalid == "Opprinnelig" } as Grunnkrets).run {
            assertThat(this.grenser.size).isEqualTo(3)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(listOf("grense1_1Lokalid", "grense3_2Lokalid", "grense4Lokalid"))
        }
        (flateSet.first { it.lokalid == "Oestre" } as Grunnkrets).run {
            assertThat(this.grenser.size).isEqualTo(5)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(kretsOest.grenser.map { it.grense.lokalid })
        }
        (flateSet.first { it.lokalid == "NordVest" } as Grunnkrets).run {
            assertThat(this.grenser.size).isEqualTo(4)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(kretsNordVest.grenser.map { it.grense.lokalid })
        }
    }

    @Test
    fun `lagreGrenser skal kaste exception hvis det ikke går å identifisere nyopprettet grunnkrets`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        val kretser =
            getTilhorigetTilNyopprettetKretsTestSetup(Grensetype.Grunnkretsgrense, gyldighetsdato, ::saveGrunnkrets)

        saveKommune("1", grunnkretser = kretser)

        val kretsOpprinnelig = kretser[0]
        val kretsNordVest = kretser[2]

        val grense2SomSkalEndreTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense2Lokalid" }!!.grense
        val nyGrunnkrets = saveGrunnkrets("nyGrunnkrets", Lokalid("nyGrunnkrets"), grunnkretsnummer = "420")

        val tilhorighetTilKretsNordVestOgNyGrunnkretsFEIL = listOf(
            KontekstEgenskaper(
                id = null,
                kommuneId = null,
                kretsNummer = null,
                type = KontekstEnhetType.GRUNNKRETS,
                version = nyGrunnkrets.version
            ),
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(kretsNordVest.lokalid), gyldighetsdato),
                kommuneId = null,
                kretsNummer = null,
                type = KontekstEnhetType.GRUNNKRETS,
                version = kretsNordVest.version
            )
        )

        val grense2MedEndretTilhorighet = FeatureBuilder(id = grense2SomSkalEndreTilhorighet.lokalid).copy(grense2SomSkalEndreTilhorighet.grenseToFeature())
            .withKontekstEgenskaper(tilhorighetTilKretsNordVestOgNyGrunnkretsFEIL)
            .withVersion(2)
            .build()

        assertThatThrownBy {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features =
                listOf(
                    grense2MedEndretTilhorighet,
                )
                ),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }.isInstanceOf(RuntimeException::class.java).hasMessageContaining("KontekstEgenskaper for feature med id: ${grense2MedEndretTilhorighet.id} er null")
    }

    @Test
    fun `lagreGrenser klarer å endre tilhørighet på stemmekretsgrense`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        // oppretter tre kretser som deler grenser der én grense har feil grensekoblinger grunnet at en annen grense har fått endret geometrien sin.
        val (kretsG1, kretsG2, kretsG3) = setupTilhorighetTest(gyldighetsdato, Grensetype.Stemmekretsgrense, ::saveStemmekrets)

        saveKommuneOgUnikLokalid(stemmekretser = listOf(kretsG1, kretsG2, kretsG3), gyldigFra = gyldighetsdato)

        // Fra krets finner vi fram til de to grensene som har endringer i dette fiktive utkastet
        val grenseSomSkalEndreTilhorighet = kretsG1.grenser.map { it.grense }.first { it.lokalid == "grenseSørVestLokalid" }
        val grenseSomEndreGeometrien = kretsG1.grenser.map { it.grense }.first { it.lokalid == "grenseDelt2Lokalid" }

        val nyeTilhorigheter = listOf(KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("G2"), gyldighetsdato),
            type = KontekstEnhetType.STEMMEKRETS,
            version = 1
        ), KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("G3"), gyldighetsdato),
            type = KontekstEnhetType.STEMMEKRETS,
            version = 1
        ))

        val featureMedEndretTilhorighet = utfoerGrenseTilhorighetEndring(grenseSomSkalEndreTilhorighet, nyeTilhorigheter)
        val featureMedEndretGeometri = utfoerGeometriEndring(grenseSomEndreGeometrien, grenseSomEndreGeometrien.stemmekretser.map { it.stemmekrets }, gyldighetsdato, KontekstEnhetType.STEMMEKRETS)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureMedEndretTilhorighet, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )

        assertStemmekretsTilhorighetTest(flateSet, 3)
    }

    private fun lagNyGrense(id: String, grensetype: Grensetype, lokalId: String, gyldigFra: LocalDate, coordinates: List<Coordinate>): Grense {
        return Grense(id = id, lokalid = lokalId, grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    coordinates.toTypedArray()
                )
            }
    }

    @Test
    fun `lagreGrenser klarer å endre tilhørighet på stemmekretsgrense til nyopprettet stemmekrets`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        val (kretsOpprinnelig, kretsOest, kretsNordVest) =
            getTilhorigetTilNyopprettetKretsTestSetup(Grensetype.Stemmekretsgrense, gyldighetsdato, ::saveStemmekrets)

        // når kretsen er ny vil den ligget i databasen på det tidspunktet lagreGrenser blir kjørt.
        val nyStemmekrets = saveStemmekrets("nyStemmekrets", Lokalid("nyStemmekrets"), stemmekretsnummer = "420", stemmekretsnavn = "nyStemmekrets")

        val kommune = saveKommuneOgUnikLokalid(stemmekretser = listOf(kretsOpprinnelig, kretsOest, kretsNordVest, nyStemmekrets), gyldigFra = gyldighetsdato)

        val grense3_2SomSkalEndreGeometri = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_2Lokalid" }!!.grense
        val grense3_1SomSkalEndreGeometriOgTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_1Lokalid" }!!.grense
        val grense2SomSkalEndreTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense2Lokalid" }!!.grense
        val grense1_2SomSkalEndreTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense1_2Lokalid" }!!.grense

        val tilhorighetTilKretsNordVestOgNyStemmekrets = listOf(
            KontekstEgenskaper(
                id = null,
                kommuneId = ObjektIdentifikator(Lokalid(kommune.lokalid), gyldighetsdato),
                kretsNummer = nyStemmekrets.stemmekretsnummer,
                type = KontekstEnhetType.STEMMEKRETS,
                version = nyStemmekrets.version
            ),
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(kretsNordVest.lokalid), gyldighetsdato),
                kommuneId = null,
                kretsNummer = null,
                type = KontekstEnhetType.STEMMEKRETS,
                version = kretsNordVest.version
            )
        )
        val tilhorighetTilKretsOestOgNyStemmekrets = listOf(
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(kretsOest.lokalid), gyldighetsdato),
                kommuneId = null,
                kretsNummer = null,
                type = KontekstEnhetType.STEMMEKRETS,
                version = kretsOest.version
            ),
            KontekstEgenskaper(
                id = null,
                kommuneId = ObjektIdentifikator(Lokalid(kommune.lokalid), gyldighetsdato),
                kretsNummer = nyStemmekrets.stemmekretsnummer,
                type = KontekstEnhetType.STEMMEKRETS,
                version = nyStemmekrets.version
            )
        )

        val grense3_2MedEndretGeometri = FeatureBuilder(id = grense3_2SomSkalEndreGeometri.lokalid).copy(grense3_2SomSkalEndreGeometri.grenseToFeature())
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(5.0, 0.0),
                    Coordinate(0.0, 2.5)
                )
            ))
            .withVersion(2)
            .build()

        val grense3_1MedEndretGeometriOgTilhorighet = FeatureBuilder(id = grense3_1SomSkalEndreGeometriOgTilhorighet.lokalid).copy(grense3_1SomSkalEndreGeometriOgTilhorighet.grenseToFeature())
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(5.0, 5.0),
                    Coordinate(0.0, 2.5)
                )
            ))
            .withKontekstEgenskaper(tilhorighetTilKretsOestOgNyStemmekrets)
            .withVersion(2)
            .build()

        val grense2MedEndretTilhorighet = FeatureBuilder(id = grense2SomSkalEndreTilhorighet.lokalid).copy(grense2SomSkalEndreTilhorighet.grenseToFeature())
            .withKontekstEgenskaper(tilhorighetTilKretsNordVestOgNyStemmekrets)
            .withVersion(2)
            .build()

        val grense1_2MedEndretTilhorighet = FeatureBuilder(id = grense1_2SomSkalEndreTilhorighet.lokalid).copy(grense1_2SomSkalEndreTilhorighet.grenseToFeature())
            .withKontekstEgenskaper(tilhorighetTilKretsNordVestOgNyStemmekrets)
            .withVersion(2)
            .build()

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features =
            listOf(
                grense3_2MedEndretGeometri,
                grense3_1MedEndretGeometriOgTilhorighet,
                grense2MedEndretTilhorighet,
                grense1_2MedEndretTilhorighet
            )
            ),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        assertThat(flateSet.size).isEqualTo(4)
        assertThat(flateSet.map { it.lokalid }).containsAll(listOf("nyStemmekrets", "Opprinnelig", "Oestre", "NordVest"))
        (flateSet.first { it.lokalid == "nyStemmekrets" } as Stemmekrets).run {
            assertThat(this.grenser.size).isEqualTo(3)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(listOf("grense1_2Lokalid", "grense2Lokalid", "grense3_1Lokalid"))
        }
        (flateSet.first { it.lokalid == "Opprinnelig" } as Stemmekrets).run {
            assertThat(this.grenser.size).isEqualTo(3)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(listOf("grense1_1Lokalid", "grense3_2Lokalid", "grense4Lokalid"))
        }
        (flateSet.first { it.lokalid == "Oestre" } as Stemmekrets).run {
            assertThat(this.grenser.size).isEqualTo(5)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(kretsOest.grenser.map { it.grense.lokalid })
        }
        (flateSet.first { it.lokalid == "NordVest" } as Stemmekrets).run {
            assertThat(this.grenser.size).isEqualTo(4)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(kretsNordVest.grenser.map { it.grense.lokalid })
        }
    }

    @Test
    fun `lagreGrenser skal kaste exception hvis kontektst mangler kommuneidentifikator for nyopprettet stemmekrets`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        val (kretsOpprinnelig, kretsOest, kretsNordVest) =
            getTilhorigetTilNyopprettetKretsTestSetup(Grensetype.Stemmekretsgrense, gyldighetsdato, ::saveStemmekrets)

        // når kretsen er ny vil den ligget i databasen på det tidspunktet lagreGrenser blir kjørt.
        val nyStemmekrets = saveStemmekrets("nyStemmekrets", Lokalid("nyStemmekrets"), stemmekretsnummer = "420", stemmekretsnavn = "nyStemmekrets")

        val kommune = saveKommuneOgUnikLokalid(stemmekretser = listOf(kretsOpprinnelig, kretsOest, kretsNordVest, nyStemmekrets), gyldigFra = gyldighetsdato)

        val grense3_2SomSkalEndreGeometri = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_2Lokalid" }!!.grense
        val grense3_1SomSkalEndreGeometriOgTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_1Lokalid" }!!.grense
        val grense2SomSkalEndreTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense2Lokalid" }!!.grense
        val grense1_2SomSkalEndreTilhorighet = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense1_2Lokalid" }!!.grense

        val tilhorighetTilKretsNordVestOgNyStemmekretsFEIL = listOf(
            KontekstEgenskaper(
                id = null,
                kommuneId = null,
                kretsNummer = nyStemmekrets.stemmekretsnummer,
                type = KontekstEnhetType.STEMMEKRETS,
                version = nyStemmekrets.version
            ),
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(kretsNordVest.lokalid), gyldighetsdato),
                kommuneId = null,
                kretsNummer = null,
                type = KontekstEnhetType.STEMMEKRETS,
                version = kretsNordVest.version
            )
        )
        val tilhorighetTilKretsOestOgNyStemmekrets = listOf(
            KontekstEgenskaper(
                id = ObjektIdentifikator(Lokalid(kretsOest.lokalid), gyldighetsdato),
                kommuneId = null,
                kretsNummer = null,
                type = KontekstEnhetType.STEMMEKRETS,
                version = kretsOest.version
            ),
            KontekstEgenskaper(
                id = null,
                kommuneId = ObjektIdentifikator(Lokalid(kommune.lokalid), gyldighetsdato),
                kretsNummer = nyStemmekrets.stemmekretsnummer,
                type = KontekstEnhetType.STEMMEKRETS,
                version = nyStemmekrets.version
            )
        )

        val grense3_2MedEndretGeometri = FeatureBuilder(id = grense3_2SomSkalEndreGeometri.lokalid).copy(grense3_2SomSkalEndreGeometri.grenseToFeature())
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(5.0, 0.0),
                    Coordinate(0.0, 2.5)
                )
            ))
            .withVersion(2)
            .build()

        val grense3_1MedEndretGeometriOgTilhorighet = FeatureBuilder(id = grense3_1SomSkalEndreGeometriOgTilhorighet.lokalid).copy(grense3_1SomSkalEndreGeometriOgTilhorighet.grenseToFeature())
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(5.0, 5.0),
                    Coordinate(0.0, 2.5)
                )
            ))
            .withKontekstEgenskaper(tilhorighetTilKretsOestOgNyStemmekrets)
            .withVersion(2)
            .build()

        val grense2MedEndretTilhorighet = FeatureBuilder(id = grense2SomSkalEndreTilhorighet.lokalid).copy(grense2SomSkalEndreTilhorighet.grenseToFeature())
            .withKontekstEgenskaper(tilhorighetTilKretsNordVestOgNyStemmekretsFEIL)
            .withVersion(2)
            .build()

        val grense1_2MedEndretTilhorighet = FeatureBuilder(id = grense1_2SomSkalEndreTilhorighet.lokalid).copy(grense1_2SomSkalEndreTilhorighet.grenseToFeature())
            .withKontekstEgenskaper(tilhorighetTilKretsNordVestOgNyStemmekretsFEIL)
            .withVersion(2)
            .build()

        assertThatThrownBy {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features =
                listOf(
                    grense3_2MedEndretGeometri,
                    grense3_1MedEndretGeometriOgTilhorighet,
                    grense2MedEndretTilhorighet,
                    grense1_2MedEndretTilhorighet
                )
                ),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Kretsnummer og kommune-id må være satt når man setter tilhørighet til en ny stemmekrets.")
    }

    private fun <T : KobletTilGrense> utfoerGeometriEndring(grense: Grense, kretser: List<T>, gyldighetsdato: LocalDate, kontekstEnhetType: KontekstEnhetType): Feature {
        return FeatureBuilder(id = grense.lokalid)
            .withLokalid(grense.lokalid)
            .withGeometry(GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(0.0, 0.0),
                    Coordinate(10.0, 5.0)
                )
            ))
            .withGrenseversjon(2)
            .withKontekstEgenskaper(
                listOf(
                    KontekstEgenskaper(
                        ObjektIdentifikator(Lokalid(kretser[0].lokalid), gyldighetsdato),
                        null,
                        null,
                        kontekstEnhetType,
                        kretser[0].version
                    ),
                    KontekstEgenskaper(
                        ObjektIdentifikator(Lokalid(kretser[1].lokalid), gyldighetsdato),
                        null,
                        null,
                        kontekstEnhetType,
                        kretser[1].version
                    )
                )
            )
            .withGrensetype(grense.grensetype)
            .build()
    }

    private fun utfoerGrenseTilhorighetEndring(
        grense: Grense, nyeTilhorigheter: List<KontekstEgenskaper>
    ): Feature {
        return FeatureBuilder(id = grense.lokalid)
            .withLokalid(grense.lokalid)
            .withGeometry(grense.grense)
            .withGrenseversjon(2)
            .withGrensetype(grense.grensetype)
            .withKontekstEgenskaper(nyeTilhorigheter)
            .build()
    }

    private fun <T : KobletTilGrense> setupKretserForNyeGrenserTest(
        gyldigFra: LocalDate,
        grensetype: Grensetype,
        saveKrets: (String, Lokalid, String, String, LocalDate, LocalDate?, List<Grense>) -> T
    ): List<T> {
        val lagredeGrenserForKretsG1 = grenseRepository.saveAll(GrenseHelper.getGrenserForVestligKrets(gyldigFra, grensetype))
        val lagredeGrenserForKretsG2 = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, grensetype))

        grenseRepository.flush()

        val kretsG1 = saveKrets(
            "G1",
            Lokalid("G1"),
            "03010020",
            "Vestre krets",
            gyldigFra,
            null,
            lagredeGrenserForKretsG1
        )

        val kretsG2 = saveKrets(
            "G2",
            Lokalid("G2"),
            "03010021",
            "Østre krets",
            gyldigFra,
            null,
            lagredeGrenserForKretsG2
        )

        grenseRepository.flush()
        return listOf(kretsG1, kretsG2)
    }

    private fun <T : KobletTilGrense> getTilhorigetTilNyopprettetKretsTestSetup(
        grensetype: Grensetype, gyldigFra: LocalDate, saveKrets: (String, Lokalid, String, String, LocalDate, LocalDate?, List<Grense>) -> T
    ): List<T> {
        val grense1_1 = Grense(id = "grense1_1", lokalid = "grense1_1Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 2.5),
                        Coordinate(0.0, 0.0)
                    )
                )
            }

        val grense1_2 = Grense(id = "grense1_2", lokalid = "grense1_2Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 2.5),
                        Coordinate(0.0, 5.0)
                    )
                )
            }

        val grense2 = Grense(id = "grense2", lokalid = "grense2Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 5.0),
                        Coordinate(0.0, 5.0),
                    )
                )
            }
        val grense3_1 = Grense(id = "grense3_1", lokalid = "grense3_1Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 5.0),
                        Coordinate(5.0, 2.5),
                    )
                )
            }
        val grense3_2 = Grense(id = "grense3_2", lokalid = "grense3_2Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 0.0),
                        Coordinate(5.0, 2.5),
                    )
                )
            }
        val grense4 = Grense(id = "grense4", lokalid = "grense4Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 0.0),
                        Coordinate(0.0, 0.0)
                    )
                )
            }
        val grense5 = Grense(id = "grense5", lokalid = "grense5Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 5.0),
                        Coordinate(7.0, 5.0)
                    )
                )
            }
        val grense6 = Grense(id = "grense6", lokalid = "grense6Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(7.0, 0.0),
                        Coordinate(7.0, 5.0)
                    )
                )
            }
        val grense7 = Grense(id = "grense7", lokalid = "grense7Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(7.0, 0.0),
                        Coordinate(5.0, 0.0)
                    )
                )
            }
        val grense8 = Grense(id = "grense8", lokalid = "grense8Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 2.5),
                        Coordinate(-1.0, 10.0)
                    )
                )
            }
        val grense9 = Grense(id = "grense9", lokalid = "grense9Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(-1.0, 10.0),
                        Coordinate(5.0, 5.0)
                    )
                )
            }

        val dokumentasjonsreferanse = DokumentasjonsreferanseBuilder()
            .fastSettingAt(LocalDate.now())
            .addLenker(dokLink("http://1.no"), dokLink("https://2.no"))
            .addInternRefs(internReferanse("ref1"))
            .build()

        grense3_2.dokumentasjonsreferanser.add(dokumentasjonsreferanse)
        val lagredeGrenserForOpprinneligKrets = grenseRepository.saveAll(listOf(grense1_1, grense1_2, grense2, grense3_1, grense3_2, grense4))
        val lagredeGrenserForOestreKrets = grenseRepository.saveAll(listOf(grense3_2, grense3_1, grense5, grense6, grense7))
        val lagredeGrenserForNordVestKrets = grenseRepository.saveAll(listOf(grense8, grense9, grense2, grense1_2))

        val kretsOpprinnelig = saveKrets("1", Lokalid("Opprinnelig"), "1", "Opprinnelig", gyldigFra, null, lagredeGrenserForOpprinneligKrets)
        val kretsOest = saveKrets("2", Lokalid("Oestre"), "2", "Oestre", gyldigFra, null, lagredeGrenserForOestreKrets)
        val kretsNordVest = saveKrets("3", Lokalid("NordVest"), "3", "NordVest", gyldigFra, null, lagredeGrenserForNordVestKrets)

        return listOf(kretsOpprinnelig, kretsOest, kretsNordVest)
    }

    /**
     *
     *  10    B -------- C -------- F           B -------- C -------- F
     *        |          |          |           |          |          |
     *   5    |    G1    D    G2    |    -->    |    G1    D    G2    |
     *        |          |          |           | ,-------'           |
     *   0    A -------- E -------- G           A -------- E -------- G
     *        |          |                      |          |
     *        |    G3    |                      |    G3    |
     *   -6   H -------- J                      H -------- J
     *        0    5     10   15    20
     *
     *  Grense D-E endres til D-A. Da endres grunnkretsene for grensa A-E fra G1 og G3 til G2 og G3.
     *  G1 hadde 5 grenser før, har 4 grenser etter.
     *  G2 hadde 5 grenser før, har 6 grenser etter.
     *  G3 har samme antall grenser før og etter, skal egentlig ikke være noen endring pÂ G3.
     */

    private fun <T : KobletTilGrense> setupTilhorighetTest(
        gyldigFra: LocalDate,
        grenseType: Grensetype,
        saveKrets: (String, Lokalid, String, String, LocalDate, LocalDate?, List<Grense>) -> T,
        medDeltKommuneGrense: Boolean = false
    ): List<T> {
        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForKretsG1 = grenseRepository.saveAll(GrenseHelper.getGrenserForVestligKrets(gyldigFra, grenseType, medToDelteGrenser = true, medDeltKommuneGrense))
        val lagredeGrenserForKretsG2 = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, grenseType, medToDelteGrenser = true, medDeltKommuneGrense))

        val grenserForKretsG3 = mutableListOf<Grense>()
        grenserForKretsG3.add(Grense(id = "grenseUnderNordVest", lokalid = "grenseUnderNordVestLokalid", grensetype = if (medDeltKommuneGrense) Grensetype.Kommunegrense else grenseType)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 0.0),
                        Coordinate(0.0, -6.0)
                    )
                )
            }
        )
        grenserForKretsG3.add(Grense(id = "grenseUnderSoerVest", lokalid = "grenseUnderSoerVestLokalid", grensetype = if (medDeltKommuneGrense) Grensetype.Kommunegrense else grenseType)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, -6.0),
                        Coordinate(10.0, -6.0)
                    )
                )
            }
        )
        grenserForKretsG3.add(Grense(id = "grenseUnderDelt", lokalid = "grenseUnderDeltLokalid", grensetype = if (medDeltKommuneGrense) Grensetype.Kommunegrense else grenseType)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(10.0, -6.0),
                        Coordinate(10.0, 0.0)
                    )
                )
            }
        )
        val lagredeGrenserForKretsG3 = grenseRepository.saveAll(grenserForKretsG3)
        val grenseSoerVest = lagredeGrenserForKretsG1.first { it.lokalid == "grenseSørVestLokalid" }

        val kretsG1 = saveKrets(
            "G1",
            Lokalid("G1"),
            "03010020",
            "Vestre krets",
            gyldigFra,
            null,
            lagredeGrenserForKretsG1
        )

        val kretsG2 = saveKrets(
            "G2",
            Lokalid("G2"),
            "03010021",
            "Østre krets",
            gyldigFra,
            null,
            lagredeGrenserForKretsG2
        )
        val kretsG3 = saveKrets(
            "G3",
            Lokalid("G3"),
            if (medDeltKommuneGrense) "03020023" else "03010023",
            "Krets under",
            gyldigFra,
            null,
            lagredeGrenserForKretsG3 + grenseSoerVest
        )

        return listOf(kretsG1, kretsG2, kretsG3)
    }


    @Test
    fun `Skal legge til nye referanser i eksisterende dokumentasjonsreferanse`() {
        val gyldighetsdato = LocalDate.now()
        val (kretsOpprinnelig, kretsOest, kretsNordVest) =
            getTilhorigetTilNyopprettetKretsTestSetup(Grensetype.Kommunegrense, gyldighetsdato, ::saveStemmekrets)
        saveKommuneOgUnikLokalid(stemmekretser = listOf(kretsOpprinnelig, kretsOest, kretsNordVest), gyldigFra = gyldighetsdato)

        val grenseSomSkalEndreDokref = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_2Lokalid" }?.grense

        assertThat(grenseSomSkalEndreDokref).isNotNull()

        grenseSomSkalEndreDokref?.dokumentasjonsreferanser?.first()?.addDokumentLenke().apply {
            this?.dokumentlink = "https://3.no"
        }

        val grenseMedNyDokref = FeatureBuilder(id = grenseSomSkalEndreDokref!!.lokalid)
            .copy(grenseSomSkalEndreDokref.grenseToFeature())
            .withDokref(grenseSomSkalEndreDokref.dokumentasjonsreferanser.map { val dokref = it.toDokRef(); dokref.vedtakGyldigFra = LocalDate.now(); dokref })
            .withVersion(2)
            .build()

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features =
            listOf(
                grenseMedNyDokref
            )),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        val persistedGrense = grenseService.hentGrense(Identifikator(lokalid = Lokalid(grenseSomSkalEndreDokref.lokalid), gyldighetsdato = gyldighetsdato))
        assertThat(persistedGrense?.dokumentasjonsreferanser).isNotNull().isNotEmpty()
        assertThat(persistedGrense?.dokumentasjonsreferanser!!.first().dokumentlenker).hasSize(3)
    }

    @Test
    fun `Skal tillate samme tekst i ulike dokrefs internreferanser og lenker`() {
        val gyldighetsdato = LocalDate.now()
        val (kretsOpprinnelig, kretsOest, kretsNordVest) =
            getTilhorigetTilNyopprettetKretsTestSetup(Grensetype.Kommunegrense, gyldighetsdato, ::saveStemmekrets)
        saveKommuneOgUnikLokalid(stemmekretser = listOf(kretsOpprinnelig, kretsOest, kretsNordVest), gyldigFra = gyldighetsdato)

        val grenseSomSkalHaDokref = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_1Lokalid" }?.grense

        assertThat(grenseSomSkalHaDokref).isNotNull()

        val dokrefs = listOf(
            Dokumentasjonsreferanse().apply {
                this.fastsettingsdato = LocalDate.now()
                this.rettskildetittel = "Tittel 1"
                this.addDokumentLenke().apply {
                    this.dokumentlink = "https://google.no"
                }
                this.addInternReferanse().apply {
                    this.internReferanseKartverket = "1"
                }
            },
            Dokumentasjonsreferanse().apply {
                this.fastsettingsdato = LocalDate.now()
                this.rettskildetittel = "Tittel 2"
                this.addDokumentLenke().apply {
                    this.dokumentlink = "https://google.no"
                }
                this.addInternReferanse().apply {
                    this.internReferanseKartverket = "1"
                }
            }
        )

        val grenseMedDokrefs = FeatureBuilder(id = grenseSomSkalHaDokref!!.lokalid)
            .copy(grenseSomSkalHaDokref.grenseToFeature())
            .withDokref(dokrefs.map {
                it.toDokRef().apply {
                    id = null
                    vedtakGyldigFra = LocalDate.now()
                }
            })
            .withVersion(2)
            .build()

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features =
            listOf(
                grenseMedDokrefs
            )),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        val lagretGrense = grenseService.hentGrense(Identifikator(lokalid = Lokalid(grenseSomSkalHaDokref.lokalid), gyldighetsdato = gyldighetsdato))

        assertThat(lagretGrense).isNotNull
        assertThat(lagretGrense!!.dokumentasjonsreferanser).hasSize(2)

        assertThat(lagretGrense.dokumentasjonsreferanser.first().dokumentlenker.first().dokumentlink).isEqualTo("https://google.no")
        assertThat(lagretGrense.dokumentasjonsreferanser.elementAt(1).dokumentlenker.first().dokumentlink).isEqualTo("https://google.no")

        assertThat(lagretGrense.dokumentasjonsreferanser.first().internReferanserKartverket.first().internReferanseKartverket).isEqualTo("1")
        assertThat(lagretGrense.dokumentasjonsreferanser.elementAt(1).internReferanserKartverket.first().internReferanseKartverket).isEqualTo("1")
    }

}
