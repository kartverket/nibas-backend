package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.FylkeApi
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Fylkesnummer
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.response.FylkeRef
import no.kartverket.nibas.api.v1.response.FylkeResponse
import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.unwrap
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class FylkeController(
    private val fylkeService: FylkeService,
    private val kodelisteService: KodelisteService,
    private val historikkService: HistorikkService,
) : FylkeApi {

    override fun hentFylker(): List<FylkeRef> = fylkeService.hentAlleFylker().map { toFylkeRef(it) }

    override fun hentFylke(id: String): FylkeResponse =
        fylkeService.hentFylke(id)?.toFylkeResponse(kodelisteService) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    override fun hentGrenserForFylke(id: String): FeatureCollection {
        val grenser = fylkeService.hentGrenserForFylke(id)
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

    override fun oppdaterFylke(id: String, oppdatertFylke: FylkeRequest): FylkeResponse {
        return fylkeService.oppdaterFylke(id, oppdatertFylke)?.toFylkeResponse(kodelisteService) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    override fun hentFylkesRevisjoner(id: String): List<RevisjonRef> =
        historikkService.hentRevisjoner(id, Fylke::class.java).map { toRevisjonRef(it) }

    override fun hentFylkesRevisjon(id: String, revisjon: Int): FylkeResponse =
        historikkService.hentRevisjon(id, revisjon, Fylke::class.java).toFylkeResponse(kodelisteService)

}

@Service
class FylkeService(
    private val fylkeRepository: FylkeRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository
) {

    fun hentAlleFylker() = fylkeRepository.findAll().toList()
    fun hentFylke(id: String) = fylkeRepository.findById(id).unwrap()

    fun hentGrenserForFylke(id: String): Set<GrenseKobling> {
        return fylkeRepository.findById(id).unwrap()?.getGrenser() ?: emptySet()
    }

    fun oppdaterFylke(id: String, oppdatertFylke: FylkeRequest): Fylke? {
        return hentFylke(id)?.let { gammel ->
            gammel.samiskforvaltningsomraade = oppdatertFylke.samiskforvaltningsomraade
            gammel.fylkesnummer = oppdatertFylke.fylkesnummerId
            gammel.navnerom = oppdatertFylke.navnerom
            gammel.lokalid = oppdatertFylke.lokalid

            // Finner basert på språkkode, og oppdaterer den
            oppdatertFylke.administrativenhetnavn.forEach { navn ->
                gammel.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
                    gammeltNavn.navn = navn.navn
                    administrativenhetnavnRepository.save(gammeltNavn)
                }
            }
            fylkeRepository.save(gammel)
        }
    }
}

fun toFylkeRef(fylke: Fylke): FylkeRef = FylkeRef(
    id = fylke.id,
    navn = fylke.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!) },
    href = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(fylke.id).build().toUriString()
)

fun Fylke.toFylkeResponse(kodelisteService: KodelisteService): FylkeResponse = FylkeResponse(
    id = this.id,
    administrativenhetnavn = this.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!) },
    lokalid = this.lokalid,
    navnerom = this.navnerom,
    fylkesnummer = Fylkesnummer(
        id = this.fylkesnummer,
        kodeverdi = kodelisteService.getCodeValueByFylkesnummerId(this.fylkesnummer)?.toInt()!!
    ),
    samiskforvaltningsomraade = this.samiskforvaltningsomraade!!,
    oppdateringsdato = this.auditInfo.oppdateringsdato,
    features = FeatureCollection(features = listOf(posisjonToFeature(this.posisjon), flateToFeature(this, kodelisteService)))
)
