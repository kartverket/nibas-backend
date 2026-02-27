package no.kartverket.nibas.domain

import no.kartverket.nibas.domain.Bopliktomraade.Companion.tillatteGrensetyper
import no.kartverket.nibas.domain.BopliktomraadeGrenser.Companion.DELVIS_BOPLIKT
import no.kartverket.nibas.domain.BopliktomraadeGrenser.Companion.FULL_BOPLIKT
import org.hibernate.annotations.DiscriminatorFormula
import java.time.LocalDate
import jakarta.persistence.CascadeType
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Embeddable
import jakarta.persistence.OneToMany

@Suppress("JpaObjectClassSignatureInspection")
@Embeddable
@DiscriminatorFormula("""
    CASE 
        WHEN omraade IS NULL THEN '$FULL_BOPLIKT' 
        ELSE '$DELVIS_BOPLIKT' 
    END
    """)
sealed class BopliktomraadeGrenser() {
    @Embeddable
    @DiscriminatorValue(DELVIS_BOPLIKT)
    class DelvisBoplikt : BopliktomraadeGrenser() {
        @Suppress("SENSELESS_NULL_IN_WHEN") // Hibernate...
        @OneToMany(mappedBy = "bopliktomraade", cascade = [CascadeType.ALL])
        var grenser: MutableSet<BopliktomraadeGrenseKobling> = hashSetOf()
            get() = when(val value = field) {
                null -> hashSetOf<BopliktomraadeGrenseKobling>().also { field = it }
                else -> value
            }

        fun getGrensekoblinger(gyldighetsdato: LocalDate): Set<BopliktomraadeGrenseKobling> {
            return grenser.filter { it.grense.gyldigPaaDato(gyldighetsdato) }.toSet()
        }

        fun addGrensekobling(omraade: Bopliktomraade, grense: Grense) {
            require(tillatteGrensetyper().contains(grense.grensetype)) {
                "Kan ikke legge til ${grense.grensetype.name} på Bopliktomraade"
            }
            val bopliktomraadeGrenseKobling = BopliktomraadeGrenseKobling(
                bopliktomraade = omraade,
                grense = grense
            )
            grenser.add(bopliktomraadeGrenseKobling)
            grense.bopliktomraader.add(bopliktomraadeGrenseKobling)
        }

        fun removeGrensekobling(omraade: Bopliktomraade, gammelGrense: Grense) {
            if (grenser.removeIf { it.grense.id == gammelGrense.id }) {
                gammelGrense.bopliktomraader.removeIf { it.bopliktomraade.id == omraade.id }
            }
        }

        fun replaceGrensekobling(omraade: Bopliktomraade, gammelGrense: Grense, grense: Grense?) {
            require(grense == null || tillatteGrensetyper().contains(grense.grensetype)) {
                "Kan ikke oppdatere/legge til ${grense?.grensetype?.name} på Bopliktomraade"
            }
            if (grenser.removeIf { it.grense.id == gammelGrense.id }) {
                gammelGrense.bopliktomraader.removeIf { it.bopliktomraade.id == omraade.id }
                // Only add if grense is not null
                if (grense != null) {
                    addGrensekobling(omraade, grense)
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DelvisBoplikt

            return grenser == other.grenser
        }

        override fun hashCode(): Int {
            return grenser.hashCode()
        }


    }

    @Embeddable
    @DiscriminatorValue(FULL_BOPLIKT)
    class FullBoplikt : BopliktomraadeGrenser() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null) return false
            return javaClass == other.javaClass
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }


    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    companion object {
        private const val DELVIS_BOPLIKT = "DELVIS"
        private const val FULL_BOPLIKT = "FULL"
    }
}
