package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.response.ObjektIdentifikator
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.sosi.sosiimport.IkkeSammenhengendePolygonException
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
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
    fun `lagreGrenser klarer å endre tilhørighet på grunnkretsgrense`() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val komplettPolygonUendret = mockTilhorighetPolygon("A", gyldigFra, Grensetype.Grunnkretsgrense, TilhorighetPolygons.UENDRET)
        val komplettPolygonFrafallende = mockTilhorighetPolygon("B", gyldigFra, Grensetype.Grunnkretsgrense, TilhorighetPolygons.FRAFALLENDE)
        val komplettPolygonNy = mockTilhorighetPolygon("C", gyldigFra, Grensetype.Grunnkretsgrense, TilhorighetPolygons.NY)
        grenseRepository.saveAll(komplettPolygonNy+komplettPolygonUendret+komplettPolygonFrafallende)
        grenseRepository.flush()

        val A = saveGrunnkrets(id = "A", lokalid = Lokalid("A"), gyldigFra = gyldigFra, grenser = komplettPolygonUendret)
        val B = saveGrunnkrets(id = "B", lokalid = Lokalid("B"), gyldigFra = gyldigFra, grenser = komplettPolygonFrafallende)
        val C = saveGrunnkrets(id = "C", lokalid = Lokalid("C"), gyldigFra = gyldigFra, grenser = komplettPolygonNy)

        leggTilEkstraGrenseForGrunnkretsB(B, A, gyldigFra)
        fjernEkstraGrenseFraGrunnkretsC(C)
        grenseRepository.flush()

        saveKommuneOgUnikLokalid(grunnkretser = listOf(A), gyldigFra = gyldigFra)
        saveKommuneOgUnikLokalid(grunnkretser = listOf(B), gyldigFra = gyldigFra)
        saveKommuneOgUnikLokalid(grunnkretser = listOf(C), gyldigFra = gyldigFra)

        val feature = utfoerGrenseTilhorighetEndring(B.grenser.map { it.grense }, "B1", gyldighetsdato)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(feature)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.KVALITETSHEVING
        )

        assertThat(flateSet.size).isEqualTo(2)
        assertThat(flateSet.map { it.lokalid }).containsAll(listOf("B", "C"))
        assertThat(flateSet.map { (it as Grunnkrets).grenser.size }).isEqualTo(4)
    }

    private fun fjernEkstraGrenseFraGrunnkretsC(C: Grunnkrets) {
        C.apply {
            val grenseSomSkalFjernes = GrenseHelper.geometryFactory.createLineString(arrayOf(
                Coordinate(5.0, 0.0),
                Coordinate(5.0, 5.0)
            ))
            grenser = this.grenser.filter { gk -> !gk.grense.grense.equals(grenseSomSkalFjernes) }.toMutableSet()
        }
    }

    private fun leggTilEkstraGrenseForGrunnkretsB(B: Grunnkrets, A: Grunnkrets, gyldigFra: LocalDate){
        val ekstraGrense =  Grense(
            "frafallendeGrense", "frafallendeGrense", Grensetype.Grunnkretsgrense,
        ).apply {
            setGyldigFra(gyldigFra)
            grense = GrenseHelper.geometryFactory.createLineString(
                arrayOf(
                    Coordinate(5.0, 0.0),
                    Coordinate(5.0, 5.0)
                )
            )
        }
        grenseRepository.save(ekstraGrense)

        A.replaceGrensekobling(A.grenser.map { it.grense }.first{it.grense.equals(GrenseHelper.geometryFactory.createLineString(
            arrayOf(Coordinate(5.0, 0.0), Coordinate(5.0, 5.0))))}, ekstraGrense, GrenseKoblingAttributter())
        B.addGrensekobling(ekstraGrense, GrenseKoblingAttributter())
    }

    private fun utfoerGrenseTilhorighetEndring(grenser: List<Grense>, idForGrenseSomFaarTilhorighetEndring: String, gyldighetsdato: LocalDate): Feature {
        val grenseSomFaarTilhorighetEndring = grenser.first{it.lokalid == idForGrenseSomFaarTilhorighetEndring}

        val nyeTilhorigheter = listOf(KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("A"), gyldighetsdato),
            type = KontekstEnhetType.GRUNNKRETS,
            version = 1
        ), KontekstEgenskaper(
            id = ObjektIdentifikator(Lokalid("C"), gyldighetsdato),
            type = KontekstEnhetType.GRUNNKRETS,
            version = 1
        ))

        return FeatureBuilder(id = idForGrenseSomFaarTilhorighetEndring)
            .withLokalid(idForGrenseSomFaarTilhorighetEndring)
            .withGeometry(grenseSomFaarTilhorighetEndring.grense)
            .withGrenseversjon(1)
            .withGrensetype(grenseSomFaarTilhorighetEndring.grensetype)
            .withKontekstEgenskaper(nyeTilhorigheter)
            .build()
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

        saveKommuneOgUnikLokalid(stemmekretser = listOf(stemmekrets), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val arkivertFeature = utfoerStemmekretsgrenseArkivering(stemmekrets, gyldighetsdato)

        Assertions.assertThatThrownBy {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(arkivertFeature)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )

        }.isInstanceOf(IkkeSammenhengendePolygonException::class.java)
            .hasMessageContaining("Polygon som skal dannes har ikke-sammenhengende punkter, sluttpunkt: (0.0, 0.0, NaN), startPunkt: (0.0, 5.0, NaN). Validation-feil: Interior is disconnected.")
    }

    @Test
    fun arkiverStemmekretsgrenseOgLukkKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val stemmekretsRequest = StemmekretsRequestBuilder.DEFAULT_REQUEST
        val identifikator = Identifikator(Lokalid(stemmekretsRequest.identifikasjon.lokalid), gyldigFra)

        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, gyldighetsdato = gyldighetsdato, Grensetype.Stemmekretsgrense))
        grenseRepository.flush()

        val stemmekrets = saveStemmekrets(id = "1", lokalid = identifikator.lokalid, stemmekretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(stemmekretser = listOf(stemmekrets), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val arkivertFeature = utfoerStemmekretsgrenseArkivering(stemmekrets, gyldighetsdato)
        val featureMedEndretGeometri = utfoerStemmekretsgrensejusteringEtterArkivering(stemmekrets, gyldighetsdato)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(arkivertFeature, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
        assertThat(flateSet).hasSize(1)
        val flate = flateSet.first()
        assertThat(flate.lokalid).isEqualTo(stemmekretsRequest.identifikasjon.lokalid)

        val stemmekretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(flate.lokalid), gyldighetsdato))
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

        saveKommuneOgUnikLokalid(grunnkretser = listOf(grunnkrets), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val arkivertFeature = utfoerGrunnkretsgrenseArkivering(grunnkrets, gyldighetsdato)

        Assertions.assertThatThrownBy {
            grenseService.lagreGrenser(
                grenser = FeatureCollection(features = listOf(arkivertFeature)),
                gyldighetsdato = gyldighetsdato,
                endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
            )
        }.isInstanceOf(IkkeSammenhengendePolygonException::class.java)
            .hasMessageContaining("Polygon som skal dannes har ikke-sammenhengende punkter, sluttpunkt: (0.0, 0.0, NaN), startPunkt: (0.0, 5.0, NaN). Validation-feil: Interior is disconnected.")
    }

    @Test
    fun arkiverGrunnkretsgrenseOgLukkKrets() {
        val gyldigFra = november28In2022
        val gyldighetsdato = LocalDate.now()

        val grunnkretsRequest = GrunnkretsRequestBuilder.DEFAULT_REQUEST
        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), gyldigFra)

        val lagredeGrenser = grenseRepository.saveAll(getGrenserForEnkelKrets(gyldigFra, gyldighetsdato = gyldighetsdato, Grensetype.Grunnkretsgrense))
        grenseRepository.flush()

        val grunnkrets = saveGrunnkrets(id = "1", lokalid = identifikator.lokalid, grunnkretsnummer = "20", gyldigFra = gyldigFra, grenser = lagredeGrenser)

        saveKommuneOgUnikLokalid(grunnkretser = listOf(grunnkrets), gyldigFra = gyldigFra)
        grenseRepository.flush()

        val arkivertFeature = utfoerGrunnkretsgrenseArkivering(grunnkrets, gyldighetsdato)
        val featureMedEndretGeometri = utfoerGrunnkretsgrensejusteringEtterArkivering(grunnkrets, gyldighetsdato)

        val flateSet = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(arkivertFeature, featureMedEndretGeometri)),
            gyldighetsdato = gyldighetsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
        assertThat(flateSet).hasSize(1)
        val flate = flateSet.first()
        assertThat(flate.lokalid).isEqualTo(grunnkretsRequest.identifikasjon.lokalid)

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(flate.lokalid), gyldighetsdato))
        assertThat(grunnkretsEtterLagring?.grenser).hasSize(3).extracting("grense.lokalid")
            .doesNotContain("grense1Lokalid")
    }

    private fun utfoerStemmekretsgrenseArkivering(kretsSomSkalHaGrenseendring: Stemmekrets, opprinneligDato: LocalDate): Feature {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrenser(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == "grense1Lokalid" }


        return utfoerGrenseArkivering(opprinneligDato, grenseKobling, kretsSomSkalHaGrenseendring.lokalid, KontekstEnhetType.STEMMEKRETS, Grensetype.Stemmekretsgrense)
    }

    private fun utfoerGrunnkretsgrenseArkivering(kretsSomSkalHaGrenseendring: Grunnkrets, opprinneligDato: LocalDate): Feature {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrenser(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == "grense1Lokalid" }

        return utfoerGrenseArkivering(opprinneligDato, grenseKobling, kretsSomSkalHaGrenseendring.lokalid, KontekstEnhetType.GRUNNKRETS, Grensetype.Grunnkretsgrense)
    }

    private fun utfoerGrenseArkivering(opprinneligDato: LocalDate, grenseKobling: GrenseKobling, lokalid: String, kontekstEnhetType: KontekstEnhetType, grensetype: Grensetype): Feature {

        return FeatureBuilder(id = "grense1Lokalid")
            .withGrenseversjon(grenseKobling.grense.version)
            .withGrensetype(grensetype)
            .withShouldArchive(true)
            .build()
    }

    private fun utfoerStemmekretsgrensejusteringEtterArkivering(kretsSomSkalHaGrenseendring: Stemmekrets, opprinneligDato: LocalDate): Feature {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrenser(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == "grense2Lokalid" }

        return utfoerGrensejustering(grenseKobling, opprinneligDato, kretsSomSkalHaGrenseendring.lokalid, KontekstEnhetType.STEMMEKRETS, Grensetype.Stemmekretsgrense)
    }

    private fun utfoerGrunnkretsgrensejusteringEtterArkivering(kretsSomSkalHaGrenseendring: Grunnkrets, opprinneligDato: LocalDate): Feature {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrenser(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == "grense2Lokalid" }

        return utfoerGrensejustering(grenseKobling, opprinneligDato, kretsSomSkalHaGrenseendring.lokalid, KontekstEnhetType.GRUNNKRETS, Grensetype.Grunnkretsgrense)
    }

    private fun utfoerGrensejustering(grenseKobling: GrenseKobling, opprinneligDato: LocalDate, lokalid: String, kontekstEnhetType: KontekstEnhetType, grensetype: Grensetype): Feature {
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            Coordinate(5.0, 5.0),
            Coordinate(0.0, 0.0),
        ))

        return FeatureBuilder(id = "grense2Lokalid")
            .withGrenseversjon(grenseKobling.grense.version)
            .withGrensetype(grensetype)
            .withGeometry(oppdatertGeometri)
            .build()
    }

    private enum class TilhorighetPolygons{
        NY,
        UENDRET,
        FRAFALLENDE
    }
    private fun mockTilhorighetPolygon(forKrets: String, gyldigFra: LocalDate, grensetype: Grensetype, tilhorighetPolygons: TilhorighetPolygons): List<Grense> {
        var koordinater = listOf<Pair<Coordinate, Coordinate>>()
        when(tilhorighetPolygons){
            TilhorighetPolygons.NY -> koordinater = listOf(
                Pair(Coordinate(0.0,0.0), Coordinate(0.0,5.0)),
                Pair(Coordinate(0.0,5.0), Coordinate(5.0,5.0)),
                Pair(Coordinate(5.0,5.0), Coordinate(5.0,0.0)),
                Pair(Coordinate(5.0,0.0), Coordinate(0.0,0.0))
            )
            TilhorighetPolygons.UENDRET -> koordinater = listOf(
                Pair(Coordinate(5.0,0.0), Coordinate(5.0,5.0)),
                Pair(Coordinate(5.0,5.0), Coordinate(10.0,5.0)),
                Pair(Coordinate(10.0,5.0), Coordinate(10.0,0.0)),
                Pair(Coordinate(10.0,0.0), Coordinate(5.0,0.0))
            )
            TilhorighetPolygons.FRAFALLENDE -> koordinater = listOf(
                Pair(Coordinate(5.0,5.0), Coordinate(5.0,10.0)),
                Pair(Coordinate(5.0,10.0), Coordinate(10.0,10.0)),
                Pair(Coordinate(10.0,10.0), Coordinate(10.0,5.0)),
                Pair(Coordinate(10.0,5.0), Coordinate(5.0,5.0))
            )
        }
        return mockPolygon(
            forKrets,
            koordinater,
            gyldigFra,
            grensetype
        )
    }

    private fun mockPolygon(forKrets: String, coordinatePairs: List<Pair<Coordinate, Coordinate>>, gyldigFra: LocalDate, grensetype: Grensetype): List<Grense> {
        return coordinatePairs.mapIndexed { index, pair ->
            Grense(id = forKrets+index.toString(), lokalid = forKrets+index.toString(), grensetype = grensetype)
                .apply {
                    setGyldigFra(gyldigFra)
                    grense = GrenseHelper.geometryFactory.createLineString(
                        arrayOf(
                            pair.first,
                            pair.second
                        )
                    )
                }
        }
    }

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
}
