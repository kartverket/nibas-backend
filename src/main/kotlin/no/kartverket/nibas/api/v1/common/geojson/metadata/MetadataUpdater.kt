package no.kartverket.nibas.api.v1.common.geojson.metadata

import no.kartverket.nibas.domain.CommonMetadataProperties
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import java.time.LocalDate

/**
 * Hjelpeklasse/mappinglogikk metadata -> grense.
 * Skilte ut egen klasse da extension-funksjonene ble avhengig av injectet repository.
 */
class MetadataUpdater() {

    fun updateAdministrativGrense(grense: Grense, metadata: AdministrativGrenseMetadata, gyldighetsdato: LocalDate, endringstype: Endringstype): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        grense.updateDokumentasjonsreferanser(metadata.dokumentasjonsreferanser, gyldighetsdato, endringstype)

        return grense
    }

    fun updateRiksgrense(grense: Grense, metadata: RiksgrenseMetadata, gyldighetsdato: LocalDate, endringstype: Endringstype): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        grense.updateDokumentasjonsreferanser(metadata.dokumentasjonsreferanser, gyldighetsdato, endringstype)
        return grense
    }

    fun updateStatistiskGrense(grense: Grense, metadata: StatistiskgrenseMetadata): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        return grense
    }

    fun updateKommunalKretsgrense(grense: Grense, metadata: KommunalKretsgrenseMetadata): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        return grense
    }

    fun updateAvtaltavgrensningslinje(grense: Grense, metadata: AvtaltAvgrensningslinjeMetadata, gyldighetsdato: LocalDate, endringstype: Endringstype): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        grense.updateDokumentasjonsreferanser(metadata.dokumentasjonsreferanser, gyldighetsdato, endringstype)
        return grense
    }

    fun updateTerritoritalgrense(grense: Grense, metadata: TerritorialgrenseMetadata, gyldighetsdato: LocalDate, endringstype: Endringstype): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        grense.updateDokumentasjonsreferanser(metadata.dokumentasjonsreferanser, gyldighetsdato, endringstype)
        return grense
    }

    /**
     * Identifikasjon ignoreres, dette skal ikke kunne oppdateres, burde egentlig ikke være med på tur inn.
     * Sporingsinformasjon ignoreres også, dette settes av interceptor.
     */
    private fun CommonMetadataProperties.updateWithCommonMetadata(common: CommonMetadata) {
        this.datafangstdato = common.datafangstdato
        this.setInfo(common.informasjon)
        this.opphav = common.opphav
    }

    private fun updateWithGrenseMetadata(grense: Grense, commonGrenseMetadata: CommonGrenseMetadata) {
        grense.kvalitet = commonGrenseMetadata.posisjonskvalitet?.toEmbeddable()
    }
}
