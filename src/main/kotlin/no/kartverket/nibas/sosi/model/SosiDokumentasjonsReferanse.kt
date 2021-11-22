package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.SosiKoder
import java.time.LocalDate


class SosiDokumentasjonsReferanse(niva: Int, verdier: MutableList<SosiVerdi>) : SosiGeometri(type = "DOKUMENTASJONSREFERANSE", niva, verdier = verdier) {
    fun getRettskildeId(): String? = getVerdiAvBarn(SosiKoder.RETTSKILDEID)
    fun getRettskildeTittel(): String? = getVerdiAvBarn(SosiKoder.RETTSKILDETITTEL)
    fun getDokumentLink(): String? = getVerdiAvBarn(SosiKoder.DOKUMENTLINK)
    fun getFastsettingsdato(): LocalDate? = LocalDate.parse(getVerdiAvBarn(SosiKoder.FASTSETTINGSDATO), SOSI_DATOFORMAT)
}
