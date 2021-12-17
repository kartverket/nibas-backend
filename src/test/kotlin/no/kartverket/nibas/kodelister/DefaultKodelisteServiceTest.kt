package no.kartverket.nibas.kodelister

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

internal class DefaultKodelisteServiceTest {

    @Mock
    private lateinit var kodelisteFetcher: KodelisteFetcher

    private lateinit var kodelisteService: KodelisteService

    private val codeValueForTysnes = "4616"
    private val uuidForTysnes = "34ed7216-73af-4f83-9b72-005cb5e7afb5"
    private val codeValueForJanMayen = "22"
    private val uuidForJanMayen = "b80dc6a5-07a0-41e2-8687-0e891bf8f4cf"
    private val codeValueForOrtofoto = "42"
    private val uuidForOrtofoto = "b067d9fc-64a4-4bd4-aa10-049c4d6ab6d6"

    @BeforeEach
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        kodelisteService = DefaultKodelisteService(kodelisteFetcher)
    }

    @Test
    fun getKommunenummerIdByCodeValue() {
        doMockKommunenummer()
        val kommunenummerId = kodelisteService.getKommunenummerIdByCodeValue(codeValueForTysnes)
        assertThat(kommunenummerId).isEqualTo(uuidForTysnes)
    }

    @Test
    fun getKommunenummerIdByCodeValue_ValueDoesNotExist() {
        doMockKommunenummer()
        assertThatThrownBy { kodelisteService.getKommunenummerIdByCodeValue("87") }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun getFylkesnummerIdByCodeValue() {
        doMockFylkesnummer()
        val fylkesnummerId = kodelisteService.getFylkesnummerIdByCodeValue(codeValueForJanMayen)
        assertThat(fylkesnummerId).isEqualTo(uuidForJanMayen)
    }

    @Test
    fun getFylkesnummerIdByCodeValue_ValueDoesNotExist() {
        doMockFylkesnummer()
        assertThatThrownBy { kodelisteService.getFylkesnummerIdByCodeValue("1") }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun getMalemetodeIdByCodeValue() {
        doMockMalemetoder()
        val malemetodeId = kodelisteService.getMalemetodeIdByCodeValue(codeValueForOrtofoto)
        assertThat(malemetodeId).isEqualTo(uuidForOrtofoto)
    }

    @Test
    fun getMalemetodeIdByCodeValue_ValueDoesNotExist() {
        doMockMalemetoder()
        assertThatThrownBy { kodelisteService.getMalemetodeIdByCodeValue("43") }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun kommunenummerIdExists() {
        doMockKommunenummer()
        assertThat(kodelisteService.kommunenummerIdExists(uuidForTysnes)).isTrue
        assertThat(kodelisteService.kommunenummerIdExists("0faa6b9d-202f-4bbb-809b-00563a01c69c")).isFalse
    }

    @Test
    fun fylkesnummerIdExists() {
        doMockFylkesnummer()
        assertThat(kodelisteService.fylkesnummerIdExists(uuidForJanMayen)).isTrue
        assertThat(kodelisteService.fylkesnummerIdExists("0faa6b9d-202f-4bbb-809b-00563a01c69c")).isFalse
    }

    private fun doMockKommunenummer() {
        `when`(kodelisteFetcher.fetchForKodeliste(anyString())).thenReturn(
            listOf(
                GeonorgeKodelisteItem(
                    id = "https://register.geonorge.no/sosi-kodelister/kommunenummer/5417/0faa6b9d-202f-4bbb-809b-00563a01c69b",
                    label = "5417",
                    lang = "no",
                    uuid = "0faa6b9d-202f-4bbb-809b-00563a01c69b",
                    status = "Gyldig",
                    description = "Salangen",
                    codevalue = "5417"
                ),
                GeonorgeKodelisteItem(
                    id = "https://register.geonorge.no/sosi-kodelister/kommunenummer/4616/34ed7216-73af-4f83-9b72-005cb5e7afb5",
                    label = "4616",
                    lang = "no",
                    uuid = uuidForTysnes,
                    status = "Gyldig",
                    description = "Tysnes",
                    codevalue = codeValueForTysnes
                ),
            )
        )
    }

    private fun doMockFylkesnummer() {
        `when`(kodelisteFetcher.fetchForKodeliste(anyString())).thenReturn(
            listOf(
                GeonorgeKodelisteItem(
                    id = "https://register.geonorge.no/sosi-kodelister/fylkesnummer/22/b80dc6a5-07a0-41e2-8687-0e891bf8f4cf",
                    label = "22",
                    lang = "no",
                    uuid = uuidForJanMayen,
                    status = "Gyldig",
                    description = "Jan Mayen",
                    codevalue = codeValueForJanMayen
                ),
                GeonorgeKodelisteItem(
                    id = "https://register.geonorge.no/sosi-kodelister/fylkesnummer/42/4e2d77a8-1196-4736-9575-21c0219570f0",
                    label = "42",
                    lang = "no",
                    uuid = "4e2d77a8-1196-4736-9575-21c0219570f0",
                    status = "Gyldig",
                    description = "Agder",
                    codevalue = "42"
                ),
            )
        )
    }

    private fun doMockMalemetoder() {
        `when`(kodelisteFetcher.fetchForKodeliste(anyString())).thenReturn(
            listOf(
                GeonorgeKodelisteItem(
                    id = "https://register.geonorge.no/sosi-kodelister/malemetode-kode/terrengmalt-uspesifisert-maleinstrument/7f48625b-e46f-413e-ae4d-0381ac64264b",
                    label = "Terrengmålt: Uspesifisert måleinstrument",
                    lang = "no",
                    uuid = "7f48625b-e46f-413e-ae4d-0381ac64264b",
                    status = "Gyldig",
                    description = "Målt i terrenget , uspesifisert metode/måleinstrument",
                    codevalue = "10"
                ),
                GeonorgeKodelisteItem(
                    id = "https://register.geonorge.no/sosi-kodelister/malemetode-kode/digitaliseringbord-ortofoto-fotokopi/b067d9fc-64a4-4bd4-aa10-049c4d6ab6d6",
                    label = "Digitaliseringbord: Ortofoto, fotokopi",
                    lang = "no",
                    uuid = uuidForOrtofoto,
                    status = "Gyldig",
                    description = "Geometri overført fra ortofoto ved hjelp av manuell registrering på et digitaliseringsbord. Bildemedium er fotokopi",
                    codevalue = codeValueForOrtofoto
                ),
            )
        )
    }

}
