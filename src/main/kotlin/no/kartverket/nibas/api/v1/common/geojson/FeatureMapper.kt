package no.kartverket.nibas.api.v1.common.geojson

import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.domain.Grense
import java.time.LocalDate

class FeatureMapper {

    fun toKontekstEgenskaperForGrunnkretsGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeGrunnkretsGrenseKoblinger(gyldighetsdato).map {
            lagKontekstEgenskaper(
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
        return toKontekstEgenskaperForGrunnkretsGrense(grense, gyldighetsdato)
    }

    fun toKontekstEgenskaperForStemmekretsGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeStemmekretsGrenseKoblinger(gyldighetsdato).map {
            lagKontekstEgenskaper(
                lokalid = Lokalid(it.stemmekrets.lokalid),
                kommuneLokalid = Lokalid(it.stemmekrets.kommuneLokalid.lokalid),
                kretsNummer = it.stemmekrets.stemmekretsnummer,
                gyldighetsdato = it.stemmekrets.gyldighet.gyldigFra,
                kontekstType = KontekstEnhetType.STEMMEKRETS,
                version = it.stemmekrets.version
            )
        }
    }

    fun toKontekstEgenskaperForGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return toKontekstEgenskaperForGrunnkretsGrense(grense, gyldighetsdato)
            .plus(toKontekstEgenskaperForStemmekretsGrense(grense, gyldighetsdato))
    }

    fun toKontekstEgenskaperForBopliktGrense(grense: Grense, gyldighetsdato: LocalDate = LocalDate.now()): List<KontekstEgenskaper> {
        return grense.gyldigeBopliktomraadeGrenseKoblinger(gyldighetsdato).map {
            lagKontekstEgenskaper(
                lokalid = Lokalid(it.bopliktomraade.lokalid),
                kommuneLokalid = Lokalid(it.bopliktomraade.kommuneLokalid.lokalid),
                gyldighetsdato = it.bopliktomraade.gyldighet.gyldigFra,
                kontekstType = KontekstEnhetType.BOPLIKTOMRAADE,
                version = it.bopliktomraade.version,
            )
        }
    }

    private fun lagKontekstEgenskaper(
        lokalid: Lokalid,
        kommuneLokalid: Lokalid,
        kretsNummer: String? = null,
        gyldighetsdato: LocalDate,
        kontekstType: KontekstEnhetType,
        version: Int,
    ): KontekstEgenskaper {
        return KontekstEgenskaper(
            id = ObjektIdentifikator(lokalid, gyldighetsdato),
            kommuneId = ObjektIdentifikator(kommuneLokalid, gyldighetsdato),
            kretsNummer = kretsNummer,
            type = kontekstType,
            version = version,
        )
    }
}
