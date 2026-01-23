package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.builders.BopliktomraadeBuilder
import no.kartverket.nibas.builders.GrenseBuilder
import no.kartverket.nibas.builders.GrunnkretsBuilder
import no.kartverket.nibas.builders.StemmekretsBuilder
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.EnumSet

class KobletTilGrenseTest {
    @Test
    fun testAddGrensekoblingGrunnkrets() {
        testAddGrensekobling(Grunnkrets.tillatteGrensetyper()) {
            GrunnkretsBuilder.DEFAULT_GRUNNKRETS
        }
    }

    @Test
    fun testAddGrensekoblingStemmekrets() {
        testAddGrensekobling(Stemmekrets.tillatteGrensetyper()) {
            StemmekretsBuilder.DEFAULT_STEMMEKRETS
        }
    }

    @Test
    fun testAddGrensekoblingBopliktomraade() {
        testAddGrensekobling(Bopliktomraade.tillatteGrensetyper()) {
            BopliktomraadeBuilder.DEFAULT_BOPLIKTOMRAADE
        }
    }

    companion object {
        private fun testAddGrensekobling(tillatteGrensetyper: Set<Grensetype>, createKrets: () -> KobletTilGrense) {
            val ikkeTillatteGrensetyper = EnumSet.complementOf(EnumSet.copyOf(tillatteGrensetyper))
            assertThatCollection(ikkeTillatteGrensetyper).allSatisfy { grensetype ->
                val krets: KobletTilGrense = createKrets()
                assertThatThrownBy { krets.addGrenseWithType(grensetype) }
                    .hasMessageMatching(".*${grensetype.name}.*${krets::class.simpleName}.*")
                    .isInstanceOf(IllegalArgumentException::class.java)
            }
            assertThatCollection(tillatteGrensetyper).allSatisfy { grensetype ->
                val krets: KobletTilGrense = createKrets()
                assertDoesNotThrow { krets.addGrenseWithType(grensetype) }
            }
        }

        private fun KobletTilGrense.addGrenseWithType(grensetype: Grensetype) {
            addGrensekobling(GrenseBuilder().withGrensetype(grensetype).build())
        }
    }

}
