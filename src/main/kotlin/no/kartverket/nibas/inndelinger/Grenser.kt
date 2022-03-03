package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.GrenseApi
import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.ContextProperties
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.common.geojson.Identifikasjon
import no.kartverket.nibas.api.v1.response.HistorikkRef
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.AvtaltAvgrensningslinjeRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.FylkesgrenseRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.KommunegrenseRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.RiksgrenseRepository
import no.kartverket.nibas.repository.TerritorialgrenseRepository
import no.kartverket.nibas.unwrap
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
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
    private val flateService: FlateService
) : GrenseApi {

    override fun lagreGrenser(grenser: FeatureCollection) {
        grenser.features.groupBy { it.properties.type }.forEach { entry ->
            when (GrenseType.valueOf(entry.key)) {
                GrenseType.Kommunegrense -> grenseService.lagreKommunegrenser(entry.value)
                GrenseType.Fylkesgrense -> grenseService.lagreFylkesgrenser(entry.value)
                GrenseType.Riksgrense -> grenseService.lagreRiksgrenser(entry.value)
                GrenseType.AvtaltAvgrensningslinje -> grenseService.lagreAvtaltAvgrensningslinjer(entry.value)
                GrenseType.Territorialgrense -> grenseService.lagreTerritorialgrenser(entry.value)
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
            else -> grenseToFeature(grense = grense)
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
        grenseToFeature(grense = historikkService.hentHistorikkRevisjon(id, revision, type))

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
    private val nasjonRepository: NasjonRepository
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
        }.findById(id).unwrap() as T
    }

    fun lagreKommunegrenser(grenser: List<Feature>) = lagreGrenser(grenser, kommunegrenseRepository)
    fun lagreFylkesgrenser(grenser: List<Feature>) = lagreGrenser(grenser, fylkesgrenseRepository)
    fun lagreTerritorialgrenser(grenser: List<Feature>) = lagreGrenser(grenser, territorialgrenseRepository)
    fun lagreRiksgrenser(grenser: List<Feature>) = lagreGrenser(grenser, riksgrenseRepository)
    fun lagreAvtaltAvgrensningslinjer(grenser: List<Feature>) =
        lagreGrenser(grenser, avtaltAvgrensningslinjeRepository)

    data class GrenseMedRetningOgKontekst<T : Grense>(
        val grenseMedAttributter: GrenseMedAttributter<T>,
        val kontekstType: AdministrativEnhetType,
        val kontekstId: String
    )

    @Transactional
    fun <T : Grense> lagreGrenser(grenserSomSkalLagres: List<Feature>, grenseRepository: CrudRepository<T, String>) {
        val eksisterendeGrenser = grenseRepository.findAllById(
            grenserSomSkalLagres.filter { feature -> feature.id != null }.map { it.id })

        val oppdaterteGrenser = grenserSomSkalLagres.map { feature ->
            val grense = when (val grenseId = feature.id) {
                null -> lagGrenseForType(feature)
                else -> eksisterendeGrenser.find { it.id == grenseId }!!
            }
            grense.grense = feature.geometry as LineString

            val contextProps = feature.properties.contextProperties!!
            GrenseMedRetningOgKontekst(
                GrenseMedAttributter(grense, GrenseKoblingAttributter(contextProps.directionClockwise, contextProps.order, contextProps.holeIndex)),
                contextProps.type!!,
                contextProps.id!!)
        }

        grenseRepository.saveAll(oppdaterteGrenser.map { it.grenseMedAttributter.grense })

        oppdaterteGrenser.groupBy { it.kontekstType }.forEach { entry ->
            when (entry.key) {
                AdministrativEnhetType.KOMMUNE -> oppdaterEnheter(entry.value, kommuneRepository)
                AdministrativEnhetType.FYLKE -> oppdaterEnheter(entry.value, fylkeRepository)
                AdministrativEnhetType.NASJON -> oppdaterEnheter(entry.value, nasjonRepository)
            }
        }
    }

    private fun <T : Grense, S : AdministrativEnhet> oppdaterEnheter(grenser: List<GrenseMedRetningOgKontekst<T>>, repository: CrudRepository<S, String>) {
        val enheter = repository.findAllById(grenser.map { it.kontekstId })
        repository.saveAll(
            enheter.map { enhet ->
                grenser.filter { it.kontekstId == enhet.id }.map { it.grenseMedAttributter }.forEach { grenseMedAttributter ->
                    enhet.oppdaterEllerLeggTilGrenseKobling(grenseMedAttributter.grense, grenseMedAttributter.attributter)
                }
                enhet
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Grense> lagGrenseForType(feature: Feature): T {
        return when (GrenseType.valueOf(feature.properties.type)) {
            GrenseType.Kommunegrense -> Kommunegrense()
            GrenseType.Fylkesgrense -> Fylkesgrense()
            GrenseType.Riksgrense -> Riksgrense()
            GrenseType.AvtaltAvgrensningslinje -> Avtaltavgrensningslinje()
            GrenseType.Territorialgrense -> Territorialgrense()
        } as T
    }

}

enum class GrenseType {
    Kommunegrense, Fylkesgrense, Riksgrense, AvtaltAvgrensningslinje, Territorialgrense
}

class GrenseMedAttributter<T : Grense>(val grense: T, val attributter: GrenseKoblingAttributter)

fun grenseKoblingToFeature(
    kontekstId: String? = null,
    kontekstType: AdministrativEnhetType? = null,
    grenseKobling: GrenseKobling
): Feature {
    val feature = grenseToFeature(grenseKobling.getGrense())
    return feature.copy(properties = feature.properties.copy(contextProperties = ContextProperties(
        id = kontekstId,
        type = kontekstType,
        directionClockwise = grenseKobling.grenseKoblingAttributter.retningMedKlokken,
        order = grenseKobling.grenseKoblingAttributter.rekkefoelge,
        holeIndex = grenseKobling.grenseKoblingAttributter.hullIndeks
    )))
}

fun grenseToFeature(grense: Grense): Feature {
    var featureProperties = FeatureProperties(
        type = grense.objtype!!,
        identifikasjon = Identifikasjon(grense.lokalid, grense.navnerom)
    )
    if (grense is ContainingMetadataForAdmGrenser) {
        featureProperties = featureProperties.copy(
            metadata = grense.toMetadata()
        )
    }
    return Feature(
        id = grense.id,
        properties = featureProperties,
        geometry = grense.grense!!
    )
}

fun posisjonToFeature(posisjon: Point?): Feature =
    Feature(properties = FeatureProperties(type = "Posisjon"), geometry = posisjon!!)

fun flateToFeature(flate: Polygon?): Feature =
    Feature(properties = FeatureProperties(type = "Flate"), geometry = flate!!)
