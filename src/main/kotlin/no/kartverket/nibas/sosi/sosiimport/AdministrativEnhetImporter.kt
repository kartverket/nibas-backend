package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.stereotype.Component

@Component
class AdministrativEnhetImporter {

    fun importer(flate: SosiFlate): AdministrativEnhet {
        val lokalid: String? = flate.getIdent()?.getValidLokalId()

        val administrativEnhet: AdministrativEnhet = when (flate.getObjType()) {
            SosiKoder.KOMMUNE -> if (lokalid != null) Kommune(lokalid = lokalid) else Kommune()
            SosiKoder.FYLKE -> if (lokalid != null) Fylke(lokalid =lokalid) else Fylke()
            SosiKoder.NASJON -> if (lokalid != null) Nasjon(lokalid = lokalid) else Nasjon()
            else -> throw RuntimeException("Ukjent administrativ enhet")
        }

        administrativEnhet.opphav = flate.getOpphav()
        administrativEnhet.gyldighet = Gyldighet(flate.getGyldigFra(), flate.getGyldigTil())
        administrativEnhet.datafangstdato = flate.getDatafangstDato()
        administrativEnhet.posisjon =
            SosiGeometryFunctions.sosiKoordinatToPoint(flate.getAlleSosiNordOest(), flate.koordsys)

        return administrativEnhet
    }

}
