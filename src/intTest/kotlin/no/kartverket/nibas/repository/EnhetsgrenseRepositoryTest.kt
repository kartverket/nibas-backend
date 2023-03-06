package no.kartverket.nibas.repository

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.KommuneBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
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
    private lateinit var kommuneRepository: KommuneRepository

    @Autowired
    private lateinit var grunnkretsRepository: GrunnkretsRepository

    @Autowired
    private lateinit var grenseRepository: GrenseRepository

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
        val grunnkrets1 = saveGrunnkrets(id = "101", lokalid = grunnkretsLokalid, grunnkrnr = grunnkrnr, gyldigFra = october1In2022, gyldigTil = december1In2022, grenser = listOf(grense1))
        val grunnkrets2 = saveGrunnkrets(id = "102", lokalid = grunnkretsLokalid, grunnkrnr = grunnkrnr, gyldigFra = december1In2022, gyldigTil = jan1In2024, grenser = listOf(grense1))
        val grunnkrets3 = saveGrunnkrets(id = "103", lokalid = grunnkretsLokalid, grunnkrnr = grunnkrnr, gyldigFra = jan1In2024, grenser = listOf(grense1))

        saveKommune(id = "123", lokalid = Lokalid("750"), kommunenr = kommunenr, grunnkretser = listOf(grunnkrets1, grunnkrets2, grunnkrets3))
        grenseRepository.flush()

        val rows = enhetsGrenseRepository.hentGrenserMedRiktigRetning(kommunenr, tillatteGrensetyperKommune, jan1In2023)
        Assertions.assertThat(rows).hasSize(1)
        Assertions.assertThat(rows[0].id).isEqualTo("2001")
        Assertions.assertThat(rows[0].lokalid).isEqualTo(grenseLokallid)
    }

    private fun saveKommune(id: String = "9000",
                            lokalid: Lokalid = Lokalid("85000"),
                            kommunenr: String = "0301",
                            grunnkretser: List<Grunnkrets> = emptyList()): Kommune {
        return kommuneRepository.save(KommuneBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = october1In2022,
            kommunenummer = kommunenr,
            grunnkretser = grunnkretser
        ).buildWithoutAdministrativenhetnavn())
    }

    private fun saveGrunnkrets(id: String = "9000",
                               lokalid: Lokalid = Lokalid("1001"),
                               gyldigFra: LocalDate = october1In2022,
                               gyldigTil: LocalDate? = null,
                               grunnkrnr: String = "42270304",
                               grenser: List<Grense> = emptyList()): Grunnkrets {
        return grunnkretsRepository.save(GrunnkretsBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            grunnkretsnummer = grunnkrnr,
            kommune = null,
            grenser = grenser
        ).build())
    }

    fun saveGrenseAvTypeKommunegrense(id: String = "12000",
                                      lokalid: Lokalid = Lokalid("ac7e05cd-fb07-4277-ad95-63c32c1370d0"),
                                      gyldigFra: LocalDate = october1In2022,
                                      gyldigTil: LocalDate? = null,
                                      opphav: String = "Eidsvoll",
                                      nesteVersjon: Grense? = null): Grense {
        return grenseRepository.save(GrenseBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav,
            nesteVersjon = nesteVersjon
        ).buildKommunegrense())
    }

}
