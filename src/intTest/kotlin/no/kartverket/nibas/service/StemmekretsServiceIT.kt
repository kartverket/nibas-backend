package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.common.geojson.metadata.KodelisteEntry
import no.kartverket.nibas.api.v1.common.geojson.metadata.KommunalKretsgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.Posisjonskvalitet
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.builders.FeatureBuilder
import no.kartverket.nibas.builders.MaalemetodeKodeEnum
import no.kartverket.nibas.builders.StemmekretsRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month
import java.util.*

@ActiveProfiles("security-off")
@Transactional
class StemmekretsServiceIT : TestWithDbContainer() {

    @Autowired
    private lateinit var stemmekretsService: StemmekretsService

    @Autowired
    private lateinit var stemmekretsRepository: StemmekretsRepository

    @Autowired
    private lateinit var grenseRepository: GrenseRepository

    @Autowired
    private lateinit var grenseService: GrenseService

    @MockBean
    private lateinit var kodelisteServiceMock: KodelisteService

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val stemmekretsLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-88c320137ba6")
    private val grenseLokallid = Lokalid("888")

    @BeforeEach
    fun beforeEach() {
        Mockito.`when`(kodelisteServiceMock.getCodeValueByKommunenummerId(Mockito.anyString())).thenReturn("1234")
    }

