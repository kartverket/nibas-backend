package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.builders.FylkeBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.builders.NasjonBuilder
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.findAllGyldigPaaDato
import no.kartverket.nibas.service.FlateService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate
import kotlin.test.assertEquals
import org.mockito.Mockito.`when` as whenever

@SpringBootTest(classes = [SosiImportFlateService::class])
class SosiImportFlateServiceTest {

    @Autowired
    lateinit var sosiImportFlateService: SosiImportFlateService

    @MockBean
    lateinit var flateService: FlateService

    @MockBean
    lateinit var kommuneRepository: KommuneRepository

    @MockBean
    lateinit var fylkeRepository: FylkeRepository

    @MockBean
    lateinit var nasjonRepository: NasjonRepository

    @Test
    fun `resultat-oppsummering håndterer både feilende og ikke-feilende flater`() {
        val kommune = KommuneBuilder().build()
        val kommuneSomFeiler = KommuneBuilder().build()
        val fylke = FylkeBuilder().build()
        val fylkeSomFeiler = FylkeBuilder().build()
        val nasjonSomFeiler = NasjonBuilder().build()

        val gyldighetsdato = LocalDate.now()
        whenever(kommuneRepository.findAllGyldigPaaDato(gyldighetsdato)).thenReturn(listOf(kommune, kommuneSomFeiler))
        whenever(fylkeRepository.findAllGyldigPaaDato(gyldighetsdato)).thenReturn(listOf(fylke, fylkeSomFeiler))
        whenever(nasjonRepository.findAllGyldigPaaDato(gyldighetsdato)).thenReturn(listOf(nasjonSomFeiler))

        whenever(flateService.oppdaterFlaterFor(setOf(kommuneSomFeiler))).thenThrow(RuntimeException())
        whenever(flateService.oppdaterFlaterFor(setOf(fylkeSomFeiler))).thenThrow(RuntimeException())
        whenever(flateService.oppdaterFlaterFor(setOf(nasjonSomFeiler))).thenThrow(RuntimeException())

        val resultat = sosiImportFlateService.settFlaterPaaAlleAdministrativeEnheter(gyldighetsdato)
        assertEquals(5, resultat.size)
        assertEquals(3, resultat.filter { it.isFailure }.size)
    }

}
