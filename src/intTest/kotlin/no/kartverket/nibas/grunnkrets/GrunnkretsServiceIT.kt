package no.kartverket.nibas.grunnkrets

import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.inndelinger.GrunnkretsService
import no.kartverket.nibas.testcontainer.TestWithDbContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Transactional
class GrunnkretsServiceIT : TestWithDbContainer() {

    @Autowired
    private lateinit var grunnkretsService: GrunnkretsService

    private val lokalidOfGrunnkrets = Lokalid("c54fb55e-15d0-4528-8b88-f58d6c74bd87")
    private val earlyDate = LocalDate.of(2000, 1, 1)

    @Test
    fun hentEksisterendeGrunnkretsMedDagensDato() {
        val grunnkrets = grunnkretsService.hentGrunnkrets(lokalidOfGrunnkrets, LocalDate.now())
        assertThat(grunnkrets).isNotNull
    }

    @Test
    fun hentEksisterendeGrunnkretsMedTidligereDato() {
        assertThat(grunnkretsService.hentGrunnkrets(lokalidOfGrunnkrets, earlyDate)).isNull()
    }

    @Test
    fun hentGrenserForEksisterendeGrunnkretsMedDagensDato() {
        val grunnkrets = grunnkretsService.hentGrunnkrets(lokalidOfGrunnkrets, LocalDate.now()) ?: throw RuntimeException("Grunnkrets finnes ikke")

        lagGrenserForGrunnkrets(grunnkrets)

        val grenserForGrunnkrets = grunnkretsService.hentGrenserForGrunnkrets(lokalidOfGrunnkrets, LocalDate.now())
        assertThat(grenserForGrunnkrets).isNotEmpty
    }

    @Test
    fun hentGrenserForEksisterendeGrunnkretsMedTidligereDato() {
        val grenserForGrunnkrets = grunnkretsService.hentGrenserForGrunnkrets(lokalidOfGrunnkrets, earlyDate)
        assertThat(grenserForGrunnkrets).isEmpty()
    }

    private fun lagGrenserForGrunnkrets(grunnkrets: Grunnkrets) {

        val x1 = 23857.0
        val y1 = 675635.0

        val y2 = 675635.0
        val x2 = 23867.0

        val x3 = 23862.0
        val y3 = 675645.0

        val grense1 = GrenseBuilder(UUID.randomUUID().toString())
            .withLokalId(UUID.randomUUID().toString())
            .withGrenseGeometri(listOf(Coordinate(x1, y1), Coordinate(x2, y2)))
            .buildGrunnkretsgrense()

        val grense2 = GrenseBuilder(UUID.randomUUID().toString())
            .withLokalId(UUID.randomUUID().toString())
            .withGrenseGeometri(listOf(Coordinate(x2, y2), Coordinate(x3, y3)))
            .buildGrunnkretsgrense()

        val grense3 = GrenseBuilder(UUID.randomUUID().toString())
            .withLokalId(UUID.randomUUID().toString())
            .withGrenseGeometri(listOf(Coordinate(x3, y3), Coordinate(x1, y1)))
            .buildGrunnkretsgrense()

        val grenseKoblingAttributter = GrenseKoblingAttributter(true, 1, 0, 0)
        grunnkrets.addGrensekobling(grense1, grenseKoblingAttributter)
        grunnkrets.addGrensekobling(grense2, grenseKoblingAttributter)
        grunnkrets.addGrensekobling(grense3, grenseKoblingAttributter)

        grenseRepository.save(grense1)
        grenseRepository.save(grense2)
        grenseRepository.save(grense3)

        grunnkretsRepository.save(grunnkrets)
    }
}