    @Test
    fun `hent eksisterende stemmekrets med dagens dato`() {
        val stemmekrets = saveStemmekrets(lokalid = stemmekretsLokalid)
        saveKommune(stemmekretser = listOf(stemmekrets))

        assertThat(stemmekretsRepository.findAllByLokalid(stemmekretsLokalid.value).size).isEqualTo(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(stemmekretsService.hentStemmekrets(Identifikator(stemmekretsLokalid, november28In2022))).isNotNull
    }

    @Test
    fun `hent versjon av stemmekrets fram i tid`() {
        saveStemmekretsMed1FramtidigVersjon(stemmekretsLokalid)
        assertThat(stemmekretsRepository.findAllByLokalid(stemmekretsLokalid.value).size).isEqualTo(2)

        val framtidigStemmekrets = stemmekretsService.hentStemmekrets(Identifikator(stemmekretsLokalid, jan1In2023))
        assertThat(framtidigStemmekrets).isNotNull
        assertThat(framtidigStemmekrets!!.stemmekretsnavn).isEqualTo("Nedre Singsaker")
    }

    @Test
    fun `finn stemmekrets med samme stemmekretsnummer og ulik lokalid`() {
        saveStemmekretsMed1FramtidigVersjon(stemmekretsLokalid)

        val stemmekrets = stemmekretsService.hentStemmekrets(Identifikator(stemmekretsLokalid, jan1In2023))
        val muligeStemmekretsnumre = lagMuligeStemmekretsnumre(stemmekrets!!.stemmekretsnummer!!)

        //Uten å ta hensyn til gyldighet og med vilkårlig lokalid skal det finnes to stemmekretser med gitt stemmekretsnummer
        var stemmekretser = stemmekretsRepository.findAllByStemmekretsnummerInAndLokalidIsNotAndKommune_Lokalid(
            muligeStemmekretsnumre, "xx", stemmekrets.kommune.lokalid)
        assertThat(stemmekretser).hasSize(2)

        //Uten å ta hensyn til gyldighet og med gitt lokalid som ikke skal finnes skal det ikke finnes noen stemmekretser med gitt stemmekretsnummer
        //Dvs det finnes ikke noen ANDRE stemmekretser enn den som skal valideres med gitt stemmekretsnummer og kommune
        stemmekretser = stemmekretsRepository.findAllByStemmekretsnummerInAndLokalidIsNotAndKommune_Lokalid(
            muligeStemmekretsnumre, stemmekrets.lokalid, stemmekrets.kommune.lokalid)
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
        val grense2 = saveGrenseAvTypeKommunegrense(id = "4001", lokalid = grenseLokallid, nesteVersjon = grense3, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrenseAvTypeKommunegrense(id = "4000", lokalid = grenseLokallid, nesteVersjon = grense2, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        val stemmekrets1 = saveStemmekrets(id = "301", lokalid = stemmekretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val stemmekrets2 = saveStemmekrets(id = "302", lokalid = stemmekretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1))
        val stemmekrets3 = saveStemmekrets(id = "303", lokalid = stemmekretsLokalid, gyldigFra = jan1In2024, grenser = listOf(grense1))

        saveKommune(stemmekretser = listOf(stemmekrets1, stemmekrets2, stemmekrets3))

        val grensekoblinger = stemmekretsService.hentGrenserForStemmekrets(identifikatorFor1Jan23)

        assertGrenser(
            grensekoblinger,
            gyldighetsdato = identifikatorFor1Jan23.gyldighetsdato,
            grenseIGrensekoblingId = "4000",
            relevantGrenseId = "4001",
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
    fun `hentFramtidigeVersjonerForStemmekrets skal returnere en liste med kun deg gyldige elementet om det er ingen framtidige endringer`() {
        saveStemmekrets(id = "301", lokalid = stemmekretsLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022)
        saveStemmekrets(id = "302", lokalid = stemmekretsLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024)

        assertThat(stemmekretsService.hentFramtidigeVersjonerForStemmekrets(stemmekretsLokalid, december1In2022)).hasSize(1)
    }

    @Test
    fun `oppdaterStemmekrets skal lage ny stemmekrets og sette gyldigtil for gammel krets`() {
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

        saveKommune(kommunenummer = "4227", stemmekretser = listOf(stemmekrets))
        val now = LocalDate.now()
        val opprinneligStemmekrets = stemmekretsService.hentStemmekrets(identifikator)

        stemmekretsService.oppdaterStemmekrets(
            lokalId = identifikator.lokalid,
            oppdatertStemmekrets = stemmekretsRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligStemmekretsEtter = stemmekretsService.hentStemmekrets(identifikator)
        val stemmekretsEtter = stemmekretsService.hentStemmekrets(Identifikator(identifikator.lokalid))

        assertThat(stemmekretsEtter!!.stemmekretsnummer).isNotEqualTo(stemmekrets.stemmekretsnummer)
            .isEqualTo(stemmekretsRequest.stemmekretsnummer)
        assertThat(stemmekretsEtter.stemmekretsnavn).isNotEqualTo(stemmekrets.stemmekretsnavn)
            .isEqualTo(stemmekretsRequest.stemmekretsnavn)
        assertThat(stemmekretsEtter.omraade).isEqualTo(stemmekrets.omraade)
        assertThat(stemmekretsEtter.posisjon).isEqualTo(stemmekrets.posisjon)
        assertThat(stemmekretsEtter.gyldighet.gyldigFra).isEqualTo(now)

        assertThat(opprinneligStemmekretsEtter!!.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligStemmekretsEtter.gyldighet.gyldigTil).isEqualTo(now)

        val grenseObjIdsForOpprinneligStemmekrets = opprinneligStemmekrets!!.grenser.map { it.grense.id }
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
    fun `slaaSammenStemmekretser skal skal kaste konflikt om den feiler optimistiskLaas-sjekken for den viderefoerte stemmekretsen`() {
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = LocalDate.parse("2022-01-01"))

        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 5),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oestreKrets.lokalid, 1)),
            stemmekretsNavn = "Sammenslått krets",
            stemmekretsNummer = "nytt nummer"
        )

        assertThrows<OptimistiskLaasException> {
            stemmekretsService.slaaSammenStemmekretser(
                sammenslaaingsendringRequest = sammenslaaingsendringRequest,
                sammenslaaingsDato = LocalDate.parse("2022-02-01"),
                endringstype = Endringstype.VEDTATT_SAMMENSLAAING
            )
        }
    }

    @Test
    fun `slaaSammenStemmekretser skal skal kaste konflikt om den feiler optimistiskLaas-sjekken for en av stemmekretsene til sammenslaaing`() {
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = LocalDate.parse("2022-01-01"))

        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 1),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oestreKrets.lokalid, 5)),
            stemmekretsNavn = "Sammenslått krets",
            stemmekretsNummer = "nytt nummer"
        )

        assertThrows<OptimistiskLaasException> {
            stemmekretsService.slaaSammenStemmekretser(
                sammenslaaingsendringRequest = sammenslaaingsendringRequest,
                sammenslaaingsDato = LocalDate.parse("2022-02-01"),
                endringstype = Endringstype.VEDTATT_SAMMENSLAAING
            )
        }
    }

    @Disabled("Trenger litt smartere metode for å danne grenser for test-kretser")
    @Test
    fun `slaaSammenStemmekretser skal beholde hull i stemmekretsen`() {
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = LocalDate.parse("2022-01-01"), medHull = true)

        val sammenslattStemmekrets = utfoerSammenslaaing(vestreKrets, oestreKrets)

        // Det er en delt grense, så vi vil beholde alle grenser utenom 1 fra hver
        val antallGrenser = vestreKrets.grenser.size + oestreKrets.grenser.size - 2
        assertThat(sammenslattStemmekrets?.grenser?.size).isEqualTo(antallGrenser)

        // Vi skal ha 4 grenser som danner ett hull
        val hullGrenser = sammenslattStemmekrets?.grenser?.filter { it.grenseKoblingAttributter.hullIndeks != null }
        assertThat(hullGrenser).hasSize(4)
    }

    private fun utfoerSammenslaaing(vestreKrets: Stemmekrets, oestreKrets: Stemmekrets): Stemmekrets? {
        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 1),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oestreKrets.lokalid, 1)),
            stemmekretsNavn = "Sammenslått krets",
            stemmekretsNummer = "999"
        )

        stemmekretsService.slaaSammenStemmekretser(
            sammenslaaingsendringRequest = sammenslaaingsendringRequest,
            sammenslaaingsDato = LocalDate.parse("2022-02-01"),
            endringstype = Endringstype.VEDTATT_SAMMENSLAAING
        )

        return stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), LocalDate.parse("2022-02-01")))
    }

    @Test
    fun `endre delt grense for stemmekrets skal opprette nye versjoner for grensa og dens stemmekretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(deltGrense!!.stemmekretser).hasSize(2)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato, endringsdato = endringsdato)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(opprinneligDeltGrense!!.stemmekretser).hasSize(2)
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

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(deltGrense!!.stemmekretser).hasSize(2)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(vestreKrets, opprinneligDato, endringsdato, "grenseNordVestLokalid")
        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato,
            endringsdato = endringsdato, featureForAnnenGrense)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(opprinneligDeltGrense!!.stemmekretser).hasSize(2)
        assertThat(opprinneligDeltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertAnnenGrenseEtterGrenseendring("grenseNordVest", "grenseNordVestLokalid", endringsdato, opprinneligDato,
            vestreKretsEtterLagring, opprinneligVestreKrets)
    }

    @Test
    fun `endre to grenser for to stemmekretser skal opprette nye versjoner for grensene og deres stemmekretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val endringsdato = LocalDate.parse("2022-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(deltGrense!!.stemmekretser).hasSize(2)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id)

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(oestreKrets, opprinneligDato, endringsdato, "grenseNordØstLokalid")
        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = opprinneligDato,
            endringsdato = endringsdato, featureForAnnenGrense)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), opprinneligDato))
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), endringsdato))
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))
        assertThat(opprinneligDeltGrense!!.stemmekretser).hasSize(2)
        assertThat(opprinneligDeltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(opprinneligVestreKrets.id, opprinneligOestreKrets.id)

        assertDeltGrenseEtterGrenseendring(deltGrenseLokalid, endringsdato, opprinneligDeltGrense,
            vestreKretsEtterLagring, oestreKretsEtterLagring, opprinneligVestreKrets, opprinneligOestreKrets)

        assertAnnenGrenseEtterGrenseendring("grenseNordØst", "grenseNordØstLokalid", endringsdato, opprinneligDato,
            oestreKretsEtterLagring, opprinneligOestreKrets)
    }

    @Test
    fun `endre delt grense etter oppdatering av stemmekrets skal opprette nye versjoner for grensa og dens stemmekretser`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val datoForGrenseendring = LocalDate.parse("2023-02-01")
        val deltGrenseLokalid = "grenseDeltLokalid"
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val vestreStemmekretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = vestreKrets, datoForOppdatering = datoForOppdatering)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))
        assertThat(deltGrense!!.stemmekretser).hasSize(3)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, vestreStemmekretsEtterOppdatering!!.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)
        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))

        //Sjekker at gammel versjon av grensa ikke har fått endra sine stemmekretser
        assertThat(opprinneligDeltGrense!!.stemmekretser).hasSize(3)
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

        val oestreStemmekretsEtterOppdatering = utfoerMetadataoppdatering(kretsSomSkalOppdateres = oestreKrets, datoForOppdatering = datoForOppdatering)

        val deltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForOppdatering))
        assertThat(deltGrense!!.stemmekretser).hasSize(3)
        assertThat(deltGrense.stemmekretser.map { it.stemmekrets.id })
            .containsExactlyInAnyOrder(vestreKrets.id, oestreKrets.id, oestreStemmekretsEtterOppdatering!!.id)

        val flaterSomErEndret = utfoerGrenseendring(kretsSomSkalHaGrenseendring = vestreKrets, opprinneligDato = datoForOppdatering,
            endringsdato = datoForGrenseendring)

        val opprinneligVestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForOppdatering))
        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForOppdatering))
        assertThat(opprinneligVestreKrets!!.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)

        val vestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), datoForGrenseendring))
        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), datoForGrenseendring))
        assertOmradeGyldighetVersjon(vestreKretsEtterLagring!!, oestreKretsEtterLagring!!, opprinneligVestreKrets, opprinneligOestreKrets)

        assertThat(flaterSomErEndret).containsExactlyInAnyOrder(vestreKretsEtterLagring, oestreKretsEtterLagring)

        val opprinneligDeltGrense = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), opprinneligDato))

        //Sjekker at gammel versjon av grensa ikke har fått endra sine stemmekretser
        assertThat(opprinneligDeltGrense!!.stemmekretser).hasSize(3)
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

        val featureForAnnenGrense = lagFeatureEndreYkoordinat(oestreKrets, opprinneligDato, endringsdato, "grenseNordØstLokalid")
        val kommunalKretsgrenseMetadata = featureForAnnenGrense.properties.metadata as KommunalKretsgrenseMetadata
        kommunalKretsgrenseMetadata.common.informasjon = "En eller annen informasjon"
        kommunalKretsgrenseMetadata.commonGrense.posisjonskvalitet =
            Posisjonskvalitet(maalemetode = KodelisteEntry(id = MaalemetodeKodeEnum.TERRENGMAALT.uuid, href = "maalemetodeHref"), noeyaktighet = 100)

        val flaterSomErEndret = grenseService.lagreGrenser(grenser = FeatureCollection(features = listOf(featureForAnnenGrense)),
            gyldighetsdato = endringsdato, endringstype = Endringstype.FASTSETTING
        )

        val opprinneligOestreKrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), opprinneligDato))
        assertThat(opprinneligOestreKrets!!.gyldighet.gyldigTil).isEqualTo(endringsdato)

        val oestreKretsEtterLagring = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(oestreKrets.lokalid), endringsdato))
        assertThat(oestreKretsEtterLagring!!.gyldighet.gyldigTil).isNull()
        assertThat(oestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligOestreKrets.omraade)
        assertThat(oestreKretsEtterLagring.version).isGreaterThan(opprinneligOestreKrets.version)
        assertThat(oestreKretsEtterLagring.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)

        assertThat(flaterSomErEndret).containsExactly(oestreKretsEtterLagring)
        assertAnnenGrenseEtterGrenseendring("grenseNordØst", "grenseNordØstLokalid", endringsdato, opprinneligDato,
            oestreKretsEtterLagring, opprinneligOestreKrets)

        val nyGrense = grenseService.hentGrense(Identifikator(Lokalid("grenseNordØstLokalid"), endringsdato))
        assertThat(nyGrense!!.kvalitet!!.noeyaktighet).isEqualTo(100)
        assertThat(nyGrense.kvalitet!!.maalemetode).isEqualTo(MaalemetodeKodeEnum.TERRENGMAALT.uuid)
        assertThat(nyGrense.informasjonstekst).isEqualTo("En eller annen informasjon")
        assertThat(nyGrense.auditInfo.typeEndring).isEqualTo(Endringstype.FASTSETTING)
    }

    @Test
    fun `oppdatering av stemmekrets til et allerede brukt stemmekretsnummer skal gi feil`() {
        val opprinneligDato = LocalDate.parse("2022-01-01")
        val datoForOppdatering = october1In2022
        val (vestreKrets, oestreKrets) = saveNaboStemmekretser(gyldigFra = opprinneligDato)

        val stemmekretsRequest = StemmekretsRequestBuilder().withLokalId(vestreKrets.lokalid).withStemmekretsnummer(oestreKrets.stemmekretsnummer!!).build()
        val identifikator = Identifikator(Lokalid(stemmekretsRequest.identifikasjon.lokalid), datoForOppdatering)

        Assertions.assertThatThrownBy {
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
            stemmekretsNavn = "Sammenslått krets",
            stemmekretsNummer = tredjeKrets.stemmekretsnummer!!
        )

        val sammenslaingsdato = LocalDate.parse("2022-02-01")
        Assertions.assertThatThrownBy {
            stemmekretsService.slaaSammenStemmekretser(
                sammenslaaingsendringRequest = sammenslaaingsendringRequest,
                sammenslaaingsDato = sammenslaingsdato,
                endringstype = Endringstype.VEDTATT_SAMMENSLAAING
            )
        }.isInstanceOf(ResponseException.BadRequestException::class.java)
            .hasMessageContaining("Kretsnummer 22 er brukt på flere stemmekretser innenfor kommunen")
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
        val deltGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForGrenseendring))
        assertThat(deltGrenseEtterGrenseendring!!.stemmekretser).hasSize(2)
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

        val opprinneligGrense = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), opprinneligDato))
        assertThat(opprinneligGrense!!.stemmekretser).hasSize(1)
        assertThat(opprinneligGrense.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(opprinneligKrets.id)

        val annenGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), datoForGrenseendring))
        assertThat(annenGrenseEtterGrenseendring!!.stemmekretser).hasSize(1)
        assertThat(annenGrenseEtterGrenseendring.stemmekretser.map { it.stemmekrets.id }).containsExactlyInAnyOrder(kretsEtterLagring.id)

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
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrenser(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == "grenseDeltLokalid" }

        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            Coordinate(10.0, 10.0),
            Coordinate(5.0, 5.0),
            Coordinate(10.0, 0.0)
        ))

        val oppdatertGrensefeature = FeatureBuilder(id = "grenseDeltLokalid")
            .withKontekstType(KontekstEnhetType.STEMMEKRETS)
            .withGrenseversjon(grenseKobling.grense.version)
            .withKontekstId(kontekstId = kretsSomSkalHaGrenseendring.lokalid, gyldighetsdato = endringsdato)
            .withGrensetype(Grensetype.Stemmekretsgrense)
            .withGeometry(oppdatertGeometri)
            .withRekkefoelge(grenseKobling.grenseKoblingAttributter.rekkefoelge)
            .withRetning(grenseKobling.grenseKoblingAttributter.retningMedKlokken)
            .build()

        val features = if (featureAnnenOppdatertGrense != null)
            listOf(oppdatertGrensefeature, featureAnnenOppdatertGrense) else listOf(oppdatertGrensefeature)

        return grenseService.lagreGrenser(
            grenser = FeatureCollection(features = features),
            gyldighetsdato = endringsdato,
            endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        )
    }

    private fun lagFeatureEndreYkoordinat(kretsSomSkalHaGrenseendring: Stemmekrets, opprinneligDato: LocalDate, endringsdato: LocalDate,
                                          lokalidGrenseSomSkalEndres: String): Feature {
        val grenseKobling = kretsSomSkalHaGrenseendring.getGrenser(gyldighetsdato = opprinneligDato)
            .first { it.grense.lokalid == lokalidGrenseSomSkalEndres }
        val grense = grenseKobling.grense.grense
        val oppdatertGeometri = GeometryFactory().createLineString(arrayOf(
            grense.getCoordinateN(0).copy(),
            grense.getCoordinateN(1).copy().also { it.setY(it.getY() + 4) },
            grense.getCoordinateN(2).copy()
        ))

        return FeatureBuilder(id = lokalidGrenseSomSkalEndres)
            .withKontekstType(KontekstEnhetType.STEMMEKRETS)
            .withGrenseversjon(grenseKobling.grense.version)
            .withKontekstId(kontekstId = kretsSomSkalHaGrenseendring.lokalid, gyldighetsdato = endringsdato)
            .withGrensetype(Grensetype.Stemmekretsgrense)
            .withGeometry(oppdatertGeometri)
            .withRekkefoelge(grenseKobling.grenseKoblingAttributter.rekkefoelge)
            .withRetning(grenseKobling.grenseKoblingAttributter.retningMedKlokken)
            .build()
    }

    private fun saveNaboStemmekretser(gyldigFra: LocalDate, medHull: Boolean = false, medTredjeKrets: Boolean = false): List<Stemmekrets> {
        val grenserForVest = if (medHull) {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Stemmekretsgrense) +
                GrenseHelper.getHullVestligKrets(gyldigFra, Grensetype.Stemmekretsgrense)
        } else {
            GrenseHelper.getGrenserForVestligKrets(gyldigFra, Grensetype.Stemmekretsgrense)
        }

        //Lagrer grensene først, for å være sikker på at de er tilgjengelige for å knyttes til kretsene
        val lagredeGrenserForVest = grenseRepository.saveAll(grenserForVest)
        val lagredeGrenserForOest = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(gyldigFra, Grensetype.Stemmekretsgrense))
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
        saveKommune(stemmekretser = stemmekretser)
        grenseRepository.flush()
        return stemmekretser
    }

    private fun saveStemmekretsMed1FramtidigVersjon(stemmekretsLokalid: Lokalid) {
        val stemmekrets = saveStemmekrets(id = "9000", lokalid = stemmekretsLokalid, gyldigTil = jan1In2023)
        val stemmekrets2 = saveStemmekrets(id = "9001", lokalid = stemmekretsLokalid, stemmekretsnavn = "Nedre Singsaker", gyldigFra = jan1In2023)
        saveKommune(stemmekretser = listOf(stemmekrets, stemmekrets2))
    }

}

fun assertGrenser(grensekoblinger: List<GrenseKobling>,
                  gyldighetsdato: LocalDate,
                  grenseIGrensekoblingId: String,
                  relevantGrenseId: String,
                  enhetKobletTilGrenseLokalid: Lokalid) {
    assertThat(grensekoblinger).hasSize(1)
    val grensekobling = grensekoblinger[0]
    val grenseIGrensekobling = grensekobling.grense
    val relevantGrense = grensekobling.getRelevantGrense(gyldighetsdato)
    assertThat(grenseIGrensekobling.id).isEqualTo(grenseIGrensekoblingId)
    assertThat(relevantGrense.id).isEqualTo(relevantGrenseId)
    assertThat(grensekobling.getEnhetKobletTilGrense().lokalid).isEqualTo(enhetKobletTilGrenseLokalid.value)
}
