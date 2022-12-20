package no.kartverket.nibas.domain

import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.common.Identifikator
import org.locationtech.jts.geom.LineString
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Embeddable


interface GrenseKobling {
    var grenseKoblingAttributter: GrenseKoblingAttributter
    var grense: Grense
    fun oppdaterAttributter(nyeAttributter: GrenseKoblingAttributter)
    fun getEnhetKobletTilGrense(): KobletTilGrense

    fun getRelevantGrense(gyldighetsdato: LocalDate = LocalDate.now()): Grense {
        return grense.finnRelevantGrenseForDato(gyldighetsdato)
            ?: throw RelevantGrenseNotFoundEx("Fant ikke relevant grense for gyldighetsdato $gyldighetsdato og basert på grense i grensekoblng med lokalid ${grense.lokalid}")
    }

    class RelevantGrenseNotFoundEx(msg: String) : RuntimeException(msg)
}

fun GrenseKobling.getGrenseJustertForRetning(): LineString {
    val lineString = this.getRelevantGrense().grense
    return when (this.grenseKoblingAttributter.retningMedKlokken) {
        true -> lineString
        false -> lineString.reverse()
    }
}

@Embeddable
data class GrenseKoblingAttributter(
    @Column(name = "retning_med_klokken", nullable = false)
    var retningMedKlokken: Boolean = true,

    @Column(name = "rekkefoelge", nullable = false)
    var rekkefoelge: Int = 0,

    @Column(name = "flate_indeks", nullable = false)
    var flateIndeks: Int = 0,

    @Column(name = "hull_indeks", nullable = true)
    var hullIndeks: Int? = null

) {
    fun oppdater(grenseKoblingAttributter: GrenseKoblingAttributter): GrenseKoblingAttributter {
        return this.copy(
            flateIndeks = grenseKoblingAttributter.flateIndeks,
            retningMedKlokken = grenseKoblingAttributter.retningMedKlokken,
            rekkefoelge = grenseKoblingAttributter.rekkefoelge,
            hullIndeks = grenseKoblingAttributter.hullIndeks
        )
    }
}

class GrenseMedAttributter(val grense: Grense, val attributter: GrenseKoblingAttributter)

data class GrenseMedRetningOgKontekst(
    val grenseMedAttributter: GrenseMedAttributter,
    val kontekstType: KontekstEnhetType,
    val kontekstId: Identifikator
)

