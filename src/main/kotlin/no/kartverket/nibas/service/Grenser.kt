package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.MultiPolygon
import no.kartverket.nibas.api.v1.common.geojson.metadata.AdministrativGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.AvtaltAvgrensningslinjeMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.KommunalKretsgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataMapper
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataUpdater
import no.kartverket.nibas.api.v1.common.geojson.metadata.RiksgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.StatistiskgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.TerritorialgrenseMetadata
import no.kartverket.nibas.api.v1.response.ObjektIdentifikator
import no.kartverket.nibas.api.v1.response.toIdentifikator
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrenseMedAttributter
import no.kartverket.nibas.domain.GrenseMedRetningOgKontekst
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KonfliktVersjon
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.findAllGyldigeObjekterForIdentifikatorer
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.sosi.sosiimport.SosiGeometryFunctions
import org.locationtech.jts.geom.Point
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.system.measureTimeMillis

@Transactional(readOnly = true)
@Service
class GrenseService(
    private val grenseRepository: GrenseRepository,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val stemmekretsRepository: StemmekretsRepository,
    private val flateService: FlateService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGrense(identifikator: Identifikator): Grense? = grenseRepository.findGyldigObjektForIdentifikator(identifikator)

    // TODO: Dato må sendes inn her på utkast. Men de fleste grenseendringer er kvalitetshevinger som skal skje umiddelbart, så dagens dato duger en stund.
    @Transactional(readOnly = false)
    fun lagreGrenser(grenser: FeatureCollection, gyldighetsdato: LocalDate = LocalDate.now()) {
        val millis = measureTimeMillis {
            val features = grenser.features
            features
                .groupBy { it.properties.type }
                .forEach { mapEntry ->
                    doLagreGrenser(mapEntry.value, gyldighetsdato)
                }

            var enheterSomSkalFlatedannes: Set<Flate> = features.flatMap {
                val identifikator = featureIdToIdentifikator(it.id, gyldighetsdato)
                val grense = hentGrense(identifikator) ?: throw ResponseException.NotFoundException("Grense $identifikator ble ikke funnet.")
                when (Grensetype.fromValue(it.properties.type)) {
                    //TODO: Når vi slår sammen administrative grenser for stemme- og grunnkrets må vi huske å oppdatere stemmekretsene også når vi oppdatererer en administrativ grense. Pr nå tas ikke disse flatene med utenfor "stemmekrets-sfæren"
                    Grensetype.Kommunegrense -> grense.finnFlaterOppTilKommune()
                    Grensetype.Fylkesgrense -> grense.finnFlaterOppTilFylke()
                    Grensetype.Riksgrense -> grense.finnFlaterOppTilNasjon()
                    Grensetype.AvtaltAvgrensningslinje -> grense.finnFlaterOppTilNasjon()
                    Grensetype.Territorialgrense -> grense.finnFlaterOppTilNasjon()
                    Grensetype.Grunnkretsgrense -> grense.finnFlaterOppTilGrunnkrets()
                    Grensetype.Delomraadegrense -> grense.finnFlaterOppTilGrunnkrets()
                    Grensetype.Stemmekretsgrense -> grense.finnFlaterOppTilStemmekrets()
                    else -> throw RuntimeException("Not yet implemented for grensetype: ${it.properties.type}")
                }
            }.toSet()


            flateService.oppdaterFlaterFor(enheterSomSkalFlatedannes)
        }

        logger.debug("Tid brukt på å lagre grenser og oppdatere flater: $millis")
    }

    // Feature.id vil være lokalid
    private fun doLagreGrenser(features: List<Feature>, gyldighetsdato: LocalDate = LocalDate.now()) {
        // Hvis det er innkommende features med id ikke satt kastes HTTP 400
        if (features.any { feature -> feature.id == null }) {
            throw BadRequestException("Missing feature id.")
        }
        // Henter eksisterende grenser på identifikator. Sorterer på lokalid for testens skyld.
        val grenseIdentifikatorer = features.map { featureIdToIdentifikator(it.id, gyldighetsdato) }.sortedBy { it.lokalid.value }
        val identifikatorToGrenseMap = grenseRepository.findAllGyldigeObjekterForIdentifikatorer(grenseIdentifikatorer)
        val eksisterendeGrenser: List<Grense> = identifikatorToGrenseMap.values.toList()

        // Hvis det er innkommende features med id som ikke ligger i databasen kastes HTTP 400
        val identifikatorDiffs: List<Identifikator> = grenseIdentifikatorer - identifikatorToGrenseMap.keys
        if (identifikatorDiffs.isNotEmpty()) {
            throw BadRequestException("Non-existing grenseidentifikatorer: [${identifikatorDiffs.joinToString(",")}])")
        }

        val konflikterIGrenser: List<KonfliktVersjon> = features.mapNotNull { feature ->
            val featureIdentifikator = featureIdToIdentifikator(feature.id, gyldighetsdato)
            verifiserOptimistiskLaas(feature.properties.version, findGrenseForIdentifikator(eksisterendeGrenser, featureIdentifikator))
        }
        if (konflikterIGrenser.isNotEmpty()) {
            throw OptimistiskLaasException(*konflikterIGrenser.toTypedArray())
        }

        val grenserMedRetningOgKontekst = features.map { feature ->
            var grense: Grense = findGrenseForIdentifikator(eksisterendeGrenser, featureIdToIdentifikator(feature.id, gyldighetsdato))

            grense.grense = feature.geometry.toLineString(feature.properties.srid)
            grense = updateMetadataOnGrense(feature, grense)

            val kontekstEgenskaper = feature.properties.kontekstEgenskaper!!
            GrenseMedRetningOgKontekst(
                GrenseMedAttributter(
                    grense,
                    GrenseKoblingAttributter(
                        kontekstEgenskaper.retningMedKlokken,
                        kontekstEgenskaper.rekkefoelge,
                        kontekstEgenskaper.flateIndeks,
                        kontekstEgenskaper.hullIndeks
                    )
                ),
                kontekstEgenskaper.type!!,
                kontekstEgenskaper.id!!.toIdentifikator()
            )
        }

        val grenser: List<Grense> = grenserMedRetningOgKontekst.map { it.grenseMedAttributter.grense }
        grenser.forEach { logger.info("Lagrer grense: [${it.grensetype.name} - id ${it.id}]") }
        grenseRepository.saveAllAndFlush(grenser)

        grenserMedRetningOgKontekst
            .groupBy { it.kontekstType }
            .forEach { entry ->
                oppdaterGrensekoblinger(entry.key, entry.value)
            }
    }

    private fun featureIdToIdentifikator(lokalid: String?, gyldighetsdato: LocalDate): Identifikator = Identifikator(Lokalid(lokalid!!), gyldighetsdato)
    private fun findGrenseForIdentifikator(grenser: List<Grense>, identifikator: Identifikator): Grense {
        return grenser.find { it.lokalid == identifikator.lokalid.value && it.gyldigPaaDato(identifikator.gyldighetsdato) }!!
    }

    private fun oppdaterGrensekoblinger(kontekstType: KontekstEnhetType, grenserMedRetningOgKontekst: List<GrenseMedRetningOgKontekst>) {
        val enhetKobletTilGrenseRepository = when (kontekstType) {
            KontekstEnhetType.GRUNNKRETS -> grunnkretsRepository
            KontekstEnhetType.STEMMEKRETS -> stemmekretsRepository
        }
        val kretsIdentifikators = grenserMedRetningOgKontekst.map { it.kontekstId }
        kretsIdentifikators.forEach { kretsIdentifikator ->
            val enhetKobletTilGrense = enhetKobletTilGrenseRepository.findGyldigObjektForIdentifikator(kretsIdentifikator)
                ?: throw RuntimeException("Fant ikke enhet for identifikator $kretsIdentifikator")
            grenserMedRetningOgKontekst
                .filter { grenseMedRetningOgKontekst ->
                    val kontekstIdentifikator = grenseMedRetningOgKontekst.kontekstId
                    // Må matche på Kretsens lokalid og være gyldig på identifikatorens gyldighetsdato
                    kontekstIdentifikator.lokalid.value == enhetKobletTilGrense.lokalid && enhetKobletTilGrense.gyldigPaaDato(kontekstIdentifikator.gyldighetsdato)
                }
                .map { it.grenseMedAttributter }
                .forEach { grenseMedAttributter -> enhetKobletTilGrense.oppdaterEllerLeggTilGrenseKobling(grenseMedAttributter) }
            logger.info("Oppdaterer enhet: [${enhetKobletTilGrense::class.java.name} - id ${enhetKobletTilGrense.id}]")
        }
    }

    private fun updateMetadataOnGrense(
        feature: Feature,
        grense: Grense
    ): Grense {
        val featureMetadata = feature.properties.metadata
        if (featureMetadata != null) {
            val metadataUpdater = MetadataUpdater()
            when (grense.grensetype) {
                Grensetype.Kommunegrense -> metadataUpdater.updateAdministrativGrense(grense, (featureMetadata as AdministrativGrenseMetadata))
                Grensetype.Fylkesgrense -> metadataUpdater.updateAdministrativGrense(grense, (featureMetadata as AdministrativGrenseMetadata))
                Grensetype.Riksgrense -> metadataUpdater.updateRiksgrense(grense, (featureMetadata as RiksgrenseMetadata))
                Grensetype.AvtaltAvgrensningslinje -> metadataUpdater.updateAvtaltavgrensningslinje(
                    grense = grense,
                    metadata = (featureMetadata as AvtaltAvgrensningslinjeMetadata)
                )

                Grensetype.Territorialgrense -> metadataUpdater.updateTerritoritalgrense(grense, (featureMetadata as TerritorialgrenseMetadata))
                Grensetype.Delomraadegrense -> metadataUpdater.updateStatistiskGrense(grense, (featureMetadata as StatistiskgrenseMetadata))
                Grensetype.Grunnkretsgrense -> metadataUpdater.updateStatistiskGrense(grense, (featureMetadata as StatistiskgrenseMetadata))
                Grensetype.Stemmekretsgrense -> metadataUpdater.updateKommunalKretsgrense(grense, (featureMetadata as KommunalKretsgrenseMetadata))
                else -> throw RuntimeException("Ikke støttet å oppdatere metadata for grensetype: " + grense.grensetype.name)
            }
        }
        return grense
    }

}

