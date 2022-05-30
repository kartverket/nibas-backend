package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.sosi.common.SosiKoder
import org.locationtech.jts.geom.Coordinate
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

open class SosiGeometri(type: String, niva: Int, verdier: MutableList<SosiVerdi>) :
    SosiObjekt(type = type, niva, verdier = verdier) {

    fun getId(): String? = verdier.getOrNull(0)?.verdi?.replace(":", "")
    fun getObjType(): String? = getVerdiAvBarn(SosiKoder.OBJTYPE)
    fun getNoyaktighetsklasse(): String? = getVerdiAvBarn(SosiKoder.NOYAKTIGHETSKLASSE)
    fun getOmtvistet(): Boolean = when (getVerdiAvBarn(SosiKoder.OMTVISTET)) {
        "JA" -> true
        else -> false
    }

    fun getOpphav(): String? = getVerdiAvBarn(SosiKoder.OPPHAV)
    fun getDataUttaksdato(): LocalDate? = stringAsDate(getVerdiAvBarn(SosiKoder.DATAUTTAKSDATO))
    fun getOppdateringsdato(): LocalDate? = stringAsDate(getVerdiAvBarn(SosiKoder.OPPDATERINGSDATO))
    fun getAlleSosiNordOest(): List<SosiNordOest> = barn.filter { it is SosiNordOest }.map { it as SosiNordOest }
    fun getReferanser(): List<SosiReferanse> = barn.filter { it is SosiReferanse }.map { it as SosiReferanse }
    fun getIdent(): SosiIdent? = barn.find { it is SosiIdent } as? SosiIdent
    fun getKommunenavn(): List<SosiNavn> =
        barn.filter { it is SosiNavn && it.type == SosiKoder.KOMMUNENAVN }.map { it as SosiNavn }

    fun getFylkesnummer(): String? = getVerdiAvBarn(SosiKoder.FYLKESNUMMER)
    fun getFylkesnavn(): List<SosiNavn> =
        barn.filter { it is SosiNavn && it.type == SosiKoder.FYLKESNAVN }.map { it as SosiNavn }

    fun getNasjonnavn(): List<SosiNavn> =
        barn.filter { it is SosiNavn && it.type == SosiKoder.NASJONNAVN }.map { it as SosiNavn }

    fun getFastsettingstype(): String? = getVerdiAvBarn(SosiKoder.FASTSETTINGSTYPE)
    fun getGyldigFra(): LocalDate? = stringAsDate(getVerdiAvBarn(SosiKoder.GYLDIGFRA))
    fun getGyldigTil(): LocalDate? = stringAsDate(getVerdiAvBarn(SosiKoder.GYLDIGTIL))
    fun getSluttdato(): LocalDate? = stringAsDate(getVerdiAvBarn(SosiKoder.SLUTTDATO))
    fun getDatafangstDato(): LocalDate? = stringAsDate(getVerdiAvBarn(SosiKoder.DATAFANGSTDATO))
    fun getGrensestatus(): String? = getVerdiAvBarn(SosiKoder.GRENSESTATUS)
    fun getFolgerTerrengdetalj(): String? = getVerdiAvBarn(SosiKoder.FOLGERTERRENGDETALJ)
    fun getKvalitet(): SosiKvalitet? = barn.find { it is SosiKvalitet } as? SosiKvalitet
    fun getEndringsflagg(): SosiEndringsflagg? = barn.find {it is SosiEndringsflagg} as? SosiEndringsflagg
    fun getKopidata(): SosiKopidata? = barn.find {it is SosiKopidata} as? SosiKopidata
    fun getGrunnkretsId(): SosiGrunnkretsId? = barn.find {it is SosiGrunnkretsId} as? SosiGrunnkretsId

    fun getRegistreringsversjon(): SosiRegistreringsversjon? = barn.find {it is SosiRegistreringsversjon} as? SosiRegistreringsversjon
    fun getLandkode(): String? = getVerdiAvBarn(SosiKoder.LANDKODE)
    fun getLandkoder(): List<String> = getVerdierAvBarn(SosiKoder.LANDKODE)
    fun getDokumentasjonsReferanse(): List<SosiDokumentasjonsReferanse> =
        barn.filter { it is SosiDokumentasjonsReferanse }.map { it as SosiDokumentasjonsReferanse }

    fun getInformasjon(): String? = getVerdiAvBarn(SosiKoder.INFORMASJON)
    fun getKommunenumre(): List<String> = getVerdierAvBarn(SosiKoder.KOMMUNENUMMER)
    fun getKommunenummer(): String? = getVerdiAvBarn(SosiKoder.KOMMUNENUMMER)
    fun getStemmekretsId(): SosiStemmekretsId? = barn.find { it is SosiStemmekretsId } as? SosiStemmekretsId

    fun getPosisjonskvalitet(kodelisteService: KodelisteService): PosisjonskvalitetEmbeddable? {
        val sosiKvalitet = this.getKvalitet()
        if (sosiKvalitet != null) {
            return PosisjonskvalitetEmbeddable(
                //Hvis kvalitet er satt må målemetode være angitt
                maalemetode = when (val maalemetode = sosiKvalitet.getMalemetode()) {
                    null -> throw RuntimeException("Maalemetode er påkrevd")
                    else -> kodelisteService.getMalemetodeIdByCodeValue(maalemetode)
                },
                noeyaktighet = sosiKvalitet.getNoyaktighet()
            )
        }
        return null
    }

    fun startKoordinat(retningMedKlokken: Boolean): Coordinate {
        return when(retningMedKlokken) {
            true -> {
                val forsteKoordinat = getAlleSosiNordOest().first()
                return forsteKoordinat.getKoordinater().first().toCoordinate(forsteKoordinat.enhet)
            }
            false -> sluttKoordinat(true)
        }
    }

    fun sluttKoordinat(retningMedKlokken: Boolean): Coordinate {
        return when(retningMedKlokken) {
            true -> {
                val sisteKoordinat = getAlleSosiNordOest().last()
                return sisteKoordinat.getKoordinater().last().toCoordinate(sisteKoordinat.enhet)
            }
            false -> startKoordinat(true)
        }
    }
}
