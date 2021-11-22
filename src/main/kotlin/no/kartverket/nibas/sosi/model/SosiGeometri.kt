package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.*
import java.time.LocalDate

open class SosiGeometri(type: String, niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = type, niva, verdier = verdier) {
    fun getId(): String? = verdier.getOrNull(0)?.verdi?.replace(":", "")
    fun getObjType(): String? = getVerdiAvBarn(SosiKoder.OBJTYPE)
    fun getNoyaktighetsklasse(): String? = getVerdiAvBarn(SosiKoder.NOYAKTIGHETSKLASSE)
    fun getOmtvistet(): String? = getVerdiAvBarn(SosiKoder.OMTVISTET)
    fun getOpphav(): String? = getVerdiAvBarn(SosiKoder.OPPHAV)
    fun getDataUttaksdato(): LocalDate? = LocalDate.parse(getVerdiAvBarn(SosiKoder.DATAUTTAKSDATO), SOSI_DATOFORMAT)
    fun getOppdateringsdato(): LocalDate? = LocalDate.parse(getVerdiAvBarn(SosiKoder.OPPDATERINGSDATO), SOSI_DATOFORMAT)
    fun getSosiNo(): List<SosiNo> = barn.filter { it is SosiNo }.map { it as SosiNo }
    fun getSosiNoh(): List<SosiNoh> = barn.filter { it is SosiNoh }.map { it as SosiNoh }
    fun getRef(): List<SosiRef> = barn.filter { it is SosiRef }.map { it as SosiRef }
    fun getIdent(): SosiIdent? = barn.find { it is SosiIdent } as? SosiIdent
    fun getKommunenummer(): String? = getVerdiAvBarn(SosiKoder.KOMMUNENUMMER)
    fun getKommunenavn(): SosiNavn? = barn.find { it is SosiNavn && it.type == SosiKoder.KOMMUNENAVN } as? SosiNavn
    fun getFylkesnummer(): String? = getVerdiAvBarn(SosiKoder.FYLKESNUMMER)
    fun getFylkesnavn(): SosiNavn? = barn.find { it is SosiNavn && it.type == SosiKoder.FYLKESNAVN } as? SosiNavn
}