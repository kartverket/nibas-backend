package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month

@ActiveProfiles("security-off")
@Transactional
class GrenseServiceIT : TestWithDbContainer() {

    @Autowired
    private lateinit var grenseService: GrenseService

    @Autowired
    private lateinit var grenseRepository: GrenseRepository

    private val october1In2022 = LocalDate.of(2022, Month.OCTOBER, 1)
    private val november28In2022 = LocalDate.of(2022, Month.NOVEMBER, 28)
    private val jan1In2023 = LocalDate.of(2023, Month.JANUARY, 1)

    @Test
    fun hentEksisterendeGrenseMedDagensDato() {
        val lokalIdOfGrense = Lokalid("ac7e05ca-fb65-4277-ad95-63c32c1370d0")
        saveGrenseAvTypeKommune(lokalid = lokalIdOfGrense)
        assertThat(grenseRepository.findAllByLokalid(lokalIdOfGrense.value).size).isEqualTo(1)

        // Later som 28.11.22 er dagens dato. Det fungerer så lenge den er senere enn 1.10.22
        assertThat(grenseService.hentGrense(Identifikator(lokalIdOfGrense, november28In2022))).isNotNull
    }

    @Test
    fun hentVersjonAvKommunegrenseFramITid() {
        val lokalIdOfGrense = Lokalid("ac7e05ca-fb65-4277-ad95-63c32c1370d0")
        saveGrenseAvTypeKommune(id = "9000", lokalid = lokalIdOfGrense, gyldigTil = jan1In2023)
        saveGrenseAvTypeKommune(id = "9001", lokalid = lokalIdOfGrense, gyldigFra = jan1In2023, opphav = "Oslo")
        assertThat(grenseRepository.findAllByLokalid(lokalIdOfGrense.value).size).isEqualTo(2)

        val framtidigVersjonAvGrense = grenseService.hentGrense(Identifikator(lokalIdOfGrense, jan1In2023))
        assertThat(framtidigVersjonAvGrense).isNotNull
        assertThat(framtidigVersjonAvGrense!!.opphav).isEqualTo("Oslo")
    }

    private fun saveGrenseAvTypeKommune(id: String = "9000",
                                        lokalid: Lokalid = Lokalid("ac7e05cd-fb07-4277-ad95-63c32c1370d0"),
                                        gyldigFra: LocalDate = october1In2022,
                                        gyldigTil: LocalDate? = null,
                                        opphav: String = "Eidsvoll") {
        grenseRepository.save(GrenseBuilder(
            id = id,
            lokalId = lokalid,
            gyldigFra = gyldigFra,
            gyldigTil = gyldigTil,
            opphav = opphav
        ).buildKommunegrense())
    }

}