/**
 * Default angitt kontekst er Grunnkrets.
 * Kontekstegenskapene retning, rekkefoelge, flateIndeks, hullIndeks gir nå kun mening for
 * Grunnkrets/Stemmekrets.
 * Metoden tar inn gyldighetsdato, defaulter til dagens dato. Dette for å kunne velge rett versjon av Grense-objektet dersom det finnes framtidige versjoner.
 */
fun grenseKoblingToFeature(
    grenseKobling: GrenseKobling,
    kontekstType: KontekstEnhetType = KontekstEnhetType.GRUNNKRETS,
    gyldighetsdato: LocalDate = LocalDate.now()
): Feature {
    val feature = grenseKobling.getRelevantGrense(gyldighetsdato).toFeature()
    val koblingAttributter = grenseKobling.grenseKoblingAttributter
    return feature.copy(
        properties = feature.properties.copy(
            kontekstEgenskaper = KontekstEgenskaper(
                id = ObjektIdentifikator(
                    lokalid = Lokalid(grenseKobling.getEnhetKobletTilGrense().lokalid),
                    gyldighetsdato = gyldighetsdato
                ),
                type = kontekstType,
                retningMedKlokken = koblingAttributter.retningMedKlokken,
                rekkefoelge = koblingAttributter.rekkefoelge,
                flateIndeks = koblingAttributter.flateIndeks,
                hullIndeks = koblingAttributter.hullIndeks
            )
        )
    )
}

