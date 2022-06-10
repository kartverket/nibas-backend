package no.kartverket.nibas.api.v1.common.geojson.metadata

import no.kartverket.nibas.api.v1.FASTSETTINGSTYPE_PATH
import no.kartverket.nibas.api.v1.GRENSESTATUSKODE_PATH
import no.kartverket.nibas.api.v1.KODELISTE_BASE_URL
import no.kartverket.nibas.api.v1.LANDKODE_PATH
import no.kartverket.nibas.api.v1.MAALEMETODE_KODE_PATH
import no.kartverket.nibas.api.v1.NOEYAKTIGHETSKLASSEKODE_PATH
import no.kartverket.nibas.api.v1.TERRENGDETALJKODE_PATH
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.AdministrativGrense
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.CommonMetadataProperties
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnlinje
import no.kartverket.nibas.domain.MaritimGrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.StatistiskEnhet
import no.kartverket.nibas.domain.StatistiskGrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.inndelinger.createKommunenummerFromUUID
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.sporing.toSporingsinformasjon
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Hjelpeklasse/mappinglogikk grense/flate -> metadata.
 * Skilte ut egen klasse da extension-funksjonene ble avhengig av injectede avhengigheter.
 */
class MetadataMapper(private val kodelisteService: KodelisteService,
                     private val kodelisteHrefProvider: KodelisteHrefProviderI = KodelisteHrefProvider()) {

    fun toMetadata(flate: Flate): FlateMetadata = FlateMetadata(
        common = flate.getCommonMetadata()
    )

    fun toMetadata(grense: AdministrativGrense): AdministrativGrenseMetadata = AdministrativGrenseMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
        dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
        foelgerTerrengdetalj = KodelisteEntry(id = grense.foelgerterrengdetalj?.id, href = kodelisteHrefProvider.terrengDetaljHref()),
        noeyaktighetsklasse = KodelisteEntry(id = grense.noeyaktighetsklasse?.id, href = kodelisteHrefProvider.noeyaktighetsklasseHref()),
        omtvistet = grense.omtvistet
    )

    fun toMetadata(grense: Riksgrense): RiksgrenseMetadata = RiksgrenseMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
        dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
        foelgerTerrengdetalj = KodelisteEntry(id = grense.foelgerterrengdetalj?.id, href = kodelisteHrefProvider.terrengDetaljHref()),
        stedfestingVerifisert = grense.stedfestingverifisert,
        ansvarligeMyndigheter = grense.riksgrenseAnsvarligeMyndigheter
            .filter { it.ansvarligmyndighet != null }
            .map { TekstHolder(it.id, it.ansvarligmyndighet!!) },
        grensegateBredde = grense.grensegatebredde
    )

    fun toMetadata(grense: Avtaltavgrensningslinje): AvtaltAvgrensningslinjeMetadata = AvtaltAvgrensningslinjeMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
        dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
        maritimeGrenser = grense.getMaritimeMetadata()
    )

    fun toMetadata(grense: Territorialgrense): TerritorialgrenseMetadata = TerritorialgrenseMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
        dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
        maritimeGrenser = grense.getMaritimeMetadata(),
        beregningsdato = grense.beregningsdato
    )

    fun toMetadata(grense: Grunnlinje): GrunnlinjeMetadata {
        val grunnlinjeMetadata = GrunnlinjeMetadata(
            common = grense.getCommonMetadata(),
            commonGrense = getCommonGrenseMetadata(grense),
            dokumentasjonsreferanser = grense.dokumentasjonsreferanser.map { it.toDokRef() },
            maritimeGrenser = grense.getMaritimeMetadata()
        )
        val kommunenummerId = grense.kommunenummerId
        if (kommunenummerId != null) {
            grunnlinjeMetadata.kommunenummer = createKommunenummerFromUUID(kommunenummerId, kodelisteService)
        }
        return grunnlinjeMetadata
    }

    fun toMetadata(grense: StatistiskGrense): StatistiskgrenseMetadata = StatistiskgrenseMetadata(
        common = grense.getCommonMetadata(),
        commonGrense = getCommonGrenseMetadata(grense),
        foelgerTerrengdetalj = KodelisteEntry(id = grense.foelgerterrengdetalj?.id, href = kodelisteHrefProvider.terrengDetaljHref()),
        noeyaktighetsklasse = KodelisteEntry(id = grense.noeyaktighetsklasse?.id, href = kodelisteHrefProvider.noeyaktighetsklasseHref()),
    )

    private fun getCommonGrenseMetadata(grense: Grense): CommonGrenseMetadata = CommonGrenseMetadata(
        posisjonskvalitet = grense.kvalitet?.toKvalitet(kodelisteHrefProvider),
        fastsettingstype = KodelisteEntry(id = grense.fastsettingstype?.id, href = kodelisteHrefProvider.fastsettingstypeKodeHref()),
        grensestatus = KodelisteEntry(id = grense.grensestatus?.id, href = kodelisteHrefProvider.grensestatusKodeHref())
    )

    private fun CommonMetadataProperties.getCommonMetadata(): CommonMetadata = CommonMetadata(
        identifikasjon = Identifikasjon(this.lokalid, this.navnerom, this.versjonid),
        datafangstdato = this.datafangstdato,
        gyldigFra = this.gyldigFra,
        gyldigTil = this.gyldigTil,
        informasjon = this.getInfo(),
        sporingsinformasjon = this.auditInfo.toSporingsinformasjon(),
        opphav = this.opphav,
    )

    private fun MaritimGrense.getMaritimeMetadata(): MaritimeGrenserMetadata = MaritimeGrenserMetadata(
        ansvarligeMyndigheter = this.getAnsvarligeMyndigheter()
            .filter { it.ansvarligmyndighet != null }
            .map { TekstHolder(it.id, it.ansvarligmyndighet!!) },
        gjelderOmraade = this.gjelderomraade,
        transformasjonsdato = this.transformasjonsdato,
        virkeomraadenavn = this.virkeomraadenavn,
        land = Landkoder(
            ids = this.getLandIds().sorted(),
            href = kodelisteHrefProvider.landkodeHref())
    )

}

/**
 * Interface virker unødvendig, men alternativet er å åpne klassen som heller ikke er ønskelig. (Mockito og final class)
 */
interface KodelisteHrefProviderI {
    fun terrengDetaljHref(): String
    fun landkodeHref(): String
    fun noeyaktighetsklasseHref(): String
    fun maalemetodeKodeHref(): String
    fun fastsettingstypeKodeHref(): String
    fun grensestatusKodeHref(): String
}

class KodelisteHrefProvider : KodelisteHrefProviderI {
    override fun terrengDetaljHref(): String = hrefWithSubPath(TERRENGDETALJKODE_PATH)
    override fun landkodeHref(): String = hrefWithSubPath(LANDKODE_PATH)
    override fun noeyaktighetsklasseHref(): String = hrefWithSubPath(NOEYAKTIGHETSKLASSEKODE_PATH)
    override fun maalemetodeKodeHref(): String = hrefWithSubPath(MAALEMETODE_KODE_PATH)
    override fun fastsettingstypeKodeHref(): String = hrefWithSubPath(FASTSETTINGSTYPE_PATH)
    override fun grensestatusKodeHref(): String = hrefWithSubPath(GRENSESTATUSKODE_PATH)

    private fun hrefWithSubPath(subPath: String) = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path(KODELISTE_BASE_URL)
        .path(subPath)
        .toUriString()

}
