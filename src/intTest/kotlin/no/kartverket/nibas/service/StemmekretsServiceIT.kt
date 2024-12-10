package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.KodelisteEntry
import no.kartverket.nibas.api.v1.common.geojson.metadata.KommunalKretsgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.Posisjonskvalitet
import no.kartverket.nibas.api.v1.common.getUnikeKretsDelingEndringRequests
import no.kartverket.nibas.api.v1.request.KretsDelingEndringRequest
import no.kartverket.nibas.api.v1.request.KretsNavnOgNummer
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.common.Operasjoner
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.builders.UtkastRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.domain.utkast.UtkastBuilder
import no.kartverket.nibas.extensions.geometry.geometryToDto
import no.kartverket.nibas.extensions.geometry.grenseToFeature
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.OptimistiskLaasException
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
import kotlin.test.assertFailsWith

@ActiveProfiles("security-off")
@Transactional
class StemmekretsServiceIT : TestWithDbContainer() {

    @MockkBean
    private lateinit var kodelisteServiceMock: KodelisteService

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)
    private val sammenslaaingDato =  LocalDate.of(2022, Month.FEBRUARY, 1)
    private val datoEtterSammanslaaing = LocalDate.of(2022, Month.MARCH, 1)
    private val datoFoerSammenslaaing = LocalDate.of(2021, Month.JANUARY, 1)

    private val stemmekretsLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-88c320137ba6")
    private val grenseLokallid = Lokalid("888")

    @Test
    fun `hent eksisterende stemmekrets med dagens dato`() {
        val stemmekrets = saveStemmekrets(lokalid = stemmekretsLokalid)
        saveKommuneOgUnikLokalid(stemmekretser = listOf(stemmekrets))

        assertThat(stemmekretsRepository.findAllByLokalid(stemmekretsLokalid.value).size).isEqualTo(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(stemmekretsService.hentStemmekrets(Identifikator(stemmekretsLokalid, november28In2022))).isNotNull
    }

    @Test
    fun `hent versjon av stemmekrets fram i tid`() {
        saveStemmekretsMed1FramtidigVersjon(stemmekretsLokalid)
        assertThat(stemmekretsRepository.findAllByLokalid(stemmekretsLokalid.value).size).isEqualTo(2)

        val framtidigStemmekrets = stemmekretsService.hentStemmekrets(Identifikator(stemmekretsLokalid, jan1In2023))!!
        assertThat(framtidigStemmekrets.stemmekretsnavn).isEqualTo("Nedre Singsaker")
    }

    @Test
    fun `finn stemmekrets med samme stemmekretsnummer og ulik lokalid`() {
        saveStemmekretsMed1FramtidigVersjon(stemmekretsLokalid)
        grenseRepository.flush()

        val stemmekrets = stemmekretsService.hentStemmekrets(Identifikator(stemmekretsLokalid, jan1In2023))!!
        val muligeStemmekretsnumre = lagMuligeStemmekretsnumre(stemmekrets.stemmekretsnummer!!)

        //Uten å ta hensyn til gyldighet og med vilkårlig lokalid skal det finnes to stemmekretser med gitt stemmekretsnummer
        val kommune = stemmekrets.getKommune(jan1In2023)!!
        var stemmekretser = stemmekretsRepository.findAllByStemmekretsnummerInAndLokalidIsNotAndKommuneLokalid(
            muligeStemmekretsnumre, "xx", kommune.lokalid)
        assertThat(stemmekretser).hasSize(2)

        //Uten å ta hensyn til gyldighet og med gitt lokalid som ikke skal finnes skal det ikke finnes noen stemmekretser med gitt stemmekretsnummer
        //Dvs det finnes ikke noen ANDRE stemmekretser enn den som skal valideres med gitt stemmekretsnummer og kommune
        stemmekretser = stemmekretsRepository.findAllByStemmekretsnummerInAndLokalidIsNotAndKommuneLokalid(
            muligeStemmekretsnumre, stemmekrets.lokalid, kommune.lokalid)
        assertThat(stemmekretser).isEmpty()
    }

    @Test
    fun `hent antall framtidige versjoner`() {
        saveStemmekretsMed1FramtidigVersjon(stemmekretsLokalid)
        assertThat(stemmekretsService.hentAntallGyldigeVersjonerFramITidFor(Identifikator(stemmekretsLokalid, november28In2022))).isEqualTo(1)
    }

    @Test
    fun `hent grenser for stemmekrets fram i tid`() {
        val identifikatorFor1Jan23 = Identifikator(stemmekretsLokalid, jan1In2023)

        val grense3 = saveGrenseAvTypeKommunegrense(id = "4002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrenseAvTypeKommunegrense(id = "4001", lokalid = grenseLokallid, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrenseAvTypeKommunegrense(id = "4000", lokalid = grenseLokallid, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        val stemmekrets1 = saveStemmekrets(id = "301", lokalid = stemmekretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val stemmekrets2 = saveStemmekrets(id = "302", lokalid = stemmekretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1, grense2))
        val stemmekrets3 = saveStemmekrets(id = "303", lokalid = stemmekretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense3))

        saveKommuneOgUnikLokalid(stemmekretser = listOf(stemmekrets1, stemmekrets2, stemmekrets3))

        val grensekoblinger = stemmekretsService.hentGrensekoblingerForStemmekrets(identifikatorFor1Jan23)

        assertGrenser(
            grensekoblinger,
            grenseIGrensekoblingId = "4001",
            enhetKobletTilGrenseLokalid = stemmekretsLokalid)
    }

    @Test
    fun `hentFramtidigeVersjonerForStemmekrets skal returnere en liste hvis det finnes nyere versjoner`() {
        saveStemmekrets(id = "301", lokalid = stemmekretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022)
        saveStemmekrets(id = "302", lokalid = stemmekretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024)
        saveStemmekrets(id = "303", lokalid = stemmekretsLokalid, gyldigFra = jan1In2024)

        assertThat(stemmekretsService.hentFramtidigeVersjonerForStemmekrets(stemmekretsLokalid, december1In2022)).hasSize(2)
    }

    @Test
    fun `hentFramtidigeVersjonerForStemmekrets skal returnere en liste med kun det gyldige elementet om det er ingen framtidige endringer`() {
        saveStemmekrets(id = "301", lokalid = stemmekretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022)
        saveStemmekrets(id = "302", lokalid = stemmekretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024)

        assertThat(stemmekretsService.hentFramtidigeVersjonerForStemmekrets(stemmekretsLokalid, december1In2022)).hasSize(1)
    }

    @Test
    fun `oppdaterStemmekrets skal lage ny stemmekrets og sette gyldigtil for gammel krets`() {
        val stemmekretsRequest = StemmekretsRequestBuilder.DEFAULT_REQUEST
        assertThat(stemmekretsRequest.tellekretsnummer).isNotNull()
        assertThat(stemmekretsRequest.tellekretsnavn).isNotNull()

        val identifikator = Identifikator(Lokalid(stemmekretsRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Stemmekretsgrense))
        grenseRepository.flush()

        val stemmekrets = saveStemmekrets(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )

        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", stemmekretser = listOf(stemmekrets))

        val now = LocalDate.now()
        val opprinneligStemmekrets = stemmekretsService.hentStemmekrets(identifikator)!!

        stemmekretsService.oppdaterStemmekrets(
            lokalId = identifikator.lokalid,
            oppdatertStemmekrets = stemmekretsRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligStemmekretsEtter = stemmekretsService.hentStemmekrets(identifikator)!!
        val stemmekretsEtter = stemmekretsService.hentStemmekrets(Identifikator(identifikator.lokalid))!!

        assertThat(stemmekretsEtter.stemmekretsnummer).isNotEqualTo(stemmekrets.stemmekretsnummer)
            .isEqualTo(stemmekretsRequest.nummer)
        assertThat(stemmekretsEtter.stemmekretsnavn).isNotEqualTo(stemmekrets.stemmekretsnavn)
            .isEqualTo(stemmekretsRequest.navn)
        assertThat(stemmekretsEtter.omraade).isEqualTo(stemmekrets.omraade)
        assertThat(stemmekretsEtter.posisjon).isEqualTo(stemmekrets.posisjon)
        assertThat(stemmekretsEtter.gyldighet.gyldigFra).isEqualTo(now)
        assertThat(stemmekretsEtter.tellekretsnummer).isNotNull().isNotEqualTo(stemmekrets.tellekretsnummer).isEqualTo(stemmekretsRequest.tellekretsnummer)
        assertThat(stemmekretsEtter.tellekretsnavn).isNotNull().isNotEqualTo(stemmekrets.tellekretsnavn).isEqualTo(stemmekretsRequest.tellekretsnavn)

        assertThat(opprinneligStemmekretsEtter.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligStemmekretsEtter.gyldighet.gyldigTil).isEqualTo(now)

        val grenseObjIdsForOpprinneligStemmekrets = opprinneligStemmekrets.grenser.map { it.grense.id }
        val grenseObjIdsForOpprinneligStemmekretsEtter = opprinneligStemmekretsEtter.grenser.map { it.grense.id }
        val grenseObjIdsForNyStemmekrets = stemmekretsEtter.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligStemmekrets).containsAll(grenseObjIdsForOpprinneligStemmekretsEtter)
        assertThat(grenseObjIdsForOpprinneligStemmekrets).containsAll(grenseObjIdsForNyStemmekrets)
        assertThat(grenseObjIdsForNyStemmekrets).containsAll(grenseObjIdsForOpprinneligStemmekrets)
        assertThat(grenseObjIdsForNyStemmekrets).containsAll(grenseObjIdsForOpprinneligStemmekretsEtter)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligStemmekrets)
        assertThat(grenser).hasSize(4)
        grenser.forEach { grense ->
            assertThat(grense.stemmekretser.map { it.stemmekrets.id }).hasSize(2).containsExactlyInAnyOrder(opprinneligStemmekrets.id, stemmekretsEtter.id)
        }
    }


    @Test
    fun `oppdaterStemmekrets skal kunne blanke ut tellekrets`() {
        val stemmekretsRequest = StemmekretsRequestBuilder.DEFAULT_REQUEST.copy(tellekretsnummer = null, tellekretsnavn = null)

        val identifikator = Identifikator(Lokalid(stemmekretsRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Stemmekretsgrense))
        grenseRepository.flush()

        val stemmekrets = saveStemmekrets(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )
        assertThat(stemmekrets.tellekretsnummer).isNotNull()
        assertThat(stemmekrets.tellekretsnavn).isNotNull()

        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", stemmekretser = listOf(stemmekrets))

        val now = LocalDate.now()
        val opprinneligStemmekrets = stemmekretsService.hentStemmekrets(identifikator)!!

        stemmekretsService.oppdaterStemmekrets(
            lokalId = identifikator.lokalid,
            oppdatertStemmekrets = stemmekretsRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligStemmekretsEtter = stemmekretsService.hentStemmekrets(identifikator)!!
        val stemmekretsEtter = stemmekretsService.hentStemmekrets(Identifikator(identifikator.lokalid))!!

        assertThat(stemmekretsEtter.stemmekretsnummer).isNotEqualTo(stemmekrets.stemmekretsnummer)
            .isEqualTo(stemmekretsRequest.nummer)
        assertThat(stemmekretsEtter.stemmekretsnavn).isNotEqualTo(stemmekrets.stemmekretsnavn)
            .isEqualTo(stemmekretsRequest.navn)
        assertThat(stemmekretsEtter.omraade).isEqualTo(stemmekrets.omraade)
        assertThat(stemmekretsEtter.posisjon).isEqualTo(stemmekrets.posisjon)
        assertThat(stemmekretsEtter.gyldighet.gyldigFra).isEqualTo(now)
        assertThat(stemmekretsEtter.tellekretsnummer).isNull()
        assertThat(stemmekretsEtter.tellekretsnavn).isNull()

        assertThat(opprinneligStemmekretsEtter.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligStemmekretsEtter.gyldighet.gyldigTil).isEqualTo(now)
        assertThat(opprinneligStemmekretsEtter.tellekretsnummer).isNotNull()
        assertThat(opprinneligStemmekretsEtter.tellekretsnavn).isNotNull()

        val grenseObjIdsForOpprinneligStemmekrets = opprinneligStemmekrets.grenser.map { it.grense.id }
        val grenseObjIdsForOpprinneligStemmekretsEtter = opprinneligStemmekretsEtter.grenser.map { it.grense.id }
        val grenseObjIdsForNyStemmekrets = stemmekretsEtter.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligStemmekrets).containsAll(grenseObjIdsForOpprinneligStemmekretsEtter)
        assertThat(grenseObjIdsForOpprinneligStemmekrets).containsAll(grenseObjIdsForNyStemmekrets)
        assertThat(grenseObjIdsForNyStemmekrets).containsAll(grenseObjIdsForOpprinneligStemmekrets)
        assertThat(grenseObjIdsForNyStemmekrets).containsAll(grenseObjIdsForOpprinneligStemmekretsEtter)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligStemmekrets)
        assertThat(grenser).hasSize(4)
        grenser.forEach { grense ->
            assertThat(grense.stemmekretser.map { it.stemmekrets.id }).hasSize(2).containsExactlyInAnyOrder(opprinneligStemmekrets.id, stemmekretsEtter.id)
        }
    }

    @Test
    fun `oppdaterStemmekrets skal kaste exception ved ugyldig kommune for stemmekretsen`() {
        val stemmekretsRequest = StemmekretsRequestBuilder.DEFAULT_REQUEST

        val identifikator = Identifikator(Lokalid(stemmekretsRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Stemmekretsgrense))
        grenseRepository.flush()

        val stemmekrets = saveStemmekrets(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )

        //Kommunen har gyldig fra etter stemmekretsens gyldig fra, og gyldig til før ønsket oppdateringsdato
        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", gyldigFra = november28In2022, gyldigTil = jan1In2023, stemmekretser = listOf(stemmekrets))
        val now = LocalDate.now()

        assertThatThrownBy {
            stemmekretsService.oppdaterStemmekrets(
                lokalId = identifikator.lokalid,
                oppdatertStemmekrets = stemmekretsRequest,
                gyldigFra = now,
                endringstype = Endringstype.RETTING
            )
        }.isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fant ikke kommune for stemmekrets med id ${stemmekrets.id}")

        assertThatThrownBy {
            stemmekretsService.createNyVersjonAvStemmekrets(stemmekrets = stemmekrets, gyldigFra = now, endringstype = Endringstype.RETTING)
        }.isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fant ikke kommune for stemmekrets med id ${stemmekrets.id}")

        assertThatThrownBy {
            stemmekretsService.finnesAnnenStemmekretsMedSammeKommuneOgStemmekretsnummer(stemmekrets = stemmekrets)
        }.isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fant ikke kommune for stemmekrets med id ${stemmekrets.id}")
    }

    @Test
    fun `createNyVersjonAvStemmekretsIAnnenKommune skal lage ny stemmekrets og sette gyldigtil for gammel krets`() {
        val identifikator = Identifikator(Lokalid("stemmekretsSomSkalFlyttesTilHaram"), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Stemmekretsgrense))
        grenseRepository.flush()

        val stemmekrets = saveStemmekrets(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )

        val gamleAalesund = saveKommuneOgUnikLokalid(id = "idGamleAalesund", lokalid = "1507Ålesund", kommunenummerUuid = "afc65fde-8a62-497d-b35d-77345efddee3",
            kommunenummer = "1507", stemmekretser = listOf(stemmekrets))
        val haram = saveKommuneOgUnikLokalid(id = "idHaram", lokalid = "1580Haram", kommunenummerUuid = "687f3337-899f-42f8-849d-6f2d5b603dd3",
            kommunenummer = "1580")
        saveFylkeOgUnikLokalid(lokalid = "15MøreOgRomsdal", fylkesnummerUuid = "c499a315-0cd5-4e99-8acf-bd6f4803799a",
            fylkesnummer = "15", kommuner = listOf(gamleAalesund, haram))
        val opprinneligStemmekrets = stemmekretsService.hentStemmekrets(identifikator)!!

        assertThat(gamleAalesund.kommuneLokalid.stemmekretser).hasSize(1).extracting("id").contains(stemmekrets.id)
        assertThat(haram.kommuneLokalid.stemmekretser).isEmpty()

        val stemmekretsIHaram = stemmekretsService.createNyVersjonAvStemmekretsIAnnenKommune(stemmekrets = stemmekrets, kommune = haram,
            gyldigFra = jan1In2024, endringstype = Endringstype.IMPORT)

        val opprinneligStemmekretsEtter = stemmekretsService.hentStemmekrets(identifikator)!!
        val stemmekretsEtter = stemmekretsService.hentStemmekrets(Identifikator(identifikator.lokalid, jan1In2024))!!

        assertThat(stemmekretsIHaram.id).isNotEqualTo(stemmekrets.id).isEqualTo(stemmekretsEtter.id)
        assertThat(stemmekretsIHaram.lokalid).isEqualTo(stemmekretsEtter.lokalid).isEqualTo(stemmekrets.lokalid)
        assertThat(stemmekretsIHaram.stemmekretsnummer).isEqualTo(stemmekretsEtter.stemmekretsnummer).isEqualTo(stemmekrets.stemmekretsnummer)
        assertThat(stemmekretsIHaram.stemmekretsnavn).isEqualTo(stemmekretsEtter.stemmekretsnavn).isEqualTo(stemmekrets.stemmekretsnavn)
        assertThat(stemmekretsEtter.omraade).isEqualTo(stemmekrets.omraade)
        assertThat(stemmekretsEtter.posisjon).isEqualTo(stemmekrets.posisjon)
        assertThat(stemmekretsEtter.gyldighet.gyldigFra).isEqualTo(jan1In2024)

        assertThat(opprinneligStemmekretsEtter.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligStemmekretsEtter.gyldighet.gyldigTil).isEqualTo(jan1In2024)

        val gamleAalesundEtter = kommuneRepository.findById(gamleAalesund.id).get()
        val haramEtter = kommuneRepository.findById(haram.id).get()
        assertThat(gamleAalesundEtter.kommuneLokalid.stemmekretser).hasSize(1).extracting("id").contains(stemmekrets.id)
        assertThat(haramEtter.kommuneLokalid.stemmekretser).hasSize(1).extracting("id").contains(stemmekretsIHaram.id)

        val grenseObjIdsForOpprinneligStemmekrets = opprinneligStemmekrets.grenser.map { it.grense.id }
        val grenseObjIdsForOpprinneligStemmekretsEtter = opprinneligStemmekretsEtter.grenser.map { it.grense.id }
        val grenseObjIdsForNyStemmekrets = stemmekretsEtter.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligStemmekrets).containsAll(grenseObjIdsForOpprinneligStemmekretsEtter)
        assertThat(grenseObjIdsForOpprinneligStemmekrets).containsAll(grenseObjIdsForNyStemmekrets)
        assertThat(grenseObjIdsForNyStemmekrets).containsAll(grenseObjIdsForOpprinneligStemmekrets)
        assertThat(grenseObjIdsForNyStemmekrets).containsAll(grenseObjIdsForOpprinneligStemmekretsEtter)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligStemmekrets)
        assertThat(grenser).hasSize(4)
        grenser.forEach { grense ->
            assertThat(grense.stemmekretser.map { it.stemmekrets.id }).hasSize(2).containsExactlyInAnyOrder(opprinneligStemmekrets.id, stemmekretsEtter.id)
        }
    }

    @Test
    fun `slaaSammenStemmekretser ny krets skal bruke viderefoert navn og nummer`() {
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = LocalDate.parse("2022-01-01"))

        val sammenslattStemmekrets = utfoerSammenslaaing(vestreKrets, oestreKrets)

        assertThat(sammenslattStemmekrets?.stemmekretsnavn).isEqualTo("Sammenslått krets")
        assertThat(sammenslattStemmekrets?.stemmekretsnummer).isEqualTo("999")
    }

    @Test
    fun `slaaSammenStemmekretser skal inneholde alle grenser utenom delte grenser`() {
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = LocalDate.parse("2022-01-01"))

        val sammenslattStemmekrets = utfoerSammenslaaing(vestreKrets, oestreKrets)

        val antallGrenser = vestreKrets.grenser.size + oestreKrets.grenser.size
        val grenseIder = sammenslattStemmekrets?.grenser?.map { it.grense.id }

        assertThat(sammenslattStemmekrets?.grenser?.size).isLessThan(antallGrenser)
        assertThat(grenseIder).doesNotContain("grenseDelt")
    }

    @Test
    fun `slaaSammenStemmekretser skal sette tilhørigheter korrekt på alle grensene`() {
        // Starter med å sette opp 2 nabostemmekretser som skal slåes sammen
        // og sørger for at de ytre grensene på disse kretsene (altså grensene som ikke er delt)
        // har tilhørighet til en 3. stemmekrets. Disse kretsene er derfor hull i den 3 kretsen.
        // Vi ønsker å sjekke at de "ytre"-grensene på den sammenslåtte stemmekretsen har de
        // riktige 2 tilhørighetene etter sammenslåingen og ikke mister noen knytninger her.
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = datoFoerSammenslaaing)
        val grenseRundt = grenseRepository.save(GrenseHelper.getGrenseRundtVestreOgOestreKrets(gyldigFra = datoFoerSammenslaaing, grensetype = Grensetype.Stemmekretsgrense))
        val stemmekretsRundt = saveStemmekrets(
            id = "stemmekrets-rundt",
            lokalid = Lokalid("stemmekrets-rundt-lokalid"),
            stemmekretsnummer = "55",
            gyldigFra = datoFoerSammenslaaing,
            grenser = listOf(grenseRundt)
        )

        val ikkeDelteGrenserVestreKrets = vestreKrets.getGrensekoblinger(datoFoerSammenslaaing).filter { it.grense.stemmekretser(datoFoerSammenslaaing).size < 2 }
        val ikkeDelteGrenserOestreKrets = oestreKrets.getGrensekoblinger(datoFoerSammenslaaing).filter { it.grense.stemmekretser(datoFoerSammenslaaing).size < 2 }
        val ikkeDelteGrenser = ikkeDelteGrenserOestreKrets + ikkeDelteGrenserVestreKrets

        ikkeDelteGrenser.forEach { stemmekretsRundt.addGrensekobling(it.grense) }

        val sammenslattStemmekrets = utfoerSammenslaaing(vestreKrets, oestreKrets)!!

        assertThat(sammenslattStemmekrets.stemmekretsnavn).isEqualTo("Sammenslått krets")
        assertThat(sammenslattStemmekrets.stemmekretsnummer).isEqualTo("999")

        // Vi skal beholde kun de ikke-delte grensene
        assertThat(sammenslattStemmekrets.getGrensekoblinger(datoEtterSammanslaaing)).hasSize(ikkeDelteGrenser.size)

        sammenslattStemmekrets.getGrensekoblinger(datoEtterSammanslaaing).forEach {
            assertThat(it.grense.stemmekretser(datoEtterSammanslaaing))
                .hasSize(2)
                .extracting("lokalid")
                .containsExactlyInAnyOrder(sammenslattStemmekrets.lokalid, stemmekretsRundt.lokalid)
        }
    }

    private fun utfoerSammenslaaing(vestreKrets: Stemmekrets, oestreKrets: Stemmekrets): Stemmekrets? {
        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 1),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oestreKrets.lokalid, 1)),
            navn = "Sammenslått krets",
            nummer = "999"
        )

        stemmekretsService.slaaSammenStemmekretser(
            sammenslaaingsendringRequest = sammenslaaingsendringRequest,
            sammenslaaingsDato = sammenslaaingDato,
            endringstype = Endringstype.VEDTATT_SAMMENSLAAING
        )

        return stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), datoEtterSammanslaaing))
    }

    @Test
    fun `endre delt grense for stemmekrets skal opprette nye versjoner for grensa og dens stemmekretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(deltGrense.stemmekretser).hasSize(2)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato, endringsdato = endringsdato)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))!!
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))!!
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(opprinneligDeltGrense.stemmekretser).hasSize(2)
        assertThat(opprinneligDeltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `endre to grenser for stemmekrets skal opprette nye versjoner for grensene og deres stemmekretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(deltGrense.stemmekretser).hasSize(2)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(vestreKrets, opprinneligDato, "grenseDeltLokalid")
        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato,
            endringsdato = endringsdato, featureForAnnenGrense)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))!!
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))!!
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(opprinneligDeltGrense.stemmekretser).hasSize(2)
        assertThat(opprinneligDeltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertAnnenGrenseEtterGrenseendring("grenseDelt", "grenseDeltLokalid", endringsdato, opprinneligDato,
            vestreKretsEtterLagring, opprinneligVestreKrets)
    }

    @Test
    fun `endre to grenser for to stemmekretser skal opprette nye versjoner for grensene og deres stemmekretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(deltGrense.stemmekretser).hasSize(2)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(oestreKrets, opprinneligDato, "grenseDeltLokalid")
        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato,
            endringsdato = endringsdato, featureForAnnenGrense)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))!!
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))!!
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!
        assertThat(opprinneligDeltGrense.stemmekretser).hasSize(2)
        assertThat(opprinneligDeltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertAnnenGrenseEtterGrenseendring("grenseDelt", "grenseDeltLokalid", endringsdato, opprinneligDato,
            oestreKretsEtterLagring, opprinneligOestreKrets)
    }

    @Test
    fun `endre delt grense etter oppdatering av stemmekrets skal opprette nye versjoner for grensa og dens stemmekretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val vestreStemmekretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = vestreKrets, datoForOppdatering = datoForOppdatering)!!

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))!!
        assertThat(deltGrense.stemmekretser).hasSize(3)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, vestreStemmekretsEtterOppdatering.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))!!
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))!!
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!

        //Sjekker at gammel versjon av grensa ikke har fått endra sine stemmekretser
        assertThat(opprinneligDeltGrense.stemmekretser).hasSize(3)
        assertThat(opprinneligDeltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, vestreStemmekretsEtterOppdatering.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, datoForGrenseendring, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `endre delt grense etter oppdatering av stemmekrets paa andre sida skal opprette nye versjoner for grensa og dens stemmekretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val oestreStemmekretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = oestreKrets, datoForOppdatering = datoForOppdatering)!!

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))!!
        assertThat(deltGrense.stemmekretser).hasSize(3)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreStemmekretsEtterOppdatering.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))!!
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))!!
        assertThat(opprinneligVestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))!!
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))!!
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))!!

        //Sjekker at gammel versjon av grensa ikke har fått endra sine stemmekretser
        assertThat(opprinneligDeltGrense.stemmekretser).hasSize(3)
        assertThat(opprinneligDeltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreStemmekretsEtterOppdatering.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, datoForGrenseendring, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)
    }

    @Test
    fun `endre metadata for grense skal lagres paa ny grense`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val (_, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(oestreKrets, opprinneligDato, "grenseDeltLokalid")
        val kommunalKretsgrenseMetadata = featureForAnnenGrense.properties.metadata as KommunalKretsgrenseMetadata
        kommunalKretsgrenseMetadata.common.informasjon = "En eller annen informasjon"
        kommunalKretsgrenseMetadata.commonGrense.posisjonskvalitet =
            Posisjonskvalitet(maalemetode = KodelisteEntry(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, href = "maalemetodeHref"), noeyaktighet = 100)

        val flaterSomErEndret = grenseService.lagreGrenser(grenser = FeatureCollection(features = listOf(featureForAnnenGrense)),
            gyldighetsdato = endringsdato, endringstype = Endringstype.FASTSETTING
        )

        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))!!
        assertThat(opprinneligOestreKrets.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))!!
        assertThat(oestreKretsEtterLagring.gyldighet.gyldigTil).isNull()
        assertThat(oestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligOestreKrets.omraade)
        assertThat(oestreKretsEtterLagring.version).isGreaterThan(opprinneligOestreKrets.version)
        assertThat(oestreKretsEtterLagring.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)

        assertThat(flaterSomErEndret).contains(oestreKretsEtterLagring)
        assertAnnenGrenseEtterGrenseendring("grenseDelt", "grenseDeltLokalid", endringsdato, opprinneligDato,
            oestreKretsEtterLagring, opprinneligOestreKrets)

        val nyGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseDeltLokalid"), endringsdato))!!
        assertThat(nyGrense.kvalitet!!.noeyaktighet).isEqualTo(100)
        assertThat(nyGrense.kvalitet!!.maalemetode).isEqualTo(MaalemetodeKodeEnum.TERRENGMAALT.uuid)
        assertThat(nyGrense.informasjon).isEqualTo("En eller annen informasjon")
        assertThat(nyGrense.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)
    }

    @Test
    fun `oppdatering av stemmekrets til et allerede brukt stemmekretsnummer skal gi feil`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val stemmekretsRequest = StemmekretsRequestBuilder().withLokalId(vestreKrets.lokalid).withStemmekretsnummer(oestreKrets.stemmekretsnummer!!).build()
        val identifikator = Identifikator(Lokalid(stemmekretsRequest.identifikasjon.lokalid), datoForOppdatering)

        assertThatThrownBy {
            stemmekretsService.oppdaterStemmekrets(
                lokalId = identifikator.lokalid,
                oppdatertStemmekrets = stemmekretsRequest,
                gyldigFra = datoForOppdatering,
                endringstype = Endringstype.RETTING
            )
        }.isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Kretsnummer ${oestreKrets.stemmekretsnummer} er brukt på flere stemmekretser innenfor kommunen")
    }

    @Test
    fun `slaa sammen Vestre og Oestre stemmekretser med gjenbruk av nummer fra Tredje skal gi feil`() {
        val (vestreKrets, oestreKrets, tredjeKrets) = saveNaboStemmekretser(gyldigFra = LocalDate.parse("2022-01-01"), medTredjeKrets = true)

        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 1),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oestreKrets.lokalid, 1)),
            navn = "Sammenslått krets",
            nummer = tredjeKrets.stemmekretsnummer!!
        )

        val sammenslaingsdato = LocalDate.parse("2022-02-01")
        assertThatThrownBy {
            stemmekretsService.slaaSammenStemmekretser(
                sammenslaaingsendringRequest = sammenslaaingsendringRequest,
                sammenslaaingsDato = sammenslaingsdato,
                endringstype = Endringstype.VEDTATT_SAMMENSLAAING
            )
        }.isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Kretsnummer 22 er brukt på flere stemmekretser innenfor kommunen")
    }

    @Test
    fun `to nabostemmekretser i hver sin kommune i samme fylke som splittes til to fylker`() {
        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue("39") } returns "583a2846-e90c-4911-b8bb-b2537fd896a0"
        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue("40") } returns "f26cc714-7289-4026-a076-35f83f9f2332"
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue("3909") } returns "3909"
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue("4010") } returns "4010"

        val opprinneligDato = october1In2022
        val (vestreKrets, oestreKrets) = saveNaboStemmekretserIHverSinKommune(gyldigFra = opprinneligDato,
            kommunenummer1 = "3812", kommunenummer2 = "3805", medToDelteGrenser = true)
        settGrensetypeForGrenserSomIkkeErDelt(vestreKrets, oestreKrets, Grensetype.Fylkesgrense)
        val gamleSiljan = vestreKrets.getKommune(opprinneligDato)!!
        val gamleLarvik = oestreKrets.getKommune(opprinneligDato)!!

        val norskNavn = saveAdministrativenhetnavn(navn = "Vestfold og Telemark", spraakid = "nor")
        val vestfoldOgTelemark = saveFylkeOgUnikLokalid(id = "fylke38", lokalid = "38VestfoldOgTelemark", fylkesnummerUuid = "e79b7a63-13ff-4ba5-b4ef-0dfa4e6af1df",
            fylkesnummer = "38", administrativenhetnavn = listOf(norskNavn), gyldigFra = opprinneligDato, kommuner = listOf(gamleSiljan, gamleLarvik))
        saveNasjonOgUnikLokalid(fylker = listOf(vestfoldOgTelemark))
        grenseRepository.flush()

        fylkeService.createNyttFylkeMedNyUnikLokalid(
            kopierFraFylke = vestfoldOgTelemark,
            nyttFylkesnummer = "39",
            admenhetnavnList = emptyList(),
            gyldigFra = jan1In2024,
            endringstype = Endringstype.FASTSETTING
        )
        fylkeService.createNyttFylkeMedNyUnikLokalid(
            kopierFraFylke = vestfoldOgTelemark,
            nyttFylkesnummer = "40",
            admenhetnavnList = emptyList(),
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

        val nyVestreKretsINySiljan = stemmekretsService.createNyVersjonAvStemmekretsIAnnenKommune(vestreKrets, nySiljan, jan1In2024, Endringstype.FASTSETTING)
        val nyOestreKretsINyLarvik = stemmekretsService.createNyVersjonAvStemmekretsIAnnenKommune(oestreKrets, nyLarvik, jan1In2024, Endringstype.FASTSETTING)

        grenseService.endreGrensetypeForStemmekretser(listOf(nyVestreKretsINySiljan, nyOestreKretsINyLarvik), jan1In2024, Endringstype.FASTSETTING)

        assertDelteGrenser(opprinneligDato, vestreKrets, oestreKrets, Grensetype.Kommunegrense,
            jan1In2024, nyVestreKretsINySiljan, nyOestreKretsINyLarvik, Grensetype.Fylkesgrense)
    }

    @Test
    fun `to nabostemmekretser i samme kommune som skal deles til hver sin kommune`() {
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue("0308") } returns "0308"
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue("0380") } returns "0380"

        val opprinneligDato = LocalDate.parse("2022-01-01")
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato, medToDelteGrenser = true)
        settGrensetypeForGrenserSomIkkeErDelt(vestreKrets, oestreKrets, Grensetype.Kommunegrense)
        grenseRepository.flush()

        val gamleAalesund = vestreKrets.getKommune(opprinneligDato)!!
        val moereOgRomsdal = saveFylkeOgUnikLokalid(id = "fylke03", lokalid = "03MoereOgRomsdal", fylkesnummerUuid = "03", fylkesnummer = "03",
            gyldigFra = opprinneligDato, kommuner = listOf(gamleAalesund))
        saveNasjonOgUnikLokalid(fylker = listOf(moereOgRomsdal))
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

        val nyVestreKretsINyAalesund = stemmekretsService.createNyVersjonAvStemmekretsIAnnenKommune(vestreKrets, nyAalesund,
            jan1In2024, Endringstype.FASTSETTING)
        val nyOestreKretsIHaram = stemmekretsService.createNyVersjonAvStemmekretsIAnnenKommune(oestreKrets, haram, jan1In2024, Endringstype.FASTSETTING)

        grenseService.endreGrensetypeForStemmekretser(listOf(nyVestreKretsINyAalesund, nyOestreKretsIHaram), jan1In2024, Endringstype.FASTSETTING)

        assertDelteGrenser(opprinneligDato, vestreKrets, oestreKrets, Grensetype.Stemmekretsgrense,
            jan1In2024, nyVestreKretsINyAalesund, nyOestreKretsIHaram, Grensetype.Kommunegrense)
    }

    @Test
    fun `delStemmekrets klarer å opprette en ny stemmekrets`(){
        val gyldighetsdato = jan1In2023

        val opprinneligStemmekrets = saveStemmekrets("opprinneligKrets", Lokalid("opprinneligKrets"))
        val kommune = saveKommune(stemmekretser = listOf(opprinneligStemmekrets), gyldigFra = gyldighetsdato)
        assertThat(opprinneligStemmekrets.valgdistriktsnummer).isNotNull()
        assertThat(opprinneligStemmekrets.tellekretsnummer).isNotNull()
        assertThat(opprinneligStemmekrets.tellekretsnavn).isNotNull()

        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(opprinneligStemmekrets.lokalid, opprinneligStemmekrets.version),
            ObjektIdentifikator(Lokalid(kommune.lokalid), gyldighetsdato),
            Flatetype.STEMMEKRETS,
            nyeKretser = listOf(KretsNavnOgNummer("nyStemmekrets", "1"))
        )

        val unikKretsDelingEndring = getUnikeKretsDelingEndringRequests(delingEndringRequest).first()
        val nyStemmekrets = stemmekretsService.delStemmekrets(unikKretsDelingEndring, gyldighetsdato, Endringstype.VEDTATT_DELING)

        assertThat(nyStemmekrets).isNotNull
        assertThat(nyStemmekrets.utledetFra?.lokalid).isEqualTo(opprinneligStemmekrets.lokalid)
        assertThat(nyStemmekrets.grenser.size).isEqualTo(0)
        assertThat(nyStemmekrets.stemmekretsnummer).isEqualTo(unikKretsDelingEndring.kretsNummer)
        assertThat(nyStemmekrets.stemmekretsnavn).isEqualTo(unikKretsDelingEndring.kretsNavn)
        assertThat(nyStemmekrets.version).isEqualTo(1)
        assertThat(nyStemmekrets.valgdistriktsnummer).isNotNull().isEqualTo(opprinneligStemmekrets.valgdistriktsnummer)
        assertThat(nyStemmekrets.tellekretsnummer).isEqualTo(opprinneligStemmekrets.tellekretsnummer)
        assertThat(nyStemmekrets.tellekretsnavn).isEqualTo(opprinneligStemmekrets.tellekretsnavn)
    }

    @Test
    fun `delStemmekrets uten tellekrets klarer å opprette en ny stemmekrets`(){
        val gyldighetsdato = jan1In2023

        val opprinneligStemmekrets = saveStemmekrets("opprinneligKrets", Lokalid("opprinneligKrets"))
            .apply {
                tellekretsnummer = null
                tellekretsnavn = null
                setInfo("Info fra en test")
            }

        val kommune = saveKommune(stemmekretser = listOf(opprinneligStemmekrets), gyldigFra = gyldighetsdato)
        assertThat(opprinneligStemmekrets.valgdistriktsnummer).isNotNull()
        assertThat(opprinneligStemmekrets.tellekretsnummer).isNull()
        assertThat(opprinneligStemmekrets.tellekretsnavn).isNull()
        assertThat(opprinneligStemmekrets.getInfo()).isNotNull()

        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(opprinneligStemmekrets.lokalid, opprinneligStemmekrets.version),
            ObjektIdentifikator(Lokalid(kommune.lokalid), gyldighetsdato),
            Flatetype.STEMMEKRETS,
            nyeKretser = listOf(KretsNavnOgNummer("nyStemmekrets", "1"))
        )

        val unikKretsDelingEndring = getUnikeKretsDelingEndringRequests(delingEndringRequest).first()
        val nyStemmekrets = stemmekretsService.delStemmekrets(unikKretsDelingEndring, gyldighetsdato, Endringstype.VEDTATT_DELING)

        assertThat(nyStemmekrets).isNotNull
        assertThat(nyStemmekrets.utledetFra?.lokalid).isEqualTo(opprinneligStemmekrets.lokalid)
        assertThat(nyStemmekrets.grenser.size).isEqualTo(0)
        assertThat(nyStemmekrets.stemmekretsnummer).isEqualTo(unikKretsDelingEndring.kretsNummer)
        assertThat(nyStemmekrets.stemmekretsnavn).isEqualTo(unikKretsDelingEndring.kretsNavn)
        assertThat(nyStemmekrets.version).isEqualTo(1)
        assertThat(nyStemmekrets.valgdistriktsnummer).isNotNull().isEqualTo(opprinneligStemmekrets.valgdistriktsnummer)
        assertThat(nyStemmekrets.tellekretsnummer).isNull()
        assertThat(nyStemmekrets.tellekretsnavn).isNull()
        assertThat(nyStemmekrets.getInfo()).isNotNull().isEqualTo(opprinneligStemmekrets.getInfo())
    }

    @Test
    fun `delStemmekrets kaster hvis feil flatetype er satt i request`(){
        val opprinneligStemmekrets = saveStemmekrets("opprinneligKrets", Lokalid("opprinneligKrets"))

        val kommune = saveKommune(stemmekretser = listOf(opprinneligStemmekrets), gyldigFra = jan1In2023)

        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(opprinneligStemmekrets.lokalid, opprinneligStemmekrets.version),
            ObjektIdentifikator(Lokalid(kommune.lokalid), jan1In2023),
            Flatetype.GRUNNKRETS,
            nyeKretser = listOf(KretsNavnOgNummer("nyStemmekrets", "1"))
        )

        val unikKretsDelingEndring = getUnikeKretsDelingEndringRequests(delingEndringRequest).first()

        assertThatThrownBy {
            stemmekretsService.delStemmekrets(unikKretsDelingEndring, jan1In2023, Endringstype.VEDTATT_DELING)
        }
            .isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Kan ikke dele og opprette en ny ${delingEndringRequest.flatetype} fra en stemmekrets")
    }

    @Test
    fun `delStemmekrets kaster exception hvis ny stemmekrets skaper duplikat`(){
        val gyldighetsdato = jan1In2023

        val opprinneligStemmekrets = saveStemmekrets("opprinneligKrets", Lokalid("opprinneligKrets"))

        val kommune = saveKommune(stemmekretser = listOf(opprinneligStemmekrets), gyldigFra = gyldighetsdato)

        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(opprinneligStemmekrets.lokalid, opprinneligStemmekrets.version),
            ObjektIdentifikator(Lokalid(kommune.lokalid), gyldighetsdato),
            Flatetype.STEMMEKRETS,
            nyeKretser = listOf(KretsNavnOgNummer("nyStemmekrets", "1"))
        )

        val unikKretsDelingEndring = getUnikeKretsDelingEndringRequests(delingEndringRequest).first()

        val eksiterendeStemmekretsSomTriggerDuplikat = saveStemmekrets("eksisterende", Lokalid("eksisterende"), stemmekretsnummer = unikKretsDelingEndring.kretsNummer)

        saveKommune(stemmekretser = listOf(opprinneligStemmekrets, eksiterendeStemmekretsSomTriggerDuplikat), gyldigFra = gyldighetsdato)

        assertThatThrownBy {
            stemmekretsService.delStemmekrets(unikKretsDelingEndring, gyldighetsdato, Endringstype.VEDTATT_DELING)
        }
            .isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Kretsnummer 1 er brukt på flere stemmekretser innenfor kommunen (0301 UKJENT KOMMUNE) med gyldighet som overlapper med 01.01.2023 og fremover. Vennligst gi stemmekretsene unike kretsnumre før du fortsetter.")
    }

    @Test
    fun `delStemmekrets kaster OptimistiskLaasException ved versjonsfeil på den opprinnelige kretsen`(){
        val opprinneligStemmekrets = saveStemmekrets("opprinneligKrets", Lokalid("opprinneligKrets"))

        val kommune = saveKommune(stemmekretser = listOf(opprinneligStemmekrets), gyldigFra = jan1In2023)
        stemmekretsService.createNyVersjonAvStemmekrets(opprinneligStemmekrets, gyldigFra = jan1In2023, Endringstype.RETTING)

        val delingEndringRequest = KretsDelingEndringRequest(
            IdentifikatorMedVersjon(opprinneligStemmekrets.lokalid, opprinneligStemmekrets.version - 1),
            ObjektIdentifikator(Lokalid(kommune.lokalid), jan1In2023),
            Flatetype.STEMMEKRETS,
            nyeKretser = listOf(KretsNavnOgNummer("nyStemmekrets", "1"))
        )

        val utkastRequestBuilder = UtkastRequestBuilder()
            .withOperasjoner(Operasjoner(kretsDelingEndringer = listOf(delingEndringRequest)))

        val utkast = UtkastBuilder.builder("test", gyldigFra = LocalDate.now())
            .withOperasjoner(Operasjoner(kretsDelingEndringer = listOf(delingEndringRequest)))
            .build()
        utkastRepository.save(utkast)

        val oppdaterutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.oppdaterUtkast(utkast.id, utkastRequestBuilder
                .build())
        }
        assertThat(oppdaterutkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon for stemmekrets 9 Øvre Singsaker. Ønsket versjon = 0, eksisterende versjon = 2")

        val publiserutkastexception = assertFailsWith<OptimistiskLaasException> {
            utkastService.publiserUtkast(utkast.id)
        }
        assertThat(publiserutkastexception.message).contains("Det er ikke samsvar med versjon")
        assertThat(publiserutkastexception.konflikter.first().getMessage())
            .contains("Det er ikke samsvar med versjon for stemmekrets 9 Øvre Singsaker. Ønsket versjon = 0, eksisterende versjon = 2")
    }

    private fun assertDelteGrenser(oensketGammelDato: LocalDate, oensketGammelKrets1: Stemmekrets, oensketGammelKrets2: Stemmekrets,
                                   oensketGammelGrensetype: Grensetype,
                                   oensketNyDato: LocalDate, oensketNyKrets1: Stemmekrets, oensketNyKrets2: Stemmekrets, oensketNyGrensetype: Grensetype) {
        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseDeltLokalid"), oensketGammelDato))!!
        assertThat(deltGrense.stemmekretser).hasSize(2)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(oensketGammelKrets1.id, oensketGammelKrets2.id)
        assertThat(deltGrense.grensetype).isEqualTo(oensketGammelGrensetype)

        val deltGrense2 = grenseService.hentGrense(Identifikator(Lokalid("grenseDelt2Lokalid"), oensketGammelDato))!!
        assertThat(deltGrense2.stemmekretser).hasSize(2)
        assertThat(deltGrense2.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(oensketGammelKrets1.id, oensketGammelKrets2.id)
        assertThat(deltGrense2.grensetype).isEqualTo(oensketGammelGrensetype)

        val nyDeltGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseDeltLokalid"), oensketNyDato))!!
        assertThat(nyDeltGrense.stemmekretser).hasSize(2)
        assertThat(nyDeltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(oensketNyKrets1.id, oensketNyKrets2.id)
        assertThat(nyDeltGrense.grensetype).isEqualTo(oensketNyGrensetype)

        val nyDeltGrense2 = grenseService.hentGrense(Identifikator(Lokalid("grenseDelt2Lokalid"), oensketNyDato))!!
        assertThat(nyDeltGrense2.stemmekretser).hasSize(2)
        assertThat(nyDeltGrense2.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(oensketNyKrets1.id, oensketNyKrets2.id)
        assertThat(nyDeltGrense2.grensetype).isEqualTo(oensketNyGrensetype)
    }

    private fun assertOmradeGyldighetVersjon(vestreKretsEtterLagring: Stemmekrets, oestreKretsEtterLagring: Stemmekrets,
                                             opprinneligVestreKrets: Stemmekrets, opprinneligOestreKrets: Stemmekrets) {
        assertThat(vestreKretsEtterLagring.gyldighet.gyldigTil).isNull()
        assertThat(oestreKretsEtterLagring.gyldighet.gyldigTil).isNull()
        assertThat(vestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligVestreKrets.omraade)
        assertThat(oestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligOestreKrets.omraade)
        assertThat(vestreKretsEtterLagring.version).isGreaterThan(opprinneligVestreKrets.version)
        assertThat(oestreKretsEtterLagring.version).isGreaterThan(opprinneligOestreKrets.version)
    }

    private fun assertDeltGrenseEtterGrenseendring(
        @Suppress("SameParameterValue") deltGrenseLokalid: String, datoForGrenseendring: LocalDate, opprinneligGrense: Grense,
        vestreKretsEtterLagring: Stemmekrets, oestreKretsEtterLagring: Stemmekrets, opprinneligVestreKrets: Stemmekrets, opprinneligOestreKrets: Stemmekrets
    ) {
        val deltGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForGrenseendring))!!
        assertThat(deltGrenseEtterGrenseendring.stemmekretser).hasSize(2)
        assertThat(deltGrenseEtterGrenseendring.stemmekretser.map { it.stemmekrets.id })
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
                                                    opprinneligDato: LocalDate, kretsEtterLagring: Stemmekrets, opprinneligKrets: Stemmekrets) {

        val opprinneligGrense = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), opprinneligDato))!!
        assertThat(opprinneligGrense.stemmekretser).hasSize(2)
        assertThat(opprinneligGrense.stemmekretser.map { it.stemmekrets.id }).contains(opprinneligKrets.id)

        val annenGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), datoForGrenseendring))!!
        assertThat(annenGrenseEtterGrenseendring.stemmekretser).hasSize(2)
        assertThat(annenGrenseEtterGrenseendring.stemmekretser.map { it.stemmekrets.id }).contains(kretsEtterLagring.id)

        assertThat(opprinneligGrense.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(annenGrenseEtterGrenseendring.gyldighet.gyldigTil).isNull()
        assertThat(opprinneligGrense.lokalid).isEqualTo(annenGrenseEtterGrenseendring.lokalid)
        assertThat(opprinneligGrense.grense).isNotEqualTo(annenGrenseEtterGrenseendring.grense)
        assertThat(annenGrenseEtterGrenseendring.version).isGreaterThan(opprinneligGrense.version)

        assertThat(opprinneligKrets.grenser.map { it.grense.id }).contains(annenGrenseId)
        assertThat(kretsEtterLagring.grenser.map { it.grense.id }).doesNotContain(annenGrenseId)
        assertThat(kretsEtterLagring.grenser.map { it.grense.lokalid }).contains(annenGrenseLokalid)
    }

    private fun utfoerMetadataoppdatering(kretsSomSkalOppdateres: Stemmekrets, datoForOppdatering: LocalDate): Stemmekrets? {
        val stemmekretsRequest = StemmekretsRequestBuilder().withLokalId(kretsSomSkalOppdateres.lokalid).build()
        val identifikator = Identifikator(Lokalid(stemmekretsRequest.identifikasjon.lokalid), datoForOppdatering)

        stemmekretsService.oppdaterStemmekrets(
            lokalId = identifikator.lokalid,
            oppdatertStemmekrets = stemmekretsRequest,
            gyldigFra = datoForOppdatering,
            endringstype = Endringstype.RETTING
        )
        return stemmekretsService.hentStemmekrets(identifikator)
    }

    private fun utfoerGrenseendring(kretsSomSkalHaGrenseendring: Stemmekrets, opprinneligDato: LocalDate, endringsdato: LocalDate,
                                    featureAnnenOppdatertGrense: Feature? = null): Set<Flate> {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrensekoblinger(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == "grenseDeltLokalid" }

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            Coordinate(10.0, 10.0),
            Coordinate(5.0, 5.0),
            Coordinate(10.0, 0.0)
        ))

        val oppdatertGrensefeature = grenseKobling.grense.grenseToFeature(opprinneligDato)
        oppdatertGeometri.geometryToDto().also { oppdatertGrensefeature.geometry = it }

        val features = if (featureAnnenOppdatertGrense != null)
            listOf(oppdatertGrensefeature, featureAnnenOppdatertGrense) else listOf(oppdatertGrensefeature)

        return grenseService.lagreGrenser(
            grenser = FeatureCollection(features = features),
            gyldighetsdato = endringsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
    }

    private fun lagFeatureEndreYkoordinat(kretsSomSkalHaGrenseendring: Stemmekrets, opprinneligDato: LocalDate, lokalidGrenseSomSkalEndres: String): Feature {
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

    fun settGrensetypeForGrenserSomIkkeErDelt(vestreKrets: Stemmekrets, oestreKrets: Stemmekrets, grensetype: Grensetype) {
        vestreKrets.grenser.stream().forEach { if (it.grense.id != "grenseDelt" && it.grense.id != "grenseDelt2") it.grense.grensetype = grensetype }
        oestreKrets.grenser.stream().forEach { if (it.grense.id != "grenseDelt" && it.grense.id != "grenseDelt2") it.grense.grensetype = grensetype }
    }

    private fun saveNaboStemmekretser(gyldigFra: LocalDate, medHull: Boolean = false, medTredjeKrets: Boolean = false, medToDelteGrenser: Boolean = false)
    : List<Stemmekrets> {
        val grenserForVest = if (medHull) {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Stemmekretsgrense, medToDelteGrenser) +
                GrenseHelper.getHullVestligKrets(gyldigFra, Grensetype.Stemmekretsgrense)
        } else {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Stemmekretsgrense, medToDelteGrenser)
        }

        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForVest = grenseRepository.saveAll(grenserForVest)
        val lagredeGrenserForOest = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, Grensetype.Stemmekretsgrense, medToDelteGrenser))
        grenseRepository.flush()

        val kretsVest = saveStemmekrets(
            id = "1",
            lokalid = Lokalid("123"),
            stemmekretsnummer = "20",
            stemmekretsnavn = "Vestre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForVest
        )

        val kretsOest = saveStemmekrets(
            id = "2",
            lokalid = Lokalid("234"),
            stemmekretsnummer = "21",
            stemmekretsnavn = "Østre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForOest
        )

        val stemmekretsMutableList = mutableListOf(kretsVest, kretsOest)
        if (medTredjeKrets) {
            val tredjeKrets = saveStemmekrets(
                id = "3",
                lokalid = Lokalid("345"),
                stemmekretsnummer = "22",
                stemmekretsnavn = "Tredje krets",
                gyldigFra = gyldigFra
            )
            stemmekretsMutableList.add(tredjeKrets)
        }

        val stemmekretser = stemmekretsMutableList.toList()
        saveKommuneOgUnikLokalid(stemmekretser = stemmekretser, gyldigFra = gyldigFra)
        grenseRepository.flush()
        return stemmekretser
    }

    private fun saveNaboStemmekretserIHverSinKommune(gyldigFra: LocalDate, medHull: Boolean = false, medToDelteGrenser: Boolean = false,
                                                     kommunenummer1: String? = "0301", kommunenummer2: String? = "0302")
    : List<Stemmekrets> {
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

        val kretsVest = saveStemmekrets(
            id = "1",
            lokalid = Lokalid("123"),
            stemmekretsnummer = "20",
            stemmekretsnavn = "Vestre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForVest
        )

        val kretsOest = saveStemmekrets(
            id = "2",
            lokalid = Lokalid("234"),
            stemmekretsnummer = "21",
            stemmekretsnavn = "Østre krets",
            gyldigFra = gyldigFra,
            grenser = lagredeGrenserForOest
        )

        saveKommuneOgUnikLokalid(id = "idKommuneVest", lokalid = "kommuneVest", stemmekretser = listOf(kretsVest), gyldigFra = gyldigFra, kommunenummerUuid = kommunenummer1!!)
        saveKommuneOgUnikLokalid(id = "idKommuneOest", lokalid = "kommuneOest", stemmekretser = listOf(kretsOest), gyldigFra = gyldigFra, kommunenummerUuid = kommunenummer2!!)
        grenseRepository.flush()
        return listOf(kretsVest, kretsOest)
    }

    private fun saveStemmekretsMed1FramtidigVersjon(stemmekretsLokalid: Lokalid) {
        val stemmekrets = saveStemmekrets(id = "9000", lokalid = stemmekretsLokalid, gyldigTil = jan1In2023)
        val stemmekrets2 = saveStemmekrets(id = "9001", lokalid = stemmekretsLokalid, stemmekretsnavn = "Nedre Singsaker", gyldigFra = jan1In2023)

        saveKommuneOgUnikLokalid(stemmekretser = listOf(stemmekrets, stemmekrets2))
    }

}

fun assertGrenser(grensekoblinger: List<GrenseKobling>,
                  grenseIGrensekoblingId: String,
                  enhetKobletTilGrenseLokalid: Lokalid) {
    assertThat(grensekoblinger).hasSize(1)
    val grensekobling = grensekoblinger[0]
    val grenseIGrensekobling = grensekobling.grense
    assertThat(grenseIGrensekobling.id).isEqualTo(grenseIGrensekoblingId)
    assertThat(grensekobling.getEnhetKobletTilGrense().lokalid).isEqualTo(enhetKobletTilGrenseLokalid.value)
}
