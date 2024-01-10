package no.kartverket.nibas.api.v1.common.geojson

import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.extensions.geometry.grenseKoblingToKontekstEgenskaper
import java.time.LocalDate

class FeatureMapper {

    fun toKontekstEgenskaperForGrunnkretsGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.grunnkrets.lokalid), it.grunnkrets.gyldighet.gyldigFra,
                KontekstEnhetType.GRUNNKRETS, it.grunnkrets.version)
        }
    }

    fun toKontekstEgenskaperForDelomraadeGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.grunnkrets.lokalid), it.grunnkrets.gyldighet.gyldigFra,
                KontekstEnhetType.GRUNNKRETS, it.grunnkrets.version)
        }
    }

    fun toKontekstEgenskaperForStemmekretsGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeStemmekretsGrenseKoblinger(gyldighetsdato).map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.stemmekrets.lokalid), it.stemmekrets.gyldighet.gyldigFra,
                KontekstEnhetType.STEMMEKRETS, it.stemmekrets.version)
        }
    }

    fun toKontekstEgenskaperForGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.grunnkrets.lokalid), it.grunnkrets.gyldighet.gyldigFra,
                KontekstEnhetType.GRUNNKRETS, it.grunnkrets.version)
        }.plus(grense.gyldigeStemmekretsGrenseKoblinger(gyldighetsdato).map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.stemmekrets.lokalid), it.stemmekrets.gyldighet.gyldigFra,
                KontekstEnhetType.STEMMEKRETS, it.stemmekrets.version)
        })
    }
}