fun Grense.toFeature(): Feature {
    val grensetype = this.grensetype
    var featureProperties = FeatureProperties(type = grensetype.value, srid = this.grense.srid, version = this.version)
    val metadataMapper = MetadataMapper()
    featureProperties = when (grensetype) {
        Grensetype.Fylkesgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForAdministrativGrense(this))
        Grensetype.Kommunegrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForAdministrativGrense(this))
        Grensetype.Riksgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForRiksgrense(this))
        Grensetype.AvtaltAvgrensningslinje -> featureProperties.copy(metadata = metadataMapper.toMetadataForAvtaltAvgrensningslinje(this))
        Grensetype.Territorialgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForTerritorialgrense(this))
        Grensetype.Grunnlinje -> featureProperties.copy(metadata = metadataMapper.toMetadataForGrunnlinje(this))
        Grensetype.Delomraadegrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForStatistiskGrense(this))
        Grensetype.Grunnkretsgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForStatistiskGrense(this))
        Grensetype.Stemmekretsgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForKommunalKretsgrense(this))
        else -> throw RuntimeException("Ikke støttet å mappe til metadata for grensetype: " + grensetype.name)
    }

    return Feature(
        // Feature må ha en id, men denne må være objektets lokalid
        id = this.lokalid,
        properties = featureProperties,
        geometry = this.grense.toDto()
    )
}

fun posisjonToFeature(posisjon: Point?, version: Int): Feature =
    Feature(
        properties = FeatureProperties(type = "Posisjon", srid = posisjon!!.srid, version = version),
        geometry = posisjon.toDto()
    )

fun flateToFeature(flate: Flate, version: Int): Feature {
    var featureProperties = FeatureProperties(
        type = flate.type.name,
        srid = flate.omraade?.srid ?: SosiGeometryFunctions.defaultSrid,
        version = version
    )
    val metadataMapper = MetadataMapper()

    featureProperties = featureProperties.copy(metadata = metadataMapper.toMetadata(flate))

    return Feature(
        // Feature må ha en id, men denne må være objektets lokalid
        id = flate.lokalid,
        properties = featureProperties,
        geometry = flate.omraade?.toDto() ?: MultiPolygon(coordinates = emptyList()),
    )
}
