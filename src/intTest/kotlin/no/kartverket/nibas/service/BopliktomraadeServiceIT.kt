package no.kartverket.nibas.service

import com.ninjasquad.springmockk.MockkBean
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.BopliktomraadeRequestBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import no.kartverket.nibas.testcontainer.assertGrenser
import no.kartverket.nibas.utils.GrenseHelper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("localhost")
@Transactional
class BopliktomraadeServiceIT : TestWithDbContainer() {

    @MockkBean
    private lateinit var kodelisteServiceMock: KodelisteService

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val bopliktomraadeLokalid = Lokalid("ac7e05cd-fb07")
    private val grenseLokallid = Lokalid("888")

    @Test
    fun `hent eksisterende bopliktomraade med dagens dato`() {
        val bopliktomraade = saveBopliktomraade(lokalid = bopliktomraadeLokalid)
        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade))

        assertThat(bopliktomraadeRepository.findAllByLokalid(bopliktomraadeLokalid.value)).hasSize(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(bopliktomraadeService.hentBopliktomraade(Identifikator(bopliktomraadeLokalid, november28In2022))).isNotNull
    }

    @Test
    fun `hent versjon av bopliktomraade fram i tid`() {
        saveBopliktomraadeMed1FramtidigVersjon(bopliktomraadeLokalid)
        assertThat(bopliktomraadeRepository.findAllByLokalid(bopliktomraadeLokalid.value)).hasSize(2)

        val framtidigBopliktomraade = bopliktomraadeService.hentBopliktomraade(Identifikator(bopliktomraadeLokalid, jan1In2023))!!
        assertThat(framtidigBopliktomraade.navn).isEqualTo("Nedre Singsaker")
    }

    @Test
    fun `hent antall framtidige versjoner`() {
        saveBopliktomraadeMed1FramtidigVersjon(bopliktomraadeLokalid)
        assertThat(bopliktomraadeService.hentAntallGyldigeVersjonerFramITidFor(Identifikator(bopliktomraadeLokalid, november28In2022))).isEqualTo(1)
    }

    @Test
    fun `hent grenser for bopliktomraade fram i tid`() {
        val identifikatorFor1Jan23 = Identifikator(bopliktomraadeLokalid, jan1In2023)

        val grense3 = saveGrense(id = "4002", lokalid = grenseLokallid, gyldigFra = jan1In2024, grensetype = Grensetype.Bopliktgrense)
        val grense2 = saveGrense(id = "4001", lokalid = grenseLokallid, gyldigFra = jan1In2023, gyldigTil = jan1In2024, grensetype = Grensetype.Bopliktgrense)
        val grense1 = saveGrense(id = "4000", lokalid = grenseLokallid, gyldigFra = october1In2022, gyldigTil = jan1In2023, grensetype = Grensetype.Bopliktgrense)

        val bopliktomraade1 = saveBopliktomraade(id = "301", lokalid = bopliktomraadeLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022,
            grenser = listOf(grense1))
        val bopliktomraade2 = saveBopliktomraade(id = "302", lokalid = bopliktomraadeLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024,
            grenser = listOf(grense1, grense2))
        val bopliktomraade3 = saveBopliktomraade(id = "303", lokalid = bopliktomraadeLokalid, gyldigFra = jan1In2024, grenser = listOf(grense3))

        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade1, bopliktomraade2, bopliktomraade3))

        val grensekoblinger = bopliktomraadeService.hentGrensekoblingerForBopliktomraade(identifikatorFor1Jan23)

        assertGrenser(
            grensekoblinger,
            grenseIGrensekoblingId = "4001",
            enhetKobletTilGrenseLokalid = bopliktomraadeLokalid)
    }

    @Test
    fun `hentFramtidigeVersjonerForBopliktomraade skal returnere en liste hvis det finnes nyere versjoner`() {
        saveBopliktomraade(id = "301", lokalid = bopliktomraadeLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022)
        saveBopliktomraade(id = "302", lokalid = bopliktomraadeLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024)
        saveBopliktomraade(id = "303", lokalid = bopliktomraadeLokalid, gyldigFra = jan1In2024)

        assertThat(bopliktomraadeService.hentFramtidigeVersjonerForBopliktomraade(bopliktomraadeLokalid, december1In2022)).hasSize(2)
    }

    @Test
    fun `hentFramtidigeVersjonerForBopliktomraade skal returnere en liste med kun det gyldige elementet om det er ingen framtidige endringer`() {
        saveBopliktomraade(id = "301", lokalid = bopliktomraadeLokalid, gyldigFra = october1In2022, gyldigTil = december1In2022)
        saveBopliktomraade(id = "302", lokalid = bopliktomraadeLokalid, gyldigFra = december1In2022, gyldigTil = jan1In2024)

        assertThat(bopliktomraadeService.hentFramtidigeVersjonerForBopliktomraade(bopliktomraadeLokalid, december1In2022)).hasSize(1)
    }

    @Test
    fun `oppdaterBopliktomraade skal lage nytt bopliktomraade og sette gyldigtil for gammel krets`() {
        val bopliktomraadeRequest = BopliktomraadeRequestBuilder.DEFAULT_REQUEST
        assertThat(bopliktomraadeRequest.url).isNotNull()
        assertThat(bopliktomraadeRequest.forskriftsreferanse).isNotNull()
        assertThat(bopliktomraadeRequest.delvisBoplikt).isTrue()

        val identifikator = Identifikator(Lokalid(bopliktomraadeRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )

        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", bopliktomraader = listOf(bopliktomraade))

        val now = LocalDate.now()
        val opprinneligBopliktomraade = bopliktomraadeService.hentBopliktomraade(identifikator)!!

        bopliktomraadeService.oppdaterBopliktomraade(
            lokalId = identifikator.lokalid,
            oppdatertBopliktomraade = bopliktomraadeRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligBopliktomraadeEtter = bopliktomraadeService.hentBopliktomraade(identifikator)!!
        val bopliktomraadeEtter = bopliktomraadeService.hentBopliktomraade(Identifikator(identifikator.lokalid))!!

        assertThat(bopliktomraadeEtter.nummer).isNotEqualTo(bopliktomraade.nummer)
            .isEqualTo(bopliktomraadeRequest.nummer)
        assertThat(bopliktomraadeEtter.navn).isNotEqualTo(bopliktomraade.navn)
            .isEqualTo(bopliktomraadeRequest.navn)
        assertThat(bopliktomraadeEtter.omraade).isEqualTo(bopliktomraade.omraade)
        assertThat(bopliktomraadeEtter.posisjon).isEqualTo(bopliktomraade.posisjon)
        assertThat(bopliktomraadeEtter.gyldighet.gyldigFra).isEqualTo(now)
        assertThat(bopliktomraadeEtter.url).isNotNull().isNotEqualTo(bopliktomraade.url).isEqualTo(bopliktomraadeRequest.url)
        assertThat(bopliktomraadeEtter.forskriftsreferanse).isNotNull().isNotEqualTo(bopliktomraade.forskriftsreferanse)
            .isEqualTo(bopliktomraadeRequest.forskriftsreferanse)
        assertThat(bopliktomraadeEtter.delvisBoplikt).isNotNull().isTrue.isNotEqualTo(bopliktomraade.delvisBoplikt)
            .isEqualTo(bopliktomraadeRequest.delvisBoplikt)

        assertThat(opprinneligBopliktomraadeEtter.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligBopliktomraadeEtter.gyldighet.gyldigTil).isEqualTo(now)

        val grenseObjIdsForOpprinneligBopliktomraade = opprinneligBopliktomraade.grenser.map { it.grense.id }
        val grenseObjIdsForOpprinneligBopliktomraadeEtter = opprinneligBopliktomraadeEtter.grenser.map { it.grense.id }
        val grenseObjIdsForNyBopliktomraade = bopliktomraadeEtter.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligBopliktomraade).containsAll(grenseObjIdsForOpprinneligBopliktomraadeEtter)
        assertThat(grenseObjIdsForOpprinneligBopliktomraade).containsAll(grenseObjIdsForNyBopliktomraade)
        assertThat(grenseObjIdsForNyBopliktomraade).containsAll(grenseObjIdsForOpprinneligBopliktomraade)
        assertThat(grenseObjIdsForNyBopliktomraade).containsAll(grenseObjIdsForOpprinneligBopliktomraadeEtter)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligBopliktomraade)
        assertThat(grenser).hasSize(4)
        grenser.forEach { grense ->
            assertThat(grense.bopliktomraader.map { it.bopliktomraade.id }).hasSize(2)
                .containsExactlyInAnyOrder(opprinneligBopliktomraade.id, bopliktomraadeEtter.id)
        }
    }


    @Test
    fun `oppdaterBopliktomraade skal kunne blanke ut felt`() {
        val bopliktomraadeRequest = BopliktomraadeRequestBuilder.DEFAULT_REQUEST.copy(url = null, forskriftsreferanse = null, informasjon = null)

        val identifikator = Identifikator(Lokalid(bopliktomraadeRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )
        assertThat(bopliktomraade.url).isNotNull()
        assertThat(bopliktomraade.forskriftsreferanse).isNotNull()
        assertThat(bopliktomraade.informasjon).isNotNull()

        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", bopliktomraader = listOf(bopliktomraade))

        val now = LocalDate.now()
        val opprinneligBopliktomraade = bopliktomraadeService.hentBopliktomraade(identifikator)!!

        bopliktomraadeService.oppdaterBopliktomraade(
            lokalId = identifikator.lokalid,
            oppdatertBopliktomraade = bopliktomraadeRequest,
            gyldigFra = now,
            endringstype = Endringstype.RETTING
        )

        val opprinneligBopliktomraadeEtter = bopliktomraadeService.hentBopliktomraade(identifikator)!!
        val bopliktomraadeEtter = bopliktomraadeService.hentBopliktomraade(Identifikator(identifikator.lokalid))!!

        assertThat(bopliktomraadeEtter.nummer).isNotEqualTo(bopliktomraade.nummer)
            .isEqualTo(bopliktomraadeRequest.nummer)
        assertThat(bopliktomraadeEtter.navn).isNotEqualTo(bopliktomraade.navn)
            .isEqualTo(bopliktomraadeRequest.navn)
        assertThat(bopliktomraadeEtter.omraade).isEqualTo(bopliktomraade.omraade)
        assertThat(bopliktomraadeEtter.posisjon).isEqualTo(bopliktomraade.posisjon)
        assertThat(bopliktomraadeEtter.gyldighet.gyldigFra).isEqualTo(now)
        assertThat(bopliktomraadeEtter.url).isNull()
        assertThat(bopliktomraadeEtter.forskriftsreferanse).isNull()
        assertThat(bopliktomraadeEtter.informasjon).isNull()

        assertThat(opprinneligBopliktomraadeEtter.gyldighet.gyldigFra).isEqualTo(october1In2022)
        assertThat(opprinneligBopliktomraadeEtter.gyldighet.gyldigTil).isEqualTo(now)
        assertThat(opprinneligBopliktomraadeEtter.url).isNotNull()
        assertThat(opprinneligBopliktomraadeEtter.forskriftsreferanse).isNotNull()

        val grenseObjIdsForOpprinneligBopliktomraade = opprinneligBopliktomraade.grenser.map { it.grense.id }
        val grenseObjIdsForOpprinneligBopliktomraadeEtter = opprinneligBopliktomraadeEtter.grenser.map { it.grense.id }
        val grenseObjIdsForNyBopliktomraade = bopliktomraadeEtter.grenser.map { it.grense.id }

        assertThat(grenseObjIdsForOpprinneligBopliktomraade).containsAll(grenseObjIdsForOpprinneligBopliktomraadeEtter)
        assertThat(grenseObjIdsForOpprinneligBopliktomraade).containsAll(grenseObjIdsForNyBopliktomraade)
        assertThat(grenseObjIdsForNyBopliktomraade).containsAll(grenseObjIdsForOpprinneligBopliktomraade)
        assertThat(grenseObjIdsForNyBopliktomraade).containsAll(grenseObjIdsForOpprinneligBopliktomraadeEtter)

        val grenser = grenseRepository.findAllById(grenseObjIdsForOpprinneligBopliktomraade)
        assertThat(grenser).hasSize(4)
        grenser.forEach { grense ->
            assertThat(grense.bopliktomraader.map { it.bopliktomraade.id }).hasSize(2).containsExactlyInAnyOrder(opprinneligBopliktomraade.id, bopliktomraadeEtter.id)
        }
    }

    @Test
    fun `oppdaterBopliktomraade skal kaste exception ved ugyldig kommune for bopliktomraadet`() {
        val bopliktomraadeRequest = BopliktomraadeRequestBuilder.DEFAULT_REQUEST

        val identifikator = Identifikator(Lokalid(bopliktomraadeRequest.identifikasjon.lokalid), october1In2022)
        val lagredeGrenserForKrets = grenseRepository.saveAll(GrenseHelper.getGrenserForOestligKrets(october1In2022, Grensetype.Bopliktgrense))
        grenseRepository.flush()

        val bopliktomraade = saveBopliktomraade(
            id = "2",
            lokalid = identifikator.lokalid,
            gyldigFra = october1In2022,
            grenser = lagredeGrenserForKrets
        )

        //Kommunen har gyldig fra etter bopliktområdets gyldig fra, og gyldig til før ønsket oppdateringsdato
        saveKommuneOgUnikLokalid(kommunenummerUuid = "4227", gyldigFra = november28In2022, gyldigTil = jan1In2023, bopliktomraader = listOf(bopliktomraade))
        val now = LocalDate.now()

        assertThatThrownBy {
            bopliktomraadeService.oppdaterBopliktomraade(
                lokalId = identifikator.lokalid,
                oppdatertBopliktomraade = bopliktomraadeRequest,
                gyldigFra = now,
                endringstype = Endringstype.RETTING
            )
        }.isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fant ikke kommune for bopliktområde med id ${bopliktomraade.id}")

        assertThatThrownBy {
            bopliktomraadeService.createNyVersjonAvBopliktomraade(bopliktomraade = bopliktomraade, gyldigFra = now, endringstype = Endringstype.RETTING)
        }.isInstanceOf(ResponseException.NotFoundException::class.java)
            .hasMessageContaining("Fant ikke kommune for bopliktområde med id ${bopliktomraade.id}")

    }

    private fun assertOmradeGyldighetVersjon(vestreKretsEtterLagring: Bopliktomraade, oestreKretsEtterLagring: Bopliktomraade,
                                             opprinneligVestreKrets: Bopliktomraade, opprinneligOestreKrets: Bopliktomraade) {
        assertThat(vestreKretsEtterLagring.gyldighet.gyldigTil).isNull()
        assertThat(oestreKretsEtterLagring.gyldighet.gyldigTil).isNull()
        assertThat(vestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligVestreKrets.omraade)
        assertThat(oestreKretsEtterLagring.omraade).isNotEqualTo(opprinneligOestreKrets.omraade)
        assertThat(vestreKretsEtterLagring.version).isGreaterThan(opprinneligVestreKrets.version)
        assertThat(oestreKretsEtterLagring.version).isGreaterThan(opprinneligOestreKrets.version)
    }

    private fun assertDeltGrenseEtterGrenseendring(
        @Suppress("SameParameterValue") deltGrenseLokalid: String, datoForGrenseendring: LocalDate, opprinneligGrense: Grense,
        vestreKretsEtterLagring: Bopliktomraade, oestreKretsEtterLagring: Bopliktomraade, opprinneligVestreKrets: Bopliktomraade, opprinneligOestreKrets: Bopliktomraade
    ) {
        val deltGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(deltGrenseLokalid), datoForGrenseendring))!!
        assertThat(deltGrenseEtterGrenseendring.bopliktomraader).hasSize(2)
        assertThat(deltGrenseEtterGrenseendring.bopliktomraader.map { it.bopliktomraade.id })
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
                                                    opprinneligDato: LocalDate, kretsEtterLagring: Bopliktomraade, opprinneligKrets: Bopliktomraade) {

        val opprinneligGrense = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), opprinneligDato))!!
        assertThat(opprinneligGrense.bopliktomraader).hasSize(2)
        assertThat(opprinneligGrense.bopliktomraader.map { it.bopliktomraade.id }).contains(opprinneligKrets.id)

        val annenGrenseEtterGrenseendring = grenseService.hentGrense(Identifikator(Lokalid(annenGrenseLokalid), datoForGrenseendring))!!
        assertThat(annenGrenseEtterGrenseendring.bopliktomraader).hasSize(2)
        assertThat(annenGrenseEtterGrenseendring.bopliktomraader.map { it.bopliktomraade.id }).contains(kretsEtterLagring.id)

        assertThat(opprinneligGrense.gyldighet.gyldigTil).isEqualTo(datoForGrenseendring)
        assertThat(annenGrenseEtterGrenseendring.gyldighet.gyldigTil).isNull()
        assertThat(opprinneligGrense.lokalid).isEqualTo(annenGrenseEtterGrenseendring.lokalid)
        assertThat(opprinneligGrense.grense).isNotEqualTo(annenGrenseEtterGrenseendring.grense)
        assertThat(annenGrenseEtterGrenseendring.version).isGreaterThan(opprinneligGrense.version)

        assertThat(opprinneligKrets.grenser.map { it.grense.id }).contains(annenGrenseId)
        assertThat(kretsEtterLagring.grenser.map { it.grense.id }).doesNotContain(annenGrenseId)
        assertThat(kretsEtterLagring.grenser.map { it.grense.lokalid }).contains(annenGrenseLokalid)
    }

    private fun utfoerMetadataoppdatering(kretsSomSkalOppdateres: Bopliktomraade, datoForOppdatering: LocalDate): Bopliktomraade? {
        val bopliktomraadeRequest = BopliktomraadeRequestBuilder().withLokalId(kretsSomSkalOppdateres.lokalid).build()
        val identifikator = Identifikator(Lokalid(bopliktomraadeRequest.identifikasjon.lokalid), datoForOppdatering)

        bopliktomraadeService.oppdaterBopliktomraade(
            lokalId = identifikator.lokalid,
            oppdatertBopliktomraade = bopliktomraadeRequest,
            gyldigFra = datoForOppdatering,
            endringstype = Endringstype.RETTING
        )
        return bopliktomraadeService.hentBopliktomraade(identifikator)
    }

    fun settGrensetypeForGrenserSomIkkeErDelt(vestreKrets: Bopliktomraade, oestreKrets: Bopliktomraade, grensetype: Grensetype) {
        vestreKrets.grenser.stream().forEach { if (it.grense.id != "grenseDelt" && it.grense.id != "grenseDelt2") it.grense.grensetype = grensetype }
        oestreKrets.grenser.stream().forEach { if (it.grense.id != "grenseDelt" && it.grense.id != "grenseDelt2") it.grense.grensetype = grensetype }
    }

    private fun saveBopliktomraadeMed1FramtidigVersjon(bopliktomraadeLokalid: Lokalid) {
        val bopliktomraade = saveBopliktomraade(id = "9000", lokalid = bopliktomraadeLokalid, gyldigTil = jan1In2023)
        val bopliktomraade2 = saveBopliktomraade(id = "9001", lokalid = bopliktomraadeLokalid, bopliktomraadenavn = "Nedre Singsaker", gyldigFra = jan1In2023)

        saveKommuneOgUnikLokalid(bopliktomraader = listOf(bopliktomraade, bopliktomraade2))
    }

}

