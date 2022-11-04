package no.kartverket.nibas.sosi.sosiimport

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import no.kartverket.nibas.repository.TypeendringRepository
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sporing.Gyldighet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class KommunalKretsgrenseImporter @Autowired constructor(
    private val typeendringRepository: TypeendringRepository,
    private val kodelisteService: KodelisteService,
    private val terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
    private val noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
) {

    fun importer(skalImporteres: SplitTilImport): Grense {
        val sosiGeometri = skalImporteres.splitRef.basertPaGrense

        val grense = when (sosiGeometri.getObjType()) {
            SosiKoder.STEMMEKRETSGRENSE -> createGrense(sosiGeometri.getIdent(), Grensetype.Stemmekretsgrense)
            else -> throw RuntimeException("Ukjent grensetype (${sosiGeometri.getObjType()}) for kommunal kretsgrense")
        }

        grense.datafangstdato = sosiGeometri.getDatafangstDato()
        grense.datauttaksdato = sosiGeometri.getDataUttaksdato()
        grense.endringsflagg?.tidspunktendring = sosiGeometri.getEndringsflagg()?.getTidspunktEndring()
        grense.endringsflagg?.typeendring = when (val typeendring = sosiGeometri.getEndringsflagg()?.getTypeEndring()) {
            null -> null
            else -> typeendringRepository.findByIdOrNull(typeendring)
        }
        grense.gyldighet = Gyldighet(sosiGeometri.getGyldigFra(), sosiGeometri.getGyldigTil())
        grense.kopidata?.kopidato = sosiGeometri.getKopidata()?.getKopidato()
        grense.kopidata?.omraadeid = sosiGeometri.getKopidata()?.getOmraadeId()
        grense.kopidata?.originaldatavert = sosiGeometri.getKopidata()?.getOriginalDatavert()


        grense.kvalitet = sosiGeometri.getPosisjonskvalitet(kodelisteService)

        grense.opphav = sosiGeometri.getOpphav()

        grense.registreringsversjon?.produkt = sosiGeometri.getRegistreringsversjon()?.getProdukt()
        grense.registreringsversjon?.versjon = sosiGeometri.getRegistreringsversjon()?.getVersjon()

        grense.sluttdato = sosiGeometri.getSluttdato()

        grense.foelgerterrengdetalj = sosiGeometri.getFolgerTerrengdetalj()?.let {
            terrengdetaljkodeRepository.findByIdOrNull(it) ?: throw RuntimeException("Ukjent terrengdetalj")
        }
        grense.noeyaktighetsklasse = sosiGeometri.getNoyaktighetsklasse()?.let {
            noeyaktighetsklassekodeRepository.findByIdOrNull(it) ?: throw RuntimeException("Ukjent nøyaktighetsklasse")
        }

        grense.grense = skalImporteres.splitRef.splittetGrense

        return grense

    }
}

