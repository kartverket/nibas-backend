package no.kartverket.nibas.kommuneogfylkesdeling

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.kodelister.KodelisteService
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
class KommuneDelingServiceIT: TestWithDbContainer() {
    @MockkBean
    private lateinit var kodelisteServiceMock: KodelisteService

    @MockkBean
    private lateinit var nibasEventPublisher: NibasEventPublisher

    @Autowired
    private lateinit var kommuneDelingService: KommuneDelingService

    val fylke1LokalId = "lokalIdFylke1"

    val fylkesnummerFylke1 = "01"
    val fylkesnummerFylke1id = "id-01"
    val fylkesnummerFylke2 = "02"
    val fylkesnummerFylke2id = "id-02"
    val fylkesnummerFylke3 = "03"
    val fylkesnummerFylke3id = "id-03"

    val kommune1LokalId = "lokalIdKommune1"

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

    val stemmekretsnummer1 = "01"
    val stemmekretsnummer2 = "02"

    val gyldighetsdato = LocalDate.of(LocalDate.now().year + 1, 1, 1)

    @BeforeEach
    fun beforeEachSetup() {
        justRun { nibasEventPublisher.publishNibasEvent(any(), any()) }

        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(kommuneNummer2) } returns kommuneNummer2id
        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(kommuneNummer3) } returns kommuneNummer3id

//        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(nyttKommuneNummer1) } returns nyttKommuneNummer1id
//        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(nyttKommuneNummer2) } returns nyttKommuneNummer2id
//        every { kodelisteServiceMock.getKommunenummerIdByCodeValue(nyttKommuneNummer3) } returns nyttKommuneNummer3id

