package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.GrenseApi
import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.AdministrativGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.AvtaltAvgrensningslinjeMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.KommunalKretsgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataMapper
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataUpdater
import no.kartverket.nibas.api.v1.common.geojson.metadata.RiksgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.StatistiskgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.TerritorialgrenseMetadata
import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.GrenseMedAttributter
import no.kartverket.nibas.domain.GrenseMedRetningOgKontekst
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import no.kartverket.nibas.unwrap
import org.locationtech.jts.geom.Point
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.logging.Logger

@RestController
class GrenseController(
    private val grenseService: GrenseService,
    private val historikkService: HistorikkService,
    private val kodelisteService: KodelisteService
) : GrenseApi {

    /**
     * Siden GrenseService.lagreGrenser er transactional vil kallet til lagreGrenser rulles tilbake dersom en RuntimeEx krysser transaksjonsgrensa.
     */
    override fun lagreGrenser(grenser: FeatureCollection) {
        grenseService.lagreGrenser(grenser)
    }

    override fun hentGrense(id: String): Feature = when (val grense = grenseService.hentGrense(id)) {
        null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
        else -> grenseToFeature(grense = grense, kodelisteService)
    }

    override fun hentGrenseRevisjon(id: String, revisjon: Int): Feature =
        grenseToFeature(grense = historikkService.hentRevisjon(id, revisjon, Grense::class.java), kodelisteService)

    override fun hentGrenseRevisjoner(id: String): List<RevisjonRef> =
        historikkService.hentRevisjoner(id, Grense::class.java).map { toRevisjonRef(it) }

}

