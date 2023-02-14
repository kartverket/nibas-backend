package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.geojson.metadata.IdentifikatorMedVersjon
import no.kartverket.nibas.api.v1.request.StemmekretsSammenslaaingsendringRequest
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.service.utils.StemmekretsHelper
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("security-off")
@Transactional
class StemmekretsServiceIT : TestWithDbContainer() {

    @Autowired
    private lateinit var stemmekretsService: StemmekretsService

    @Autowired
    private lateinit var kommuneRepository: KommuneRepository

    @Autowired
    private lateinit var stemmekretsRepository: StemmekretsRepository

    @Autowired
    private lateinit var grenseRepository: GrenseRepository

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val stemmekretsLokalid = Lokalid("ac7e05cd-fb07-4270-ad95-88c320137ba6")
    private val grenseLokallid = Lokalid("888")

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
    fun `slaaSammenStemmekretser ny krets skal bruke viderefoert navn og nummer`() {
        val (vestreKrets, oesteKrets) = saveStemmekretserForSammenslaaing(gyldigFra = LocalDate.parse("2022-01-01"))

        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 1),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oesteKrets.lokalid, 1)),
            stemmekretsNavn = "Sammenslått krets",
            stemmekretsNummer = "nytt nummer"
        )

        stemmekretsService.slaaSammenStemmekretser(
            sammenslaaingsendringRequest = sammenslaaingsendringRequest,
            sammenslaaingsDato = LocalDate.parse("2022-02-01"),
            endringstype = Endringstype.VEDTATT_SAMMENSLAAING
        )

        val sammenslattStemmekrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), LocalDate.parse("2022-02-01")))

        assertThat(sammenslattStemmekrets?.stemmekretsnavn).isEqualTo("Sammenslått krets")
        assertThat(sammenslattStemmekrets?.stemmekretsnummer).isEqualTo("nytt nummer")
    }

    @Test
    fun `slaaSammenStemmekretser skal inneholde alle grenser utenom delte grenser`() {
        val (vestreKrets, oesteKrets) = saveStemmekretserForSammenslaaing(gyldigFra = LocalDate.parse("2022-01-01"))

        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 1),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oesteKrets.lokalid, 1)),
            stemmekretsNavn = "Sammenslått krets",
            stemmekretsNummer = "nytt nummer"
        )

        stemmekretsService.slaaSammenStemmekretser(
            sammenslaaingsendringRequest = sammenslaaingsendringRequest,
            sammenslaaingsDato = LocalDate.parse("2022-02-01"),
            endringstype = Endringstype.VEDTATT_SAMMENSLAAING
        )

        val sammenslattStemmekrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), LocalDate.parse("2022-02-01")))

        val antallGrenser = vestreKrets.grenser.size + oesteKrets.grenser.size
        val grenseIder = sammenslattStemmekrets?.grenser?.map { it.grense.id }

        assertThat(sammenslattStemmekrets?.grenser?.size).isLessThan(antallGrenser)
        assertThat(grenseIder).doesNotContain("grenseDelt")
    }

    @Test
    fun `slaaSammenStemmekretser skal skal kaste konflikt om den feiler optimstiskLaas-sjekken for den viderefoerte stemmekretsen`() {
        val (vestreKrets, oesteKrets) = saveStemmekretserForSammenslaaing(gyldigFra = LocalDate.parse("2022-01-01"))

        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 5),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oesteKrets.lokalid, 1)),
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
    fun `slaaSammenStemmekretser skal skal kaste konflikt om den feiler optimstiskLaas-sjekken for en av stemmekretsene til sammenslaaing`() {
        val (vestreKrets, oesteKrets) = saveStemmekretserForSammenslaaing(gyldigFra = LocalDate.parse("2022-01-01"))

        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 1),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oesteKrets.lokalid, 5)),
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
        val (vestreKrets, oesteKrets) = saveStemmekretserForSammenslaaing(gyldigFra = LocalDate.parse("2022-01-01"), medHull = true)

        val sammenslaaingsendringRequest = StemmekretsSammenslaaingsendringRequest(
            viderefoertStemmekrets = IdentifikatorMedVersjon(vestreKrets.lokalid, 1),
            stemmekretserTilSammenslaaing = listOf(IdentifikatorMedVersjon(oesteKrets.lokalid, 1)),
            stemmekretsNavn = "Sammenslått krets",
            stemmekretsNummer = "nytt nummer"
        )

        stemmekretsService.slaaSammenStemmekretser(
            sammenslaaingsendringRequest = sammenslaaingsendringRequest,
            sammenslaaingsDato = LocalDate.parse("2022-02-01"),
            endringstype = Endringstype.VEDTATT_SAMMENSLAAING
        )

        val sammenslattStemmekrets = stemmekretsService.hentStemmekrets(Identifikator(Lokalid(vestreKrets.lokalid), LocalDate.parse("2022-02-01")))

        // Det er en delt grense, så vi vil beholde alle grenser utenom 1 fra hver
        val antallGrenser = vestreKrets.grenser.size + oesteKrets.grenser.size - 2
        assertThat(sammenslattStemmekrets?.grenser?.size).isEqualTo(antallGrenser)

        // Vi skal ha 4 grenser som danner ett hull
        val hullGrenser = sammenslattStemmekrets?.grenser?.filter { it.grenseKoblingAttributter.hullIndeks != null }
        assertThat(hullGrenser).hasSize(4)
    }

    fun saveGrenseAvTypeKommunegrense(
        id: String = "12000",
        lokalid: Lokalid = Lokalid("ac7e05cd-fb07-4277-ad95-63c32c1370d0"),
        gyldigFra: LocalDate = october1In2022,
        gyldigTil: LocalDate? = null,
        opphav: String = "Eidsvoll",
        nesteVersjon: Grense? = null
    ): Grense {
        return grenseRepository.save(GrenseBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav,
            nesteVersjon = nesteVersjon
        ).buildKommunegrense())
    }

    private fun saveStemmekretserForSammenslaaing(gyldigFra: LocalDate, medHull: Boolean = false): List<Stemmekrets> {
        val grenserForVest = if (medHull) {
            StemmekretsHelper.getGrensterForVestligStemmekrets() + StemmekretsHelper.getHullVestligStemmekrets()
        } else {
            StemmekretsHelper.getGrensterForVestligStemmekrets()
        }

        val kretsVest = saveStemmekrets(
            id = "1",
            lokalid = Lokalid("123"),
            gyldigFra = gyldigFra,
            stemmekretsnavn = "Vestre krets",
            grenser = grenserForVest.save()
        )

        val kretsOest = saveStemmekrets(
            id = "2",
            lokalid = Lokalid("234"),
            gyldigFra = gyldigFra,
            stemmekretsnavn = "Østre krets",
            grenser = StemmekretsHelper.getGrensterForOestligStemmekrets().save()
        )

        saveKommune(stemmekretser = listOf(kretsVest, kretsOest))
        return listOf(kretsVest, kretsOest)
    }

    private fun saveStemmekretsMed1FramtidigVersjon(stemmekretsLokalid: Lokalid) {
        val stemmekrets = saveStemmekrets(id = "9000", lokalid = stemmekretsLokalid, gyldigTil = jan1In2023)
        val stemmekrets2 = saveStemmekrets(id = "9001", lokalid = stemmekretsLokalid, gyldigFra = jan1In2023, stemmekretsnavn = "Nedre Singsaker")
        saveKommune(stemmekretser = listOf(stemmekrets, stemmekrets2))
    }

    private fun saveKommune(stemmekretser: List<Stemmekrets> = emptyList()): Kommune {
        return kommuneRepository.save(KommuneBuilder(
            id = "9000",
            lokalId = Lokalid("abc-123"),
            gyldigFra = october1In2022,
            kommunenummer = "0301",
            stemmekretser = stemmekretser
        ).buildWithoutAdministrativenhetnavn())
    }

    private fun saveStemmekrets(
        id: String = "9000",
        lokalid: Lokalid,
        gyldigFra: LocalDate = october1In2022,
        gyldigTil: LocalDate? = null,
        stemmekretsnavn: String = "Øvre Singsaker",
        grenser: List<Grense> = emptyList()
    ): Stemmekrets {
        return stemmekretsRepository.save(StemmekretsBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            stemmekretsnavn = stemmekretsnavn,
            kommune = null,
            grenser = grenser
        ).build())
    }

    private fun List<Grense>.save() =
        this.map { grenseRepository.save(it) }
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
