package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.FylkeApi
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.Fylkesnummer
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.FylkeRequest
import no.kartverket.nibas.api.v1.response.FylkeRef
import no.kartverket.nibas.api.v1.response.FylkeResponse
import no.kartverket.nibas.api.v1.response.HistorikkRef
import no.kartverket.nibas.domain.Fylke
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class FylkeController @Autowired constructor(
    private val fylkeService: FylkeService,
    private val kodelisteService: KodelisteService,
    private val historikkService: HistorikkService
) : FylkeApi {

    override fun hentAlle(): List<FylkeRef> = fylkeService.hentAlleFylker().map { toFylkeRef(it) }

    override fun hentEn(id: String): FylkeResponse {
        return when (val fylke = fylkeService.hentFylke(id)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toFylkeResponse(fylke, kodelisteService)
        }
    }

    override fun hentGrenser(id: String): FeatureCollection {
        return when (val grenser = fylkeService.hentGrenserForFylke(id)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> FeatureCollection(
                features = grenser.sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                    .map { grenseKoblingToFeature(id, KontekstEnhetType.FYLKE, it, kodelisteService) }
            )
        }
    }

    override fun oppdaterFylke(id: String, oppdatertFylke: FylkeRequest): FylkeResponse {
        return when (val lagret = fylkeService.oppdaterFylke(id, oppdatertFylke)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toFylkeResponse(lagret, kodelisteService)
        }
    }

    override fun hentHistorikkForEn(id: String): List<HistorikkRef> =
        historikkService.hentHistorikk(id, Fylke::class.java).map { toHistorikkRef(it) }

    override fun hentHistorikkRevisjon(id: String, revision: Int): FylkeResponse {
        return toFylkeResponse(
            historikkService.hentHistorikkRevisjon(id, revision, Fylke::class.java),
            kodelisteService
        )
    }
}

@Service
class FylkeService @Autowired constructor(
    private val fylkeRepository: FylkeRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository
) {

    fun hentAlleFylker() = fylkeRepository.findAll().toList()
    fun hentFylke(id: String) = fylkeRepository.findById(id).unwrap()

    fun hentGrenserForFylke(id: String): Set<GrenseKobling>? {
        return fylkeRepository.findById(id).unwrap()?.let { fylke ->
            fylke.fylkesgrenser +
                fylke.riksgrenser +
                fylke.territorialgrenser +
                fylke.avtaltavgrensningslinjer
        }
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

fun toFylkeResponse(fylke: Fylke, kodelisteService: KodelisteService): FylkeResponse = FylkeResponse(
    id = fylke.id,
    administrativenhetnavn = fylke.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!) },
    lokalid = fylke.lokalid,
    navnerom = fylke.navnerom,
    fylkesnummer = Fylkesnummer(
        id = fylke.fylkesnummer,
        kodeverdi = kodelisteService.getCodeValueByFylkesnummerId(fylke.fylkesnummer)?.toInt()!!
    ),
    samiskforvaltningsomraade = fylke.samiskforvaltningsomraade!!,
    oppdateringsdato = fylke.auditInfo.oppdateringsdato,
    features = FeatureCollection(features = listOf(posisjonToFeature(fylke.posisjon), flateToFeature(fylke.flate)))
)
