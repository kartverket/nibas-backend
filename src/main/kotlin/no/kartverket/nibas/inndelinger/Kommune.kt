package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.KommuneApi
import no.kartverket.nibas.api.v1.common.AdministrativEnhetNavn
import no.kartverket.nibas.api.v1.common.KontekstEnhetType
import no.kartverket.nibas.api.v1.common.geojson.FeatureCollection
import no.kartverket.nibas.api.v1.request.KommuneRequest
import no.kartverket.nibas.api.v1.response.HistorikkRef
import no.kartverket.nibas.api.v1.response.KommuneRef
import no.kartverket.nibas.api.v1.response.KommuneResponse
import no.kartverket.nibas.api.v1.response.Kommunenummer
import no.kartverket.nibas.domain.GrenseKobling
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class KommuneController @Autowired constructor(
    private val kommuneService: KommuneService,
    private val kodelisteService: KodelisteService,
    private val historikkService: HistorikkService
) : KommuneApi {

    override fun hentAlle(fylkeId: String?): List<KommuneRef> {
        return when (fylkeId) {
            null -> kommuneService.hentAlleKommuner().map { toKommuneRef(it) }
            else -> kommuneService.hentKommunerForFylke(fylkeId).map { toKommuneRef(it) }
        }
    }

    override fun hentEn(id: String): KommuneResponse {
        return when (val kommune = kommuneService.hentKommune(id)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toKommuneResponse(kommune, kodelisteService)
        }
    }

    override fun hentGrenser(id: String): FeatureCollection {
        return when (val grenser = kommuneService.hentGrenserForKommune(id)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> FeatureCollection(
                features = grenser.sortedBy { it.grenseKoblingAttributter.rekkefoelge }
                    .map { grenseKoblingToFeature(id, KontekstEnhetType.KOMMUNE, it, kodelisteService) }
            )
        }
    }

    override fun oppdaterKommune(id: String, oppdatertKommune: KommuneRequest): KommuneResponse {
        return when (val lagret = kommuneService.oppdaterKommune(id, oppdatertKommune)) {
            null -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
            else -> toKommuneResponse(lagret, kodelisteService)
        }
    }

    override fun hentHistorikkForEn(id: String): List<HistorikkRef> =
        historikkService.hentHistorikk(id, Kommune::class.java).map { toHistorikkRef(it) }

    override fun hentHistorikkRevisjon(id: String, revision: Int): KommuneResponse {
        return toKommuneResponse(
            historikkService.hentHistorikkRevisjon(id, revision, Kommune::class.java),
            kodelisteService
        )
    }
}

@Service
class KommuneService @Autowired constructor(
    private val kommuneRepository: KommuneRepository,
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
) {

    fun hentAlleKommuner() = kommuneRepository.findAll().toList()
    fun hentKommune(id: String) = kommuneRepository.findById(id).unwrap()
    fun hentKommunerForFylke(fylkeId: String) = kommuneRepository.findByFylkeFk_Id(fylkeId)

    fun hentGrenserForKommune(id: String): Set<GrenseKobling>? {
        return kommuneRepository.findById(id).unwrap()?.let { kommune ->
            kommune.kommunegrenser +
            kommune.fylkesgrenser +
            kommune.riksgrenser +
            kommune.territorialgrenser +
            kommune.avtaltavgrensningslinjer
        }
    }

    fun oppdaterKommune(id: String, oppdatertKommune: KommuneRequest): Kommune? {
        return hentKommune(id)?.let { gammel ->
            gammel.samiskforvaltningsomraade = oppdatertKommune.samiskforvaltningsomraade
            gammel.kommunenummer = oppdatertKommune.kommunenummerId
            gammel.navnerom = oppdatertKommune.navnerom!!
            gammel.lokalid = oppdatertKommune.lokalid

            // Finner basert på språkkode, og oppdaterer den
            oppdatertKommune.administrativenhetnavn.forEach { navn ->
                gammel.administrativenhetnavn.first { it.spraak?.id == navn.spraak }.let { gammeltNavn ->
                    gammeltNavn.navn = navn.navn
                    administrativenhetnavnRepository.save(gammeltNavn)
                }
            }

            kommuneRepository.save(gammel)
        }
    }
}

fun toKommuneRef(kommune: Kommune): KommuneRef = KommuneRef(
    id = kommune.id,
    navn = kommune.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!) },
    href = ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(kommune.id).build().toUriString()
)

fun toKommuneResponse(kommune: Kommune, kodelisteService: KodelisteService): KommuneResponse = KommuneResponse(
    id = kommune.id,
    administrativenhetnavn = kommune.administrativenhetnavn.map { AdministrativEnhetNavn(it.navn!!, it.spraak?.id!!) },
    lokalid = kommune.lokalid,
    navnerom = kommune.navnerom,
    kommunenummer = createKommunenummerFromUUID(kommune.kommunenummer, kodelisteService),
    samiskforvaltningsomraade = kommune.samiskforvaltningsomraade!!,
    oppdateringsdato = kommune.oppdateringsdato!!,
    features = FeatureCollection(features = listOf(posisjonToFeature(kommune.posisjon), flateToFeature(kommune.flate)))
)

fun createKommunenummerFromUUID(kommunenrUUID: String, kodelisteService: KodelisteService) = Kommunenummer(
    id = kommunenrUUID,
    kodeverdi = kodelisteService.getCodeValueByKommunenummerId(kommunenrUUID)?.toInt()
)
