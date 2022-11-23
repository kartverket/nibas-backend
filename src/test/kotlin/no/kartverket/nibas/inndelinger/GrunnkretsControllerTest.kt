package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.NibasMvcTest
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.kodelister.KodelisteType
import no.kartverket.nibas.kodelister.MissingKodelisteItemException
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.util.*
import org.mockito.Mockito.`when` as whenever

@WebMvcTest(GrunnkretsController::class)
internal class GrunnkretsControllerTest : NibasMvcTest() {

    @MockBean
    private lateinit var grunnkretsService: GrunnkretsService

    @Test
    fun hentGrunnkretsReturns200_OK() {
        whenever(grunnkretsService.hentGrunnkrets(any(Lokalid::class.java), any(LocalDate::class.java))).thenReturn(GrunnkretsBuilder.DEFAULT_GRUNNKRETS)
        get("/v1/grunnkretser/1", status().isOk)
    }

    @Test
    fun manglendeItemForKodelisteReturns500_InternalServerError() {
        val id: String = UUID.randomUUID().toString();
        whenever(grunnkretsService.hentGrunnkrets(any(Lokalid::class.java), any(LocalDate::class.java))).thenThrow(MissingKodelisteItemException(KodelisteType.KOMMUNENUMMER, id))
        get("/v1/grunnkretser/$id", status().isInternalServerError)
    }

    @Test
    fun hentGrunnkretsMedGyldighetDatoGir200_OK() {
        whenever(grunnkretsService.hentGrunnkrets(any(Lokalid::class.java), any(LocalDate::class.java))).thenReturn(GrunnkretsBuilder.DEFAULT_GRUNNKRETS)
        get("/v1/grunnkretser/1?gyldigDato=2022-01-01", status().isOk)
    }

    @Test
    fun hentFramtidigeVersjonerReturns200_OK() {
        whenever(grunnkretsService.hentFramtidigeVersjonerForGrunnkrets(any(Lokalid::class.java), any(LocalDate::class.java)))
            .thenReturn(listOf(GrunnkretsBuilder(lokalId = Lokalid("123")).build()))
        get("/v1/grunnkretser/123/framtidigeversjoner", status().isOk)
    }


}
