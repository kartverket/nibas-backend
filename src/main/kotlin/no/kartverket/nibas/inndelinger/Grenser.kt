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
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import kotlin.reflect.KClass

@RestController
class GrenseController @Autowired constructor(
    private val grenseService: GrenseService,
    private val historikkService: HistorikkService
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
    private val riksgrenseRepository: RiksgrenseRepository,
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository
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

    fun lagreKommunegrenser(grenser: List<Feature>) = lagreGrenser(grenser, kommunegrenseRepository)
    fun lagreFylkesgrenser(grenser: List<Feature>) = lagreGrenser(grenser, fylkesgrenseRepository)
    fun lagreTerritorialgrenser(grenser: List<Feature>) = lagreGrenser(grenser, territorialgrenseRepository)
    fun lagreRiksgrenser(grenser: List<Feature>) = lagreGrenser(grenser, riksgrenseRepository)
    fun lagreAvtaltAvgrensningslinjer(grenser: List<Feature>) =
        lagreGrenser(grenser, avtaltAvgrensningslinjeRepository)

    @Transactional
    fun <T : Grense> lagreGrenser(grenser: List<Feature>, grenseRepository: CrudRepository<T, Int>, ) {
        val oppdaterteFeatures = grenser.filter{feature -> feature.properties.id != null}
        oppdaterGrenser(grenseRepository, oppdaterteFeatures)

        val nyeFeatures = grenser - oppdaterteFeatures
        if (!nyeFeatures.isEmpty()) {

            //Opprett nye grenser av gitt type for en gitt administrativ enhet (basert på konteksttype)
            nyeFeatures.groupBy { it.properties.kontekstType }.forEach { entry ->
                when(entry.key) {
                    AdministrativEnhetType.KOMMUNE -> opprettNyeGrenser(entry.value, grenseRepository, kommuneRepository)
                    AdministrativEnhetType.FYLKE -> opprettNyeGrenser(entry.value, grenseRepository, fylkeRepository)
                    AdministrativEnhetType.NASJON -> opprettNyeGrenser(entry.value, grenseRepository, nasjonRepository)
                }
            }

        }
    }

    private fun <T : Grense> oppdaterGrenser(
        grenseRepository: CrudRepository<T, Int>,
        oppdaterteFeatures: List<Feature>
    ) {
        val eksisterendeGrenser = grenseRepository.findAllById(oppdaterteFeatures.map { it.properties.id })
        val oppdaterte = eksisterendeGrenser.map { gammel ->
            val ny = oppdaterteFeatures.find { it.properties.id == gammel.id }
            gammel.grense = ny?.geometry as LineString
            gammel.navnerom = ny.properties.navnerom
            gammel.lokalid = ny.properties.lokalid
            gammel
        }
        grenseRepository.saveAll(oppdaterte)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Grense, E : AdministrativEnhet> opprettNyeGrenser(nyeGrenser: List<Feature>, grenseRepository: CrudRepository<T, Int>, administrativEnhetRepository: CrudRepository<E, Int>) {
        val enheterMedNyeGrenser = administrativEnhetRepository.findAllById(nyeGrenser.map{it.properties.kontekstId})
        val enheterSomSkalEndres = enheterMedNyeGrenser.map { enhet ->
            val nyeGrenseObjekterForEnhet = nyeGrenser.filter { it.properties.kontekstId == enhet.id }.map { feature ->

                val grense = lagGrenseForType<T>(feature)

                grense.grense = feature.geometry as LineString
                grense.objtype = feature.properties.type
                grense.navnerom = feature.properties.navnerom
                grense
            }
            grenseRepository.saveAll(nyeGrenseObjekterForEnhet)

            // Alle grenser er av samme type, henter bare ut den første for å sjekke typen
            when (GrenseType.valueOf(nyeGrenseObjekterForEnhet[0].objtype!!)) {
                GrenseType.Kommunegrense ->
                    when (enhet) {
                        is Kommune -> enhet.kommunegrenser.addAll(nyeGrenseObjekterForEnhet as List<Kommunegrense>)
                        else -> throw RuntimeException("Kan ikke legge til kommunegrenser på valgt enhet ${enhet.objtype}")
                    }
                GrenseType.Fylkesgrense ->
                    when (enhet) {
                        is Kommune -> enhet.fylkesgrenser.addAll(nyeGrenseObjekterForEnhet as List<Fylkesgrense>)
                        is Fylke -> enhet.fylkesgrenser.addAll(nyeGrenseObjekterForEnhet as List<Fylkesgrense>)
                        else -> throw RuntimeException("Kan ikke legge til fylkesgrenser på valgt enhet ${enhet.objtype}")
                    }
                GrenseType.Riksgrense -> enhet.riksgrenser.addAll(nyeGrenseObjekterForEnhet as List<Riksgrense>)
                GrenseType.AvtaltAvgrensningslinje -> enhet.avtaltavgrensningslinjer.addAll(nyeGrenseObjekterForEnhet as List<Avtaltavgrensningslinje>)
                GrenseType.Territorialgrense -> enhet.territorialgrenser.addAll(nyeGrenseObjekterForEnhet as List<Territorialgrense>)
            }

            enhet
        }

        administrativEnhetRepository.saveAll(enheterSomSkalEndres)
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