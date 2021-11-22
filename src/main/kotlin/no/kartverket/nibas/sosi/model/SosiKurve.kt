package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.*
import java.time.LocalDate

class SosiKurve(niva: Int, verdier: MutableList<SosiVerdi>) : SosiGeometri(type = "KURVE", niva, verdier = verdier) {
    fun getFolgerTerrengdetalj(): String? = getVerdiAvBarn(SosiKoder.FOLGERTERRENGDETALJ)
    fun getGyldigFra(): LocalDate? = LocalDate.parse(getVerdiAvBarn(SosiKoder.GYLDIGFRA), SOSI_DATOFORMAT)
    fun getDatafangstDato(): LocalDate? = LocalDate.parse(getVerdiAvBarn(SosiKoder.DATAFANGSTDATO), SOSI_DATOFORMAT)
    fun getDokumentasjonsReferanse(): SosiDokumentasjonsReferanse? = barn.firstOrNull { it is SosiDokumentasjonsReferanse } as? SosiDokumentasjonsReferanse
    fun getKvalitet(): SosiKvalitet? = barn.find { it is SosiKvalitet } as? SosiKvalitet
    fun getLandkode(): String? = getVerdiAvBarn(SosiKoder.LANDKODE)
    fun getGrensestatus(): String? = getVerdiAvBarn(SosiKoder.GRENSESTATUS)
    fun getFastsettingstype(): String? = getVerdiAvBarn(SosiKoder.FASTSETTINGSTYPE)
    fun getInformasjon(): String? = getVerdiAvBarn(SosiKoder.INFORMASJON)

}
