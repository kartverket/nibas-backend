package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.model.SosiFlate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class AdministrativEnhetImporter {

    @Autowired
    lateinit var landKodeRepository: LandKodeRepository

    fun importer(flate: SosiFlate): AdministrativEnhet {
        val administrativEnhet = when(flate.getObjType()) {
            SosiKoder.KOMMUNE -> Kommune()
            SosiKoder.FYLKE -> Fylke()
            SosiKoder.NASJON -> Nasjon()
            else -> throw RuntimeException("Ukjent administrativ enhet")
        }

        administrativEnhet.lokalid = flate.getIdent()?.getLokalId()
        administrativEnhet.navnerom = flate.getIdent()?.getNavnerom()
        administrativEnhet.versjonid = flate.getIdent()?.getVersjonId()
        administrativEnhet.oppdateringsdato = flate.getOppdateringsdato()
        administrativEnhet.datauttaksdato = flate.getDataUttaksdato()
        administrativEnhet.opphav = flate.getOpphav()
        administrativEnhet.gyldigfra = flate.getGyldigFra()
        administrativEnhet.gyldigtil = flate.getGyldigTil()
        administrativEnhet.datafangstdato = flate.getDatafangstDato()
        administrativEnhet.sluttdato = flate.getSluttdato()
        administrativEnhet.datauttaksdato = flate.getDataUttaksdato()
        administrativEnhet.land = flate.getLandkode()?.let { landKodeRepository.findByIdOrNull(it) }
        administrativEnhet.posisjon =
            SosiGeometryFunctions.sosiKoordinatToPoint(flate.getAlleSosiNordOest(), flate.koordsys)

        return administrativEnhet
    }


}