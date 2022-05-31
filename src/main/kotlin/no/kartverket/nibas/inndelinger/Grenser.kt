package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.GrenseApi
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.KontekstEgenskaper
import no.kartverket.nibas.api.v1.common.geojson.metadata.AdministrativGrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.AvtaltAvgrensningslinjeMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataMapper
import no.kartverket.nibas.api.v1.common.geojson.metadata.MetadataUpdater
import no.kartverket.nibas.api.v1.common.geojson.metadata.RiksgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.StatistiskgrenseMetadata
import no.kartverket.nibas.api.v1.common.geojson.metadata.TerritorialgrenseMetadata
import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.domain.AdministrativGrense
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Delomraadegrense
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnkretsgrense
import no.kartverket.nibas.domain.Grunnlinje
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.StatistiskGrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AvtaltAvgrensningslinjeRepository
import no.kartverket.nibas.repository.DelomraadegrenseRepository
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.FylkesgrenseRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.GrunnkretsgrenseRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.KommunegrenseRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.RiksgrenseRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import no.kartverket.nibas.repository.TerritorialgrenseRepository
import no.kartverket.nibas.unwrap
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.logging.Logger
import kotlin.reflect.KClass

@RestController
class GrenseController @Autowired constructor(
    private val grenseService: GrenseService,
    private val historikkService: HistorikkService,
    private val flateService: FlateService,
    private val kodelisteService: KodelisteService
) : GrenseApi {

    override fun lagreGrenser(grenser: FeatureCollection) {
        grenser.features.groupBy { it.properties.type }.forEach { entry ->
            when (GrenseType.fromValue(entry.key)) {
                GrenseType.Kommunegrense -> grenseService.lagreKommunegrenser(entry.value)
                GrenseType.Fylkesgrense -> grenseService.lagreFylkesgrenser(entry.value)
                GrenseType.Riksgrense -> grenseService.lagreRiksgrenser(entry.value)
                GrenseType.AvtaltAvgrensningslinje -> grenseService.lagreAvtaltAvgrensningslinjer(entry.value)
                GrenseType.Territorialgrense -> grenseService.lagreTerritorialgrenser(entry.value)
                GrenseType.Grunnkretsgrense -> grenseService.lagreGrunnkretsgrenser(entry.value)
                GrenseType.Delomraadegrense -> grenseService.lagreDelomraadegrenser(entry.value)
                else -> throw RuntimeException("Lagring/oppdatering av grenser er ikke implementert for grensetype: ${entry.key}")
            }
        }

        flateService.beregnFlaterFor(grenser.features)
    }

    override fun hentKommunegrense(id: String): Feature = hentGrense(id, Kommunegrense::class)
    override fun hentFylkesgrense(id: String): Feature = hentGrense(id, Fylkesgrense::class)
    override fun hentRiksgrense(id: String): Feature = hentGrense(id, Riksgrense::class)
    override fun hentTerritorialsgrense(id: String): Feature = hentGrense(id, Territorialgrense::class)
    override fun hentAvtaltavgrensningslinje(id: String): Feature = hentGrense(id, Avtaltavgrensningslinje::class)
    override fun hentGrunnkretsgrense(id: String): Feature = hentGrense(id, Grunnkretsgrense::class)
    override fun hentDelomraadegrense(id: String): Feature = hentGrense(id, Delomraadegrense::class)

    private fun <T : Grense> hentGrense(id: String, type: KClass<T>): Feature {
        return when (val grense = grenseService.hentGrense(id, type)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> grenseToFeature(grense = grense, kodelisteService)
        }
    }

    override fun hentKommunegrenseRevisjoner(id: String) = hentGrenseRevisjoner(id, Kommunegrense::class.java)
    override fun hentFylkesgrenseRevisjoner(id: String) = hentGrenseRevisjoner(id, Fylkesgrense::class.java)
    override fun hentRiksgrenseRevisjoner(id: String) = hentGrenseRevisjoner(id, Riksgrense::class.java)
    override fun hentTerritorialgrenseRevisjoner(id: String) = hentGrenseRevisjoner(id, Territorialgrense::class.java)
    override fun hentGrunnkretsgrenseRevisjoner(id: String) = hentGrenseRevisjoner(id, Grunnkretsgrense::class.java)
    override fun hentDelomraadegrenseRevisjoner(id: String) = hentGrenseRevisjoner(id, Delomraadegrense::class.java)

    override fun hentAvtaltavgrensningslinjeRevisjoner(id: String) =
        hentGrenseRevisjoner(id, Avtaltavgrensningslinje::class.java)

    private fun <T> hentGrenseRevisjoner(id: String, type: Class<T>): List<RevisjonRef> {
        return historikkService.hentRevisjoner(id, type).map { toRevisjonRef(it) }
    }

    override fun hentKommunegrenseRevisjon(id: String, revisjon: Int) =
        hentGrenseRevisjon(id, revisjon, Kommunegrense::class.java)

    override fun hentFylkesgrenseRevisjon(id: String, revisjon: Int): Feature =
        hentGrenseRevisjon(id, revisjon, Fylkesgrense::class.java)

    override fun hentRiksgrenseRevisjon(id: String, revisjon: Int): Feature =
        hentGrenseRevisjon(id, revisjon, Riksgrense::class.java)

    override fun hentAvtaltavgrensningslinjeRevisjon(id: String, revisjon: Int): Feature =
        hentGrenseRevisjon(id, revisjon, Avtaltavgrensningslinje::class.java)

    override fun hentTerritorialgrenseRevisjon(id: String, revisjon: Int): Feature =
        hentGrenseRevisjon(id, revisjon, Territorialgrense::class.java)

    override fun hentGrunnkretsgrenseRevisjon(id: String, revisjon: Int): Feature = hentGrenseRevisjon(id, revisjon, Grunnkretsgrense::class.java)

    override fun hentDelomraadegrenseRevisjon(id: String, revisjon: Int): Feature = hentGrenseRevisjon(id, revisjon, Delomraadegrense::class.java)

    private fun <T : Grense> hentGrenseRevisjon(id: String, revisjon: Int, type: Class<T>): Feature =
        grenseToFeature(grense = historikkService.hentRevisjon(id, revisjon, type), kodelisteService)
}

