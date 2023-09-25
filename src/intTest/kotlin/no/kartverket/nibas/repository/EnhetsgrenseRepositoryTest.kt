package no.kartverket.nibas.repository

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.Month
import jakarta.transaction.Transactional

@ActiveProfiles("security-off")
@Transactional
class EnhetsgrenseRepositoryTest : TestWithDbContainer() {

    @Autowired
    private lateinit var enhetsGrenseRepository: EnhetsGrenseRepository

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val december1In2022 = LocalDate.of(2022, Month.DECEMBER, 1)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)
    private val jan1In2024 = LocalDate.of(2024, Month.JANUARY, 1)

    private val grenseLokallid = Lokalid("888")
    private val grunnkretsLokalid = Lokalid("1001")

    private val tillatteGrensetyperKommune: Set<Grensetype> = setOf(
        Grensetype.Kommunegrense,
        Grensetype.Fylkesgrense,
        Grensetype.AvtaltAvgrensningslinje,
        Grensetype.Territorialgrense,
        Grensetype.Riksgrense)

    @Test
    fun `hent grenser for grunnkrets fram i tid med query`() {
        val grense3 = saveGrenseAvTypeKommunegrense(id = "2002", lokalid = grenseLokallid, gyldigFra = jan1In2024)
        val grense2 = saveGrenseAvTypeKommunegrense(id = "2001", lokalid = grenseLokallid, nesteVersjon = grense3, gyldigFra = jan1In2023, gyldigTil = jan1In2024)
        val grense1 = saveGrenseAvTypeKommunegrense(id = "2000", lokalid = grenseLokallid, nesteVersjon = grense2, gyldigFra = october1In2022, gyldigTil = jan1In2023)

        val kommunenr = "4227"
        val grunnkrnr = kommunenr + "0304"
        val grunnkrets1 = saveGrunnkrets(id = "101", lokalid = grunnkretsLokalid, grunnkretsnummer = grunnkrnr, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val grunnkrets2 = saveGrunnkrets(id = "102", lokalid = grunnkretsLokalid, grunnkretsnummer = grunnkrnr, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1))
        val grunnkrets3 = saveGrunnkrets(id = "103", lokalid = grunnkretsLokalid, grunnkretsnummer = grunnkrnr, gyldigFra = jan1In2024, grenser = listOf(grense1))

        saveKommuneOgUnikLokalid(id = "123", lokalid = "750", kommunenummer = kommunenr, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))
        grenseRepository.flush()

        val rows = enhetsGrenseRepository.hentGrenserMedRiktigRetning(kommunenr, tillatteGrensetyperKommune, jan1In2023)
        Assertions.assertThat(rows).hasSize(1)
        Assertions.assertThat(rows[0].id).isEqualTo("2001")
        Assertions.assertThat(rows[0].lokalid).isEqualTo(grenseLokallid)
    }

}
