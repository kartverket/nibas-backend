package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.KodelisteEntry
import no.kartverket.nibas.api.v1.common.geojson.metadata.Posisjonskvalitet
import no.kartverket.nibas.api.v1.common.geojson.metadata.StatistiskgrenseMetadata
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsRequestBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.extensions.geometry.geometryToDto
import no.kartverket.nibas.extensions.geometry.grenseToFeature
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles(profiles = ["security-off", "eventsDisabled"])
@Transactional
class GrunnkretsServiceIT : TestWithDbContainer() {

    @MockBean
    private lateinit var kodelisteServiceMock: KodelisteService

    private val lokalidOfGrunnkretsInDataset = Lokalid("c54fb55e-15d0-4528-8b88-f58d6c74bd87")
    private val earlyDate = LocalDate.of(2000, 1, 1)
    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28) // "Dagens dato"
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val grenseLokallid = Lokalid("888")
    private val grunnkretsLokalid = Lokalid("1001")

    @Test
    fun hentEksisterendeGrunnkretsMedDagensDato() {
        val grunnkrets = grunnkretsService.hentGrunnkrets(Identifikator(lokalidOfGrunnkretsInDataset, november28In2022))
        assertThat(grunnkrets).isNotNull
    }

    @Test
    fun hentEksisterendeGrunnkretsMedTidligereDato() {
        assertThat(grunnkretsService.hentGrunnkrets(Identifikator(lokalidOfGrunnkretsInDataset, earlyDate))).isNull()
    }

    @Test
    fun hentVersjonAvGrunnkretsFramITid() {
        val grunnkretsLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-e3c32c137ba6")
        saveGrunnkretsMed1FramtidigVersjon(grunnkretsLokalid)
        assertThat(grunnkretsRepository.findAllByLokalid(grunnkretsLokalid.value).size).isEqualTo(2)
        assertThat(grunnkretsRepository.findAllByLokalidSortedByGyldigFra(grunnkretsLokalid).size).isEqualTo(2)

        val framtidigGrunnkrets = grunnkretsService.hentGrunnkrets(Identifikator(grunnkretsLokalid, jan1In2023))!!
        assertThat(framtidigGrunnkrets).isNotNull
        assertThat(framtidigGrunnkrets.grunnkretsnummer).isEqualTo("42270304")
        assertThat(framtidigGrunnkrets.grunnkretsnavn).isEqualTo("Måsvann")
    }

    @Test
    fun `finn grunnkrets med samme grunnkretsnummer og ulik lokalid`() {
        val grunnkretsLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-e3c32c137ba6")
        saveGrunnkretsMed1FramtidigVersjon(grunnkretsLokalid)
        val grunnkrets = grunnkretsService.hentGrunnkrets(Identifikator(grunnkretsLokalid, jan1In2023))!!

        //Uten å ta hensyn til gyldighet og med vilkårlig lokalid skal det finnes to grunnkretser med gitt grunnkretsnummer
        var grunnkretser = grunnkretsRepository.findAllByGrunnkretsnummerAndLokalidIsNot(grunnkrets.grunnkretsnummer!!, "xx")
        assertThat(grunnkretser).hasSize(2)

        //Uten å ta hensyn til gyldighet og med gitt lokalid som ikke skal finnes skal det ikke finnes noen grunnkretser med gitt grunnkretsnummer
        //Dvs det finnes ikke noen ANDRE grunnkretser enn den som skal valideres med gitt grunnkretsnummer
        grunnkretser = grunnkretsRepository.findAllByGrunnkretsnummerAndLokalidIsNot(grunnkrets.grunnkretsnummer!!, grunnkrets.lokalid)
        assertThat(grunnkretser).isEmpty()
    }

    @Test
    fun hentGrenserForEksisterendeGrunnkretsMedDagensDato() {
        val identifikator = Identifikator(Lokalid("1001"), november28In2022)
        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", gyldigFra = october1In2022)
        lagGrenserForGrunnkrets(identifikator.lokalid, kommune)

        val grensekoblinger = grunnkretsService.hentGrensekoblingerForGrunnkrets(identifikator)
        assertThat(grensekoblinger).isNotEmpty
    }

    @Test
    fun hentGrenserForEksisterendeGrunnkretsMedTidligereDato() {
        val grensekoblinger = grunnkretsService.hentGrensekoblingerForGrunnkrets(Identifikator(lokalidOfGrunnkretsInDataset, earlyDate))
        assertThat(grensekoblinger).isEmpty()
    }

    @Test
    fun hentGrenserForGrunnkretsFramITid() {
        val identifikatorFor1Jan23 = Identifikator(grunnkretsLokalid, jan1In2023)

        val grense3 = saveGrenseAvTypeKommunegrense(id = "3002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrenseAvTypeKommunegrense(id = "3001", lokalid = grenseLokallid, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrenseAvTypeKommunegrense(id = "3000", lokalid = grenseLokallid, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        val grunnkrets1 = saveGrunnkrets(id = "1301", lokalid = grunnkretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val grunnkrets2 = saveGrunnkrets(id = "1302", lokalid = grunnkretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1, grense2))
        val grunnkrets3 = saveGrunnkrets(id = "1303", lokalid = grunnkretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense3))

        saveKommuneOgUnikLokalid(gyldigFra = october1In2022, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))

        val grensekoblinger = grunnkretsService.hentGrensekoblingerForGrunnkrets(identifikatorFor1Jan23)
        assertGrenser(grensekoblinger,
            grenseIGrensekoblingId = "3001",
            enhetKobletTilGrenseLokalid = grunnkretsLokalid
        )
    }

    @Test
    fun hentAntallFramtidigeVersjoner() {
        val grunnkretsLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-e3c32c137ba6")
        saveGrunnkretsMed1FramtidigVersjon(grunnkretsLokalid)
        assertThat(grunnkretsService.hentAntallGyldigeVersjonerFramITidFor(Identifikator(grunnkretsLokalid, november28In2022))).isEqualTo(1)
    }

    @Test
    fun `oppdaterGrunnkrets skal lage ny grunnkrets og sette gyldigtil for gammel krets`() {
        val grunnkretsRequest = GrunnkretsRequestBuilder.DEFAULT_REQUEST
        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", gyldigFra = october1In2022)

        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), october1In2022)
        val grunnkrets = lagGrenserForGrunnkrets(identifikator.lokalid, kommune)

        val now = LocalDate.now()
        val opprinneligGrunnkrets = grunnkretsService.hentGrunnkrets(identifikator)!!

        grunnkretsService.oppdaterGrunnkrets(
            lokalId = identifikator.lokalid,
            oppdatertGrunnkrets = grunnkretsRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligGrunnkretsEtter = grunnkretsService.hentGrunnkrets(identifikator)!!
        val grunnkretsEtter = grunnkretsService.hentGrunnkrets(Identifikator(identifikator.lokalid))!!

        assertThat(grunnkretsEtter.grunnkretsnummer).isNotEqualTo(grunnkrets.grunnkretsnummer)
            .isEqualTo(grunnkretsRequest.grunnkretsnummer)
        assertThat(grunnkretsEtter.grunnkretsnavn).isNotEqualTo(grunnkrets.grunnkretsnavn)
            .isEqualTo(grunnkretsRequest.navn)
        assertThat(grunnkretsEtter.omraade).isEqualTo(grunnkrets.omraade)
        assertThat(grunnkretsEtter.posisjon).isEqualTo(grunnkrets.posisjon)
        assertThat(grunnkretsEtter.gyldighet.gyldigFra).isEqualTo(now)

        assertThat(opprinneligGrunnkretsEtter.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligGrunnkretsEtter.gyldighet.gyldigTil).isEqualTo(now)

        val grenseObjIdsForOpprinneligGrunnkrets = opprinneligGrunnkrets.grenser.map { it.grense.id }
        val grenseObjIdsForOpprinneligGrunnkretsEtter = opprinneligGrunnkretsEtter.grenser.map { it.grense.id }
        val grenseObjIdsForNyGrunnkrets = grunnkretsEtter.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligGrunnkrets).containsAll(grenseObjIdsForOpprinneligGrunnkretsEtter)
        assertThat(grenseObjIdsForOpprinneligGrunnkrets).containsAll(grenseObjIdsForNyGrunnkrets)
        assertThat(grenseObjIdsForNyGrunnkrets).containsAll(grenseObjIdsForOpprinneligGrunnkrets)
        assertThat(grenseObjIdsForNyGrunnkrets).containsAll(grenseObjIdsForOpprinneligGrunnkretsEtter)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligGrunnkrets)
        assertThat(grenser).hasSize(3)
        grenser.forEach { grense ->
            assertThat(grense.grunnkretser.map { it.grunnkrets.id }).hasSize(2).containsExactlyInAnyOrder(opprinneligGrunnkrets.id, grunnkretsEtter.id)
        }
    }

    @Test
    fun `oppdaterGrunnkrets skal kaste exception ved ugyldig kommune for grunnkretsen`() {
        val grunnkretsRequest = GrunnkretsRequestBuilder.DEFAULT_REQUEST

        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Grunnkretsgrense))
        grenseRepository.flush()

        val grunnkrets = saveGrunnkrets(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )

        //Kommunen har gyldig fra etter grunnkretsens gyldig fra, og gyldig til før ønsket oppdateringsdato
        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", gyldigFra = november28In2022, gyldigTil = jan1In2023, grunnkretser = listOf(grunnkrets))
        val now = LocalDate.now()

        Assertions.assertThatThrownBy {
            grunnkretsService.oppdaterGrunnkrets(
                lokalId = identifikator.lokalid,
                oppdatertGrunnkrets = grunnkretsRequest,
                gyldigFra = now,
                endringstype = Endringstype.RETTING
            )
        }.isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fant ikke kommune for grunnkrets med id ${grunnkrets.id}")

        Assertions.assertThatThrownBy {
            grunnkretsService.createNyVersjonAvGrunnkrets(grunnkrets = grunnkrets, gyldigFra = now, endringstype = Endringstype.RETTING)
        }.isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fant ikke kommune for grunnkrets med id ${grunnkrets.id}")

    }

    @Test
    fun `createNyVersjonAvGrunnkretsIAnnenKommune skal lage ny grunnkrets og sette gyldigtil for gammel krets`() {
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Grunnkretsgrense))
        grenseRepository.flush()

        val identifikator = Identifikator(Lokalid("grunnkretsSomSkalFlyttesTilHaram"), october1In2022)
        val grunnkrets = saveGrunnkrets(
            id = "22",
            lokalid = identifikator.lokalid,
            grunnkretsnummer = "15071234",
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )

        val gamleAalesund = saveKommuneOgUnikLokalid(id = "idGamleAalesund", lokalid = "1507Ålesund", kommunenummerUuid = "afc65fde-8a62-497d-b35d-77345efddee3",
            kommunenummer = "1507", grunnkretser = listOf(grunnkrets))
        val haram = saveKommuneOgUnikLokalid(id = "idHaram", lokalid = "1580Haram", kommunenummerUuid = "687f3337-899f-42f8-849d-6f2d5b603dd3",
            kommunenummer = "1580")
        saveFylkeOgUnikLokalid(lokalid = "15MøreOgRomsdal", fylkesnummerUuid = "c499a315-0cd5-4e99-8acf-bd6f4803799a",
            fylkesnummer = "15", kommuner = listOf(gamleAalesund, haram))

        val opprinneligGrunnkrets = grunnkretsService.hentGrunnkrets(identifikator)!!
        assertThat(kommuneRepository.findById(gamleAalesund.id).get().kommuneLokalid.grunnkretser).hasSize(1).extracting("id").contains(grunnkrets.id)
        assertThat(kommuneRepository.findById(haram.id).get().kommuneLokalid.grunnkretser).isEmpty()

        val grunnkretsIHaram = grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(grunnkrets = grunnkrets, kommune = haram,
            nyttGrunnkretsNummer = "15801234", gyldigFra = jan1In2024, endringstype = Endringstype.IMPORT)

        val opprinneligGrunnkretsEtter = grunnkretsService.hentGrunnkrets(identifikator)!!
        val grunnkretsEtter = grunnkretsService.hentGrunnkrets(Identifikator(identifikator.lokalid, jan1In2024))!!

        assertThat(grunnkretsIHaram.id).isNotEqualTo(grunnkrets.id).isEqualTo(grunnkretsEtter.id)
        assertThat(grunnkretsIHaram.id).isEqualTo(grunnkretsEtter.id)
        assertThat(grunnkretsIHaram.lokalid).isEqualTo(grunnkretsEtter.lokalid).isEqualTo(grunnkrets.lokalid)

        assertThat(grunnkretsEtter.grunnkretsnummer).isNotEqualTo(grunnkrets.grunnkretsnummer)
        assertThat(grunnkretsEtter.grunnkretsnummer!!.length).isEqualTo(grunnkrets.grunnkretsnummer!!.length).isEqualTo(8)
        assertThat(grunnkretsEtter.grunnkretsnummer!!.takeLast(4)).isEqualTo(grunnkrets.grunnkretsnummer!!.takeLast(4)).isEqualTo("1234")
        assertThat(grunnkrets.grunnkretsnummer!!.take(4)).isEqualTo("1507")
        assertThat(grunnkretsEtter.grunnkretsnummer!!.take(4)).isEqualTo("1580")
        assertThat(grunnkretsIHaram.grunnkretsnavn).isEqualTo(grunnkretsEtter.grunnkretsnavn).isEqualTo(grunnkrets.grunnkretsnavn)
        assertThat(grunnkretsEtter.omraade).isEqualTo(grunnkrets.omraade)
        assertThat(grunnkretsEtter.posisjon).isEqualTo(grunnkrets.posisjon)
        assertThat(grunnkretsEtter.gyldighet.gyldigFra).isEqualTo(jan1In2024)

        assertThat(opprinneligGrunnkretsEtter.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligGrunnkretsEtter.gyldighet.gyldigTil).isEqualTo(jan1In2024)

        val gamleAalesundEtter = kommuneRepository.findById(gamleAalesund.id).get()
        val haramEtter = kommuneRepository.findById(haram.id).get()
        assertThat(gamleAalesundEtter.kommuneLokalid.grunnkretser).hasSize(1).extracting("id").contains(grunnkrets.id)
        assertThat(haramEtter.kommuneLokalid.grunnkretser).hasSize(1).extracting("id").contains(grunnkretsIHaram.id)

        val grenseObjIdsForOpprinneligGrunnkrets = opprinneligGrunnkrets.grenser.map { it.grense.id }
        val grenseObjIdsForOpprinneligGrunnkretsEtter = opprinneligGrunnkretsEtter.grenser.map { it.grense.id }
        val grenseObjIdsForNyGrunnkrets = grunnkretsEtter.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligGrunnkrets).containsAll(grenseObjIdsForOpprinneligGrunnkretsEtter)
        assertThat(grenseObjIdsForOpprinneligGrunnkrets).containsAll(grenseObjIdsForNyGrunnkrets)
        assertThat(grenseObjIdsForNyGrunnkrets).containsAll(grenseObjIdsForOpprinneligGrunnkrets)
        assertThat(grenseObjIdsForNyGrunnkrets).containsAll(grenseObjIdsForOpprinneligGrunnkretsEtter)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligGrunnkrets)
        assertThat(grenser).hasSize(4)
        grenser.forEach { grense ->
            assertThat(grense.grunnkretser.map { it.grunnkrets.id }).hasSize(2).containsExactlyInAnyOrder(opprinneligGrunnkrets.id, grunnkretsEtter.id)
        }
    }

    @Test
    fun `oppdatering av grunnkrets til et allerede brukt grunnkretsnummer skal gi feil`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val grunnkretsRequest = GrunnkretsRequestBuilder().withLokalId(oestreKrets.lokalid).withGrunnkretsnummer(vestreKrets.grunnkretsnummer!!).build()
        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), datoForOppdatering)

        Assertions.assertThatThrownBy {
            grunnkretsService.oppdaterGrunnkrets(
                lokalId = identifikator.lokalid,
                oppdatertGrunnkrets = grunnkretsRequest,
                gyldigFra = datoForOppdatering,
                endringstype = Endringstype.RETTING
            )
        }.isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Kretsnummer ${vestreKrets.grunnkretsnummer} er brukt på flere grunnkretser innenfor kommunen")

    }

    @Test
    fun `endre kommunegrense for grunnkrets skal opprette ny versjon for grensa og dens grunnkrets og kommune`() {
        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", kommunenummer = "4227", gyldigFra = october1In2022)

        val grunnkrets = lagGrenserForGrunnkrets(grunnkretsLokalid = Lokalid("1001"), kommune = kommune, grensetype = Grensetype.Kommunegrense)
        saveFylkeOgUnikLokalid(fylkesnummerUuid = "42", fylkesnummer = "42", gyldigFra = october1In2022, kommuner = listOf(kommune))
        grenseRepository.flush()
        flateService.oppdaterFlaterFor(setOf(kommune))
        kommune.posisjon = kommune.omraade?.interiorPoint
        grenseRepository.flush()

        assertThat(grunnkrets.gyldighet.gyldigTil).isNull()
        assertThat(kommune.gyldighet.gyldigTil).isNull()
        assertThat(grunnkrets.omraade).isEqualTo(kommune.omraade)
        assertThat(grunnkrets.posisjon).isEqualTo(kommune.posisjon)

        val grenseKobling = grunnkrets.getGrensekoblinger(gyldighetsdato = october1In2022).first { it.grense.lokalid == "850" }
        val grense = grenseKobling.grense.grense

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            grense.getCoordinateN(0).copy(),
            Coordinate(23860.0, 675630.0),
            grense.getCoordinateN(1).copy()
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(october1In2022)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }

        val flaterSomErEndret = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(oppdatertGrensefeature)),
            gyldighetsdato = november28In2022,
            endringstype = Endringstype.KVALITETSHEVING
        )

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(grunnkrets.lokalid), november28In2022))!!
        val kommuneEtterLagring = kommuneService.hentKommune(Identifikator(Lokalid(kommune.lokalid), november28In2022))!!
        assertTopologi(kommuneEtterLagring.omraade, grunnkretsEtterLagring.omraade)
        assertThat(kommuneEtterLagring.posisjon).isEqualTo(grunnkretsEtterLagring.posisjon)

        assertThat(flaterSomErEndret).hasSize(2).extracting("id").contains(grunnkretsEtterLagring.id, kommuneEtterLagring.id)

        assertOmradeGyldighetVersjon(grunnkretsEtterLagring, grunnkrets, november28In2022, Endringstype.KVALITETSHEVING)
        assertOmradeGyldighetVersjon(kommuneEtterLagring, kommune, november28In2022, Endringstype.KVALITETSHEVING)
    }

    @Test
    fun `endre fylkegrense for grunnkrets skal opprette ny versjon for grensa og dens grunnkrets og kommune og fylke`() {
        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", kommunenummer = "4227", gyldigFra = october1In2022)

        val grunnkrets = lagGrenserForGrunnkrets(grunnkretsLokalid = Lokalid("1001"), kommune = kommune, grensetype = Grensetype.Fylkesgrense)
        val fylke = saveFylkeOgUnikLokalid(fylkesnummerUuid = "42", fylkesnummer = "42", gyldigFra = october1In2022, kommuner = listOf(kommune))
        saveNasjonOgUnikLokalid(gyldigFra = october1In2022, fylker = listOf(fylke))
        grenseRepository.flush()

        flateService.oppdaterFlaterFor(setOf(kommune, fylke))
        grenseRepository.flush()

        assertThat(grunnkrets.gyldighet.gyldigTil).isNull()
        assertThat(kommune.gyldighet.gyldigTil).isNull()
        assertThat(fylke.gyldighet.gyldigTil).isNull()
        assertTopologi(kommune.omraade, grunnkrets.omraade)
        assertTopologi(fylke.omraade, kommune.omraade)
        assertThat(fylke.posisjon).isEqualTo(kommune.posisjon).isEqualTo(grunnkrets.posisjon)

        val grenseKobling = grunnkrets.getGrensekoblinger(gyldighetsdato = october1In2022).first { it.grense.lokalid == "850" }
        val grense = grenseKobling.grense.grense

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            grense.getCoordinateN(0).copy(),
            Coordinate(23860.0, 675630.0),
            grense.getCoordinateN(1).copy()
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(october1In2022)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }

        val flaterSomErEndret = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(oppdatertGrensefeature)),
            gyldighetsdato = november28In2022,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(grunnkrets.lokalid), november28In2022))!!
        val kommuneEtterLagring = kommuneService.hentKommune(Identifikator(Lokalid(kommune.lokalid), november28In2022))!!
        val fylkeEtterLagring = fylkeService.hentFylke(Identifikator(Lokalid(fylke.lokalid), november28In2022))!!

        assertTopologi(kommuneEtterLagring.omraade, grunnkretsEtterLagring.omraade)
        assertTopologi(fylkeEtterLagring.omraade, kommuneEtterLagring.omraade)
        assertThat(fylkeEtterLagring.posisjon).isEqualTo(kommuneEtterLagring.posisjon).isEqualTo(grunnkretsEtterLagring.posisjon)

        assertThat(flaterSomErEndret).hasSize(3).extracting("id").contains(grunnkretsEtterLagring.id, kommuneEtterLagring.id, fylkeEtterLagring.id)

        assertOmradeGyldighetVersjon(grunnkretsEtterLagring, grunnkrets, november28In2022, Endringstype.VEDTATT_GRENSEJUSTERING)
        assertOmradeGyldighetVersjon(kommuneEtterLagring, kommune, november28In2022, Endringstype.VEDTATT_GRENSEJUSTERING)
        assertOmradeGyldighetVersjon(fylkeEtterLagring, fylke, november28In2022, Endringstype.VEDTATT_GRENSEJUSTERING)
    }

    @Test
    fun `endre riksgrense for grunnkrets skal opprette ny versjon for grensa og dens grunnkrets og kommune og fylke og nasjon`() {
        val kommune = saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", kommunenummer = "4227", gyldigFra = october1In2022)

        val grunnkrets = lagGrenserForGrunnkrets(grunnkretsLokalid = Lokalid("1001"), kommune = kommune, grensetype = Grensetype.Riksgrense)
        val fylke = saveFylkeOgUnikLokalid(fylkesnummerUuid = "42", fylkesnummer = "42", gyldigFra = october1In2022, kommuner = listOf(kommune))
        val nasjon = saveNasjonOgUnikLokalid(gyldigFra = october1In2022, fylker = listOf(fylke))
        grenseRepository.flush()

        flateService.oppdaterFlaterFor(setOf(kommune, fylke, nasjon))
        grenseRepository.flush()

        assertThat(grunnkrets.gyldighet.gyldigTil).isNull()
        assertThat(kommune.gyldighet.gyldigTil).isNull()
        assertThat(fylke.gyldighet.gyldigTil).isNull()
        assertThat(nasjon.gyldighet.gyldigTil).isNull()
        assertTopologi(kommune.omraade, grunnkrets.omraade)
        assertTopologi(fylke.omraade, kommune.omraade)
        assertTopologi(nasjon.omraade, fylke.omraade)
        assertThat(nasjon.posisjon).isEqualTo(fylke.posisjon).isEqualTo(kommune.posisjon).isEqualTo(grunnkrets.posisjon)

        val grenseKobling = grunnkrets.getGrensekoblinger(gyldighetsdato = october1In2022).first { it.grense.lokalid == "850" }
        val grense = grenseKobling.grense.grense

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            grense.getCoordinateN(0).copy(),
            Coordinate(23860.0, 675630.0),
            grense.getCoordinateN(1).copy()
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(october1In2022)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }

        val flaterSomErEndret = grenseService.lagreGrenser(
            grenser = FeatureCollection(features = listOf(oppdatertGrensefeature)),
            gyldighetsdato = november28In2022,
            endringstype = Endringstype.FASTSETTING
        )

        val grunnkretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(grunnkrets.lokalid), november28In2022))!!
        val kommuneEtterLagring = kommuneService.hentKommune(Identifikator(Lokalid(kommune.lokalid), november28In2022))!!
        val fylkeEtterLagring = fylkeService.hentFylke(Identifikator(Lokalid(fylke.lokalid), november28In2022))!!
        val nasjonEtterLagring = nasjonService.hentNasjon(Identifikator(Lokalid(nasjon.lokalid), november28In2022))!!

        assertTopologi(kommuneEtterLagring.omraade, grunnkretsEtterLagring.omraade)
        assertTopologi(fylkeEtterLagring.omraade, kommuneEtterLagring.omraade)
        assertTopologi(nasjonEtterLagring.omraade, fylkeEtterLagring.omraade)
        assertThat(nasjonEtterLagring.posisjon).isEqualTo(fylkeEtterLagring.posisjon).
        isEqualTo(kommuneEtterLagring.posisjon).isEqualTo(grunnkretsEtterLagring.posisjon)

        assertThat(flaterSomErEndret).hasSize(4).extracting("id")
            .contains(grunnkretsEtterLagring.id, kommuneEtterLagring.id, fylkeEtterLagring.id, nasjonEtterLagring.id)

        assertOmradeGyldighetVersjon(grunnkretsEtterLagring, grunnkrets, november28In2022, Endringstype.FASTSETTING)
        assertOmradeGyldighetVersjon(kommuneEtterLagring, kommune, november28In2022, Endringstype.FASTSETTING)
        assertOmradeGyldighetVersjon(fylkeEtterLagring, fylke, november28In2022, Endringstype.FASTSETTING)
        assertOmradeGyldighetVersjon(nasjonEtterLagring, nasjon, november28In2022, Endringstype.FASTSETTING)
    }

    private fun assertOmradeGyldighetVersjon(
        nyGrunnkrets: Grunnkrets,
        gammelGrunnkrets: Grunnkrets,
        endringsdato: LocalDate?,
        endringstype: Endringstype
    ) {
        assertOmradeGyldighet(gammelGrunnkrets, endringsdato, nyGrunnkrets, endringstype)
        assertThat(nyGrunnkrets.version).isGreaterThan(gammelGrunnkrets.version)
    }

    private fun assertOmradeGyldighetVersjon(
        nyAdmEnhet: AdministrativEnhet,
        gammelAdmEnhet: AdministrativEnhet,
        endringsdato: LocalDate?,
        endringstype: Endringstype
    ) {
        assertOmradeGyldighet(gammelAdmEnhet, endringsdato, nyAdmEnhet, endringstype)
        assertThat(nyAdmEnhet.version).isGreaterThan(gammelAdmEnhet.version)
    }

    private fun assertOmradeGyldighet(gammel: Flate, endringsdato: LocalDate?, ny: Flate, endringstype: Endringstype) {
        assertThat(gammel.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(ny.gyldighet.gyldigFra).isEqualTo(endringsdato)
        assertThat(ny.gyldighet.gyldigTil).isNull()
        assertThat(ny.omraade).isNotEqualTo(gammel.omraade)
        assertThat(endringstype).isEqualTo(ny.auditInfo.typeEndring)
    }

    @Test
    fun `endre grense for grunnkrets skal opprette nye versjoner for grensa og dens grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(deltGrense.grunnkretser).hasSize(2)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato, endringsdato = endringsdato)
        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))!!
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))!!
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets, endringsdato)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(opprinneligDeltGrense.grunnkretser).hasSize(2)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `endre to grenser for grunnkrets skal opprette nye versjoner for grensene og deres grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(deltGrense.grunnkretser).hasSize(2)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(vestreKrets, opprinneligDato, "grenseNordVestLokalid")
        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato,
            endringsdato = endringsdato, featureForAnnenGrense)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))!!
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))!!
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets, endringsdato)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(opprinneligDeltGrense.grunnkretser).hasSize(2)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertAnnenGrenseEtterGrenseendring("grenseNordVest", "grenseNordVestLokalid", endringsdato, opprinneligDato,
            vestreKretsEtterLagring, opprinneligVestreKrets)
    }

    @Test
    fun `endre to grenser for to grunnkrets skal opprette nye versjoner for grensene og deres grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(deltGrense.grunnkretser).hasSize(2)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(oestreKrets, opprinneligDato, "grenseNordØstLokalid")
        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato,
            endringsdato = endringsdato, featureForAnnenGrense)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))!!
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))!!
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets, endringsdato)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(opprinneligDeltGrense.grunnkretser).hasSize(2)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertAnnenGrenseEtterGrenseendring("grenseNordØst", "grenseNordØstLokalid", endringsdato, opprinneligDato,
            oestreKretsEtterLagring, opprinneligOestreKrets)
    }

    @Test
    fun `endre delt grense etter oppdatering av grunnkrets skal opprette nye versjoner for grensa og dens grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val vestreGrunnkretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = vestreKrets, datoForOppdatering = datoForOppdatering)!!
        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))!!
        assertThat(deltGrense.grunnkretser).hasSize(3)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, vestreGrunnkretsEtterOppdatering.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))!!
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))!!
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets, datoForGrenseendring)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!

        //Sjekker at gammel versjon av grensa ikke har fått endra sine grunnkretser
        assertThat(opprinneligDeltGrense.grunnkretser).hasSize(3)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, vestreGrunnkretsEtterOppdatering.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, datoForGrenseendring, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `endre delt grense etter oppdatering av grunnkrets paa andre sida skal opprette nye versjoner for grensa og dens grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val oestreGrunnkretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = oestreKrets, datoForOppdatering = datoForOppdatering)!!

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))!!
        assertThat(deltGrense.grunnkretser).hasSize(3)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreGrunnkretsEtterOppdatering.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))!!
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))!!
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets, datoForGrenseendring)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!

        //Sjekker at gammel versjon av grensa ikke har fått endra sine grunnkretser
        assertThat(opprinneligDeltGrense.grunnkretser).hasSize(3)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreGrunnkretsEtterOppdatering.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, datoForGrenseendring, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `endre metadata for grense skal lagres paa ny grense`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val (_, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(oestreKrets, opprinneligDato,"grenseNordØstLokalid")
        val statistiskgrenseMetadata = featureForAnnenGrense.properties.metadata as StatistiskgrenseMetadata
        statistiskgrenseMetadata.common.informasjon = "En eller annen informasjon"
        statistiskgrenseMetadata.commonGrense.posisjonskvalitet =
            Posisjonskvalitet(maalemetode = KodelisteEntry(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, href = "maalemetodeHref"), noeyaktighet = 100)

        val flaterSomErEndret = grenseService.lagreGrenser(grenser = FeatureCollection(features = listOf(featureForAnnenGrense)),
            gyldighetsdato = endringsdato, endringstype = Endringstype.FASTSETTING
        )

        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))!!
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))!!
        assertThat(oestreKretsEtterLagring.gyldighet.gyldigTil).isNull()
        assertThat(oestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligOestreKrets.omraade)
        assertThat(oestreKretsEtterLagring.version).isGreaterThan(opprinneligOestreKrets.version)
        assertThat(oestreKretsEtterLagring.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)

        assertThat(flaterSomErEndret).containsExactly(oestreKretsEtterLagring)
        assertAnnenGrenseEtterGrenseendring("grenseNordØst", "grenseNordØstLokalid", endringsdato, opprinneligDato,
            oestreKretsEtterLagring, opprinneligOestreKrets)

        val nyGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseNordØstLokalid"), endringsdato))!!
        assertThat(nyGrense.kvalitet!!.noeyaktighet).isEqualTo(100)
        assertThat(nyGrense.kvalitet!!.maalemetode).isEqualTo(MaalemetodeKodeEnum.TERRENGMAALT.uuid)
        assertThat(nyGrense.informasjonstekst).isEqualTo("En eller annen informasjon")
        assertThat(nyGrense.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)
    }

    private fun assertOmradeGyldighetVersjon(vestreKretsEtterLagring: Grunnkrets, oestreKretsEtterLagring: Grunnkrets,
                                             opprinneligVestreKrets: Grunnkrets, opprinneligOestreKrets: Grunnkrets, endringsdato: LocalDate) {
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, opprinneligVestreKrets, endringsdato, Endringstype.VEDTATT_GRENSEJUSTERING)
        assertOmradeGyldighetVersjon(oestreKretsEtterLagring, opprinneligOestreKrets, endringsdato, Endringstype.VEDTATT_GRENSEJUSTERING)
    }

    private fun assertDeltGrenseEtterGrenseendring(
        @Suppress("SameParameterValue") deltGrenseLokalid: String, datoForGrenseendring: LocalDate, opprinneligGrense: Grense,
        vestreKretsEtterLagring: Grunnkrets, oestreKretsEtterLagring: Grunnkrets, opprinneligVestreKrets: Grunnkrets, opprinneligOestreKrets: Grunnkrets) {

        val deltGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForGrenseendring))!!
        assertThat(deltGrenseEtterGrenseendring.grunnkretser).hasSize(2)
        assertThat(deltGrenseEtterGrenseendring.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKretsEtterLagring.id, oestreKretsEtterLagring.id)

        assertThat(opprinneligGrense.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(deltGrenseEtterGrenseendring.gyldighet.gyldigTil).isNull()
        assertThat(opprinneligGrense.lokalid).isEqualTo(deltGrenseEtterGrenseendring.lokalid)
        assertThat(opprinneligGrense.grense).isNotEqualTo(deltGrenseEtterGrenseendring.grense)
        assertThat(deltGrenseEtterGrenseendring.version).isGreaterThan(opprinneligGrense.version)

        assertThat(opprinneligVestreKrets.grenser.map { it.grense.id }).contains("grenseDelt")
        assertThat(opprinneligOestreKrets.grenser.map { it.grense.id }).contains("grenseDelt")
        assertThat(vestreKretsEtterLagring.grenser.map { it.grense.id }).doesNotContain("grenseDelt")
        assertThat(oestreKretsEtterLagring.grenser.map { it.grense.id }).doesNotContain("grenseDelt")
        assertThat(vestreKretsEtterLagring.grenser.map { it.grense.lokalid }).contains(deltGrenseLokalid)
        assertThat(oestreKretsEtterLagring.grenser.map { it.grense.lokalid }).contains(deltGrenseLokalid)
    }

    private fun assertAnnenGrenseEtterGrenseendring(annenGrenseId: String, annenGrenseLokalid: String, datoForGrenseendring: LocalDate,
                                                    opprinneligDato: LocalDate, kretsEtterLagring: Grunnkrets, opprinneligKrets: Grunnkrets) {
        val opprinneligGrense = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), opprinneligDato))!!
        assertThat(opprinneligGrense.grunnkretser).hasSize(1)
        assertThat(opprinneligGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(opprinneligKrets.id)

        val annenGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), datoForGrenseendring))!!
        assertThat(annenGrenseEtterGrenseendring.grunnkretser).hasSize(1)
        assertThat(annenGrenseEtterGrenseendring.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(kretsEtterLagring.id)

        assertThat(opprinneligGrense.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(annenGrenseEtterGrenseendring.gyldighet.gyldigTil).isNull()
        assertThat(opprinneligGrense.lokalid).isEqualTo(annenGrenseEtterGrenseendring.lokalid)
        assertThat(opprinneligGrense.grense).isNotEqualTo(annenGrenseEtterGrenseendring.grense)
        assertThat(annenGrenseEtterGrenseendring.version).isGreaterThan(opprinneligGrense.version)

        assertThat(opprinneligKrets.grenser.map { it.grense.id }).contains(annenGrenseId)
        assertThat(kretsEtterLagring.grenser.map { it.grense.id }).doesNotContain(annenGrenseId)
        assertThat(kretsEtterLagring.grenser.map { it.grense.lokalid }).contains(annenGrenseLokalid)
    }

    @Test
    fun `endre delt kommunegrense etter oppdatering av grunnkrets paa andre sida skal opprette nye versjoner for grensa og dens grunnkretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretserIHverSinKommune(gyldigFra = opprinneligDato)
        val vestreKommune = vestreKrets.getKommune(opprinneligDato)!!
        val oestreKommune = oestreKrets.getKommune(opprinneligDato)!!
        saveFylkeOgUnikLokalid(fylkesnummerUuid = "03", fylkesnummer = "03", gyldigFra = opprinneligDato, kommuner = listOf(vestreKommune, oestreKommune))
        grenseRepository.flush()

        val oestreGrunnkretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = oestreKrets, datoForOppdatering = datoForOppdatering)!!
        grenseRepository.flush()

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))!!
        assertThat(deltGrense.grunnkretser).hasSize(3)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreGrunnkretsEtterOppdatering.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))!!
        val opprinneligOestreKrets = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))!!
        val oestreKretsEtterLagring = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))!!
        val vestreKommuneEtterLagring = kommuneService.hentKommune(Identifikator(Lokalid(vestreKommune.lokalid), datoForGrenseendring))!!
        val oestreKommuneEtterLagring = kommuneService.hentKommune(Identifikator(Lokalid(oestreKommune.lokalid), datoForGrenseendring))!!
        assertThat(vestreKommune.id).isNotEqualTo(vestreKommuneEtterLagring.id)
        assertThat(oestreKommune.id).isNotEqualTo(oestreKommuneEtterLagring.id)
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets, datoForGrenseendring)
        assertThat(flaterSomErEndret).hasSize(4).extracting("id")
            .contains(vestreKommuneEtterLagring.id, oestreKommuneEtterLagring.id, vestreKretsEtterLagring.id, oestreKretsEtterLagring.id)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!

        //Sjekker at gammel versjon av grensa ikke har fått endra sine grunnkretser
        assertThat(opprinneligDeltGrense.grunnkretser).hasSize(3)
        assertThat(opprinneligDeltGrense.grunnkretser.map { it.grunnkrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreGrunnkretsEtterOppdatering.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, datoForGrenseendring, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `to grunnkretser i hver sin kommune i samme fylke som splittes til to fylker`() {
        Mockito.`when`(kodelisteServiceMock.getFylkesnummerIdByCodeValue("39")).thenReturn("583a2846-e90c-4911-b8bb-b2537fd896a0")
        Mockito.`when`(kodelisteServiceMock.getFylkesnummerIdByCodeValue("40")).thenReturn("f26cc714-7289-4026-a076-35f83f9f2332")
        Mockito.`when`(kodelisteServiceMock.getKommunenummerIdByCodeValue("3909")).thenReturn("3909")
        Mockito.`when`(kodelisteServiceMock.getKommunenummerIdByCodeValue("4010")).thenReturn("4010")

        val opprinneligDato = october1In2022
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretserIHverSinKommune(gyldigFra = opprinneligDato,
            kommunenummer1 = "3812", kommunenummer2 = "3805", medToDelteGrenser = true)
        settGrensetypeForGrenserSomIkkeErDelt(vestreKrets, oestreKrets, Grensetype.Fylkesgrense)
        val gamleSiljan = vestreKrets.getKommune(opprinneligDato)!!
        val gamleLarvik = oestreKrets.getKommune(opprinneligDato)!!

        val norskNavn = saveAdministrativenhetnavn(navn = "Vestfold og Telemark", spraakid = "nor")
        val vestfoldOgTelemark = saveFylkeOgUnikLokalid(id = "fylke38", lokalid = "38VestfoldOgTelemark",
            fylkesnummerUuid = "e79b7a63-13ff-4ba5-b4ef-0dfa4e6af1df", fylkesnummer = "38",
            administrativenhetnavn = listOf(norskNavn), gyldigFra = opprinneligDato, kommuner = listOf(gamleSiljan, gamleLarvik))
        saveNasjonOgUnikLokalid(fylker = listOf(vestfoldOgTelemark))

        grenseRepository.flush()
        flateService.oppdaterFlaterFor(setOf(gamleSiljan, gamleLarvik, vestfoldOgTelemark))
        gamleSiljan.posisjon = gamleSiljan.omraade?.interiorPoint
        gamleLarvik.posisjon = gamleLarvik.omraade?.interiorPoint
        vestfoldOgTelemark.posisjon = vestfoldOgTelemark.omraade?.interiorPoint
        grenseRepository.flush()

        val vestfoldNavn = administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Vestfold", spraakForkortelse = "nor")
        val telemarkNavn = administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Telemark", spraakForkortelse = "nor")

        val nyVestfold = fylkeService.createNyttFylkeMedNyUnikLokalid(
            kopierFraFylke = vestfoldOgTelemark,
            nyttFylkesnummer = "39",
            admenhetnavnList = listOf(vestfoldNavn),
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )
        val nyTelemark = fylkeService.createNyttFylkeMedNyUnikLokalid(
            kopierFraFylke = vestfoldOgTelemark,
            nyttFylkesnummer = "40",
            admenhetnavnList = listOf(telemarkNavn),
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )
        val nyLarvik = kommuneService.createNyKommuneMedNyUnikLokalid(
            kopierFraKommune = gamleLarvik,
            nyttKommunenummer = "3909",
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )
        val nySiljan = kommuneService.createNyKommuneMedNyUnikLokalid(
            kopierFraKommune = gamleSiljan,
            nyttKommunenummer = "4010",
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )

        val nyVestreKretsINySiljan = grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(vestreKrets, nySiljan, "40190120",
            jan1In2024, Endringstype.FASTSETTING)
        val nyOestreKretsINyLarvik = grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(oestreKrets, nyLarvik, "39090121",
            jan1In2024, Endringstype.FASTSETTING)

        grenseService.endreGrensetypeForGrunnkretser(listOf(nyVestreKretsINySiljan, nyOestreKretsINyLarvik),
            jan1In2024, Endringstype.FASTSETTING)

        val hentNyVestfold = fylkeRepository.findById(nyVestfold.id).get()
        val hentNyTelemark = fylkeRepository.findById(nyTelemark.id).get()

        // Oppdaterer flaten på fylket og ser at grensene danner riktige grenser
        // Sjekker at område faktisk har endret seg (og danner gyldig polygon) + at unionen av fylkene er likt det gamle fylket
        flateService.oppdaterFlaterFor(setOf(vestfoldOgTelemark, hentNyTelemark, hentNyVestfold))
        assertThat(vestfoldOgTelemark.omraade)
            .isNotEqualTo(hentNyTelemark.omraade)
            .isNotEqualTo(hentNyVestfold.omraade)

        val kombinertOmraadeVestfoldOgTelemark = hentNyVestfold.omraade?.union(hentNyTelemark.omraade)
        val forskjellIGeometri = kombinertOmraadeVestfoldOgTelemark?.difference(vestfoldOgTelemark.omraade)
        assertThat(forskjellIGeometri?.isEmpty).isTrue()

        assertDelteGrenser(opprinneligDato, vestreKrets, oestreKrets, Grensetype.Kommunegrense,
            jan1In2024, nyVestreKretsINySiljan, nyOestreKretsINyLarvik, Grensetype.Fylkesgrense)
    }

    /**
     * Har to grunnkretser i hver sin kommune. Alle grensene er kommunegrenser.
     * G1 i K1 har grensene A-B-C-D-E-A. G2 i K2 har C-F-G-E-D-C.
     * Testen lager ny versjon av grensa D-E og endrer den til å gå mellom A og D.
     * Pga rekkefølgeendringer må det lages ny versjon av grensa C-D,
     * og ny versjon av grensa A-E siden den flyttes fra G1 til G2.
     *
     *      B -------- C -------- F           B -------- C -------- F
     *      |          |          |           |          |          |
     *      | G1 i K1  D  G2 i K2 |    -->    | G1 i K1  D  G2 i K2 |
     *      |          |          |           | ,-------'           |
     *      A -------- E -------- G           A -------- E -------- G
     */
    @Test
    fun `flytt grense til grunnkrets i annen kommune`() {
        val opprinneligDato = october1In2022
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretserIHverSinKommune(gyldigFra = opprinneligDato,
            kommunenummer1 = "3812", kommunenummer2 = "3805", medToDelteGrenser = true)
        val gamleSiljan = vestreKrets.getKommune(opprinneligDato)!!
        val gamleLarvik = oestreKrets.getKommune(opprinneligDato)!!

        val vestfoldOgTelemark = saveFylkeOgUnikLokalid(id = "fylke38", lokalid = "38VestfoldOgTelemark", fylkesnummerUuid = "e79b7a63-13ff-4ba5-b4ef-0dfa4e6af1df",
            fylkesnummer = "38", administrativenhetnavn = emptyList(), gyldigFra = opprinneligDato, kommuner = listOf(gamleSiljan, gamleLarvik))
        saveNasjonOgUnikLokalid(fylker = listOf(vestfoldOgTelemark))

        grenseRepository.flush()
        flateService.oppdaterFlaterFor(setOf(gamleSiljan, gamleLarvik))
        gamleSiljan.posisjon = gamleSiljan.omraade?.interiorPoint
        gamleLarvik.posisjon = gamleLarvik.omraade?.interiorPoint
        grenseRepository.flush()

        val vestreKoblingGrenseDelt = vestreKrets.getGrensekoblinger(gyldighetsdato = opprinneligDato).first { it.grense.lokalid == "grenseDeltLokalid" }
        val vestreKoblingGrenseDelt2 = vestreKrets.getGrensekoblinger(gyldighetsdato = opprinneligDato).first { it.grense.lokalid == "grenseDelt2Lokalid" }
        val vestreKoblingGrenseSoerVest = vestreKrets.getGrensekoblinger(gyldighetsdato = opprinneligDato).first { it.grense.lokalid == "grenseSørVestLokalid" }
        val oestreKoblingGrenseDelt = oestreKrets.getGrensekoblinger(gyldighetsdato = opprinneligDato).first { it.grense.lokalid == "grenseDeltLokalid" }
        val oestreKoblingGrenseDelt2 = oestreKrets.getGrensekoblinger(gyldighetsdato = opprinneligDato).first { it.grense.lokalid == "grenseDelt2Lokalid" }

        val gammelGrenseDelt2 = vestreKoblingGrenseDelt2.grense
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            Coordinate(0.0, 0.0),
            gammelGrenseDelt2.grense.getCoordinateN(1).copy()
        ))
        val nyGrenseDelt2 = grenseService.createNyVersjonAvGrense(gammelGrenseDelt2, november28In2022, Endringstype.KVALITETSHEVING)
            .also {
                it.grense = oppdatertGeometri
            }

        val gammelGrenseDelt = oestreKoblingGrenseDelt.grense
        val nyGrenseDelt = grenseService.createNyVersjonAvGrense(gammelGrenseDelt, november28In2022, Endringstype.KVALITETSHEVING)

        val gammelGrenseSoerVest = vestreKoblingGrenseSoerVest.grense
        val nyGrenseSoerVest = grenseService.createNyVersjonAvGrense(gammelGrenseSoerVest, november28In2022, Endringstype.KVALITETSHEVING)

        val nyVersjonAvOestreKrets = grunnkretsService.createNyVersjonAvGrunnkrets(oestreKrets, november28In2022, Endringstype.KVALITETSHEVING)
        val nyVersjonAvVestreKrets = grunnkretsService.createNyVersjonAvGrunnkrets(vestreKrets, november28In2022, Endringstype.KVALITETSHEVING)

        nyVersjonAvOestreKrets.replaceGrensekobling(gammelGrenseDelt2, nyGrenseDelt2, GrenseKoblingAttributter(
            oestreKoblingGrenseDelt2.grenseKoblingAttributter.retningMedKlokken,
            4,
            oestreKoblingGrenseDelt2.grenseKoblingAttributter.flateIndeks,
            oestreKoblingGrenseDelt2.grenseKoblingAttributter.hullIndeks
        ))

        nyVersjonAvOestreKrets.addGrensekobling(nyGrenseSoerVest, GrenseKoblingAttributter(rekkefoelge = 3))

        nyVersjonAvOestreKrets.replaceGrensekobling(gammelGrenseDelt, nyGrenseDelt, GrenseKoblingAttributter(
            oestreKoblingGrenseDelt.grenseKoblingAttributter.retningMedKlokken,
            5,
            oestreKoblingGrenseDelt.grenseKoblingAttributter.flateIndeks,
            oestreKoblingGrenseDelt.grenseKoblingAttributter.hullIndeks
        ))

        nyVersjonAvVestreKrets.replaceGrensekobling(gammelGrenseDelt2, nyGrenseDelt2, GrenseKoblingAttributter(
            vestreKoblingGrenseDelt2.grenseKoblingAttributter.retningMedKlokken,
            vestreKoblingGrenseDelt2.grenseKoblingAttributter.rekkefoelge,
            vestreKoblingGrenseDelt2.grenseKoblingAttributter.flateIndeks,
            vestreKoblingGrenseDelt2.grenseKoblingAttributter.hullIndeks))

        nyVersjonAvVestreKrets.replaceGrensekobling(gammelGrenseDelt, nyGrenseDelt, GrenseKoblingAttributter(
            vestreKoblingGrenseDelt.grenseKoblingAttributter.retningMedKlokken,
            vestreKoblingGrenseDelt.grenseKoblingAttributter.rekkefoelge,
            vestreKoblingGrenseDelt.grenseKoblingAttributter.flateIndeks,
            vestreKoblingGrenseDelt.grenseKoblingAttributter.hullIndeks))

        if (nyVersjonAvVestreKrets.grenser.removeIf { it.grense.id == "grenseSørVest" }) {
            //Grensa ble tatt vekk fra grunnkretsen. Fjerner grunnkretsen også fra grensa (koblingen fra den andre veien), ellers feiler tester
            nyGrenseSoerVest.grunnkretser.removeIf { it.grunnkrets.id == nyVersjonAvVestreKrets.id }
        }

        grenseRepository.flush()

        val nySiljan = kommuneService.createNyVersjonAvKommune(kommune = gamleSiljan, gyldigFra = november28In2022, endringstype = Endringstype.KVALITETSHEVING)
        val nyLarvik = kommuneService.createNyVersjonAvKommune(kommune = gamleLarvik, gyldigFra = november28In2022, endringstype = Endringstype.KVALITETSHEVING)

        nyVersjonAvVestreKrets.oppdaterOmraadeOgPosisjon()
        nyVersjonAvOestreKrets.oppdaterOmraadeOgPosisjon()
        flateService.oppdaterFlaterFor(setOf(nySiljan, nyLarvik))

        assertTopologi(nySiljan.omraade, nyVersjonAvVestreKrets.omraade)
        assertTopologi(nySiljan.omraade, gamleSiljan.omraade, lik = false)
        assertThat(nySiljan.posisjon).isEqualTo(nyVersjonAvVestreKrets.posisjon)

        assertTopologi(nyLarvik.omraade, nyVersjonAvOestreKrets.omraade)
        assertTopologi(nyLarvik.omraade, gamleLarvik.omraade, lik = false)
        assertThat(nyLarvik.posisjon).isEqualTo(nyVersjonAvOestreKrets.posisjon)
    }

    @Test
    fun `to nabogrunnkretser i samme kommune som skal deles til hver sin kommune`() {
        Mockito.`when`(kodelisteServiceMock.getKommunenummerIdByCodeValue("0308")).thenReturn("0308")
        Mockito.`when`(kodelisteServiceMock.getKommunenummerIdByCodeValue("0380")).thenReturn("0380")

        val opprinneligDato = LocalDate.parse("2022-01-01")
        val (vestreKrets, oestreKrets) = saveNaboGrunnkretser(gyldigFra = opprinneligDato, medToDelteGrenser = true)
        settGrensetypeForGrenserSomIkkeErDelt(vestreKrets, oestreKrets, Grensetype.Kommunegrense)
        grenseRepository.flush()

        val gamleAalesund = vestreKrets.getKommune(opprinneligDato)!!
        val moereOgRomsdal = saveFylkeOgUnikLokalid(id = "fylke03", lokalid = "03MoereOgRomsdal", fylkesnummerUuid = "03", fylkesnummer = "03",
            gyldigFra = opprinneligDato, kommuner = listOf(gamleAalesund))
        saveNasjonOgUnikLokalid(fylker = listOf(moereOgRomsdal))

        grenseRepository.flush()
        flateService.oppdaterFlaterFor(setOf(gamleAalesund))
        gamleAalesund.posisjon = gamleAalesund.omraade?.interiorPoint
        grenseRepository.flush()

        val haram = kommuneService.createNyKommuneMedNyUnikLokalid(
            kopierFraKommune = gamleAalesund,
            nyttKommunenummer = "0380",
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )
        val nyAalesund = kommuneService.createNyKommuneMedNyUnikLokalid(
            kopierFraKommune = gamleAalesund,
            nyttKommunenummer = "0308",
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )

        val nyVestreKretsINyAalesund = grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(vestreKrets, nyAalesund, "03080120",
            jan1In2024, Endringstype.FASTSETTING)
        val nyOestreKretsIHaram = grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(oestreKrets, haram, "03800121",
            jan1In2024, Endringstype.FASTSETTING)

        grenseService.endreGrensetypeForGrunnkretser(listOf(nyVestreKretsINyAalesund, nyOestreKretsIHaram),
            jan1In2024, Endringstype.FASTSETTING)

        val hentNyAalesund = kommuneRepository.findById(nyAalesund.id).get()
        val hentHaram = kommuneRepository.findById(haram.id).get()

        // Oppdaterer flaten på kommunene og ser at grensene danner riktige polygoner
        // Sjekker at område faktisk har endret seg (og danner gyldig polygon) + at unionen av kommunene er likt den gamle kommunen
        flateService.oppdaterFlaterFor(setOf(hentHaram, hentNyAalesund, gamleAalesund))
        assertThat(gamleAalesund.omraade)
            .isNotEqualTo(hentNyAalesund.omraade)
            .isNotEqualTo(hentHaram.omraade)

        val kombinertOmraadeAalesundOgHaram = hentNyAalesund.omraade?.union(hentHaram.omraade)
        val forskjellIGeometri = kombinertOmraadeAalesundOgHaram?.difference(gamleAalesund.omraade)
        assertThat(forskjellIGeometri?.isEmpty).isTrue()

        assertDelteGrenser(opprinneligDato, vestreKrets, oestreKrets, Grensetype.Grunnkretsgrense,
            jan1In2024, nyVestreKretsINyAalesund, nyOestreKretsIHaram, Grensetype.Kommunegrense)
    }

    @Test
    fun `delGrunnkrets klarer å opprette en ny grunnkrets`(){
        val gyldighetsdato = jan1In2023

        val opprinneligGrunnkrets = saveGrunnkrets("opprinneligKrets", Lokalid("opprinneligKrets"))

        saveKommune(grunnkretser = listOf(opprinneligGrunnkrets), gyldigFra = gyldighetsdato)

        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(opprinneligGrunnkrets.lokalid, opprinneligGrunnkrets.version),
            Flatetype.GRUNNKRETS,
            "nyGrunnkrets",
            "1"
        )

        val nyGrunnkrets = grunnkretsService.delGrunnkrets(delingEndringRequest, gyldighetsdato, Endringstype.VEDTATT_DELING)

        assertThat(nyGrunnkrets).isNotNull
        assertThat(nyGrunnkrets.grenser.size).isEqualTo(0)
        assertThat(nyGrunnkrets.utledetFra?.lokalid).isEqualTo(opprinneligGrunnkrets.lokalid)
        assertThat(nyGrunnkrets.grunnkretsnummer).isEqualTo(delingEndringRequest.kretsNummer)
        assertThat(nyGrunnkrets.grunnkretsnavn).isEqualTo(delingEndringRequest.kretsNavn)
        assertThat(nyGrunnkrets.version).isEqualTo(1)
    }

    @Test
    fun `delGrunnkrets kaster hvis feil flatetype er satt i request`(){
        val opprinneligGrunnkrets = saveGrunnkrets("opprinneligKrets", Lokalid("opprinneligKrets"))

        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(opprinneligGrunnkrets.lokalid, opprinneligGrunnkrets.version),
            Flatetype.STEMMEKRETS,
            "nyGrunnkrets",
            "1"
        )

        Assertions.assertThatThrownBy {
            grunnkretsService.delGrunnkrets(delingEndringRequest, jan1In2023, Endringstype.VEDTATT_DELING)
        }
            .isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Kan ikke dele og opprette en ny ${delingEndringRequest.flatetype} fra en grunnkrets")
    }

    @Test
    fun `delGrunnkrets kaster exception hvis ny grunnkrets skaper duplikat`(){
        val gyldighetsdato = jan1In2023

        val opprinneligGrunnkrets = saveGrunnkrets("opprinneligKrets", Lokalid("opprinneligKrets"))

        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(opprinneligGrunnkrets.lokalid, opprinneligGrunnkrets.version),
            Flatetype.GRUNNKRETS,
            "nyStemmekrets",
            "1"
        )
        val eksiterendeGrunnkretsSomTriggerDuplikat = saveGrunnkrets("eksisterende", Lokalid("eksisterende"), grunnkretsnummer = delingEndringRequest.kretsNummer)

        saveKommune(grunnkretser = listOf(opprinneligGrunnkrets, eksiterendeGrunnkretsSomTriggerDuplikat), gyldigFra = gyldighetsdato)

        Assertions.assertThatThrownBy {
            grunnkretsService.delGrunnkrets(delingEndringRequest, gyldighetsdato, Endringstype.VEDTATT_DELING)
        }
            .isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Kretsnummer 1 er brukt på flere grunnkretser innenfor kommunen (0301 UKJENT KOMMUNE) med gyldighet som overlapper med 01.01.2023 og fremover. Vennligst gi grunnkretsene unike kretsnumre før du fortsetter.")
    }

    @Test
    fun `delGrunnkrets kaster OptimistiksLaasException ved versjonsfeil på den opprinnelige kretsen`(){
        val opprinneligGrunnkrets = saveGrunnkrets("opprinneligKrets", Lokalid("opprinneligKrets"))

        saveKommune(grunnkretser = listOf(opprinneligGrunnkrets), gyldigFra = jan1In2023)

        grunnkretsService.createNyVersjonAvGrunnkrets(opprinneligGrunnkrets, gyldigFra = jan1In2023, Endringstype.RETTING)

        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(opprinneligGrunnkrets.lokalid, opprinneligGrunnkrets.version),
            Flatetype.GRUNNKRETS,
            "nyGrunnkrets",
            "1"
        )

        Assertions.assertThatThrownBy {
            grunnkretsService.delGrunnkrets(delingEndringRequest, jan1In2023, Endringstype.VEDTATT_DELING)
        }
            .isInstanceOf(OptimistiskLaasException::class.java)
            .hasMessageContaining("Det er ikke samsvar med versjon")
    }

    private fun assertDelteGrenser(oensketGammelDato: LocalDate, oensketGammelKrets1: Grunnkrets, oensketGammelKrets2: Grunnkrets,
                                   oensketGammelGrensetype: Grensetype,
                                   oensketNyDato: LocalDate, oensketNyKrets1: Grunnkrets, oensketNyKrets2: Grunnkrets, oensketNyGrensetype: Grensetype) {
        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseDeltLokalid"), oensketGammelDato))!!
        assertThat(deltGrense.grunnkretser).hasSize(2)
        assertThat(deltGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(oensketGammelKrets1.id, oensketGammelKrets2.id)
        assertThat(deltGrense.grensetype).isEqualTo(oensketGammelGrensetype)

        val deltGrense2 = grenseService.hentGrense(Identifikator(Lokalid("grenseDelt2Lokalid"), oensketGammelDato))!!
        assertThat(deltGrense2.grunnkretser).hasSize(2)
        assertThat(deltGrense2.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(oensketGammelKrets1.id, oensketGammelKrets2.id)
        assertThat(deltGrense2.grensetype).isEqualTo(oensketGammelGrensetype)

        val nyDeltGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseDeltLokalid"), oensketNyDato))!!
        assertThat(nyDeltGrense.grunnkretser).hasSize(2)
        assertThat(nyDeltGrense.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(oensketNyKrets1.id, oensketNyKrets2.id)
        assertThat(nyDeltGrense.grensetype).isEqualTo(oensketNyGrensetype)

        val nyDeltGrense2 = grenseService.hentGrense(Identifikator(Lokalid("grenseDelt2Lokalid"), oensketNyDato))!!
        assertThat(nyDeltGrense2.grunnkretser).hasSize(2)
        assertThat(nyDeltGrense2.grunnkretser.map { it.grunnkrets.id }).containsExactlyInAnyOrder(oensketNyKrets1.id, oensketNyKrets2.id)
        assertThat(nyDeltGrense2.grensetype).isEqualTo(oensketNyGrensetype)
    }

    fun settGrensetypeForGrenserSomIkkeErDelt(vestreKrets: Grunnkrets, oestreKrets: Grunnkrets, grensetype: Grensetype) {
        vestreKrets.grenser.stream().forEach { if (it.grense.id != "grenseDelt" && it.grense.id != "grenseDelt2") it.grense.grensetype = grensetype }
        oestreKrets.grenser.stream().forEach { if (it.grense.id != "grenseDelt" && it.grense.id != "grenseDelt2") it.grense.grensetype = grensetype }
    }

    private fun utfoerMetadataoppdatering(kretsSomSkalOppdateres: Grunnkrets, datoForOppdatering: LocalDate): Grunnkrets? {
        val grunnkretsRequest = GrunnkretsRequestBuilder().withLokalId(kretsSomSkalOppdateres.lokalid)
            .withGrunnkretsnummer(kretsSomSkalOppdateres.grunnkretsnummer!!).build()
        val identifikator = Identifikator(Lokalid(grunnkretsRequest.identifikasjon.lokalid), datoForOppdatering)

        grunnkretsService.oppdaterGrunnkrets(
            lokalId = identifikator.lokalid,
            oppdatertGrunnkrets = grunnkretsRequest,
            gyldigFra = datoForOppdatering,
            endringstype = Endringstype.RETTING
        )
        return grunnkretsService.hentGrunnkrets(identifikator)
    }

    private fun utfoerGrenseendring(
        kretsSomSkalHaGrenseendring: Grunnkrets,
        opprinneligDato: LocalDate,
        endringsdato: LocalDate,
        featureAnnenOppdatertGrense: Feature? = null,
    ): Set<Flate> {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrensekoblinger(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == "grenseDeltLokalid" }

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            Coordinate(10.0, 10.0),
            Coordinate(5.0, 5.0),
            Coordinate(10.0, 0.0)
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(opprinneligDato)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }

        val features = if (featureAnnenOppdatertGrense != null) listOf(oppdatertGrensefeature, featureAnnenOppdatertGrense) else listOf(oppdatertGrensefeature)
        return grenseService.lagreGrenser(
            grenser = FeatureCollection(features = features),
            gyldighetsdato = endringsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
    }

    private fun lagFeatureEndreYkoordinat(kretsSomSkalHaGrenseendring: Grunnkrets, opprinneligDato: LocalDate, lokalidGrenseSomSkalEndres: String): Feature {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrensekoblinger(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == lokalidGrenseSomSkalEndres }
        val grense = grenseKobling.grense.grense
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            grense.getCoordinateN(0).copy(),
            grense.getCoordinateN(1).copy().also { it.setY(it.getY() + 4) },
            grense.getCoordinateN(2).copy()
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(opprinneligDato)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }
        return oppdatertGrensefeature
    }

    private fun lagGrenserForGrunnkrets(grunnkretsLokalid: Lokalid,
                                        kommune: Kommune = KommuneBuilder.DEFAULT_KOMMUNE,
                                        grensetype: Grensetype = Grensetype.Grunnkretsgrense): Grunnkrets {
        val x1 = 23857.0
        val y1 = 675635.0

        val x2 = 23867.0
        val y2 = 675635.0

        val x3 = 23862.0
        val y3 = 675645.0

        val grense1 = grenseRepository.save(GrenseBuilder(id = "1", lokalId = Lokalid("850"), gyldigFra = october1In2022)
            .withGrenseGeometri(listOf(Coordinate(x1, y1), Coordinate(x2, y2)))
            .withGrensetype(grensetype).build())

        val grense2 = grenseRepository.save(GrenseBuilder(id = "2", lokalId = Lokalid("851"), gyldigFra = october1In2022)
            .withGrenseGeometri(listOf(Coordinate(x3, y3), Coordinate(x2, y2)))
            .withGrensetype(grensetype).build())

        val grense3 = grenseRepository.save(GrenseBuilder(id = "3", lokalId = Lokalid("852"), gyldigFra = october1In2022)
            .withGrenseGeometri(listOf(Coordinate(x3, y3), Coordinate(x1, y1)))
            .withGrensetype(grensetype).build())

        grenseRepository.flush()

        val grunnkrets = saveGrunnkrets(lokalid = grunnkretsLokalid, grenser = listOf(grense1, grense2, grense3))
        kommune.addGrunnkrets(grunnkrets)
        return grunnkrets
    }

    private fun saveGrunnkretsMed1FramtidigVersjon(grunnkretsLokalid: Lokalid) {
        val grunnkrets = saveGrunnkrets(
            id = "9000",
            lokalid = grunnkretsLokalid,
            gyldigFra = october1In2022,
            gyldigTil = jan1In2023
        )
        val grunnkretsFramtidigVersjon = saveGrunnkrets(
            id = "9001",
            lokalid = grunnkretsLokalid,
            grunnkretsnavn = "Måsvann",
            gyldigFra = jan1In2023,
        )
        saveKommuneOgUnikLokalid(grunnkretser = listOf(grunnkrets, grunnkretsFramtidigVersjon))
    }

    fun saveNaboGrunnkretser(gyldigFra: LocalDate, medHull: Boolean = false, medToDelteGrenser: Boolean = false): List<Grunnkrets> {
        val grenserForVest = if (medHull) {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Grunnkretsgrense, medToDelteGrenser) +
                GrenseHelper.getHullVestligKrets(gyldigFra, Grensetype.Grunnkretsgrense)
        } else {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Grunnkretsgrense, medToDelteGrenser)
        }

        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForVest = grenseRepository.saveAll(grenserForVest)
        val lagredeGrenserForOest = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, Grensetype.Grunnkretsgrense, medToDelteGrenser))
        grenseRepository.flush()

        val kretsVest = saveGrunnkrets(
            id = "1",
            lokalid = Lokalid("123"),
            grunnkretsnummer = "03010020",
            grunnkretsnavn = "Vestre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForVest
        )

        val kretsOest = saveGrunnkrets(
            id = "2",
            lokalid = Lokalid("234"),
            grunnkretsnummer = "03010021",
            grunnkretsnavn = "Østre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForOest
        )

        saveKommuneOgUnikLokalid(grunnkretser = listOf(kretsVest, kretsOest), gyldigFra = gyldigFra)
        grenseRepository.flush()
        return listOf(kretsVest, kretsOest)
    }

    private fun saveNaboGrunnkretserIHverSinKommune(gyldigFra: LocalDate, medHull: Boolean = false, medToDelteGrenser: Boolean = false,
                                                    kommunenummer1: String = "0301", kommunenummer2: String = "0302"): List<Grunnkrets> {
        val grenserForVest = if (medHull) {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Kommunegrense, medToDelteGrenser) +
                GrenseHelper.getHullVestligKrets(gyldigFra, Grensetype.Kommunegrense)
        } else {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Kommunegrense, medToDelteGrenser)
        }

        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForVest = grenseRepository.saveAll(grenserForVest)
        val lagredeGrenserForOest = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, Grensetype.Kommunegrense, medToDelteGrenser))
        grenseRepository.flush()

        val kretsVest = saveGrunnkrets(
            id = "1",
            lokalid = Lokalid("123"),
            grunnkretsnummer = kommunenummer1 + "0020",
            grunnkretsnavn = "Vestre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForVest
        )

        val kretsOest = saveGrunnkrets(
            id = "2",
            lokalid = Lokalid("234"),
            grunnkretsnummer = kommunenummer2 + "0021",
            grunnkretsnavn = "Østre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForOest
        )

        saveKommuneOgUnikLokalid(id = "idKommuneVest", lokalid = "kommuneVest", grunnkretser = listOf(kretsVest), gyldigFra = gyldigFra,
            kommunenummerUuid = kommunenummer1, kommunenummer = kommunenummer1)
        saveKommuneOgUnikLokalid(id = "idKommuneOest", lokalid = "kommuneOest", grunnkretser = listOf(kretsOest), gyldigFra = gyldigFra,
            kommunenummerUuid = kommunenummer2, kommunenummer = kommunenummer2)
        grenseRepository.flush()
        return listOf(kretsVest, kretsOest)
    }

}