@Service
class GrenseService @Autowired constructor(
    private val kommunegrenseRepository: KommunegrenseRepository,
    private val fylkesgrenseRepository: FylkesgrenseRepository,
    private val territorialgrenseRepository: TerritorialgrenseRepository,
    private val avtaltAvgrensningslinjeRepository: AvtaltAvgrensningslinjeRepository,
    private val riksgrenseRepository: RiksgrenseRepository,
    private val grunnkretsgrenseRepository: GrunnkretsgrenseRepository,
    private val delomraadegrenseRepository: DelomraadegrenseRepository,
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository,
    private val landKodeRepository: LandKodeRepository,
    private val grunnkretsRepository: GrunnkretsRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
    private val noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
    private val grensestatusRepository: GrensestatusRepository
) {
    private val logger: Logger = Logger.getLogger(this::class.java.name)

    @Suppress("UNCHECKED_CAST")
    fun <T : Grense> hentGrense(id: String, kClass: KClass<T>): T? {
        return when (kClass) {
            Kommunegrense::class -> kommunegrenseRepository
            Fylkesgrense::class -> fylkesgrenseRepository
            Territorialgrense::class -> territorialgrenseRepository
            Avtaltavgrensningslinje::class -> avtaltAvgrensningslinjeRepository
            Riksgrense::class -> riksgrenseRepository
            Grunnkretsgrense::class -> grunnkretsgrenseRepository
            Delomraadegrense::class -> delomraadegrenseRepository
            else -> throw RuntimeException("Ukjent grensetype")
        }.findById(id).unwrap() as T?
    }

    fun lagreKommunegrenser(grenser: List<Feature>) =
        lagreGrenser(grenser, kommunegrenseRepository)

    fun lagreFylkesgrenser(grenser: List<Feature>) =
        lagreGrenser(grenser, fylkesgrenseRepository)

    fun lagreTerritorialgrenser(grenser: List<Feature>) =
        lagreGrenser(grenser, territorialgrenseRepository)

    fun lagreGrunnkretsgrenser(grenser: List<Feature>) =
        lagreGrenser(grenser, grunnkretsgrenseRepository)

    fun lagreDelomraadegrenser(grenser: List<Feature>) =
        lagreGrenser(grenser, delomraadegrenseRepository)

    fun lagreRiksgrenser(grenser: List<Feature>) = lagreGrenser(grenser, riksgrenseRepository)
    fun lagreAvtaltAvgrensningslinjer(grenser: List<Feature>) =
        lagreGrenser(grenser, avtaltAvgrensningslinjeRepository)

    data class GrenseMedRetningOgKontekst<T : Grense>(
        val grenseMedAttributter: GrenseMedAttributter<T>,
        val kontekstType: KontekstEnhetType,
        val kontekstId: String
    )

    @Transactional
    fun <T : Grense> lagreGrenser(
        grenserSomSkalLagres: List<Feature>,
        grenseRepository: CrudRepository<T, String>
    ) {
        val eksisterendeGrenser: Iterable<T> = grenseRepository.findAllById(
            grenserSomSkalLagres.filter { feature -> feature.id != null }.map { it.id }
        )
        // Hvis det er innkommende features med id satt som ikke ligger i databasen, kastes 400
        if ((grenserSomSkalLagres.filter { it.id != null }.map { it.id!! } - eksisterendeGrenser.map { it.id }.toSet()).isNotEmpty()) {
            logger.warning("Invalid request, non-existing grenseIds were used in request.")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-existing grenseIds")
        }

        val oppdaterteGrenser = grenserSomSkalLagres.map { feature ->
            var grense = when (val grenseId = feature.id) {
                null -> lagGrenseForType(feature)
                else -> eksisterendeGrenser.find { it.id == grenseId }!!
            }
            grense.grense = feature.geometry as LineString
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
        oppdaterteGrenser.forEach {
            val grense = it.grenseMedAttributter.grense
            logger.info("Lagrer grense: [${grense.objtype} - id ${grense.id}]")
        }

        grenseRepository.saveAll(oppdaterteGrenser.map { it.grenseMedAttributter.grense })

        oppdaterteGrenser.groupBy { it.kontekstType }.forEach { entry ->
            when (entry.key) {
                KontekstEnhetType.KOMMUNE -> oppdaterFlater(entry.value, kommuneRepository)
                KontekstEnhetType.FYLKE -> oppdaterFlater(entry.value, fylkeRepository)
                KontekstEnhetType.NASJON -> oppdaterFlater(entry.value, nasjonRepository)
                KontekstEnhetType.GRUNNKRETS -> oppdaterFlater(entry.value, grunnkretsRepository)
            }
        }
    }

    private fun <T : Grense> updateMetadataOnGrense(
        feature: Feature,
        grense: T
    ): T {
        val featureMetadata = feature.properties.metadata
        if (featureMetadata != null) {
            val metadataUpdater = MetadataUpdater(landKodeRepository,
                fastsettingstypeRepository,
                terrengdetaljkodeRepository,
                noeyaktighetsklassekodeRepository,
                grensestatusRepository
            )
            when (grense) {
                is AdministrativGrense -> metadataUpdater.updateAdministrativGrense(grense, (featureMetadata as AdministrativGrenseMetadata))
                is Riksgrense -> metadataUpdater.updateRiksgrense(grense, (featureMetadata as RiksgrenseMetadata))
                is Avtaltavgrensningslinje -> metadataUpdater.updateAvtaltavgrensningslinje(grense, (featureMetadata as AvtaltAvgrensningslinjeMetadata))
                is Territorialgrense -> metadataUpdater.updateTerritoritalgrense(grense, (featureMetadata as TerritorialgrenseMetadata))
                is StatistiskGrense -> metadataUpdater.updateStatistiskGrense(grense, (featureMetadata as StatistiskgrenseMetadata))
            }
        }
        return grense
    }

    private fun <T : Grense, S : Flate> oppdaterFlater(
        grenser: List<GrenseMedRetningOgKontekst<T>>,
        repository: CrudRepository<S, String>
    ) {
        val enheter = repository.findAllById(grenser.map { it.kontekstId })
        val enheterSomSkalLagres = enheter.map { enhet ->
            grenser.filter { it.kontekstId == enhet.id }
                .map { it.grenseMedAttributter }
                .forEach { grenseMedAttributter ->
                    enhet.oppdaterEllerLeggTilGrenseKobling(
                        grenseMedAttributter.grense,
                        grenseMedAttributter.attributter
                    )
                }
            logger.info("Lagrer enhet: [${enhet.type.name} - id ${enhet.id}]")

            enhet
        }

        repository.saveAll(enheterSomSkalLagres)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Grense> lagGrenseForType(feature: Feature): T {
        return when (GrenseType.fromValue(feature.properties.type)) {
            GrenseType.Kommunegrense -> Kommunegrense()
            GrenseType.Fylkesgrense -> Fylkesgrense()
            GrenseType.Riksgrense -> Riksgrense()
            GrenseType.AvtaltAvgrensningslinje -> Avtaltavgrensningslinje()
            GrenseType.Territorialgrense -> Territorialgrense()
            else -> throw RuntimeException("Not yet implemented for grensetype: ${feature.properties.type}")
        } as T
    }

}

/**
 * Introduserte en value-attributt for å kunne ha norske tegn i verdi, men ikke i enum-konstant-navn.
 */
enum class GrenseType(val value: String) {
    Kommunegrense("Kommunegrense"),
    Fylkesgrense("Fylkesgrense"),
    Riksgrense("Riksgrense"),
    AvtaltAvgrensningslinje("AvtaltAvgrensningslinje"),
    Territorialgrense("Territorialgrense"),
    Grunnlinje("Grunnlinje"),
    Lovvirkeomraadegrense("LovVirkeområdeGrense"),
    Grunnkretsgrense("Grunnkretsgrense"),
    Delomraadegrense("Delområdegrense"),
    Stemmekretsgrense("Stemmekretsgrense");

    companion object {
        fun fromValue(valueIn: String): GrenseType {
            try {
                return values().first { it.value == valueIn }
            } catch (e: NoSuchElementException) {
                throw IllegalArgumentException("Invalid value: $valueIn")
            }
        }
    }
}

class GrenseMedAttributter<T : Grense>(val grense: T, val attributter: GrenseKoblingAttributter)

fun grenseKoblingToFeature(
    kontekstId: String? = null,
    kontekstType: KontekstEnhetType? = null,
    grenseKobling: GrenseKobling,
    kodelisteService: KodelisteService
): Feature {
    val feature = grenseToFeature(grenseKobling.getGrense(), kodelisteService)
    return feature.copy(
        properties = feature.properties.copy(
            kontekstEgenskaper = KontekstEgenskaper(
                id = kontekstId,
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
    var featureProperties = FeatureProperties(
        type = grense.objtype!!,
    )
    val metadataMapper = MetadataMapper(kodelisteService)
    when (grense) {
        is AdministrativGrense -> featureProperties = featureProperties.copy(metadata = metadataMapper.toMetadata(grense))
        is Riksgrense -> featureProperties = featureProperties.copy(metadata = metadataMapper.toMetadata(grense))
        is Avtaltavgrensningslinje -> featureProperties = featureProperties.copy(metadata = metadataMapper.toMetadata(grense))
        is Territorialgrense -> featureProperties = featureProperties.copy(metadata = metadataMapper.toMetadata(grense))
        is Grunnlinje -> featureProperties = featureProperties.copy(metadata = metadataMapper.toMetadata(grense))
        is StatistiskGrense -> featureProperties = featureProperties.copy(metadata = metadataMapper.toMetadata(grense))
    }

    return Feature(
        id = grense.id,
        properties = featureProperties,
        geometry = grense.grense!!
    )
}

fun posisjonToFeature(posisjon: Point?): Feature =
    Feature(properties = FeatureProperties(type = "Posisjon"), geometry = posisjon!!)

fun flateToFeature(flate: MultiPolygon?): Feature =
    Feature(properties = FeatureProperties(type = "Flate"), geometry = flate!!)
