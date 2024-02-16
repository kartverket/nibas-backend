package no.kartverket.nibas.api.v1.common.geojson.metadata

import no.kartverket.nibas.api.v1.KODELISTE_BASE_URL
import no.kartverket.nibas.api.v1.MAALEMETODE_KODE_PATH
import no.kartverket.nibas.domain.CommonMetadataProperties
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.sporing.toSporingsinformasjon
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Hjelpeklasse/mappinglogikk grense/flate -> metadata.
 * Skilte ut egen klasse da extension-funksjonene ble avhengig av injectede avhengigheter.
 */
class MetadataMapper(private val kodelisteHrefProvider: KodelisteHrefProviderI = KodelisteHrefProvider()) {

    fun toMetadata(flate: Flate): FlateMetadata = FlateMetadata(
        common = flate.getCommonMetadata()
    )

    fun toMetadataForAdministrativGrense(grense: Grense): AdministrativGrenseMetadata = AdministrativGrenseMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
        dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
    )

    fun toMetadataForRiksgrense(grense: Grense): RiksgrenseMetadata = RiksgrenseMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
        dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
    )

    fun toMetadataForAvtaltAvgrensningslinje(grense: Grense): AvtaltAvgrensningslinjeMetadata = AvtaltAvgrensningslinjeMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
        dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
    )

    fun toMetadataForTerritorialgrense(grense: Grense): TerritorialgrenseMetadata = TerritorialgrenseMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
        dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
    )

    fun toMetadataForGrunnlinje(grense: Grense): GrunnlinjeMetadata {
        val grunnlinjeMetadata = GrunnlinjeMetadata(
            common = grense.getCommonMetadata(),
            commonGrense = getCommonGrenseMetadata(grense),
            dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
        )
        return grunnlinjeMetadata
    }

    fun toMetadataForStatistiskGrense(grense: Grense): StatistiskgrenseMetadata = StatistiskgrenseMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
    )

    fun toMetadataForKommunalKretsgrense(grense: Grense): KommunalKretsgrenseMetadata = KommunalKretsgrenseMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
    )

    private fun getCommonGrenseMetadata(grense: Grense): CommonGrenseMetadata = CommonGrenseMetadata(
        posisjonskvalitet = grense.kvalitet?.toKvalitet(kodelisteHrefProvider),
    )

    private fun CommonMetadataProperties.getCommonMetadata(): CommonMetadata = CommonMetadata(
        identifikasjon = Identifikasjon(this.lokalid),
        datafangstdato = this.datafangstdato,
        gyldigFra = this.gyldighet.gyldigFra,
        gyldigTil = this.gyldighet.gyldigTil,
        informasjon = this.getInfo(),
        sporingsinformasjon = this.auditInfo.toSporingsinformasjon(),
        opphav = this.opphav,
    )
}

/**
 * Interface virker unødvendig, men alternativet er å åpne klassen som heller ikke er ønskelig. (Mockito og final class)
 */
interface KodelisteHrefProviderI {
    fun maalemetodeKodeHref(): String
}

class KodelisteHrefProvider : KodelisteHrefProviderI {
    override fun maalemetodeKodeHref(): String = hrefWithSubPath(MAALEMETODE_KODE_PATH)

    private fun hrefWithSubPath(subPath: String) = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path(KODELISTE_BASE_URL)
        .path(subPath)
        .toUriString()

}
