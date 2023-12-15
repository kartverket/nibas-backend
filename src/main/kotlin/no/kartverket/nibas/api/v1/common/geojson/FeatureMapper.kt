package no.kartverket.nibas.api.v1.common.geojson

import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.extensions.geometry.grenseKoblingToKontekstEgenskaper

class FeatureMapper {

    fun toKontekstEgenskaperForGrunnkretsGrense(grense: Grense): List<KontekstEgenskaper> {
        return grense.grunnkretser.map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.grunnkrets.lokalid), it.grunnkrets.gyldighet.gyldigFra, KontekstEnhetType.GRUNNKRETS, it.grunnkrets.version)
        }
    }

    fun toKontekstEgenskaperForDelomraadeGrense(grense: Grense): List<KontekstEgenskaper> {
        return grense.grunnkretser.map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.grunnkrets.lokalid), it.grunnkrets.gyldighet.gyldigFra, KontekstEnhetType.GRUNNKRETS, it.grunnkrets.version)
        }
    }

    fun toKontekstEgenskaperForStemmekretsGrense(grense: Grense): List<KontekstEgenskaper> {
        return grense.stemmekretser.map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.stemmekrets.lokalid), it.stemmekrets.gyldighet.gyldigFra, KontekstEnhetType.STEMMEKRETS, it.stemmekrets.version)
        }
    }

    fun toKontekstEgenskaperForGrense(grense: Grense): List<KontekstEgenskaper> {
        return grense.grunnkretser.map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.grunnkrets.lokalid), it.grunnkrets.gyldighet.gyldigFra, KontekstEnhetType.GRUNNKRETS, it.grunnkrets.version)
        }.plus(grense.stemmekretser.map {
            it.grenseKoblingToKontekstEgenskaper(Lokalid(it.stemmekrets.lokalid), it.stemmekrets.gyldighet.gyldigFra, KontekstEnhetType.STEMMEKRETS, it.stemmekrets.version)
        })
    }
}
