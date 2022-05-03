package no.kartverket.nibas.api.v1.common.geojson.metadata

import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import java.time.LocalDate

/**
 * Hjelpeklasse/mappinglogikk metadata -> grense.
 * Skilte ut egen klasse da extension-funksjonene ble avhengig av injectet repository.
 */
class MetadataUpdater(private val landKodeRepository: LandKodeRepository,
                      private val fastsettingstypeRepository: FastsettingstypeRepository,
                      private val terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
                      private val noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
                      private val grensestatusRepository: GrensestatusRepository) {

    fun updateAdministrativGrense(grense: AdministrativGrense, metadata: AdministrativGrenseMetadata): AdministrativGrense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        updateDokumentasjonsreferanser(grense, metadata.dokumentasjonsreferanser)

        val terrengdetaljKodeId = metadata.foelgerTerrengdetalj.id
        grense.foelgerterrengdetalj = if (terrengdetaljKodeId != null) {
            val terrengdetaljkodeOptional = terrengdetaljkodeRepository.findById(terrengdetaljKodeId)
            if (terrengdetaljkodeOptional.isEmpty) { // Har sendt inn en ugyldig id fra klienten
                throw RuntimeException("Ugyldig terrengdetaljKodeId: $terrengdetaljKodeId")
            } else {
                terrengdetaljkodeOptional.get()
            }
        } else {
            null
        }

        val noeyaktighetsklasseId = metadata.noeyaktighetsklasse.id
        grense.noeyaktighetsklasse = if (noeyaktighetsklasseId != null) {
            val noeyaktighetsklassekodeOptional = noeyaktighetsklassekodeRepository.findById(noeyaktighetsklasseId)
            if (noeyaktighetsklassekodeOptional.isEmpty) { // Har sendt inn en ugyldig id fra klienten
                throw RuntimeException("Ugyldig noeyaktighetsklasseId: $noeyaktighetsklasseId")
            } else {
                noeyaktighetsklassekodeOptional.get()
            }
        } else {
            null
        }

        grense.omtvistet = metadata.omtvistet
        return grense
    }

    fun updateRiksgrense(grense: Riksgrense, metadata: RiksgrenseMetadata): Riksgrense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        updateDokumentasjonsreferanser(grense, metadata.dokumentasjonsreferanser)

        val terrengdetaljKodeId = metadata.foelgerTerrengdetalj.id
        grense.foelgerterrengdetalj = if (terrengdetaljKodeId != null) {
            val terrengdetaljkodeOptional = terrengdetaljkodeRepository.findById(terrengdetaljKodeId)
            if (terrengdetaljkodeOptional.isEmpty) { // Har sendt inn en ugyldig id fra klienten
                throw RuntimeException("Ugyldig terrengdetaljKodeId: $terrengdetaljKodeId")
            } else {
                terrengdetaljkodeOptional.get()
            }
        } else {
            null
        }

        grense.stedfestingverifisert = metadata.stedfestingVerifisert
        grense.setAnsvarligeMyndigheter(toAnsvarligeMyndigheter(metadata.ansvarligeMyndigheter))
        grense.grensegatebredde = metadata.grensegateBredde
        return grense
    }

    fun updateAvtaltavgrensningslinje(grense: Avtaltavgrensningslinje, metadata: AvtaltAvgrensningslinjeMetadata): Avtaltavgrensningslinje {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        updateDokumentasjonsreferanser(grense, metadata.dokumentasjonsreferanser)
        grense.updateWithMaritimeMetadata(metadata.maritimeGrenser)
        return grense
    }

    fun updateTerritoritalgrense(grense: Territorialgrense, metadata: TerritorialgrenseMetadata): Territorialgrense {
        grense.updateWithCommonMetadata(metadata.common)
        updateWithGrenseMetadata(grense, metadata.commonGrense)
        updateDokumentasjonsreferanser(grense, metadata.dokumentasjonsreferanser)
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

    // Identifikasjon ignoreres, dette skal ikke kunne oppdateres, burde egentlig ikke være med på tur inn
    private fun CommonMetadataProperties.updateWithCommonMetadata(common: CommonMetadata) {
        this.datafangstdato = common.datafangstdato
        this.gyldigFra = common.gyldigFra
        this.gyldigTil = common.gyldigTil
        this.setInfo(common.informasjon)
        this.oppdateringsdato = LocalDate.now()
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

    private fun updateDokumentasjonsreferanser(grense: GrenseMedDokumentasjonsreferanser, dokRefList: List<Dokref>) {
        dokRefList.forEach { dokRef ->
            var dokumentasjonsreferanse = grense.dokumentasjonsreferanser.find { dokumentasjonsreferanse -> dokumentasjonsreferanse.id == dokRef.id }
            if (dokumentasjonsreferanse == null) { // Legger til ny Dokumentasjonsreferanse hvis ulik id/id er null
                dokumentasjonsreferanse = Dokumentasjonsreferanse()
                grense.dokumentasjonsreferanser.add(dokumentasjonsreferanse)
            }

            dokumentasjonsreferanse.fastsettingsdato = dokRef.fastsettingsdato
            dokumentasjonsreferanse.fastsettingsmyndighet = dokRef.fastsettingsmyndighet
            dokumentasjonsreferanse.hjemmel = dokRef.hjemmel
            dokumentasjonsreferanse.rettskildeid = dokRef.rettskildeId
            dokumentasjonsreferanse.rettskildetittel = dokRef.rettskildeTittel

            dokRef.dokumentlenker.forEach { dokumentLenkeHolder ->
                val dokumentlenker = dokumentasjonsreferanse.dokumentlenker
                var dokumentLink = dokumentlenker.find { dokumentLink -> dokumentLink.id == dokumentLenkeHolder.id }
                if (dokumentLink == null) { // Hvis ny instans, opprettes den
                    dokumentLink = DokumentasjonsreferanseDokumentlink()
                    dokumentlenker.add(dokumentLink)
                }
                dokumentLink.dokumentlink = dokumentLenkeHolder.beskrivelse
            }

            dokRef.internReferanserKartverket.forEach { internRefHolder ->
                val internReferanser = dokumentasjonsreferanse.internReferanserKartverket
                var internReferanse = internReferanser.find { internRef -> internRef.id == internRefHolder.id }
                if (internReferanse == null) { // Hvis ny instans, opprettes den
                    internReferanse = DokumentasjonsreferanseInternReferanseKartverket()
                    internReferanser.add(internReferanse)
                }
                internReferanse.internReferanseKartverket = internRefHolder.beskrivelse
            }
        }
    }
}
