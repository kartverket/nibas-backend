package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.domain.updateSosiIdentifikasjon
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.stereotype.Component

@Component
class AdministrativEnhetImporter {

    fun importer(flate: SosiFlate): AdministrativEnhet {
        val administrativEnhet = when (flate.getObjType()) {
            SosiKoder.KOMMUNE -> Kommune()
            SosiKoder.FYLKE -> Fylke()
            SosiKoder.NASJON -> Nasjon()
            else -> throw RuntimeException("Ukjent administrativ enhet")
        }

        administrativEnhet.updateSosiIdentifikasjon(flate.getIdent())
        administrativEnhet.opphav = flate.getOpphav()
        administrativEnhet.gyldighet = Gyldighet(flate.getGyldigFra(), flate.getGyldigTil())
        administrativEnhet.datafangstdato = flate.getDatafangstDato()
        administrativEnhet.posisjon =
            SosiGeometryFunctions.sosiKoordinatToPoint(flate.getAlleSosiNordOest(), flate.koordsys)

        return administrativEnhet
    }

}
