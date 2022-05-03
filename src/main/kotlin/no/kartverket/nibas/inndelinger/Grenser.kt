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
import no.kartverket.nibas.api.v1.common.geojson.metadata.TerritorialgrenseMetadata
import no.kartverket.nibas.api.v1.response.HistorikkRef
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.AdministrativGrense
import no.kartverket.nibas.domain.Avtaltavgrensningslinje
import no.kartverket.nibas.domain.Fylkesgrense
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.GrenseKoblingAttributter
import no.kartverket.nibas.domain.Grunnlinje
import no.kartverket.nibas.domain.Kommunegrense
import no.kartverket.nibas.domain.Riksgrense
import no.kartverket.nibas.domain.StatistiskGrense
import no.kartverket.nibas.domain.Territorialgrense
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AvtaltAvgrensningslinjeRepository
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.FylkesgrenseRepository
import no.kartverket.nibas.repository.GrensestatusRepository
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
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
                else -> throw RuntimeException("Not yet implemented for grensetype: ${entry.key}")
            }
        }

        flateService.beregnFlaterFor(grenser.features)
    }

    override fun hentKommunegrense(id: String): Feature = hentGrense(id, Kommunegrense::class)
    override fun hentFylkesgrense(id: String): Feature = hentGrense(id, Fylkesgrense::class)
    override fun hentRiksgrense(id: String): Feature = hentGrense(id, Riksgrense::class)
    override fun hentTerritorialsgrense(id: String): Feature = hentGrense(id, Territorialgrense::class)
    override fun hentAvtaltavgrensningslinje(id: String): Feature = hentGrense(id, Avtaltavgrensningslinje::class)
    private fun <T : Grense> hentGrense(id: String, type: KClass<T>): Feature {
        return when (val grense = grenseService.hentGrense(id, type)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> grenseToFeature(grense = grense, kodelisteService)
        }
    }

    override fun hentKommunegrenseHistorikk(id: String) = hentGrenseHistorikk(id, Kommunegrense::class.java)
    override fun hentFylkesgrenseHistorikk(id: String) = hentGrenseHistorikk(id, Fylkesgrense::class.java)
    override fun hentRiksgrenseHistorikk(id: String) = hentGrenseHistorikk(id, Riksgrense::class.java)
    override fun hentTerritorialgrenseHistorikk(id: String) = hentGrenseHistorikk(id, Territorialgrense::class.java)
    override fun hentAvtaltavgrensningslinjeHistorikk(id: String) =
        hentGrenseHistorikk(id, Avtaltavgrensningslinje::class.java)

    private fun <T> hentGrenseHistorikk(id: String, type: Class<T>): List<HistorikkRef> {
        return historikkService.hentHistorikk(id, type).map { pair ->
            HistorikkRef(
                pair.first.id,
                pair.first.revisionDate,
                toHistorikkStatus(pair.second),
                ServletUriComponentsBuilder.fromCurrentRequest().pathSegment(pair.first.id.toString()).build()
                    .toUriString()
            )
        }
    }

    override fun hentKommunegrenseRevisjon(id: String, revision: Int) =
        hentGrenseRevisjon(id, revision, Kommunegrense::class.java)

    override fun hentFylkesgrenseRevisjon(id: String, revision: Int): Feature =
        hentGrenseRevisjon(id, revision, Fylkesgrense::class.java)

    override fun hentRiksgrenseRevisjon(id: String, revision: Int): Feature =
        hentGrenseRevisjon(id, revision, Riksgrense::class.java)

    override fun hentAvtaltavgrensningslinjeRevisjon(id: String, revision: Int): Feature =
        hentGrenseRevisjon(id, revision, Avtaltavgrensningslinje::class.java)

    override fun hentTerritorialgrenseRevisjon(id: String, revision: Int): Feature =
        hentGrenseRevisjon(id, revision, Territorialgrense::class.java)

    private fun <T : Grense> hentGrenseRevisjon(id: String, revision: Int, type: Class<T>): Feature =
        grenseToFeature(grense = historikkService.hentHistorikkRevisjon(id, revision, type), kodelisteService)
}

@Service
class GrenseService @Autowired constructor(
    private val kommunegrenseRepository: KommunegrenseRepository,
    private val fylkesgrenseRepository: FylkesgrenseRepository,
    private val territorialgrenseRepository: TerritorialgrenseRepository,
    private val avtaltAvgrensningslinjeRepository: AvtaltAvgrensningslinjeRepository,
    private val riksgrenseRepository: RiksgrenseRepository,
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository,
    private val landKodeRepository: LandKodeRepository,
    private val fastsettingstypeRepository: FastsettingstypeRepository,
    private val terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
    private val noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
    private val grensestatusRepository: GrensestatusRepository
) {

    @Suppress("UNCHECKED_CAST")
    fun <T : Grense> hentGrense(id: String, kClass: KClass<T>): T? {
        return when (kClass) {
            Kommunegrense::class -> kommunegrenseRepository
            Fylkesgrense::class -> fylkesgrenseRepository
            Territorialgrense::class -> territorialgrenseRepository
            Avtaltavgrensningslinje::class -> avtaltAvgrensningslinjeRepository
            Riksgrense::class -> riksgrenseRepository
            else -> throw RuntimeException("Ukjent grensetype")
        }.findById(id).unwrap() as T?
    }

    fun lagreKommunegrenser(grenser: List<Feature>) =
        lagreGrenser(grenser, kommunegrenseRepository)

    fun lagreFylkesgrenser(grenser: List<Feature>) =
        lagreGrenser(grenser, fylkesgrenseRepository)

    fun lagreTerritorialgrenser(grenser: List<Feature>) =
        lagreGrenser(grenser, territorialgrenseRepository)

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
        val eksisterendeGrenser = grenseRepository.findAllById(
            grenserSomSkalLagres.filter { feature -> feature.id != null }.map { it.id }
        )

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

        grenseRepository.saveAll(oppdaterteGrenser.map { it.grenseMedAttributter.grense })

        oppdaterteGrenser.groupBy { it.kontekstType }.forEach { entry ->
            when (entry.key) {
                KontekstEnhetType.KOMMUNE -> oppdaterEnheter(entry.value, kommuneRepository)
                KontekstEnhetType.FYLKE -> oppdaterEnheter(entry.value, fylkeRepository)
                KontekstEnhetType.NASJON -> oppdaterEnheter(entry.value, nasjonRepository)
                KontekstEnhetType.GRUNNKRETS -> throw IllegalArgumentException("Kan ikke oppdatere grunnkretser ennå")
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
            }
        }
        return grense
    }

    private fun <T : Grense, S : AdministrativEnhet> oppdaterEnheter(
        grenser: List<GrenseMedRetningOgKontekst<T>>,
        repository: CrudRepository<S, String>
    ) {
        val enheter = repository.findAllById(grenser.map { it.kontekstId })
        repository.saveAll(
            enheter.map { enhet ->
                grenser.filter { it.kontekstId == enhet.id }.map { it.grenseMedAttributter }
                    .forEach { grenseMedAttributter ->
                        enhet.oppdaterEllerLeggTilGrenseKobling(
                            grenseMedAttributter.grense,
                            grenseMedAttributter.attributter
                        )
                    }
                enhet
            }
        )
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
    Lovvirkeomraadegrense("LovVirkeområdeGrense");

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
