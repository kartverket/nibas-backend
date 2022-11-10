package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.NasjonApi
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.NasjonRequest
import no.kartverket.nibas.api.v1.response.NasjonResponse
import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Nasjon
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.OptimistiskLaasException
import no.kartverket.nibas.repository.verifiserOptimistiskLaas
import no.kartverket.nibas.unwrap
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import no.kartverket.nibas.api.v1.response.NasjonRef as NasjonRefResponse

@RestController
class NasjonController(
    private val nasjonService: NasjonService,
    private val historikkService: HistorikkService,
    private val kodelisteService: KodelisteService,
) : NasjonApi {

    override fun hentNasjoner(): List<NasjonRefResponse> = nasjonService.hentAlleNasjoner().map { toNasjonRef(it) }

    override fun hentNasjon(id: String): NasjonResponse =
        nasjonService.hentNasjon(id)?.toNasjonResponse(kodelisteService) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    override fun hentGrenserForNasjon(id: String): FeatureCollection {
        val grenser = nasjonService.hentGrenserForNasjon(id)
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

    override fun hentNasjonRevisjoner(id: String): List<RevisjonRef> =
        historikkService.hentRevisjoner(id, Nasjon::class.java).map { toRevisjonRef(it) }

    override fun hentNasjonRevisjon(id: String, revisjon: Int): NasjonResponse =
        historikkService.hentRevisjon(id, revisjon, Nasjon::class.java).toNasjonResponse(kodelisteService)

}

@Transactional(readOnly = true)
@Service
class NasjonService(
    private val nasjonRepository: NasjonRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository
) {

    fun hentAlleNasjoner() = nasjonRepository.findAll().toList()
    fun hentNasjon(id: String) = nasjonRepository.findById(id).unwrap()

    fun hentGrenserForNasjon(id: String): Set<GrenseKobling> {
        return nasjonRepository.findById(id).unwrap()?.getGrenser() ?: emptySet()
    }

    @Transactional(readOnly = false)
    fun oppdaterNasjon(id: String, oppdatertNasjon: NasjonRequest): Nasjon? {
        return hentNasjon(id)?.let { gammel ->
            verifiserOptimistiskLaas(oppdatertNasjon.version, gammel)?.let { throw OptimistiskLaasException(it) }

            // Finner basert på språkkode, og oppdaterer den
            oppdatertNasjon.administrativenhetnavn.forEach { navn ->
                gammel.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
                    verifiserOptimistiskLaas(navn.version, gammeltNavn)?.let { throw OptimistiskLaasException(it) }
                    gammeltNavn.navn = navn.navn
                    administrativenhetnavnRepository.save(gammeltNavn)
                }
            }
            nasjonRepository.save(gammel)
        }
    }
}

fun toNasjonRef(nasjon: Nasjon): NasjonRefResponse = NasjonRefResponse(
    id = nasjon.id,
    navn = nasjon.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version) },
    href = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(nasjon.id).build().toUriString()
)

fun Nasjon.toNasjonResponse(kodelisteService: KodelisteService): NasjonResponse = NasjonResponse(
    id = this.id,
    administrativenhetnavn = this.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!, it.version) },
    lokalid = this.lokalid,
    features = FeatureCollection(features = listOf(posisjonToFeature(this.posisjon, this.version), flateToFeature(this, kodelisteService, this.version))),
    version = this.version
)
