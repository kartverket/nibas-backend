package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.GrenseApi
import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.common.geojson.FeatureProperties
import no.kartverket.nibas.api.v1.response.HistorikkRef
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.unwrap
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import kotlin.reflect.KClass

@RestController
class GrenseController @Autowired constructor(
    private val grenseService: GrenseService,
    private val historikkService: HistorikkService
) : GrenseApi {

    override fun oppdaterGrenser(grenser: FeatureCollection) {
        grenser.features.groupBy { it.properties.type }.forEach { entry ->
            when (GrenseType.valueOf(entry.key)) {
                GrenseType.Kommunegrense -> grenseService.oppdaterKommunegrenser(entry.value)
                GrenseType.Fylkesgrense -> grenseService.oppdaterFylkesgrenser(entry.value)
                GrenseType.Riksgrense -> grenseService.oppdaterRiksgrenser(entry.value)
                GrenseType.AvtaltAvgrensningslinje -> grenseService.oppdaterAvtaltAvgrensningslinjer(entry.value)
                GrenseType.Territorialgrense -> grenseService.oppdaterTerritorialgrenser(entry.value)
            }
        }
    }

    override fun hentKommunegrense(id: Int): Feature = hentGrense(id, Kommunegrense::class)
    override fun hentFylkesgrense(id: Int): Feature = hentGrense(id, Fylkesgrense::class)
    override fun hentRiksgrense(id: Int): Feature = hentGrense(id, Riksgrense::class)
    override fun hentTerritorialsgrense(id: Int): Feature = hentGrense(id, Territorialgrense::class)
    override fun hentAvtaltavgrensningslinje(id: Int): Feature = hentGrense(id, Avtaltavgrensningslinje::class)
    private fun <T : Grense> hentGrense(id: Int, type: KClass<T>): Feature {
        return when (val grense = grenseService.hentGrense(id, type)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> grenseToFeature(grense)
        }
    }

    override fun hentKommunegrenseHistorikk(id: Int) = hentGrenseHistorikk(id, Kommunegrense::class.java)
    override fun hentFylkesgrenseHistorikk(id: Int) = hentGrenseHistorikk(id, Fylkesgrense::class.java)
    override fun hentRiksgrenseHistorikk(id: Int) = hentGrenseHistorikk(id, Riksgrense::class.java)
    override fun hentTerritorialgrenseHistorikk(id: Int) = hentGrenseHistorikk(id, Territorialgrense::class.java)
    override fun hentAvtaltavgrensningslinjeHistorikk(id: Int) =
        hentGrenseHistorikk(id, Avtaltavgrensningslinje::class.java)

    private fun <T> hentGrenseHistorikk(id: Int, type: Class<T>): List<HistorikkRef> {
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

    override fun hentKommunegrenseRevisjon(id: Int, revision: Int) =
        hentGrenseRevisjon(id, revision, Kommunegrense::class.java)

    override fun hentFylkesgrenseRevisjon(id: Int, revision: Int): Feature =
        hentGrenseRevisjon(id, revision, Fylkesgrense::class.java)

    override fun hentRiksgrenseRevisjon(id: Int, revision: Int): Feature =
        hentGrenseRevisjon(id, revision, Riksgrense::class.java)

    override fun hentAvtaltavgrensningslinjeRevisjon(id: Int, revision: Int): Feature =
        hentGrenseRevisjon(id, revision, Avtaltavgrensningslinje::class.java)

    override fun hentTerritorialgrenseRevisjon(id: Int, revision: Int): Feature =
        hentGrenseRevisjon(id, revision, Territorialgrense::class.java)

    private fun <T : Grense> hentGrenseRevisjon(id: Int, revision: Int, type: Class<T>): Feature =
        grenseToFeature(historikkService.hentHistorikkRevisjon(id, revision, type))

}

@Service
class GrenseService @Autowired constructor(
    private val kommunegrenseRepository: KommunegrenseRepository,
    private val fylkesgrenseRepository: FylkesgrenseRepository,
    private val territorialgrenseRepository: TerritorialgrenseRepository,
    private val avtaltAvgrensningslinjeRepository: AvtaltAvgrensningslinjeRepository,
    private val riksgrenseRepository: RiksgrenseRepository
) {

    @Suppress("UNCHECKED_CAST")
    fun <T : Grense> hentGrense(id: Int, kClass: KClass<T>): T? {
        return when (kClass) {
            Kommunegrense::class -> kommunegrenseRepository
            Fylkesgrense::class -> fylkesgrenseRepository
            Territorialgrense::class -> territorialgrenseRepository
            Avtaltavgrensningslinje::class -> avtaltAvgrensningslinjeRepository
            Riksgrense::class -> riksgrenseRepository
            else -> throw RuntimeException("Ukjent grensetype")
        }.findById(id).unwrap() as T
    }

    fun oppdaterKommunegrenser(grenser: List<Feature>) = oppdaterGrenser(grenser, kommunegrenseRepository)
    fun oppdaterFylkesgrenser(grenser: List<Feature>) = oppdaterGrenser(grenser, fylkesgrenseRepository)
    fun oppdaterTerritorialgrenser(grenser: List<Feature>) = oppdaterGrenser(grenser, territorialgrenseRepository)
    fun oppdaterRiksgrenser(grenser: List<Feature>) = oppdaterGrenser(grenser, riksgrenseRepository)
    fun oppdaterAvtaltAvgrensningslinjer(grenser: List<Feature>) =
        oppdaterGrenser(grenser, avtaltAvgrensningslinjeRepository)

    fun <T : Grense> oppdaterGrenser(grenser: List<Feature>, repository: CrudRepository<T, Int>) {
        val eksisterende = repository.findAllById(grenser.map { it.properties.id })
        val oppdaterte = eksisterende.map { gammel ->
            val ny = grenser.find { it.properties.id == gammel.id }
            gammel.grense = ny?.geometry as LineString
            gammel.lokalid = ny.properties.lokalid
            gammel.navnerom = ny.properties.navnerom
            gammel
        }
        repository.saveAll(oppdaterte)
    }
}

enum class GrenseType {
    Kommunegrense, Fylkesgrense, Riksgrense, AvtaltAvgrensningslinje, Territorialgrense
}

fun grenseToFeature(grense: Grense, kontekstId: Int? = null, kontekstType: AdministrativEnhetType? = null): Feature {
    return Feature(
        properties = FeatureProperties(
            type = grense.objtype!!,
            id = grense.id!!,
            lokalid = grense.lokalid,
            navnerom = grense.navnerom,
            kontekstId = kontekstId,
            kontekstType = kontekstType
        ),
        geometry = grense.grense!!
    )
}

fun posisjonToFeature(posisjon: Point?): Feature =
    Feature(properties = FeatureProperties(type = "Posisjon"), geometry = posisjon!!)

fun flateToFeature(flate: Polygon?): Feature =
    Feature(properties = FeatureProperties(type = "Flate"), geometry = flate!!)