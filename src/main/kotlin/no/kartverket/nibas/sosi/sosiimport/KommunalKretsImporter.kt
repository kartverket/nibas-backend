package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.domain.KommunalKrets
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.repository.TypeendringRepository
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class KommunalKretsImporter @Autowired constructor(
    private val typeendringRepository: TypeendringRepository
) {

    fun importer(flate: SosiFlate): KommunalKrets {
        val lokalid: String? = flate.getIdent()?.getValidLokalId()
        val krets = when (Flatetype.valueOf(flate.getObjType()?.uppercase()!!)) {
            Flatetype.STEMMEKRETS -> if (lokalid != null) Stemmekrets(lokalid = lokalid) else Stemmekrets()
            else -> throw RuntimeException("Grensetype (${flate.getObjType()}) er ikke støttet.")
        }

        krets.datafangstdato = flate.getDatafangstDato()
        krets.endringsflagg?.tidspunktendring = flate.getEndringsflagg()?.getTidspunktEndring()
        krets.endringsflagg?.typeendring = when (val typeendring = flate.getEndringsflagg()?.getTypeEndring()) {
            null -> null
            else -> typeendringRepository.findByIdOrNull(typeendring)
        }

        krets.gyldighet = Gyldighet(flate.getGyldigFra(), flate.getGyldigTil())
        krets.informasjon = flate.getInformasjon()
        krets.kopidata?.kopidato = flate.getKopidata()?.getKopidato()
        krets.kopidata?.omraadeid = flate.getKopidata()?.getOmraadeId()
        krets.kopidata?.originaldatavert = flate.getKopidata()?.getOriginalDatavert()
        krets.opphav = flate.getOpphav()
        krets.registreringsversjon?.produkt = flate.getRegistreringsversjon()?.getProdukt()
        krets.registreringsversjon?.versjon = flate.getRegistreringsversjon()?.getVersjon()
        krets.sluttdato = flate.getSluttdato()
        krets.informasjon = flate.getInformasjon()

        return krets
    }
}
