package no.kartverket.nibas.api.v1.common.geojson.metadata

import no.kartverket.nibas.domain.AnsvarligMyndighetHolder
import no.kartverket.nibas.domain.CommonMetadataProperties
import no.kartverket.nibas.domain.FoelgerGrunnkretsgrenseIdentifikasjon
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Landkode
import no.kartverket.nibas.domain.MaritimGrense
import no.kartverket.nibas.domain.Noeyaktighetsklassekode
import no.kartverket.nibas.domain.Terrengdetaljkode
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import no.kartverket.nibas.sporing.Gyldighet
import no.kartverket.nibas.unwrap

/**
 * Hjelpeklasse/mappinglogikk metadata -> grense.
 * Skilte ut egen klasse da extension-funksjonene ble avhengig av injectet repository.
 */
class MetadataUpdater(private val landKodeRepository: LandKodeRepository,
                      private val fastsettingstypeRepository: FastsettingstypeRepository,
                      private val terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
                      private val noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
                      private val grensestatusRepository: GrensestatusRepository) {

    fun updateAdministrativGrense(grense: Grense, metadata: AdministrativGrenseMetadata): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        grense.updateDokumentasjonsreferanser(metadata.dokumentasjonsreferanser)

        grense.foelgerterrengdetalj = hentTerrengDetaljKode(metadata.foelgerTerrengdetalj.id)
        grense.noeyaktighetsklasse = hentNoeyaktighetsKlasse(metadata.noeyaktighetsklasse.id)

        grense.omtvistet = metadata.omtvistet
        return grense
    }

    fun updateRiksgrense(grense: Grense, metadata: RiksgrenseMetadata): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        grense.updateDokumentasjonsreferanser(metadata.dokumentasjonsreferanser)

        grense.foelgerterrengdetalj = hentTerrengDetaljKode(metadata.foelgerTerrengdetalj.id)

        grense.stedfestingverifisert = metadata.stedfestingVerifisert
        grense.setAnsvarligeMyndigheter(toAnsvarligeMyndigheter(metadata.ansvarligeMyndigheter))
        grense.grensegatebredde = metadata.grensegateBredde
        return grense
    }

    fun updateStatistiskGrense(grense: Grense, metadata: StatistiskgrenseMetadata): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)

        val terrengdetaljKodeId = metadata.foelgerTerrengdetalj.id
        grense.foelgerterrengdetalj = hentTerrengDetaljKode(terrengdetaljKodeId)
        grense.noeyaktighetsklasse = hentNoeyaktighetsKlasse(metadata.noeyaktighetsklasse.id)

        return grense
    }

    fun updateKommunalKretsgrense(grense: Grense, metadata: KommunalKretsgrenseMetadata): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)

        grense.foelgerterrengdetalj = hentTerrengDetaljKode(metadata.foelgerTerrengdetalj.id)
        grense.noeyaktighetsklasse = hentNoeyaktighetsKlasse(metadata.noeyaktighetsklasse.id)
        metadata.foelgerGrunnkretsgrense?.let {
            grense.foelgerGrunnkretsgrenseIdentifikasjon = FoelgerGrunnkretsgrenseIdentifikasjon(it.lokalid)
        }

        return grense
    }

    fun updateAvtaltavgrensningslinje(grense: Grense, metadata: AvtaltAvgrensningslinjeMetadata): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        grense.updateDokumentasjonsreferanser(metadata.dokumentasjonsreferanser)
        grense.updateWithMaritimeMetadata(metadata.maritimeGrenser)
        return grense
    }

    fun updateTerritoritalgrense(grense: Grense, metadata: TerritorialgrenseMetadata): Grense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        grense.updateDokumentasjonsreferanser(metadata.dokumentasjonsreferanser)
        grense.updateWithMaritimeMetadata(metadata.maritimeGrenser)
        grense.beregningsdato = metadata.beregningsdato
        return grense
    }

    private fun MaritimGrense.updateWithMaritimeMetadata(maritimeGrenserMetadata: MaritimeGrenserMetadata) {
        this.setAnsvarligeMyndigheter(toAnsvarligeMyndigheter(maritimeGrenserMetadata.ansvarligeMyndigheter))
        this.gjelderomraade = maritimeGrenserMetadata.gjelderOmraade
        this.transformasjonsdato = maritimeGrenserMetadata.transformasjonsdato
        this.virkeomraadenavn = maritimeGrenserMetadata.virkeomraadenavn
        this.setLand(maritimeGrenserMetadata.toListeAvLand())
    }

    /**
     * Identifikasjon ignoreres, dette skal ikke kunne oppdateres, burde egentlig ikke være med på tur inn.
     * Sporingsinformasjon ignoreres også, dette settes av interceptor.
     */
    private fun CommonMetadataProperties.updateWithCommonMetadata(common: CommonMetadata) {
        this.datafangstdato = common.datafangstdato
        this.gyldighet = Gyldighet(common.gyldigFra, common.gyldigTil)
        this.setInfo(common.informasjon)
        this.opphav = common.opphav
    }

    private fun toAnsvarligeMyndigheter(tekstholdere: List<TekstHolder>): List<AnsvarligMyndighetHolder> =
        tekstholdere.map { AnsvarligMyndighetHolder(id = it.id, ansvarligmyndighet = it.beskrivelse) }

    private fun MaritimeGrenserMetadata.toListeAvLand(): List<Landkode> = this.land.ids.map {
        val landkodeOptional = landKodeRepository.findById(it)
        if (landkodeOptional.isEmpty) {
            throw RuntimeException("Ugyldig landkodeId, fikk ingen treff: $it")
        } else {
            landkodeOptional.get()
        }
    }

    private fun updateWithGrenseMetadata(grense: Grense, commonGrenseMetadata: CommonGrenseMetadata) {
        grense.kvalitet = commonGrenseMetadata.posisjonskvalitet?.toEmbeddable()

        val fastsettingstypeId = commonGrenseMetadata.fastsettingstype.id
        grense.fastsettingstype = if (fastsettingstypeId != null) {
            val fastsettingstypeOptional = fastsettingstypeRepository.findById(fastsettingstypeId)
            if (fastsettingstypeOptional.isEmpty) { // Har sendt inn en ugyldig id fra klienten
                throw RuntimeException("Ugyldig fastsettingstypeId: $fastsettingstypeId")
            } else {
                fastsettingstypeOptional.get()
            }
        } else {
            null
        }

        val grensestatusId = commonGrenseMetadata.grensestatus.id
        grense.grensestatus = if (grensestatusId != null) {
            val grensestatusOptional = grensestatusRepository.findById(grensestatusId)
            if (grensestatusOptional.isEmpty) { // Har sendt inn en ugyldig id fra klienten
                throw RuntimeException("Ugyldig grensestatusId: $grensestatusId")
            } else {
                grensestatusOptional.get()
            }
        } else {
            null
        }
    }

    private fun hentNoeyaktighetsKlasse(noeyaktighetsklasseId: String?): Noeyaktighetsklassekode? = noeyaktighetsklasseId?.let {
        when (val noeyaktighetsklassekode = noeyaktighetsklassekodeRepository.findById(noeyaktighetsklasseId).unwrap()) {
            null -> throw RuntimeException("Ugyldig noeyaktighetsklasseId: $noeyaktighetsklasseId")
            else -> noeyaktighetsklassekode
        }
    }

    private fun hentTerrengDetaljKode(terrengdetaljKodeId: String?): Terrengdetaljkode? = terrengdetaljKodeId?.let {
        when (val terrengdetaljkode = terrengdetaljkodeRepository.findById(terrengdetaljKodeId).unwrap()) {
            null -> throw RuntimeException("Ugyldig terrengdetaljKodeId: $terrengdetaljKodeId")
            else -> terrengdetaljkode
        }
    }
}
