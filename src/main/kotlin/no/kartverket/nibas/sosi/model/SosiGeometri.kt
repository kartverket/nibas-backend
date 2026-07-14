package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.domain.PosisjonskvalitetEmbeddable
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.sosi.common.ImportUtils
import no.kartverket.nibas.sosi.common.SosiKoder
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineString
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.collections.find

open class SosiGeometri(type: String, niva: Int, verdier: MutableList<SosiVerdi>) :
    SosiObjekt(type = type, niva, verdier = verdier) {

    lateinit var splittetGrense: LineString
    var erRetningMedKlokken: Boolean = true

    fun getId(): String? = verdier.getOrNull(0)?.verdi?.replace(":", "")
    fun getObjType(): String? = getVerdiAvBarn(SosiKoder.OBJTYPE)
    fun getNoyaktighetsklasse(): String? = getVerdiAvBarn(SosiKoder.NOYAKTIGHETSKLASSE)
    fun getOmtvistet(): Boolean = when (getVerdiAvBarn(SosiKoder.OMTVISTET)) {
        "JA" -> true
        else -> false
    }
    fun getAvgrensingstype(): String? = getVerdiAvBarn(SosiKoder.AVGRENSNINGSTYPE)

    fun getOpphav(): String? = getVerdiAvBarn(SosiKoder.OPPHAV)
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
    fun getGyldigFra(): LocalDate = stringAsDate(getVerdiAvBarn(SosiKoder.GYLDIGFRA)) ?: ImportUtils.importTime
    fun getGyldigTil(): LocalDate? = stringAsDate(getVerdiAvBarn(SosiKoder.GYLDIGTIL))
    fun getDatafangstDato(): ZonedDateTime? = stringAsZonedDateTime(getVerdiAvBarn(SosiKoder.DATAFANGSTDATO))
    fun getGrensestatus(): String? = getVerdiAvBarn(SosiKoder.GRENSESTATUS)
    fun getFolgerTerrengdetalj(): String? = getVerdiAvBarn(SosiKoder.FOLGERTERRENGDETALJ)
    fun getKvalitet(): SosiKvalitet? = barn.find { it is SosiKvalitet } as? SosiKvalitet
    fun getGrunnkretsId(): SosiGrunnkretsId? = barn.find {it is SosiGrunnkretsId} as? SosiGrunnkretsId

    fun getRegistreringsversjon(): SosiRegistreringsversjon? = barn.find {it is SosiRegistreringsversjon} as? SosiRegistreringsversjon
    fun getLandkode(): String? = getVerdiAvBarn(SosiKoder.LANDKODE)
    fun getLandkoder(): List<String> = getVerdierAvBarn(SosiKoder.LANDKODE)
    fun getDokumentasjonsReferanse(): List<SosiDokumentasjonsReferanse> =
        barn.filter { it is SosiDokumentasjonsReferanse }.map { it as SosiDokumentasjonsReferanse }

    fun getInformasjon(): String? = getVerdiAvBarn(SosiKoder.INFORMASJON)
    fun getKommunenumre(): List<String> = getVerdierAvBarn(SosiKoder.KOMMUNENUMMER)
    fun getKommunenummer(): String = getVerdiAvBarn(SosiKoder.KOMMUNENUMMER)!!
    fun getStemmekretsId(): SosiStemmekretsId? = barn.find { it is SosiStemmekretsId } as? SosiStemmekretsId

    fun getPosisjonskvalitet(kodelisteService: KodelisteService, sosiVersjon: String): PosisjonskvalitetEmbeddable {
        val sosiKvalitet = this.getKvalitet()
        var maalemetode: String? = null
        var noyaktighet: Int? = null
        if(sosiVersjon == "5.0"){
            val data = sosiKvalitet?.getMalemetode()?.split(" ")
            maalemetode = data?.get(0)
            noyaktighet = data?.get(1)?.toInt()
        } else{
            maalemetode = sosiKvalitet?.getMalemetode()
            noyaktighet = sosiKvalitet?.getNoyaktighet()
        }
        if (sosiKvalitet != null) {
            return PosisjonskvalitetEmbeddable(
                //Hvis kvalitet er satt må målemetode være angitt
                maalemetode = when (maalemetode) {
                    null -> throw RuntimeException("Maalemetode er påkrevd")
                    else -> kodelisteService.getMalemetodeIdByCodeValue(maalemetode)
                },
                noeyaktighet = noyaktighet
            )
        }
        return PosisjonskvalitetEmbeddable()
    }

    fun startKoordinat(erRetningMedKlokken: Boolean): Coordinate {
        return when(erRetningMedKlokken) {
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
