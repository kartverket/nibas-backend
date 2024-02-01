package no.kartverket.nibas.api.v1.common.geojson

import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.extensions.geometry.grenseKoblingToKontekstEgenskaper
import java.time.LocalDate

class FeatureMapper {

    fun toKontekstEgenskaperForGrunnkretsGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).map {
            it.grenseKoblingToKontekstEgenskaper(
                lokalid = Lokalid(it.grunnkrets.lokalid),
                kommuneLokalid = Lokalid(it.grunnkrets.kommuneLokalid.lokalid),
                kretsNummer = it.grunnkrets.grunnkretsnummer,
                gyldighetsdato = it.grunnkrets.gyldighet.gyldigFra,
                kontekstType = KontekstEnhetType.GRUNNKRETS,
                version = it.grunnkrets.version
            )
        }
    }

    fun toKontekstEgenskaperForDelomraadeGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).map {
            it.grenseKoblingToKontekstEgenskaper(
                lokalid = Lokalid(it.grunnkrets.lokalid),
                kommuneLokalid = Lokalid(it.grunnkrets.kommuneLokalid.lokalid),
                kretsNummer = it.grunnkrets.grunnkretsnummer,
                gyldighetsdato = it.grunnkrets.gyldighet.gyldigFra,
                kontekstType = KontekstEnhetType.GRUNNKRETS,
                version = it.grunnkrets.version
            )
        }
    }

    fun toKontekstEgenskaperForStemmekretsGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeStemmekretsGrenseKoblinger(gyldighetsdato).map {
            it.grenseKoblingToKontekstEgenskaper(
                lokalid = Lokalid(it.stemmekrets.lokalid),
                kommuneLokalid = Lokalid(it.stemmekrets.kommuneLokalid.lokalid),
                kretsNummer = it.stemmekrets.stemmekretsnummer,
                gyldighetsdato = it.stemmekrets.gyldighet.gyldigFra,
                kontekstType = KontekstEnhetType.STEMMEKRETS,
                version = it.stemmekrets.version
            )
        }
    }

    // Når vi får delt geometri for administrative grenser så må denne returnere både kontekster for grunnkretsene og stemmekretsene
    fun toKontekstEgenskaperForGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).map {
            it.grenseKoblingToKontekstEgenskaper(
                lokalid = Lokalid(it.grunnkrets.lokalid),
                kommuneLokalid = Lokalid(it.grunnkrets.kommuneLokalid.lokalid),
                kretsNummer = it.grunnkrets.grunnkretsnummer,
                gyldighetsdato = it.grunnkrets.gyldighet.gyldigFra,
                kontekstType = KontekstEnhetType.GRUNNKRETS,
                version = it.grunnkrets.version
            )
        }
    }
}