@Service
class GrenseService(
    private val grenseRepository: GrenseRepository,
    private val landKodeRepository: LandKodeRepository,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val stemmekretsRepository: StemmekretsRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
    private val noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
    private val grensestatusRepository: GrensestatusRepository,
    private val flateService: FlateService,
) {
    private val logger: Logger = Logger.getLogger(this::class.java.name)

    fun hentGrense(id: String): Grense? {
        return grenseRepository.findById(id).unwrap()
    }

    @Transactional
    fun lagreGrenser(grenser: FeatureCollection) {
        val features = grenser.features
        features
            .groupBy { it.properties.type }
            .forEach { mapEntry ->
                doLagreGrenser(mapEntry.value)
            }
        flateService.beregnFlaterFor(features)
    }

    private fun doLagreGrenser(features: List<Feature>) {
        // Hvis det er innkommende features med id ikke satt kastes HTTP 400
        if (features.any { feature -> feature.id == null }) {
            logger.warning("Invalid request: Missing feature id.")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing feature id.")
        }
        val eksisterendeGrenser: Iterable<Grense> = grenseRepository.findAllById(features.map { it.id })

        // Hvis det er innkommende features med id som ikke ligger i databasen kastes HTTP 400
        val grenseIdDiffs: List<String> = features.map { it.id!! } - eksisterendeGrenser.map { it.id }.toSet()
        if (grenseIdDiffs.isNotEmpty()) {
            logger.warning("Invalid request: Non-existing grenseIds were used in request.")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-existing grenseIds")
        }

        val grenserMedRetningOgKontekst = features.map { feature ->
            var grense: Grense = eksisterendeGrenser.find { it.id == feature.id }!!

            grense.grense = feature.geometry.toLineString()
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
                kontekstEgenskaper.id!!
            )
        }

        val grenser: List<Grense> = grenserMedRetningOgKontekst.map { it.grenseMedAttributter.grense }
        grenser.forEach { logger.info("Lagrer grense: [${it.grensetype.name} - id ${it.id}]") }
        grenseRepository.saveAll(grenser)

        grenserMedRetningOgKontekst
            .groupBy { it.kontekstType }
            .forEach { entry ->
                oppdaterGrensekoblinger(entry.key, entry.value)
            }
    }

    private fun oppdaterGrensekoblinger(kontekstType: KontekstEnhetType, grenserMedRetningOgKontekst: List<GrenseMedRetningOgKontekst>) {
        val enhetKobletTilGrenseRepository = when (kontekstType) {
            KontekstEnhetType.GRUNNKRETS -> grunnkretsRepository
            KontekstEnhetType.STEMMEKRETS -> stemmekretsRepository
        }
        enhetKobletTilGrenseRepository.findAllById(grenserMedRetningOgKontekst.map { it.kontekstId })
            .forEach { enhetKobletTilGrense ->
                grenserMedRetningOgKontekst
                    .filter { grenseMedRetningOgKontekst -> grenseMedRetningOgKontekst.kontekstId == enhetKobletTilGrense.id }
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
            val metadataUpdater = MetadataUpdater(landKodeRepository,
                fastsettingstypeRepository,
                terrengdetaljkodeRepository,
                noeyaktighetsklassekodeRepository,
                grensestatusRepository
            )
            when (grense.grensetype) {
                Grensetype.Kommunegrense -> metadataUpdater.updateAdministrativGrense(grense, (featureMetadata as AdministrativGrenseMetadata))
                Grensetype.Fylkesgrense -> metadataUpdater.updateAdministrativGrense(grense, (featureMetadata as AdministrativGrenseMetadata))
                Grensetype.Riksgrense -> metadataUpdater.updateRiksgrense(grense, (featureMetadata as RiksgrenseMetadata))
                Grensetype.AvtaltAvgrensningslinje -> metadataUpdater.updateAvtaltavgrensningslinje(grense, (featureMetadata as AvtaltAvgrensningslinjeMetadata))
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
 */
fun grenseKoblingToFeature(
    grenseKobling: GrenseKobling,
    kodelisteService: KodelisteService,
    kontekstType: KontekstEnhetType = KontekstEnhetType.GRUNNKRETS
): Feature {
    val feature = grenseToFeature(grenseKobling.grense, kodelisteService)
    return feature.copy(
        properties = feature.properties.copy(
            kontekstEgenskaper = KontekstEgenskaper(
                id = grenseKobling.getEnhetKobletTilGrense().id,
                type = kontekstType,
                retningMedKlokken = grenseKobling.grenseKoblingAttributter.retningMedKlokken,
                rekkefoelge = grenseKobling.grenseKoblingAttributter.rekkefoelge,
                flateIndeks = grenseKobling.grenseKoblingAttributter.flateIndeks,
                hullIndeks = grenseKobling.grenseKoblingAttributter.hullIndeks
            )
        )
    )
}

fun grenseToFeature(grense: Grense, kodelisteService: KodelisteService): Feature {
    var featureProperties = FeatureProperties(type = grense.grensetype.value)
    val metadataMapper = MetadataMapper(kodelisteService)
    featureProperties = when (grense.grensetype) {
        Grensetype.Fylkesgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForAdministrativGrense(grense))
        Grensetype.Kommunegrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForAdministrativGrense(grense))
        Grensetype.Riksgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForRiksgrense(grense))
        Grensetype.AvtaltAvgrensningslinje -> featureProperties.copy(metadata = metadataMapper.toMetadataForAvtaltAvgrensningslinje(grense))
        Grensetype.Territorialgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForTerritorialgrense(grense))
        Grensetype.Grunnlinje -> featureProperties.copy(metadata = metadataMapper.toMetadataForGrunnlinje(grense))
        Grensetype.Delomraadegrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForStatistiskGrense(grense))
        Grensetype.Grunnkretsgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForStatistiskGrense(grense))
        Grensetype.Stemmekretsgrense -> featureProperties.copy(metadata = metadataMapper.toMetadataForKommunalKretsgrense(grense))
        else -> throw RuntimeException("Ikke støttet å mappe til metadata for grensetype: " + grense.grensetype.name)
    }

    return Feature(
        id = grense.id,
        properties = featureProperties,
        geometry = grense.grense.toDto()
    )
}

fun posisjonToFeature(posisjon: Point?): Feature =
    Feature(properties = FeatureProperties(type = "Posisjon"), geometry = posisjon!!.toDto())

fun flateToFeature(flate: Flate, kodelisteService: KodelisteService): Feature {
    var featureProperties = FeatureProperties(
        type = flate.type.name,
    )
    val metadataMapper = MetadataMapper(kodelisteService)

    featureProperties = featureProperties.copy(metadata = metadataMapper.toMetadata(flate))

    return Feature(
        id = flate.id,
        properties = featureProperties,
        geometry = flate.flate!!.toDto()
    )
}
