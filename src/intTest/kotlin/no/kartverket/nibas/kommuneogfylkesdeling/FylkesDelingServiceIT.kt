package no.kartverket.nibas.kommuneogfylkesdeling

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.FylkeService
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ActiveProfiles("security-off")
@Transactional
class FylkesDelingServiceIT: TestWithDbContainer() {
    @MockkBean
    private lateinit var kodelisteServiceMock: KodelisteService

    @Autowired
    private lateinit var fylkesDelingService: FylkesDelingService

    @Autowired
    private lateinit var fylkeService: FylkeService

    val fylke1LokalId = "lokalIdFylke1"

    val fylkesnummerFylke1 = "01"
    val fylkesnummerFylke1id = "id-01"
    val fylkesnummerFylke2 = "02"
    val fylkesnummerFylke2id = "id-02"
    val fylkesnummerFylke3 = "03"
    val fylkesnummerFylke3id = "id-03"

    val kommune1LokalId = "lokalIdKommune1"
    val kommune2LokalId = "lokalIdKommune2"
    val kommune3LokalId = "lokalIdKommune3"

    val kommuneNummer1 = "0101"
    val kommuneNummer1id = "id-0101"
    val kommuneNummer2 = "0102"
    val kommuneNummer2id = "id-0102"
    val kommuneNummer3 = "0103"
    val kommuneNummer3id = "id-0103"

    val nyttKommuneNummer1 = "0201"
    val nyttKommuneNummer1id = "ny-id-0201"
    val nyttKommuneNummer2 = "0202"
    val nyttKommuneNummer2id = "ny-id-0202"
    val nyttKommuneNummer3 = "0301"
    val nyttKommuneNummer3id = "ny-id-0301"

    val grunnkretsnummer1 = "01010001"
    val nyttGrunnkretsnummer1 = "02010001"
    val grunnkretsnummer2 = "01020002"
    val nyttGrunnkretsnummer2 = "02020002"
    val grunnkretsnummer3 = "01030002"
    val nyttGrunnkretsnummer3 = "03010002"

    val stemmekretsnummer1 = "01"
    val stemmekretsnummer2 = "02"
    val stemmekretsnummer3 = "03"

    val gyldighetsdato = LocalDate.of(LocalDate.now().year + 1, 1, 1)

