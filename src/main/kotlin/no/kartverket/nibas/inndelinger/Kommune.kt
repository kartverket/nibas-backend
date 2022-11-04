package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.KommuneApi
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.response.GrunnkretsRef
import no.kartverket.nibas.api.v1.response.KommuneRef
import no.kartverket.nibas.api.v1.response.KommuneResponse
import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.api.v1.response.StemmekretsRef
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.kodelister.createKommunenummerFromUUID
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.unwrap
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class KommuneController(
    private val kommuneService: KommuneService,
    private val kodelisteService: KodelisteService,
    private val historikkService: HistorikkService
) : KommuneApi {

    override fun hentKommuner(fylkeId: String?): List<KommuneRef> {
        return when (fylkeId) {
            null -> kommuneService.hentAlleKommuner().map { toKommuneRef(it) }
            else -> kommuneService.hentKommunerForFylke(fylkeId).map { toKommuneRef(it) }
        }
    }

    override fun hentKommune(id: String): KommuneResponse =
        kommuneService.hentKommune(id)?.toKommuneResponse(kodelisteService)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    override fun hentGrenserForKommune(id: String): FeatureCollection {
        val grenser = kommuneService.hentGrenserForKommune(id)
        if (grenser.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return FeatureCollection(
            features = grenser
                // TODO: NIBAS-473 Hvordan sortere grensene nå?
//                .sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                .map { grenseKoblingToFeature(it, kodelisteService) }
        )
    }

    override fun hentKommuneRevisjoner(id: String): List<RevisjonRef> =
        historikkService.hentRevisjoner(id, Kommune::class.java).map { toRevisjonRef(it) }

    override fun hentKommuneRevisjon(id: String, revisjon: Int): KommuneResponse =
        historikkService.hentRevisjon(id, revisjon, Kommune::class.java).toKommuneResponse(kodelisteService)


    override fun hentKommunesGrunnkretser(id: String): List<GrunnkretsRef> {
        return kommuneService.hentKommunesGrunnkretser(id).map { toGrunnkretsRef(it) }
    }

    override fun hentKommunesStemmekretser(id: String): List<StemmekretsRef> {
        return kommuneService.hentKommunesStemmekretser(id).map { toStemmekretsRef(it) }
    }
}

@Service
class KommuneService(
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
) {

    fun hentAlleKommuner(): List<Kommune> = kommuneRepository.findAll().toList()
    fun hentKommune(id: String): Kommune? = kommuneRepository.findById(id).unwrap()

    fun hentKommuneByKommunenummer(kommunenummerUuid: String): Kommune? = kommuneRepository.findByKommunenummer(kommunenummerUuid)

    fun hentKommunerForFylke(fylkeId: String): List<Kommune> = fylkeRepository.findById(fylkeId).unwrap()!!.kommuner.toList()

    fun hentGrenserForKommune(id: String): Set<GrenseKobling> {
        return kommuneRepository.findById(id).unwrap()?.getGrenser() ?: emptySet()
    }

    fun oppdaterKommune(id: String, oppdatertKommune: KommuneRequest): Kommune? {
        return hentKommune(id)?.let { gammel ->
            verifiserOptimistiskLaas(oppdatertKommune.version, gammel)?.let { throw OptimistiskLaasException(it) }

            gammel.samiskforvaltningsomraade = oppdatertKommune.samiskforvaltningsomraade
            gammel.kommunenummer = oppdatertKommune.kommunenummerId

            // Finner basert på språkkode, og oppdaterer den
            oppdatertKommune.administrativenhetnavn.forEach { navn ->
                gammel.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
                    verifiserOptimistiskLaas(navn.version, gammeltNavn)?.let { throw OptimistiskLaasException(it) }
                    gammeltNavn.navn = navn.navn
                    administrativenhetnavnRepository.save(gammeltNavn)
                }
            }

            kommuneRepository.save(gammel)
        }
    }

    fun hentKommunesGrunnkretser(id: String): List<Grunnkrets> {
        return hentKommune(id)?.grunnkretser?.toList() ?: emptyList()
    }

    fun hentKommunesStemmekretser(id: String): List<Stemmekrets> {
        return hentKommune(id)?.stemmekretser?.toList() ?: emptyList()
    }
}

fun toKommuneRef(kommune: Kommune): KommuneRef = KommuneRef(
    id = kommune.id,
    navn = kommune.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version) },
    href = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(kommune.id).build().toUriString()
)

fun Kommune.toKommuneResponse(kodelisteService: KodelisteService): KommuneResponse = KommuneResponse(
    id = this.id,
    administrativenhetnavn = this.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version) },
    lokalid = this.lokalid,
    kommunenummer = kodelisteService.createKommunenummerFromUUID(this.kommunenummer),
    samiskforvaltningsomraade = this.samiskforvaltningsomraade!!,
    oppdateringsdato = this.auditInfo.oppdateringsdato,
    features = FeatureCollection(features = listOf(posisjonToFeature(this.posisjon, this.version), flateToFeature(this, kodelisteService, this.version))),
    version = this.version
)
