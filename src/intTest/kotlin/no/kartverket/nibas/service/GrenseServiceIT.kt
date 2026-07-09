package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.dokLink
import no.kartverket.nibas.api.v1.common.geojson.metadata.internReferanse
import no.kartverket.nibas.builders.BopliktomraadeRequestBuilder
import no.kartverket.nibas.builders.DokumentasjonsreferanseBuilder
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.geometry.UgyldigeFlaterException
import no.kartverket.nibas.domain.DokrefGrenseKobling
import no.kartverket.nibas.common.geometry.topologyValidationErrorToCustomText
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.BopliktomraadeGrenser
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.KobletTilGrense
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.extensions.geometry.geometryToDto
import no.kartverket.nibas.extensions.geometry.grenseToFeature
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.utils.GrenseHelper
import no.kartverket.nibas.utils.GrenseHelper.getGrenseRundtVestreOgOestreKrets
import no.kartverket.nibas.utils.GrenseHelper.getGrenserForOestligKrets
import no.kartverket.nibas.utils.GrenseHelper.getGrenserForVestligKrets
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.operation.valid.TopologyValidationError
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertFailsWith


@ActiveProfiles("localhost")
@Transactional
class GrenseServiceIT : TestWithDbContainer() {

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)

    @Test
    fun hentEksisterendeGrenseMedDagensDato() {
        val lokalIdOfGrense = Lokalid("ac7e05ca-fb65-4277-ad95-63c32c1370d0")
        saveGrense(lokalid = lokalIdOfGrense)
        assertThat(grenseRepository.findAllByLokalid(lokalIdOfGrense.value)).hasSize(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(grenseService.hentGrense(Identifikator(lokalIdOfGrense, november28In2022))).isNotNull
    }

    @Test
    fun hentVersjonAvKommunegrenseFramITid() {
        val lokalIdOfGrense = Lokalid("ac7e05ca-fb65-4277-ad95-63c32c1370d0")
        saveGrense(id = "9000", lokalid = lokalIdOfGrense, gyldigTil = jan1In2023)
        saveGrense(id = "9001", lokalid = lokalIdOfGrense, gyldigFra = jan1In2023, opphav = "Oslo")
        assertThat(grenseRepository.findAllByLokalid(lokalIdOfGrense.value)).hasSize(2)

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

        val stemmekretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(kretsG1.lokalid), gyldighetsdato))!!
        val stemmekrets2EtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(kretsG2.lokalid), gyldighetsdato))!!
        assertThat(stemmekretsEtterLagring.grenser).hasSize(5).extracting("grense.id").doesNotContain("grenseDelt2Id", "grenseDelt")
        assertThat(stemmekretsEtterLagring.grenser).extracting("grense.id").contains("grenseVest", "grenseNordVest", "grenseSørVest")
        assertThat(stemmekretsEtterLagring.grenser).extracting("grense.lokalid").contains("grenseDeltLokalid")

        assertThat(stemmekrets2EtterLagring.grenser).hasSize(5).extracting("grense.id").doesNotContain("grenseDelt2Id", "grenseDelt")
        assertThat(stemmekrets2EtterLagring.grenser).extracting("grense.lokalid").contains("grenseDeltLokalid")
        assertThat(stemmekrets2EtterLagring.grenser).extracting("grense.id").contains("grenseØst", "grenseNordØst", "grenseSørØst")
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

        val exception = assertFailsWith<UgyldigeFlaterException> {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(featureForGrenseSomSkalEndres, featureForNyGrense)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(2)
        ugyldigeFlater.find { it.lokalid == kretsG1.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(kretsG1.getFlateInformasjon()) }
        ugyldigeFlater.find { it.lokalid == kretsG2.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(kretsG2.getFlateInformasjon()) }

        ugyldigeFlater.forEach {
            assertThat(it.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
            assertThat(it.koordinat).isEqualTo(Coordinate(10.0, 5.0))
        }
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

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(kretsG1.lokalid), gyldighetsdato))!!
        val grunnkrets2EtterLagring = grunnkretsService.hentGrunnkrets((Identifikator(Lokalid(kretsG2.lokalid), gyldighetsdato)))!!
        assertThat(grunnkretsEtterLagring.grenser).hasSize(5).extracting("grense.id").doesNotContain("grenseDelt2Id", "grenseDelt")
        assertThat(grunnkretsEtterLagring.grenser).extracting("grense.id").contains("grenseVest", "grenseNordVest", "grenseSørVest")
        assertThat(grunnkretsEtterLagring.grenser).extracting("grense.lokalid").contains("grenseDeltLokalid")

        assertThat(grunnkrets2EtterLagring.grenser).hasSize(5).extracting("grense.id").doesNotContain("grenseDelt2Id", "grenseDelt")
        assertThat(grunnkrets2EtterLagring.grenser).extracting("grense.lokalid").contains("grenseDeltLokalid")
        assertThat(grunnkrets2EtterLagring.grenser).extracting("grense.id").contains("grenseØst", "grenseNordØst", "grenseSørØst")
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

        val exception = assertFailsWith<UgyldigeFlaterException> {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(featureForGrenseSomSkalEndres, featureForNyGrense)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(2)
        ugyldigeFlater.find { it.lokalid == kretsG1.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(kretsG1.getFlateInformasjon()) }
        ugyldigeFlater.find { it.lokalid == kretsG2.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(kretsG2.getFlateInformasjon()) }

        ugyldigeFlater.forEach {
            assertThat(it.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
            assertThat(it.koordinat).isEqualTo(Coordinate(10.0, 5.0))
        }
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

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(kretsG1.lokalid), gyldighetsdato))!!
        val grunnkrets2EtterLagring = grunnkretsService.hentGrunnkrets((Identifikator(Lokalid(kretsG2.lokalid), gyldighetsdato)))!!
        assertThat(grunnkretsEtterLagring.grenser).hasSize(4).extracting("grense.id").doesNotContain("grenseDelt2Id", "grenseDelt")
        assertThat(grunnkretsEtterLagring.grenser).extracting("grense.id").contains("grenseVest", "grenseNordVest", "grenseSørVest")
        assertThat(grunnkretsEtterLagring.grenser).extracting("grense.lokalid").doesNotContain("grenseDeltLokalid")

        assertThat(grunnkrets2EtterLagring.grenser).hasSize(4).extracting("grense.id").doesNotContain("grenseDelt2Id", "grenseDelt")
        assertThat(grunnkrets2EtterLagring.grenser).extracting("grense.lokalid").doesNotContain("grenseDeltLokalid")
        assertThat(grunnkrets2EtterLagring.grenser).extracting("grense.id").contains("grenseØst", "grenseNordØst", "grenseSørØst")
    }

    @Test
    fun arkiverStemmekretsgrenseUtenAaLukkeKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val stemmekretsRequest = StemmekretsRequestBuilder.DEFAULT_REQUEST
        val identifikator = Identifikator(Lokalid(stemmekretsRequest.identifikasjon.lokalid), gyldigFra)
        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, Grensetype.Stemmekretsgrense))
        grenseRepository.flush()

        val stemmekrets = saveStemmekrets(id = "1", lokalid = identifikator.lokalid, stemmekretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)
        val stemmekrets2 = saveStemmekrets(id = "2", lokalid = identifikator.lokalid.copy("lokal2"), stemmekretsnummer = "30", gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(stemmekretser = listOf(stemmekrets, stemmekrets2), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val grenseSomSkalArkiveres = stemmekrets.grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense
        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)

        val exception = assertFailsWith<UgyldigeFlaterException> {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(arkivertFeature)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(2)
        ugyldigeFlater.find { it.lokalid == stemmekrets.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(stemmekrets.getFlateInformasjon()) }
        ugyldigeFlater.find { it.lokalid == stemmekrets2.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(stemmekrets2.getFlateInformasjon()) }

        ugyldigeFlater.forEach {
            assertThat(it.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
            assertThat(it.koordinat).isEqualTo(Coordinate(0.0, 5.0))
        }
    }

    @Test
    fun arkiverStemmekretsgrenseOgLukkKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val lokalid1 = Lokalid("1")
        val lokalid2 = Lokalid("2")

        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, Grensetype.Stemmekretsgrense))
        grenseRepository.flush()

        val stemmekrets = saveStemmekrets(id = "1", lokalid = lokalid1, stemmekretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)
        val stemmekrets2 = saveStemmekrets(id = "2", lokalid = lokalid2, stemmekretsnummer = "21", gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(stemmekretser = listOf(stemmekrets, stemmekrets2), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val grenseSomSkalArkiveres = stemmekrets.grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense
        val grenseSomSkalJusteres = stemmekrets.grenser.first { it.grense.lokalid == "grense2Lokalid" }.grense

        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)
        val featureMedEndretGeometri = utfoerGrensejusteringEtterArkivering(grenseSomSkalJusteres, gyldighetsdato)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(arkivertFeature, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
        assertThat(flateSet).hasSize(2)
        val flate = flateSet.firstOrNull { it.lokalid == lokalid1.value }
        assertThat(flate).isNotNull

        val stemmekretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(flate!!.lokalid), gyldighetsdato))!!
        assertThat(stemmekretsEtterLagring.grenser).hasSize(3).extracting("grense.lokalid")
            .doesNotContain("grense1Lokalid")
    }

    @Test
    fun arkiverGrunnkretsgrenseUtenAaLukkeKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val grunnkretsRequest = GrunnkretsRequestBuilder.DEFAULT_REQUEST
        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), gyldigFra)
        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, Grensetype.Grunnkretsgrense))
        grenseRepository.flush()

        val grunnkrets = saveGrunnkrets(id = "1", lokalid = identifikator.lokalid, grunnkretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)
        val naboGrunnkrets = saveGrunnkrets(id = "2", lokalid = Lokalid("nabo"), grunnkretsnummer = "21", gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(grunnkretser = listOf(grunnkrets, naboGrunnkrets), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val grenseSomSkalArkiveres = grunnkrets.grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense
        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)

        val exception = assertFailsWith<UgyldigeFlaterException> {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(arkivertFeature)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(2)
        ugyldigeFlater.find { it.lokalid == grunnkrets.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(grunnkrets.getFlateInformasjon()) }
        ugyldigeFlater.find { it.lokalid == naboGrunnkrets.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(naboGrunnkrets.getFlateInformasjon()) }

        ugyldigeFlater.forEach {
            assertThat(it.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
            assertThat(it.koordinat).isEqualTo(Coordinate(0.0, 5.0))
        }
    }

    @Test
    fun arkiverGrunnkretsgrenseOgLukkKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val lokalid1 = Lokalid("1")
        val lokalid2 = Lokalid("2")

        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, Grensetype.Grunnkretsgrense))
        grenseRepository.flush()

        val grunnkrets = saveGrunnkrets(id = "1", lokalid = lokalid1, grunnkretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)
        val grunnkrets2 = saveGrunnkrets(id = "2", lokalid = lokalid2, gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(grunnkretser = listOf(grunnkrets, grunnkrets2), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val grenseSomSkalArkiveres = grunnkrets.grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense
        val grenseSomSkalJusteres = grunnkrets.grenser.first { it.grense.lokalid == "grense2Lokalid" }.grense

        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)
        val featureMedEndretGeometri = utfoerGrensejusteringEtterArkivering(grenseSomSkalJusteres, gyldighetsdato)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(arkivertFeature, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        assertThat(flateSet).hasSize(2)
        val flate = flateSet.firstOrNull { it.lokalid == lokalid1.value }
        assertThat(flate).isNotNull

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(flate!!.lokalid), gyldighetsdato))!!
        assertThat(grunnkretsEtterLagring.grenser).hasSize(3).extracting("grense.lokalid")
            .doesNotContain("grense1Lokalid")
    }

    private fun utfoerGrensejusteringEtterArkivering(grenseSomSkalJusteres: Grense, gyldighetsdato: LocalDate): Feature {
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
    fun getGrenserForEnkelKrets(gyldigFra: LocalDate, grensetype: Grensetype): List<Grense> {

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
        val (kretsG1, kretsG2, kretsG3) = setupTilhorighetTest(gyldighetsdato, Grensetype.Grunnkretsgrense, ::saveGrunnkrets, ::saveGrunnkrets)

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
    fun `lagreGrenser klarer å endre grensetype`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        // oppretter tre kretser som deler grenser der én grense har feil grensekoblinger grunnet at en annen grense har fått endret geometrien sin.
        val (kretsG1, kretsG2, kretsG3) = setupTilhorighetTest(gyldighetsdato, Grensetype.Grunnkretsgrense, ::saveGrunnkrets, ::saveGrunnkrets)

        saveKommuneOgUnikLokalid(grunnkretser = listOf(kretsG1, kretsG2, kretsG3), gyldigFra = gyldighetsdato)

        // Fra krets finner vi fram til de to grensene som har endringer i dette fiktive utkastet
        val grenseSomSkalEndreTilhorighet = kretsG1.grenser.map { it.grense }.first { it.lokalid == "grenseSørVestLokalid" }
        assertThat(grenseSomSkalEndreTilhorighet.grensetype).isEqualTo(Grensetype.Grunnkretsgrense)
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
        val featureMedEndretTilhorighet = utfoerGrenseTilhorighetEndring(grenseSomSkalEndreTilhorighet, nyeTilhorigheter, Grensetype.Delomraadegrense)
        val featureMedEndretGeometri = utfoerGeometriEndring(grenseSomEndreGeometrien, grenseSomEndreGeometrien.grunnkretser.map { it.grunnkrets }, gyldighetsdato, KontekstEnhetType.GRUNNKRETS)

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureMedEndretTilhorighet, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )

        val oppdatertGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseSørVestLokalid"), gyldighetsdato))!!
        assertThat(oppdatertGrense.id).isNotEqualTo(grenseSomSkalEndreTilhorighet.id)
        assertThat(oppdatertGrense.grensetype).isEqualTo(Grensetype.Delomraadegrense)
    }

    @Test
    fun `lagreGrenser klarer å endre tilhørighet på kommunegrense med grensekoblinger til grunnkrets og stemmekrets`() {
        val gyldighetsdato = LocalDate.now()
        val (kretsG1, kretsG2, kretsG3, kretsS1, kretsS2) =
            setupTilhorighetTest(gyldighetsdato, Grensetype.Grunnkretsgrense, ::saveGrunnkrets, ::saveStemmekrets, medDeltKommuneGrense = true)

        val kommuneNord = saveKommuneOgUnikLokalid(id = "kommuneNord", lokalid = "kommuneNord", kommunenummer = "0301", kommunenummerUuid = "0301",
            grunnkretser = listOf(kretsG1 as Grunnkrets, kretsG2 as Grunnkrets), stemmekretser = listOf(kretsS1 as Stemmekrets), gyldigFra = gyldighetsdato)
        val kommuneSoer = saveKommuneOgUnikLokalid(id = "kommuneSoer", lokalid = "kommuneSoer", kommunenummer = "0302", kommunenummerUuid = "0302",
            grunnkretser = listOf(kretsG3 as Grunnkrets), stemmekretser = listOf(kretsS2 as Stemmekrets), gyldigFra = gyldighetsdato)
        saveFylke(kommuner = listOf(kommuneNord, kommuneSoer), gyldigFra = gyldighetsdato)

        grenseRepository.flush()
        flateService.oppdaterFlaterFor(setOf(kommuneSoer, kommuneNord))
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
        ), KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("S1"), gyldighetsdato),
            type = KontekstEnhetType.STEMMEKRETS,
            version = 1
        ), KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("S2"), gyldighetsdato),
            type = KontekstEnhetType.STEMMEKRETS,
            version = 1
        ))

        val featureMedEndretTilhorighet = utfoerGrenseTilhorighetEndring(grenseSomSkalEndreTilhorighet, nyeTilhorigheter)
        val featureMedEndretGeometri = utfoerGeometriEndring(grenseSomEndreGeometrien, grenseSomEndreGeometrien.grunnkretser.map { it.grunnkrets },
            gyldighetsdato, KontekstEnhetType.GRUNNKRETS)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureMedEndretTilhorighet, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )

        assertGrunnkretsTilhorighetTest(flateSet, 7)
        assertThat(flateSet.map { it.lokalid }).containsAll(listOf("S1", "S2", "kommuneNord", "kommuneSoer"))
        (flateSet.first { it.lokalid == "S2" } as Stemmekrets).run {
            assertThat(this.id).isNotEqualToIgnoringCase("S2")
            assertThat(this.grenser).hasSize(4)
        }
        (flateSet.first { it.lokalid == "S1" } as Stemmekrets).run {
            assertThat(this.grenser).hasSize(6)
            assertThat(this.id).isNotEqualToIgnoringCase("S1")
            val grenseSomFikkEndretTilhorighet = this.grenser.find { it.grense.lokalid == "grenseSørVestLokalid" }!!.grense
            assertThat(grenseSomFikkEndretTilhorighet).isNotNull
            assertThat(grenseSomFikkEndretTilhorighet.stemmekretser).hasSize(2)
            assertThat(grenseSomFikkEndretTilhorighet.stemmekretser.map { it.stemmekrets.lokalid }).containsAll(listOf("S1", "S2"))
            assertThat(grenseSomFikkEndretTilhorighet.grunnkretser).hasSize(2)
            assertThat(grenseSomFikkEndretTilhorighet.grunnkretser.map { it.grunnkrets.lokalid }).containsAll(listOf("G2", "G3"))
        }
        (flateSet.first { it.lokalid == "kommuneNord" } as Kommune).run {
            assertThat(this.id).isNotEqualToIgnoringCase("kommuneNord")
        }
        (flateSet.first { it.lokalid == "kommuneSoer" } as Kommune).run {
            assertThat(this.id).isNotEqualToIgnoringCase("kommuneSoer")
        }
    }

    private fun assertGrunnkretsTilhorighetTest(flateSet: Set<Flate>, size: Int) {
        assertThat(flateSet).hasSize(size)
        assertThat(flateSet.map { it.lokalid }).containsAll(listOf("G1", "G2", "G3"))
        (flateSet.first { it.lokalid == "G1" } as Grunnkrets).run {
            assertThat(this.grenser).hasSize(4)
            assertThat(this.grenser.find { it.grense.lokalid == "grenseSørVestLokalid" }).isEqualTo(null)
        }
        (flateSet.first { it.lokalid == "G2" } as Grunnkrets).run {
            assertThat(this.grenser).hasSize(6)
            val grenseSomFikkEndretTilhorighet = this.grenser.find { it.grense.lokalid == "grenseSørVestLokalid" }!!.grense
            assertThat(grenseSomFikkEndretTilhorighet).isNotNull
            assertThat(grenseSomFikkEndretTilhorighet.grunnkretser).hasSize(2)
            assertThat(grenseSomFikkEndretTilhorighet.grunnkretser.map { it.grunnkrets.lokalid }).containsAll(listOf("G2", "G3"))
        }
        (flateSet.first { it.lokalid == "G3" } as Grunnkrets).run {
            assertThat(this.grenser).hasSize(4)
        }
    }

    @Test
    fun `lagreGrenser skal kaste exception hvis det ikke går å identifisere nyopprettet grunnkrets`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        val kretser = getTilhorighetTilNyopprettetKretsTestSetup(Grensetype.Grunnkretsgrense, gyldighetsdato, ::saveGrunnkrets, ::saveGrunnkrets)

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
        }.isInstanceOf(RuntimeException::class.java).hasMessageContaining("Kretsnummer må være satt når man setter tilhørighet til en ny grunnkrets.")
    }

    @Test
    fun `lagreGrenser klarer å endre tilhørighet på stemmekretsgrense`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        // oppretter tre kretser som deler grenser der én grense har feil grensekoblinger grunnet at en annen grense har fått endret geometrien sin.
        val (kretsG1, kretsG2, kretsG3) = setupTilhorighetTest(gyldighetsdato, Grensetype.Stemmekretsgrense, ::saveStemmekrets, ::saveStemmekrets)

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
        val featureMedEndretGeometri = utfoerGeometriEndring(grenseSomEndreGeometrien, grenseSomEndreGeometrien.stemmekretser.map { it.stemmekrets },
            gyldighetsdato, KontekstEnhetType.STEMMEKRETS)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureMedEndretTilhorighet, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )

        assertThat(flateSet).hasSize(3)
        assertThat(flateSet.map { it.lokalid }).containsAll(listOf("G1", "G2", "G3"))
        (flateSet.first { it.lokalid == "G1" } as Stemmekrets).run {
            assertThat(this.grenser).hasSize(4)
            assertThat(this.grenser.find { it.grense.lokalid == "grenseSørVestLokalid" }).isEqualTo(null)
        }
        (flateSet.first { it.lokalid == "G2" } as Stemmekrets).run {
            assertThat(this.grenser).hasSize(6)
            val grenseSomFikkEndretTilhorighet = this.grenser.find { it.grense.lokalid == "grenseSørVestLokalid" }!!.grense
            assertThat(grenseSomFikkEndretTilhorighet).isNotNull
            assertThat(grenseSomFikkEndretTilhorighet.stemmekretser).hasSize(2)
            assertThat(grenseSomFikkEndretTilhorighet.stemmekretser.map { it.stemmekrets.lokalid }).containsAll(listOf("G2", "G3"))
        }
        (flateSet.first { it.lokalid == "G3" } as Stemmekrets).run {
            assertThat(this.grenser).hasSize(4)
        }

    }

    @Test
    fun `lagreGrenser klarer å endre tilhørighet på stemmekretsgrense til nyopprettet stemmekrets`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        val (kretsOpprinnelig, kretsOest, kretsNordVest) =
            getTilhorighetTilNyopprettetKretsTestSetup(Grensetype.Stemmekretsgrense, gyldighetsdato, ::saveStemmekrets, ::saveGrunnkrets)

        // når kretsen er ny vil den ligget i databasen på det tidspunktet lagreGrenser blir kjørt.
        val nyStemmekrets = saveStemmekrets("nyStemmekrets", Lokalid("nyStemmekrets"), stemmekretsnummer = "420", stemmekretsnavn = "nyStemmekrets", gyldigFra = gyldighetsdato)

        val kommune = saveKommuneOgUnikLokalid(gyldigFra = gyldighetsdato,
            stemmekretser = listOf(kretsOpprinnelig as Stemmekrets, kretsOest as Stemmekrets, kretsNordVest as Stemmekrets, nyStemmekrets))

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

        assertThat(flateSet).hasSize(4)
        assertThat(flateSet.map { it.lokalid }).containsAll(listOf("nyStemmekrets", "Opprinnelig", "Oestre", "NordVest"))
        (flateSet.first { it.lokalid == "nyStemmekrets" } as Stemmekrets).run {
            assertThat(this.grenser).hasSize(3)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(listOf("grense1_2Lokalid", "grense2Lokalid", "grense3_1Lokalid"))
        }
        (flateSet.first { it.lokalid == "Opprinnelig" } as Stemmekrets).run {
            assertThat(this.grenser).hasSize(3)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(listOf("grense1_1Lokalid", "grense3_2Lokalid", "grense4Lokalid"))
        }
        (flateSet.first { it.lokalid == "Oestre" } as Stemmekrets).run {
            assertThat(this.grenser).hasSize(5)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(kretsOest.grenser.map { it.grense.lokalid })
        }
        (flateSet.first { it.lokalid == "NordVest" } as Stemmekrets).run {
            assertThat(this.grenser).hasSize(4)
            assertThat(this.grenser.map { it.grense.lokalid }).containsAll(kretsNordVest.grenser.map { it.grense.lokalid })
        }
    }

    @Test
    fun `lagreGrenser skal kaste exception hvis kontektst mangler kommuneidentifikator for nyopprettet stemmekrets`() {
        val gyldighetsdato = LocalDate.parse("2022-01-01")

        val (kretsOpprinnelig, kretsOest, kretsNordVest) =
            getTilhorighetTilNyopprettetKretsTestSetup(Grensetype.Stemmekretsgrense, gyldighetsdato, ::saveStemmekrets, ::saveStemmekrets)

        // når kretsen er ny vil den ligget i databasen på det tidspunktet lagreGrenser blir kjørt.
        val nyStemmekrets = saveStemmekrets("nyStemmekrets", Lokalid("nyStemmekrets"), stemmekretsnummer = "420", stemmekretsnavn = "nyStemmekrets", gyldigFra = gyldighetsdato)

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

    @Test
    fun `endre kommunegrense skal opprette ny versjon for grensa og dens stemmekrets og grunnkrets og kommune`() {
        val (vestreGrunnkrets, oestreGrunnkrets) = saveNaboGrunnkretserOgStemmekretserIHverSinKommune(gyldigFra = october1In2022)

        val kommuneVest = vestreGrunnkrets.getKommune()!!
        val kommuneOest = oestreGrunnkrets.getKommune()!!

        val vestreStemmekrets = kommuneVest.getStemmekretser(october1In2022).first()
        val oestreStemmekrets = kommuneOest.getStemmekretser(october1In2022).first()
        grenseRepository.flush()

        assertThat(vestreGrunnkrets.gyldighet.gyldigTil).isNull()
        assertThat(kommuneVest.gyldighet.gyldigTil).isNull()
        assertTopologi(vestreGrunnkrets.omraade, kommuneVest.omraade)

        val grenseKobling = vestreGrunnkrets.getGrensekoblinger(gyldighetsdato = october1In2022).first { it.grense.lokalid == "grenseDeltLokalid" }
        val grense = grenseKobling.grense.grense

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            grense.getCoordinateN(0).copy(),
            Coordinate(13.0, 5.0),
            grense.getCoordinateN(2).copy()
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(october1In2022)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }
        assertThat(oppdatertGrensefeature.properties.kontekstEgenskaper).hasSize(4)
        assertThat(oppdatertGrensefeature.properties.kontekstEgenskaper.map { Pair(it.type, it.id!!.lokalid.value) })
            .containsAll(listOf(Pair(KontekstEnhetType.GRUNNKRETS, vestreGrunnkrets.lokalid), Pair(KontekstEnhetType.GRUNNKRETS, oestreGrunnkrets.lokalid),
                Pair(KontekstEnhetType.STEMMEKRETS, vestreStemmekrets.lokalid), Pair(KontekstEnhetType.STEMMEKRETS, oestreStemmekrets.lokalid)))

        val flaterSomErEndret = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(oppdatertGrensefeature)),
            gyldighetsdato = november28In2022,
            endringstype = Endringstype.KVALITETSHEVING
        )

        val vestreGrunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreGrunnkrets.lokalid), november28In2022))!!
        val vestreStemmekretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreStemmekrets.lokalid), november28In2022))!!
        val vestreKommuneEtterLagring = kommuneService.hentKommune(Identifikator(Lokalid(kommuneVest.lokalid), november28In2022))!!

        assertTopologi(vestreKommuneEtterLagring.omraade, vestreGrunnkretsEtterLagring.omraade)
        assertTopologi(vestreKommuneEtterLagring.omraade, vestreStemmekretsEtterLagring.omraade)
        assertThat(flaterSomErEndret).hasSize(6).extracting("id")
            .contains(vestreGrunnkretsEtterLagring.id, vestreKommuneEtterLagring.id, vestreStemmekretsEtterLagring.id)
        assertOmradeGyldighetVersjon(vestreGrunnkretsEtterLagring, vestreGrunnkrets, november28In2022, Endringstype.KVALITETSHEVING)
        assertOmradeGyldighetVersjon(vestreStemmekretsEtterLagring, vestreStemmekrets, november28In2022, Endringstype.KVALITETSHEVING)
        assertOmradeGyldighetVersjon(vestreKommuneEtterLagring, kommuneVest, november28In2022, Endringstype.KVALITETSHEVING)
    }

    @Test
    fun `endre fylkegrense for grunnkrets skal opprette ny versjon for grensa og dens stemmekrets og grunnkrets og kommune og fylke`() {
        val (vestreGrunnkrets, oestreGrunnkrets) = saveNaboGrunnkretserOgStemmekretserIHverSinKommune(gyldigFra = october1In2022,
            kommunenummer1 = "3201", kommunenummer2 = "3301")

        val kommuneVest = vestreGrunnkrets.getKommune()!!
        val fylkeVest = kommuneVest.getFylke()!!
        val kommuneOest = oestreGrunnkrets.getKommune()!!

        val vestreStemmekrets = kommuneVest.getStemmekretser(october1In2022).first()
        val oestreStemmekrets = kommuneOest.getStemmekretser(october1In2022).first()
        grenseRepository.flush()

        assertThat(vestreGrunnkrets.gyldighet.gyldigTil).isNull()
        assertThat(kommuneVest.gyldighet.gyldigTil).isNull()
        assertTopologi(vestreGrunnkrets.omraade, kommuneVest.omraade)
        assertTopologi(vestreGrunnkrets.omraade, fylkeVest.omraade)

        val grenseKobling = vestreGrunnkrets.getGrensekoblinger(gyldighetsdato = october1In2022).first { it.grense.lokalid == "grenseDeltLokalid" }
        val grense = grenseKobling.grense.grense

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            grense.getCoordinateN(0).copy(),
            Coordinate(13.0, 5.0),
            grense.getCoordinateN(2).copy()
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(october1In2022)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }
        assertThat(oppdatertGrensefeature.properties.kontekstEgenskaper).hasSize(4)
        assertThat(oppdatertGrensefeature.properties.kontekstEgenskaper.map { Pair(it.type, it.id!!.lokalid.value) })
            .containsAll(listOf(Pair(KontekstEnhetType.GRUNNKRETS, vestreGrunnkrets.lokalid), Pair(KontekstEnhetType.GRUNNKRETS, oestreGrunnkrets.lokalid),
                Pair(KontekstEnhetType.STEMMEKRETS, vestreStemmekrets.lokalid), Pair(KontekstEnhetType.STEMMEKRETS, oestreStemmekrets.lokalid)))

        val flaterSomErEndret = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(oppdatertGrensefeature)),
            gyldighetsdato = november28In2022,
            endringstype = Endringstype.KVALITETSHEVING
        )

        val vestreGrunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreGrunnkrets.lokalid), november28In2022))!!
        val vestreStemmekretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreStemmekrets.lokalid), november28In2022))!!
        val vestreKommuneEtterLagring = kommuneService.hentKommune(Identifikator(Lokalid(kommuneVest.lokalid), november28In2022))!!
        val fylkeEtterLagring = fylkeService.hentFylke(Identifikator(Lokalid(fylkeVest.lokalid), november28In2022))!!

        assertTopologi(vestreKommuneEtterLagring.omraade, vestreGrunnkretsEtterLagring.omraade)
        assertTopologi(vestreKommuneEtterLagring.omraade, vestreStemmekretsEtterLagring.omraade)
        assertThat(flaterSomErEndret).hasSize(8).extracting("id")
            .contains(vestreGrunnkretsEtterLagring.id, vestreKommuneEtterLagring.id, vestreStemmekretsEtterLagring.id, fylkeEtterLagring.id)

        assertOmradeGyldighetVersjon(vestreGrunnkretsEtterLagring, vestreGrunnkrets, november28In2022, Endringstype.KVALITETSHEVING)
        assertOmradeGyldighetVersjon(vestreStemmekretsEtterLagring, vestreStemmekrets, november28In2022, Endringstype.KVALITETSHEVING)
        assertOmradeGyldighetVersjon(vestreKommuneEtterLagring, kommuneVest, november28In2022, Endringstype.KVALITETSHEVING)
        assertOmradeGyldighetVersjon(fylkeEtterLagring, fylkeVest, november28In2022, Endringstype.KVALITETSHEVING)
    }

    @Test
    fun `endre delt kommunegrense etter oppdatering av grunnkrets paa andre sida skal opprette nye versjoner for grensa og dens grunnkretser og stemmekretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretserOgStemmekretserIHverSinKommune(gyldigFra = opprinneligDato)

        val vestreKommune = vestreKrets.getKommune(opprinneligDato)!!
        val oestreKommune = oestreKrets.getKommune(opprinneligDato)!!

        val vestreStemmekrets = vestreKommune.getStemmekretser(opprinneligDato).first()
        val oestreStemmekrets = oestreKommune.getStemmekretser(opprinneligDato).first()
        grenseRepository.flush()
        val deltGrenseFoer = grenseRepository.findById("grenseDelt").get()
        //Sjekker at det går an å lage feature
        deltGrenseFoer.grenseToFeature(opprinneligDato)

        val oestreGrunnkretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = oestreKrets, datoForOppdatering = datoForOppdatering)!!
        grenseRepository.flush()

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))!!
        assertThat(deltGrense.stemmekretser).hasSize(2)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(vestreStemmekrets.id, oestreStemmekrets.id)
        assertThat(deltGrense.grunnkretser).hasSize(3)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreGrunnkretsEtterOppdatering.id)
        assertThat(deltGrense.grunnkretser(datoForOppdatering).map { it.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreGrunnkretsEtterOppdatering.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))!!
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))!!
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))!!
        val vestreStemmekretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreStemmekrets.lokalid), datoForGrenseendring))!!
        val oestreStemmekretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreStemmekrets.lokalid), datoForGrenseendring))!!
        val vestreKommuneEtterLagring = kommuneService.hentKommune(Identifikator(Lokalid(vestreKommune.lokalid), datoForGrenseendring))!!
        val oestreKommuneEtterLagring = kommuneService.hentKommune(Identifikator(Lokalid(oestreKommune.lokalid), datoForGrenseendring))!!

        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, opprinneligVestreKrets, datoForGrenseendring, Endringstype.VEDTATT_GRENSEJUSTERING)
        assertOmradeGyldighetVersjon(oestreKretsEtterLagring, opprinneligOestreKrets, datoForGrenseendring, Endringstype.VEDTATT_GRENSEJUSTERING)
        assertOmradeGyldighetVersjon(vestreStemmekretsEtterLagring, vestreStemmekrets, datoForGrenseendring, Endringstype.VEDTATT_GRENSEJUSTERING)
        assertOmradeGyldighetVersjon(oestreStemmekretsEtterLagring, oestreStemmekrets, datoForGrenseendring, Endringstype.VEDTATT_GRENSEJUSTERING)

        assertThat(flaterSomErEndret).hasSize(6).extracting("id")
            .contains(vestreKommuneEtterLagring.id, oestreKommuneEtterLagring.id, vestreKretsEtterLagring.id, oestreKretsEtterLagring.id,
                vestreStemmekretsEtterLagring.id, oestreStemmekretsEtterLagring.id)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!

        //Sjekker at gammel versjon av grensa ikke har fått endra sine grunnkretser
        assertThat(opprinneligDeltGrense.grunnkretser).hasSize(3)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreGrunnkretsEtterOppdatering.id)
        assertThat(opprinneligDeltGrense.stemmekretser).hasSize(2)
        assertThat(opprinneligDeltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(vestreStemmekrets.id, oestreStemmekrets.id)

        val deltGrenseEtterGrenseendring = assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, datoForGrenseendring, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertThat(deltGrenseEtterGrenseendring.stemmekretser).hasSize(2)
        assertThat(deltGrenseEtterGrenseendring.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(vestreStemmekretsEtterLagring.id, oestreStemmekretsEtterLagring.id)
        assertThat(vestreStemmekretsEtterLagring.grenser.map { it.grense.lokalid }).contains(deltGrenseLokalid)
        assertThat(oestreStemmekretsEtterLagring.grenser.map { it.grense.lokalid }).contains(deltGrenseLokalid)
        assertThat(vestreStemmekretsEtterLagring.grenser.map { it.grense.id }).doesNotContain("grenseDelt")
        assertThat(oestreStemmekretsEtterLagring.grenser.map { it.grense.id }).doesNotContain("grenseDelt")
        assertThat(vestreStemmekrets.grenser.map { it.grense.id }).contains("grenseDelt")
        assertThat(oestreStemmekrets.grenser.map { it.grense.id }).contains("grenseDelt")
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
        grense: Grense, nyeTilhorigheter: List<KontekstEgenskaper>, evtNyGrensetype: Grensetype? = null
    ): Feature {
        return FeatureBuilder(id = grense.lokalid)
            .withLokalid(grense.lokalid)
            .withGeometry(grense.grense)
            .withGrenseversjon(2)
            .withGrensetype(evtNyGrensetype ?: grense.grensetype)
            .withKontekstEgenskaper(nyeTilhorigheter)
            .build()
    }

    private fun <T : KobletTilGrense> setupKretserForNyeGrenserTest(
        gyldigFra: LocalDate,
        grensetype: Grensetype,
        saveKrets: (String, Lokalid, String, String, LocalDate, LocalDate?, List<Grense>) -> T
    ): List<T> {
        val lagredeGrenserForKretsG1 = grenseRepository.saveAll(getGrenserForVestligKrets(gyldigFra, grensetype))
        val lagredeGrenserForKretsG2 = grenseRepository.saveAll(getGrenserForOestligKrets(gyldigFra, grensetype))

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

    private fun <T : KobletTilGrense> getTilhorighetTilNyopprettetKretsTestSetup(grensetype: Grensetype, gyldigFra: LocalDate,
        saveKrets: (String, Lokalid, String, String, LocalDate, LocalDate?, List<Grense>) -> T,
        saveKrets2: (String, Lokalid, String, String, LocalDate, LocalDate?, List<Grense>) -> T
    ): List<T> {
        val grense1_1 = grenseRepository.save(Grense(id = "grense1_1", lokalid = "grense1_1Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 2.5),
                        Coordinate(0.0, 0.0)
                    )
                )
            })

        val grense1_2 = grenseRepository.save(Grense(id = "grense1_2", lokalid = "grense1_2Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 2.5),
                        Coordinate(0.0, 5.0)
                    )
                )
            })

        val grense2 = grenseRepository.save(Grense(id = "grense2", lokalid = "grense2Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 5.0),
                        Coordinate(0.0, 5.0),
                    )
                )
            })
        val grense3_1 = grenseRepository.save(Grense(id = "grense3_1", lokalid = "grense3_1Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 5.0),
                        Coordinate(5.0, 2.5),
                    )
                )
            })
        val grense3_2 = grenseRepository.save(Grense(id = "grense3_2", lokalid = "grense3_2Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 0.0),
                        Coordinate(5.0, 2.5),
                    )
                )
            })
        val grense4 = grenseRepository.save(Grense(id = "grense4", lokalid = "grense4Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 0.0),
                        Coordinate(0.0, 0.0)
                    )
                )
            })
        val grense5 = grenseRepository.save(Grense(id = "grense5", lokalid = "grense5Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(5.0, 5.0),
                        Coordinate(7.0, 5.0)
                    )
                )
            })
        val grense6 = grenseRepository.save(Grense(id = "grense6", lokalid = "grense6Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(7.0, 0.0),
                        Coordinate(7.0, 5.0)
                    )
                )
            })
        val grense7 = grenseRepository.save(Grense(id = "grense7", lokalid = "grense7Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(7.0, 0.0),
                        Coordinate(5.0, 0.0)
                    )
                )
            })
        val grense8 = grenseRepository.save(Grense(id = "grense8", lokalid = "grense8Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(0.0, 2.5),
                        Coordinate(-1.0, 10.0)
                    )
                )
            })
        val grense9 = grenseRepository.save(Grense(id = "grense9", lokalid = "grense9Lokalid", grensetype = grensetype)
            .apply {
                setGyldigFra(gyldigFra)
                grense = GrenseHelper.geometryFactory.createLineString(
                    arrayOf(
                        Coordinate(-1.0, 10.0),
                        Coordinate(5.0, 5.0)
                    )
                )
            })

        val dokumentasjonsreferanse = dokumentasjonsreferanseRepository.save(DokumentasjonsreferanseBuilder()
            .fastSettingAt(LocalDate.now())
            .addLenker(dokLink("http://1.no"), dokLink("https://2.no"))
            .addInternRefs(internReferanse("ref1"))
            .build())

        val dokrefGrensekobling = DokrefGrenseKobling().apply {
            this.dokumentasjonsreferanse = dokumentasjonsreferanse
            this.grense = grense3_2
        }
        dokumentasjonsreferanse.dokrefGrenseKobling.add(dokrefGrensekobling)
        grense3_2.dokrefGrensekobling.add(dokrefGrensekobling)

        val lagredeGrenserForOpprinneligKrets = listOf(grense1_1, grense1_2, grense2, grense3_1, grense3_2, grense4)
        val lagredeGrenserForOestreKrets = listOf(grense3_2, grense3_1, grense5, grense6, grense7)
        val lagredeGrenserForNordVestKrets = listOf(grense8, grense9, grense2, grense1_2)

        val kretsOpprinnelig = saveKrets("1", Lokalid("Opprinnelig"), "1", "Opprinnelig", gyldigFra, null, lagredeGrenserForOpprinneligKrets)
        val kretsOest = saveKrets("2", Lokalid("Oestre"), "2", "Oestre", gyldigFra, null, lagredeGrenserForOestreKrets)
        val kretsNordVest = saveKrets("3", Lokalid("NordVest"), "3", "NordVest", gyldigFra, null, lagredeGrenserForNordVestKrets)

        val mutableListOf = mutableListOf(kretsOpprinnelig, kretsOest, kretsNordVest)
        if (Grensetype.erAdministrativGrense(grensetype)) {
            val kretsOpprinnelig2 = saveKrets2("4", Lokalid("Opprinnelig2"), "03010001", "Opprinnelig 2", gyldigFra, null, lagredeGrenserForOpprinneligKrets)
            val kretsOest2 = saveKrets2("5", Lokalid("Oestre2"), "03010002", "Oestre 2", gyldigFra, null, lagredeGrenserForOestreKrets)
            val kretsNordVest2 = saveKrets2("6", Lokalid("NordVest2"), "03010003", "NordVest 2", gyldigFra, null, lagredeGrenserForNordVestKrets)
            mutableListOf.add(kretsOpprinnelig2)
            mutableListOf.add(kretsOest2)
            mutableListOf.add(kretsNordVest2)
        }

        return mutableListOf
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
     *  G3 har samme antall grenser før og etter, skal egentlig ikke være noen endring på G3.
     */

    private fun <T : KobletTilGrense> setupTilhorighetTest(
        gyldigFra: LocalDate,
        grenseType: Grensetype,
        saveKrets: (String, Lokalid, String, String, LocalDate, LocalDate?, List<Grense>) -> T,
        saveKretsType2: (String, Lokalid, String, String, LocalDate, LocalDate?, List<Grense>) -> T,
        medDeltKommuneGrense: Boolean = false
    ): List<T> {
        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForKretsG1 = grenseRepository.saveAll(getGrenserForVestligKrets(gyldigFra, grenseType, medToDelteGrenser = true, medDeltKommuneGrense))
        val lagredeGrenserForKretsG2 = grenseRepository.saveAll(getGrenserForOestligKrets(gyldigFra, grenseType, medToDelteGrenser = true, medDeltKommuneGrense))

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

        val mutableListe: MutableList<T> = mutableListOf(kretsG1, kretsG2, kretsG3)
        if (medDeltKommuneGrense) {
            val kretsS1 = saveKretsType2("S1", Lokalid("S1"), "1", "Stemmekrets 1 (S1)", gyldigFra, null,
                (lagredeGrenserForKretsG1 + lagredeGrenserForKretsG2).filter { grense -> grense.erAdministrativGrense() }.toList())
            val kretsS2 = saveKretsType2("S2", Lokalid("S2"), "1", "Stemmekrets 1 (S2)", gyldigFra, null,
                (lagredeGrenserForKretsG3 + grenseSoerVest).filter { grense -> grense.erAdministrativGrense() }.toList())
            mutableListe.add(kretsS1)
            mutableListe.add(kretsS2)
        }

        return mutableListe
    }


    @Test
    fun `skal legge til nye referanser i eksisterende dokumentasjonsreferanse`() {
        val gyldighetsdato = LocalDate.now()
        val kretsliste: List<KobletTilGrense> = getTilhorighetTilNyopprettetKretsTestSetup(Grensetype.Kommunegrense, gyldighetsdato, ::saveStemmekrets, ::saveGrunnkrets)
        val kretsOpprinnelig = kretsliste[0]
        val kretsOest = kretsliste[1]
        val kretsNordVest = kretsliste[2]
        val kretsOpprinnelig2 = kretsliste[3]
        val kretsOest2 = kretsliste[4]
        val kretsNordVest2 = kretsliste[5]
        val kommune = saveKommuneOgUnikLokalid(gyldigFra = gyldighetsdato,
            stemmekretser = listOf(kretsOpprinnelig as Stemmekrets, kretsOest as Stemmekrets, kretsNordVest as Stemmekrets),
            grunnkretser = listOf(kretsOpprinnelig2 as Grunnkrets, kretsOest2 as Grunnkrets, kretsNordVest2 as Grunnkrets))
        saveFylke(kommuner = listOf(kommune), gyldigFra = gyldighetsdato)
        val grenseSomSkalEndreDokref = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_2Lokalid" }!!.grense
        assertThat(grenseSomSkalEndreDokref).isNotNull()

        grenseSomSkalEndreDokref.dokrefGrensekobling.first().dokumentasjonsreferanse.addDokumentLenke().apply {
            dokumentlink = "https://3.no"
        }
        val grenseMedNyDokref = FeatureBuilder(id = grenseSomSkalEndreDokref.lokalid)
            .copy(grenseSomSkalEndreDokref.grenseToFeature())
            .withDokref(grenseSomSkalEndreDokref.dokrefGrensekobling.map {
                it.dokumentasjonsreferanse.toDokrefDTO().apply {
                    vedtakGyldigFra = gyldighetsdato
                }
            })
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
        entityManager.flush()
        entityManager.clear()
        val persistedGrense = grenseService.hentGrense(Identifikator(lokalid = Lokalid(grenseSomSkalEndreDokref.lokalid), gyldighetsdato = gyldighetsdato))
        assertThat(persistedGrense?.getDokumentasjonsreferanser(gyldighetsdato)).isNotNull().isNotEmpty()
        assertThat(persistedGrense?.getDokumentasjonsreferanser(gyldighetsdato)?.first()?.dokumentlenker).hasSize(3)
    }

    @Test
    fun `Skal hente nyeste dokref-versjon i grense B, dersom grense A sin dokref med samme lokalid oppdateres `() {
        val gyldighetsdato = LocalDate.now()
        val grunnkretser = saveNaboGrunnkretser(gyldigFra = gyldighetsdato, medToDelteGrenser = true, medKommunegrense = true )

        entityManager.flush()
        entityManager.clear()

        val dokumentasjonsreferanse = DokumentasjonsreferanseBuilder()
            .fastSettingAt(LocalDate.now())
            .addLenker(dokLink("http://1.no"), dokLink("https://2.no"))
            .addInternRefs(internReferanse("ref1"))
            .build()

        val grenseA = grunnkretser.flatMap { it.grenser }.first { it.grense.lokalid == "grenseDelt2Lokalid" }.grense
        val grenseB = grunnkretser.flatMap { it.grenser }.first  { it.grense.lokalid == "grenseSørVestLokalid" }.grense

        dokumentasjonsreferanse.dokrefGrenseKobling.add(DokrefGrenseKobling().apply {
            this.dokumentasjonsreferanse = dokumentasjonsreferanse
            this.grense = grenseA
        })
        dokumentasjonsreferanse.dokrefGrenseKobling.add(DokrefGrenseKobling().apply {
            this.dokumentasjonsreferanse = dokumentasjonsreferanse
            this.grense = grenseB
        })
        dokumentasjonsreferanseRepository.save(dokumentasjonsreferanse)
        entityManager.flush()

        val persistedGrenseA = grenseRepository.findLastByLokalid(grenseA.lokalid)

        val oppdatertTittel = "Oppdatert tittel"
        persistedGrenseA!!.dokrefGrensekobling.first().dokumentasjonsreferanse.rettskildetittel = oppdatertTittel

        val grenseAFeature = FeatureBuilder(id = grenseA.lokalid)
            .copy(grenseA.grenseToFeature())
            .withDokref(grenseA.dokrefGrensekobling.map { val dokref = it.dokumentasjonsreferanse.toDokrefDTO(); dokref.vedtakGyldigFra = LocalDate.now(); dokref })
            .build()

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features =
            listOf(
                grenseAFeature
            )),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )

        val persistedGrenseB = grenseService.hentGrense(Identifikator(lokalid = Lokalid(grenseB.lokalid), gyldighetsdato = gyldighetsdato))
        assertThat(persistedGrenseB?.dokrefGrensekobling?.first()?.dokumentasjonsreferanse?.rettskildetittel).isEqualTo(oppdatertTittel)

    }

    @Test
    fun `skal tillate samme tekst i ulike dokrefs internreferanser og lenker`() {
        val gyldighetsdato = LocalDate.now()
        val kretsliste: List<KobletTilGrense> = getTilhorighetTilNyopprettetKretsTestSetup(Grensetype.Kommunegrense, gyldighetsdato, ::saveStemmekrets, ::saveGrunnkrets)
        val kretsOpprinnelig = kretsliste[0]
        val kretsOest = kretsliste[1]
        val kretsNordVest = kretsliste[2]
        val kretsOpprinnelig2 = kretsliste[3]
        val kretsOest2 = kretsliste[4]
        val kretsNordVest2 = kretsliste[5]

        val kommune = saveKommuneOgUnikLokalid(gyldigFra = gyldighetsdato,
            stemmekretser = listOf(kretsOpprinnelig as Stemmekrets, kretsOest as Stemmekrets, kretsNordVest as Stemmekrets),
            grunnkretser = listOf(kretsOpprinnelig2 as Grunnkrets, kretsOest2 as Grunnkrets, kretsNordVest2 as Grunnkrets))
        saveFylke(kommuner = listOf(kommune), gyldigFra = gyldighetsdato)

        val grenseSomSkalHaDokref = kretsOpprinnelig.grenser.find { it.grense.lokalid == "grense3_1Lokalid" }!!.grense
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

        val grenseMedDokrefs = FeatureBuilder(id = grenseSomSkalHaDokref.lokalid)
            .copy(grenseSomSkalHaDokref.grenseToFeature())
            .withDokref(dokrefs.map {
                it.toDokrefDTO().apply {
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
        entityManager.flush()
        entityManager.clear()

        val lagretGrense = grenseService.hentGrense(Identifikator(lokalid = Lokalid(grenseSomSkalHaDokref.lokalid), gyldighetsdato = gyldighetsdato))

        assertThat(lagretGrense).isNotNull
        assertThat(lagretGrense!!.dokrefGrensekobling).hasSize(2)

        assertThat(lagretGrense.dokrefGrensekobling.first().dokumentasjonsreferanse.dokumentlenker.first().dokumentlink).isEqualTo("https://google.no")
        assertThat(lagretGrense.dokrefGrensekobling.elementAt(1).dokumentasjonsreferanse.dokumentlenker.first().dokumentlink).isEqualTo("https://google.no")

        assertThat(lagretGrense.dokrefGrensekobling.first().dokumentasjonsreferanse.internReferanserKartverket.first().internReferanseKartverket).isEqualTo("1")
        assertThat(lagretGrense.dokrefGrensekobling.elementAt(1).dokumentasjonsreferanse.internReferanserKartverket.first().internReferanseKartverket).isEqualTo("1")
    }

    @Test
    fun `Skal hente gammel dokref-versjon når man henter arkivert grense`() {
        val gyldighetsdato = LocalDate.parse("2023-01-01")
        val grunnkretser = saveNaboGrunnkretser(gyldighetsdato, medToDelteGrenser = true, medKommunegrense = true)
        entityManager.flush()

        val dokumentasjonsreferanse = DokumentasjonsreferanseBuilder()
            .fastSettingAt(LocalDate.now())
            .addLenker(dokLink("http://1.no"), dokLink("https://2.no"))
            .addInternRefs(internReferanse("ref1"))
            .addGyldighet(Gyldighet(gyldigFra = gyldighetsdato))
            .build()

        entityManager.persist(dokumentasjonsreferanse)
        entityManager.flush()

        val grenseA = grunnkretser.flatMap { it.grenser }.find { it.grense.lokalid == "grenseDeltLokalid" }!!.grense

        val grensekobling = DokrefGrenseKobling().apply {
            this.dokumentasjonsreferanse = dokumentasjonsreferanse
            this.grense = grenseA
        }
        dokumentasjonsreferanse.dokrefGrenseKobling.add(grensekobling)
        grenseA.dokrefGrensekobling.add(grensekobling)
        val orginalTittel = "tittel 1"
        grenseA.dokrefGrensekobling.first().dokumentasjonsreferanse.rettskildetittel = orginalTittel
        val grenseAFeature = FeatureBuilder()
            .copy(grenseA.grenseToFeature(gyldighetsdato = gyldighetsdato))
            .withLokalid(grenseA.lokalid)
            .withDokref(grenseA.dokrefGrensekobling.map {
                it.dokumentasjonsreferanse.toDokrefDTO().apply {
                    vedtakGyldigFra = gyldighetsdato
                }
            })
            .build()

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features =
            listOf(
                grenseAFeature
            )),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )
        entityManager.flush()
        entityManager.clear()
        val persistedGrenseA = grenseService.hentGrense(Identifikator(lokalid = Lokalid(grenseA.lokalid), gyldighetsdato = gyldighetsdato))
        assertThat(persistedGrenseA).isNotNull()

        val oppdatertTittel = "tittel 2"
        val nyGyldighetsdato = LocalDate.parse("2024-04-04")

        val nyVersjonAvGrenseFeature = FeatureBuilder()
            .withLokalid(persistedGrenseA!!.lokalid)
            .copy(persistedGrenseA.grenseToFeature(gyldighetsdato = nyGyldighetsdato))
            .withDokref(persistedGrenseA.dokrefGrensekobling.map {
                it.dokumentasjonsreferanse.toDokrefDTO().apply {
                    vedtakGyldigFra = nyGyldighetsdato
                    rettskildeTittel = oppdatertTittel
                }
            })
            .build()

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features =
            listOf(
                nyVersjonAvGrenseFeature
            )),
            gyldighetsdato = nyGyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )
        entityManager.flush()
        entityManager.clear()

        val gammelVersjonAvGrense = grenseRepository.findGyldigObjektForIdentifikator(Identifikator(lokalid = Lokalid(grenseA.lokalid), gyldighetsdato = gyldighetsdato))
        assertThat(gammelVersjonAvGrense).isNotNull()

        grenseRepository.findAllByLokalid(grenseA.lokalid)
        val dokrefPaaGyldighetsdato = gammelVersjonAvGrense?.getDokumentasjonsreferanser(gyldighetsdato)
        assertThat(dokrefPaaGyldighetsdato?.first()?.rettskildetittel).isEqualTo(orginalTittel)
    }

    @Test
    fun arkiverBopliktomraadegrenseOgLukkKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()
        val lokalid1 = Lokalid("1")

        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(id = "1", lokalid = lokalid1, bopliktomraadenummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val bopliktGrenser = bopliktomraade.bopliktomraadeGrenser as BopliktomraadeGrenser.DelvisBoplikt

        val grenseSomSkalArkiveres = bopliktGrenser.grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense
        val grenseSomSkalJusteres = bopliktGrenser.grenser.first { it.grense.lokalid == "grense2Lokalid" }.grense

        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)
        val featureMedEndretGeometri = utfoerGrensejusteringEtterArkivering(grenseSomSkalJusteres, gyldighetsdato)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(arkivertFeature, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
        assertThat(flateSet).hasSize(1)
        val flate = flateSet.firstOrNull { it.lokalid == lokalid1.value }
        assertThat(flate).isNotNull

        val bopliktomraadeEtterLagring = bopliktomraadeService.hentBopliktomraade(Identifikator(Lokalid(flate!!.lokalid), gyldighetsdato))!!
        assertThat(bopliktomraadeEtterLagring.getGrensekoblinger(gyldighetsdato)).hasSize(3).extracting("grense.lokalid").doesNotContain("grense1Lokalid")
    }

    @Test
    fun arkiverBopliktomraadegrenseUtenAaLukkeKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val bopliktomraadeRequest = BopliktomraadeRequestBuilder.DEFAULT_REQUEST
        val identifikator = Identifikator(Lokalid(bopliktomraadeRequest.identifikasjon.lokalid), gyldigFra)
        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(id = "1", lokalid = identifikator.lokalid, bopliktomraadenummer = "20",
            gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val grenseSomSkalArkiveres = (bopliktomraade.bopliktomraadeGrenser as BopliktomraadeGrenser.DelvisBoplikt).grenser.first { it.grense.lokalid == "grense1Lokalid" }.grense
        val arkivertFeature = grenseSomSkalArkiveres.grenseToFeature(gyldighetsdato, true)

        val exception = assertFailsWith<UgyldigeFlaterException> {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(arkivertFeature)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(1)
        ugyldigeFlater.find { it.lokalid == bopliktomraade.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(bopliktomraade.getFlateInformasjon()) }

        ugyldigeFlater.forEach {
            assertThat(it.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
            assertThat(it.koordinat).isIn(
                grenseSomSkalArkiveres.grense.startPoint.coordinate,
                grenseSomSkalArkiveres.grense.endPoint.coordinate
            )
        }
    }

    @Test
    fun `opprett ny bopliktomraadegrense som lukker bopliktomraadene`() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(id = "1", lokalid = Lokalid("2"), bopliktomraadenummer = "20",
            gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade), gyldigFra = gyldigFra)

        val grenseSomSkalEndres = (bopliktomraade.bopliktomraadeGrenser as BopliktomraadeGrenser.DelvisBoplikt).grenser.first { it.grense.lokalid == "grense2Lokalid" }.grense
        val featureForGrenseSomSkalEndres = grenseSomSkalEndres.grenseToFeature(gyldighetsdato)
        val oppdatertGeometri = GrenseHelper.geometryFactory.createLineString(
            arrayOf(
                Coordinate(0.0, 5.0),
                Coordinate(5.0, 10.0)
            )
        )
        oppdatertGeometri.geometryToDto().also { featureForGrenseSomSkalEndres.geometry = it }

        val gyldigNyGrense = lagNyGrense(
            "grense5Id",
            Grensetype.Bopliktgrense,
            "grense5Lokalid",
            gyldigFra,
            listOf(Coordinate(5.0, 5.0), Coordinate(5.0, 10.0)))
        gyldigNyGrense.bopliktomraader = grenseSomSkalEndres.bopliktomraader
        val featureForNyGrense: Feature = gyldigNyGrense.grenseToFeature(gyldighetsdato, nyGrense = true)

        grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(featureForGrenseSomSkalEndres, featureForNyGrense)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        val bopliktomraadeEtterLagring = bopliktomraadeService.hentBopliktomraade(Identifikator(Lokalid(bopliktomraade.lokalid), gyldighetsdato))!!
        assertThat((bopliktomraadeEtterLagring.bopliktomraadeGrenser as BopliktomraadeGrenser.DelvisBoplikt).grenser).hasSize(5)
            .extracting("grense.id")
            .doesNotContain("grense2").contains("grense1", "grense3", "grense4")
    }

    @Test
    fun `opprett ny bopliktomraadegrense som ikke lukker bopliktomraadene`() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(id = "1", lokalid = Lokalid("2"), bopliktomraadenummer = "20",
            gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade), gyldigFra = gyldigFra)

        val grenseSomSkalEndres = (bopliktomraade.bopliktomraadeGrenser as BopliktomraadeGrenser.DelvisBoplikt).grenser.first { it.grense.lokalid == "grense2Lokalid" }.grense

        val ugyldigNyGrense = lagNyGrense(
            "grenseDelt2Id",
            Grensetype.Bopliktgrense,
            "grenseDelt2Lokalid",
            gyldigFra,
            listOf(Coordinate(10.0, 8.0), Coordinate(10.0, 10.0)))
        ugyldigNyGrense.bopliktomraader = grenseSomSkalEndres.bopliktomraader
        val featureForNyGrense: Feature = ugyldigNyGrense.grenseToFeature(gyldighetsdato, nyGrense = true)

        val exception = assertFailsWith<UgyldigeFlaterException> {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf( featureForNyGrense)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }

        assertThat(exception.message).contains("Feil ved flatedanning")
        val ugyldigeFlater = exception.ugyldigeFlater
        assertThat(ugyldigeFlater).hasSize(1)
        ugyldigeFlater.find { it.lokalid == bopliktomraade.lokalid }!!.let { assertThat(it.flateInformasjon).isEqualTo(bopliktomraade.getFlateInformasjon()) }

        ugyldigeFlater.forEach {
            assertThat(it.topologifeil).isEqualTo(topologyValidationErrorToCustomText(TopologyValidationError.RING_NOT_CLOSED))
            assertThat(it.koordinat).isEqualTo(Coordinate(10.0, 10.0))
        }
    }

    @Test
    fun `endre fra delvis til fullt bopliktomraade`() {
        // Sett opp en kommune med én grunnkrets og et bopliktområde som dekker kommunen delvis
        val gyldigFra = november28In2022
        val gk = saveGrunnkrets(
            id = "grunnkrets-0",
            lokalid = Lokalid("grunnkrets"),
            grunnkretsnummer = "1",
            gyldigFra = gyldigFra,
            grenser = listOf(grenseRepository.save(
                getGrenseRundtVestreOgOestreKrets(gyldigFra, Grensetype.Kommunegrense)
            ))
        )
        val bopliktomraade = saveBopliktomraade(
            id = "boplikt-0",
            lokalid = Lokalid("boplikt"),
            bopliktomraadenummer = "100",
            gyldigFra = gyldigFra,
            grenser = grenseRepository.saveAll(
                getGrenserForOestligKrets(gyldigFra, Grensetype.Bopliktgrense).map { g ->
                    Grense(
                        id = "boplikt-${g.id}",
                        lokalid = "boplikt-${g.lokalid}",
                        grensetype = g.grensetype,
                    ).apply {
                        gyldighet = g.gyldighet
                        grense = g.grense
                        kvalitet = g.kvalitet
                    }
                }
            )
        )
        val kommune = saveKommuneOgUnikLokalid(
            grunnkretser = listOf(gk),
            bopliktomraader = listOf(bopliktomraade),
            gyldigFra = gyldigFra
        )
        entityManager.flush()
        entityManager.clear()
        val today = LocalDate.now()
        var bopliktEtterGrenseIds: HashSet<String>
        var kommuneEtterGrenseIds: HashSet<String>

        // Sjekk at bopliktområdet og kommunen ikke har noen felles grenser før endring
        bopliktEtterGrenseIds = entityManager.find(Bopliktomraade::class.java, bopliktomraade.id).getGrensekoblinger(today).mapTo(HashSet(1)) { it.grense.id }
        kommuneEtterGrenseIds = entityManager.find(Kommune::class.java, kommune.id).getGrensekoblinger(today).mapTo(HashSet(1)) { it.grense.id }
        assertThat(bopliktEtterGrenseIds).doesNotContainAnyElementsOf(kommuneEtterGrenseIds)
        assertThat(kommuneEtterGrenseIds).doesNotContainAnyElementsOf(bopliktEtterGrenseIds)
        entityManager.clear()

        // Endre bopliktområdet til å dekke hele kommunen
        entityManager.find(Bopliktomraade::class.java, bopliktomraade.id).omraade = null
        entityManager.flush()
        entityManager.clear()

        // Sjekk at bopliktområdet og kommunen nå har samme grenser etter endring
        bopliktEtterGrenseIds = entityManager.find(Bopliktomraade::class.java, bopliktomraade.id).getGrensekoblinger(today).mapTo(HashSet(1)) { it.grense.id }
        kommuneEtterGrenseIds = entityManager.find(Kommune::class.java, kommune.id).getGrensekoblinger(today).mapTo(HashSet(1)) { it.grense.id }
        assertThat(bopliktEtterGrenseIds).containsExactlyInAnyOrderElementsOf(kommuneEtterGrenseIds)

        // TODO: Unngå orphaned bopliktgrenser
//        // Sjekk at grensene som tidligere var koblet til bopliktområdet nå ikke er "orphaned"
//        val opprinneligBopliktGrenseIds = (bopliktomraade.bopliktomraadeGrenser as BopliktomraadeGrenser.DelvisBoplikt).grenser.map { it.grense.id }
//        val orphanedGrenser = entityManager.createQuery("""
//            SELECT g
//            FROM Grense g
//            WHERE
//                g.id IN(:grenseIds)
//                AND g.gyldighet.gyldigFra <= :gyldigFra
//                AND (g.gyldighet.gyldigTil IS NULL OR g.gyldighet.gyldigTil > :gyldigTil)
//                AND NOT g.gyldighet.slettet""".trimIndent(), Grense::class.java)
//            .setParameter("grenseIds", opprinneligBopliktGrenseIds)
//            .setParameter("gyldigFra", today)
//            .setParameter("gyldigTil", today)
//            .resultList
//
//        assertThat(orphanedGrenser).isEmpty()
    }


    @Test
    fun `endre fra full til delvis bopliktomraade`() {
        // Sett opp en kommune med én grunnkrets og et bopliktområde som dekker kommunen delvis
        val gyldigFra = november28In2022
        val gk = saveGrunnkrets(
            id = "grunnkrets-0",
            lokalid = Lokalid("grunnkrets"),
            grunnkretsnummer = "1",
            gyldigFra = gyldigFra,
            grenser = listOf(grenseRepository.save(
                getGrenseRundtVestreOgOestreKrets(gyldigFra, Grensetype.Kommunegrense)
            ))
        )
        var bopliktomraade = saveBopliktomraade(
            id = "boplikt-0",
            lokalid = Lokalid("boplikt"),
            bopliktomraadenummer = "100",
            gyldigFra = gyldigFra,
            delvisBoplikt = false
        )
        val kommune = saveKommuneOgUnikLokalid(
            grunnkretser = listOf(gk),
            bopliktomraader = listOf(bopliktomraade),
            gyldigFra = gyldigFra
        )
        entityManager.flush()
        entityManager.clear()
        val today = LocalDate.now()
        var bopliktEtterGrenseIds: HashSet<String>
        var kommuneEtterGrenseIds: HashSet<String>

        // Sjekk at bopliktområdet og kommunen nå har samme grenser etter endring
        bopliktEtterGrenseIds = entityManager.find(Bopliktomraade::class.java, bopliktomraade.id).getGrensekoblinger(today).mapTo(HashSet(1)) { it.grense.id }
        kommuneEtterGrenseIds = entityManager.find(Kommune::class.java, kommune.id).getGrensekoblinger(today).mapTo(HashSet(1)) { it.grense.id }
        assertThat(bopliktEtterGrenseIds).containsExactlyInAnyOrderElementsOf(kommuneEtterGrenseIds)
        entityManager.clear()

        // Endre bopliktområdet til å dekke delvis kommunen
        bopliktomraade = entityManager.find(Bopliktomraade::class.java, bopliktomraade.id)
        val bopliktGrenser = grenseRepository.saveAll(
            getGrenserForOestligKrets(gyldigFra, Grensetype.Bopliktgrense).map { g ->
                Grense(
                    id = "boplikt-${g.id}",
                    lokalid = "boplikt-${g.lokalid}",
                    grensetype = g.grensetype,
                ).apply {
                    gyldighet = g.gyldighet
                    grense = g.grense
                    kvalitet = g.kvalitet
                }
            }
        )
        bopliktomraade.bopliktomraadeGrenser = BopliktomraadeGrenser.DelvisBoplikt()
        for(grense in bopliktGrenser) {
            bopliktomraade.addGrensekobling(grense)
        }
        flateService.oppdaterFlaterFor(hashSetOf(bopliktomraade))
        entityManager.flush()
        entityManager.clear()

        // Sjekk at bopliktområdet nå kun har sine egne grenser
        bopliktEtterGrenseIds = entityManager.find(Bopliktomraade::class.java, bopliktomraade.id).getGrensekoblinger(today).mapTo(HashSet(1)) { it.grense.id }
        assertThat(bopliktEtterGrenseIds).containsExactlyInAnyOrderElementsOf(bopliktGrenser.map { it.id })

        // Sjekk at kommunen fortsatt har sine egne grenser
        kommuneEtterGrenseIds = entityManager.find(Kommune::class.java, kommune.id).getGrensekoblinger(today).mapTo(HashSet(1)) { it.grense.id }
        assertThat(kommuneEtterGrenseIds).containsExactlyInAnyOrderElementsOf(kommune.getGrensekoblinger(today).map { it.grense.id })
    }

}