    @BeforeEach
    fun beforeEachSetup() {
        every { kodelisteServiceMock.getCodeValueByKommunenummerId(kommuneNummer1id) } returns kommuneNummer1
        every { kodelisteServiceMock.getCodeValueByKommunenummerId(kommuneNummer2id) } returns kommuneNummer2
        every { kodelisteServiceMock.getCodeValueByKommunenummerId(kommuneNummer3id) } returns kommuneNummer3

        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(kommuneNummer1) } returns kommuneNummer1id
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(kommuneNummer2) } returns kommuneNummer2id
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(kommuneNummer3) } returns kommuneNummer3id

        every { kodelisteServiceMock.getCodeValueByKommunenummerId(nyttKommuneNummer1id) } returns nyttKommuneNummer1
        every { kodelisteServiceMock.getCodeValueByKommunenummerId(nyttKommuneNummer2id) } returns nyttKommuneNummer2
        every { kodelisteServiceMock.getCodeValueByKommunenummerId(nyttKommuneNummer3id) } returns nyttKommuneNummer3

        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(nyttKommuneNummer1) } returns nyttKommuneNummer1id
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(nyttKommuneNummer2) } returns nyttKommuneNummer2id
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(nyttKommuneNummer3) } returns nyttKommuneNummer3id

        every { kodelisteServiceMock.getCodeValueByFylkesnummerId(fylkesnummerFylke1id) } returns fylkesnummerFylke1
        every { kodelisteServiceMock.getCodeValueByFylkesnummerId(fylkesnummerFylke2id) } returns fylkesnummerFylke2
        every { kodelisteServiceMock.getCodeValueByFylkesnummerId(fylkesnummerFylke3id) } returns fylkesnummerFylke3

        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue(fylkesnummerFylke1) } returns fylkesnummerFylke1id
        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue(fylkesnummerFylke2) } returns fylkesnummerFylke2id
        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue(fylkesnummerFylke3) } returns fylkesnummerFylke3id
    }

    @Test
    fun `test en fylkesdeling med 3 kommuner fra ett fylke som blir fordelt mellom 2 nye fylker`() {
        lagKommunerOgFylker()
        fylkesDelingService.delFylke(getFylkesDelingManus())

        // Gammelt fylke skal ikke lenger være gyldig
        val gammeltFylke = fylkeService.hentFylke(Identifikator(Lokalid(fylke1LokalId), gyldighetsdato))
        assertThat(gammeltFylke).isNull()

        // De nye fylkene skal være gyldig
        val alleGyldigeFylker = fylkeService.hentAlleFylker(gyldighetsdato)
        assertThat(alleGyldigeFylker.map { it.fylkesnummer }).containsAll(listOf(fylkesnummerFylke2id, fylkesnummerFylke3id))

        // Fylkene skal ha rett navn
        val nyttFylke1 = alleGyldigeFylker.first { it.fylkesnummer == fylkesnummerFylke2id }
        val nyttFylke2 = alleGyldigeFylker.first { it.fylkesnummer == fylkesnummerFylke3id }

        val nyttFylke1Navn = nyttFylke1.getAdministrativenhetNavn(gyldighetsdato).sortedBy { it.rekkefoelge }
        val nyttFylke2Navn = nyttFylke2.getAdministrativenhetNavn(gyldighetsdato).sortedBy { it.rekkefoelge }

        assertThat(nyttFylke1Navn).hasSize(2)
        assertThat(nyttFylke2Navn).hasSize(2)
        assertThat(nyttFylke1Navn.first().navn).isEqualTo("nyttFylke2Nor")
        assertThat(nyttFylke2Navn.first().navn).isEqualTo("nyttFylke3Nor")

        // Sjekk at fylkene inneholder rett kommuner
        val kommunerFylke1 = nyttFylke1.getKommuner(gyldighetsdato)
        val kommunerFylke2 = nyttFylke2.getKommuner(gyldighetsdato)

        assertThat(kommunerFylke1).hasSize(2)
        assertThat(kommunerFylke2).hasSize(1)
        assertThat(kommunerFylke1.map { it.kommunenummer }).containsAll(listOf(nyttKommuneNummer1id, nyttKommuneNummer2id))
        assertThat(kommunerFylke2.map { it.kommunenummer }).containsAll(listOf(nyttKommuneNummer3id))

        // Sjekk at grunnkretsene har blitt omnummerert
        val grunnkretseFylke1 = kommunerFylke1.flatMap { it.getGrunnkretser(gyldighetsdato) }
        val grunnkretseFylke2 = kommunerFylke2.flatMap { it.getGrunnkretser(gyldighetsdato) }

        assertThat(grunnkretseFylke1.map { it.grunnkretsnummer }).containsAll(listOf(nyttGrunnkretsnummer1, nyttGrunnkretsnummer2))
        assertThat(grunnkretseFylke2.map { it.grunnkretsnummer }).contains(nyttGrunnkretsnummer3)

        // Sjekk at stemmekretsene ligger på de nye kommunene
        val stemmekretserFylke1 = kommunerFylke1.flatMap { it.getStemmekretser(gyldighetsdato) }
        val stemmekretserFylke2 = kommunerFylke2.flatMap { it.getStemmekretser(gyldighetsdato) }

        assertThat(stemmekretserFylke1.map { it.stemmekretsnummer }).containsAll(listOf(stemmekretsnummer1, stemmekretsnummer2))
        assertThat(stemmekretserFylke2.map { it.stemmekretsnummer }).contains(stemmekretsnummer3)
    }

    private fun lagKommunerOgFylker() {
        val kommune1 = saveKommuneOgUnikLokalid(
            id = "fylkesdeling-kommune-1",
            lokalid = kommune1LokalId,
            kommunenummer = kommuneNummer1id,
            grunnkretser = listOf(
                saveGrunnkrets(
                    id = "fylkesdeling-grunnkrets-id-1",
                    lokalid = Lokalid("fylkesdeling-grunnkrets-lokalid-1"),
                    grunnkretsnummer = grunnkretsnummer1,
                )
            ),
            stemmekretser = listOf(
                saveStemmekrets(
                    id = "fylkesdeling-stemmekrets-id-1",
                    lokalid = Lokalid("fylkesdeling-stemmekrets-lokalid-1"),
                    stemmekretsnummer = stemmekretsnummer1
                )
            )
        )

        val kommune2 = saveKommuneOgUnikLokalid(
            id = "fylkesdeling-kommune-2",
            lokalid = kommune2LokalId,
            kommunenummer = kommuneNummer2id,
            grunnkretser = listOf(
                saveGrunnkrets(
                    id = "fylkesdeling-grunnkrets-id-2",
                    lokalid = Lokalid("fylkesdeling-grunnkrets-lokalid-2"),
                    grunnkretsnummer = grunnkretsnummer2,
                )
            ),
            stemmekretser = listOf(
                saveStemmekrets(
                    id = "fylkesdeling-stemmekrets-id-2",
                    lokalid = Lokalid("fylkesdeling-stemmekrets-lokalid-2"),
                    stemmekretsnummer = stemmekretsnummer2
                )
            )
        )

        val kommune3 = saveKommuneOgUnikLokalid(
            id = "fylkesdeling-kommune-3",
            lokalid = kommune3LokalId,
            kommunenummer = kommuneNummer3id,
            grunnkretser = listOf(
                saveGrunnkrets(
                    id = "fylkesdeling-grunnkrets-id-3",
                    lokalid = Lokalid("fylkesdeling-grunnkrets-lokalid-3"),
                    grunnkretsnummer = grunnkretsnummer3,
                )
            ),
            stemmekretser = listOf(
                saveStemmekrets(
                    id = "fylkesdeling-stemmekrets-id-3",
                    lokalid = Lokalid("fylkesdeling-stemmekrets-lokalid-3"),
                    stemmekretsnummer = stemmekretsnummer3
                )
            )
        )

        val fylke = saveFylkeOgUnikLokalid(
            id = "fylkesdeling-fylke-1",
            lokalid = fylke1LokalId,
            fylkesnummer = fylkesnummerFylke1id,
            kommuner = listOf(kommune1, kommune2, kommune3)
        )

        saveNasjon(fylker = listOf(fylke))
    }

    private fun getFylkesDelingManus(): FylkesdelingManus =
        FylkesdelingManus(
            gammeltFylkesNummer = fylkesnummerFylke1,
            fraDato = gyldighetsdato,
            nyeFylker = listOf(
                NyttFylkeManus(
                    fylkesNummer = fylkesnummerFylke2,
                    navn = listOf(
                        NavnManus("nyttFylke2Nor", 1, "nor"),
                        NavnManus("nyttFylke2Sme", 2, "sme"),
                    ),
                    kommuner = listOf(
                        FylkesDelingKommuneManus(kommuneNummer1, nyttKommuneNummer1, listOf(GrunnkretsManus(grunnkretsnummer1, nyttGrunnkretsnummer1))),
                        FylkesDelingKommuneManus(kommuneNummer2, nyttKommuneNummer2, listOf(GrunnkretsManus(grunnkretsnummer2, nyttGrunnkretsnummer2)))
                    )
                ),
                NyttFylkeManus(
                    fylkesNummer = fylkesnummerFylke3,
                    navn = listOf(
                        NavnManus("nyttFylke3Nor", 1, "nor"),
                        NavnManus("nyttFylke3Sme", 2, "sme"),
                    ),
                    kommuner = listOf(
                        FylkesDelingKommuneManus(kommuneNummer3, nyttKommuneNummer3, listOf(GrunnkretsManus(grunnkretsnummer3, nyttGrunnkretsnummer3))),
                    )
                )
            )
        )
}
