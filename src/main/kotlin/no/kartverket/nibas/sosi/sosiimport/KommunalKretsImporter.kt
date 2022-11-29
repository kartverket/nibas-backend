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
class KommunalKretsImporter {
    fun importer(flate: SosiFlate): KommunalKrets {
        val lokalid: String? = flate.getIdent()?.getValidLokalId()
        val krets = when (Flatetype.valueOf(flate.getObjType()?.uppercase()!!)) {
            Flatetype.STEMMEKRETS -> if (lokalid != null) Stemmekrets(lokalid = lokalid) else Stemmekrets()
            else -> throw RuntimeException("Grensetype (${flate.getObjType()}) er ikke støttet.")
        }

        krets.datafangstdato = flate.getDatafangstDato()
        krets.gyldighet = Gyldighet(flate.getGyldigFra(), flate.getGyldigTil())
        krets.setInfo(flate.getInformasjon())
        krets.opphav = flate.getOpphav()

        return krets
    }
}