//        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue(fylkesnummerFylke1) } returns fylkesnummerFylke1id
//        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue(fylkesnummerFylke2) } returns fylkesnummerFylke2id
//        every { kodelisteServiceMock.getFylkesnummerIdByCodeValue(fylkesnummerFylke3) } returns fylkesnummerFylke3id
    }

    @Test
    fun `test en kommunedeling hvor man deler en kommune i 2 og hver av de nye kommunen skal ha hver sin stemmekrets og grunnkrets`() {
        lagKommunerOgFylker()
        kommuneDelingService.delKommune(getKommuneDelingManus())

        // Den gamle kommunen skal ikke lenger være gyldig
        val gammelKommune = kommuneService.hentKommune(Identifikator(Lokalid(kommune1LokalId), gyldighetsdato))
        assertThat(gammelKommune).isNull()

        // De nye kommunene skal være gyldige
        val alleGyldigeKommuner = kommuneService.hentAlleKommuner(gyldighetsdato)
        assertThat(alleGyldigeKommuner.map { it.kommunenummerUuid }).containsAll(listOf(kommuneNummer2id, kommuneNummer3id))

        // Kommunene skal ha rett navn
        val nyKommune1 = alleGyldigeKommuner.first { it.kommunenummerUuid == kommuneNummer2id }
        val nyKommune2 = alleGyldigeKommuner.first { it.kommunenummerUuid == kommuneNummer3id }

        val nyKommune1Navn = nyKommune1.getAdministrativenhetNavn(gyldighetsdato).sortedBy { it.rekkefoelge }
        val nyKommune2Navn = nyKommune2.getAdministrativenhetNavn(gyldighetsdato).sortedBy { it.rekkefoelge }

        assertThat(nyKommune1Navn).hasSize(2)
        assertThat(nyKommune2Navn).hasSize(2)
        assertThat(nyKommune1Navn.first().navn).isEqualTo("nyKommune2Nor")
        assertThat(nyKommune2Navn.first().navn).isEqualTo("nyKommune3Nor")

        // Sjekk at grunnkretsene har blitt omnummerert
        val grunnkretserKommune1 = nyKommune1.getGrunnkretser(gyldighetsdato)
        val grunnkretserKommune2 = nyKommune2.getGrunnkretser(gyldighetsdato)

        assertThat(grunnkretserKommune1.map { it.grunnkretsnummer }).contains(nyttGrunnkretsnummer1)
        assertThat(grunnkretserKommune2.map { it.grunnkretsnummer }).contains(nyttGrunnkretsnummer2)

        // Sjekk at stemmekretsene ligger på de nye kommunene
        val stemmekretserKommune1 = nyKommune1.getStemmekretser(gyldighetsdato)
        val stemmekretserKommune2 = nyKommune2.getStemmekretser(gyldighetsdato)

        assertThat(stemmekretserKommune1.map { it.stemmekretsnummer }).contains(stemmekretsnummer1)
        assertThat(stemmekretserKommune2.map { it.stemmekretsnummer }).contains(stemmekretsnummer2)
    }

    private fun lagKommunerOgFylker() {
        val kommune1 = saveKommuneOgUnikLokalid(
            id = "fylkesdeling-kommune-1",
            lokalid = kommune1LokalId,
            kommunenummerUuid = kommuneNummer1id,
            kommunenummer = kommuneNummer1,
            grunnkretser = listOf(
                saveGrunnkrets(
                    id = "fylkesdeling-grunnkrets-id-1",
                    lokalid = Lokalid("fylkesdeling-grunnkrets-lokalid-1"),
                    grunnkretsnummer = grunnkretsnummer1,
                ),
                saveGrunnkrets(
                    id = "fylkesdeling-grunnkrets-id-2",
                    lokalid = Lokalid("fylkesdeling-grunnkrets-lokalid-2"),
                    grunnkretsnummer = grunnkretsnummer2,
                )
            ),
            stemmekretser = listOf(
                saveStemmekrets(
                    id = "fylkesdeling-stemmekrets-id-1",
                    lokalid = Lokalid("fylkesdeling-stemmekrets-lokalid-1"),
                    stemmekretsnummer = stemmekretsnummer1
                ),
                saveStemmekrets(
                    id = "fylkesdeling-stemmekrets-id-2",
                    lokalid = Lokalid("fylkesdeling-stemmekrets-lokalid-2"),
                    stemmekretsnummer = stemmekretsnummer2
                )
            )
        )

        val fylke = saveFylkeOgUnikLokalid(
            id = "fylkesdeling-fylke-1",
            lokalid = fylke1LokalId,
            fylkesnummerUuid = fylkesnummerFylke1id,
            fylkesnummer = fylkesnummerFylke1,
            kommuner = listOf(kommune1)
        )

        saveNasjon(fylker = listOf(fylke))
    }

    private fun getKommuneDelingManus(): KommunedelingManus =
        KommunedelingManus(
            gammeltKommuneNummer = kommuneNummer1,
            fraDato = gyldighetsdato,
            nyeKommuner = listOf(
                NyKommuneManus(
                    kommunenummer = kommuneNummer2,
                    navn = listOf(
                        NavnManus("nyKommune2Nor", 1, "nor"),
                        NavnManus("nyKommune2sme", 2, "sme"),
                    ),
                    stemmekretser = listOf(
                        StemmekretsManus(stemmekretsnummer1)
                    ),
                    grunnkretser = listOf(
                        GrunnkretsManus(
                            gammeltGrunnkretsNummer = grunnkretsnummer1,
                            nyttGrunnkretsNummer = nyttGrunnkretsnummer1
                        )
                    )
                ),
                NyKommuneManus(
                    kommunenummer = kommuneNummer3,
                    navn = listOf(
                        NavnManus("nyKommune3Nor", 1, "nor"),
                        NavnManus("nyKommune3sme", 2, "sme"),
                    ),
                    stemmekretser = listOf(
                        StemmekretsManus(stemmekretsnummer2)
                    ),
                    grunnkretser = listOf(
                        GrunnkretsManus(
                            gammeltGrunnkretsNummer = grunnkretsnummer2,
                            nyttGrunnkretsNummer = nyttGrunnkretsnummer2
                        )
                    )
                )
            )
        )
}
