package no.kartverket.nibas.kodelister

import io.swagger.v3.oas.annotations.media.Schema
import no.kartverket.nibas.api.v1.KodelisteApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.logging.Logger

@RestController
class KodelisteController @Autowired constructor(private val kodelisteService: KodelisteService) : KodelisteApi {

    override fun fetchKommunenumre(): List<KodelisteItem> {
        return kodelisteService.getKommunenumre()
    }

    override fun fetchFylkesnumre(): List<KodelisteItem> {
        return kodelisteService.getFylkesnumre()
    }

    override fun fetchMaalemetodeKoder(): List<KodelisteItem> {
        return kodelisteService.getMaalemetodeKoder()
    }

}

interface KodelisteService {
    /**
     * Throws NoSuchElementException hvis ikke match
     */
    fun getKommunenummerIdByCodeValue(codeValue: String): String

    fun getCodeValueByKommunenummerId(kommunenummerId: String): String?

    fun getCodeValueByFylkesnummerId(fylkesnummerId: String): String?

    /**
     * Throws NoSuchElementException hvis ikke match
     */
    fun getFylkesnummerIdByCodeValue(codeValue: String): String

    /**
     * Throws NoSuchElementException hvis ikke match
     */
    fun getMalemetodeIdByCodeValue(codeValue: String): String

    /**
     * Returnerer true hvis gitt kommunenummerId finnes i eksternt forvaltet kodeliste.
     */
    fun kommunenummerIdExists(kommunenummerId: String): Boolean

    /**
     * Returnerer true hvis gitt fylkesnummerId finnes i eksternt forvaltet kodeliste.
     */
    fun fylkesnummerIdExists(fylkesnummerId: String): Boolean

    fun getKommunenumre(): List<KodelisteItem>
    fun getFylkesnumre(): List<KodelisteItem>
    fun getMaalemetodeKoder(): List<KodelisteItem>

    /**
     * Skedulert metode som automatisk invaliderer/tømmer kodelistene ved jevnt konfigurerbart mellomrom.
     *
     * Tømmer kodelistene.
     */
    fun clearKodelister()
}

class DefaultKodelisteService constructor(private val kodelisteFetcher: KodelisteFetcher) : KodelisteService {

    private val logger: Logger = Logger.getLogger(this::class.java.name)
    private val malemetodeKodeUri = "malemetode-kode.json?"
    private val kommunenummerUri = "kommunenummer.json?"
    private val fylkesnummerUri = "fylkesnummer.json?"
    private val kodelisteByTypeMap: MutableMap<KodelisteType, Map<String, KodelisteItem>> = mutableMapOf()

    @Scheduled(
        cron = "\${nibas.cron.kodeliste.invalidate}"
    )
    override fun clearKodelister() {
        logger.info("Invalidating kodelister.")
        kodelisteByTypeMap.clear()
    }

    override fun getKommunenummerIdByCodeValue(codeValue: String): String {
        return findMatchingUUIDFor(getKommunenumre(), codeValue)
    }

    override fun getCodeValueByKommunenummerId(kommunenummerId: String): String? {
        return getKommunenummerMap()[kommunenummerId]?.item?.codevalue
    }

    override fun getCodeValueByFylkesnummerId(fylkesnummerId: String): String? {
        return getFylkesnummerMap()[fylkesnummerId]?.item?.codevalue
    }

    override fun getFylkesnummerIdByCodeValue(codeValue: String): String {
        return findMatchingUUIDFor(getFylkesnumre(), codeValue)
    }

    override fun getMalemetodeIdByCodeValue(codeValue: String): String {
        return findMatchingUUIDFor(getMaalemetodeKoder(), codeValue)
    }

    override fun kommunenummerIdExists(kommunenummerId: String): Boolean {
        return getKommunenummerMap().containsKey(kommunenummerId)
    }

    override fun fylkesnummerIdExists(fylkesnummerId: String): Boolean {
        return getFylkesnummerMap().containsKey(fylkesnummerId)
    }

    override fun getKommunenumre(): List<KodelisteItem> {
        return toList(getKommunenummerMap())
    }

    override fun getFylkesnumre(): List<KodelisteItem> {
        return toList(getFylkesnummerMap())
    }

    override fun getMaalemetodeKoder(): List<KodelisteItem> {
        return toList(getMaalemetodeKoderMap())
    }

    private fun findMatchingUUIDFor(kodeliste: List<KodelisteItem>, codeValue: String) =
        kodeliste.first { it.item.codevalue == codeValue }.item.uuid

    private fun getKommunenummerMap(): Map<String, KodelisteItem> {
        return getKodelisteItemByIdMap(kommunenummerUri, KodelisteType.KOMMUNENUMMER)
    }

    private fun getFylkesnummerMap(): Map<String, KodelisteItem> {
        return getKodelisteItemByIdMap(fylkesnummerUri, KodelisteType.FYLKESNUMMER)
    }

    private fun getMaalemetodeKoderMap(): Map<String, KodelisteItem> {
        return getKodelisteItemByIdMap(malemetodeKodeUri, KodelisteType.MAALEMETODE_KODE)
    }

    private fun toList(map: Map<String, KodelisteItem>): List<KodelisteItem> = map.values.toList()

    private fun getKodelisteItemByIdMap(uriPostfix: String, type: KodelisteType): Map<String, KodelisteItem> {
        if (!kodelisteByTypeMap.containsKey(type)) {
            kodelisteByTypeMap[type] =
                kodelisteFetcher.fetchForKodeliste("/api/sosi-kodelister/$uriPostfix").associate { item ->
                    item.uuid to KodelisteItem(type, item)
                }
        }
        return kodelisteByTypeMap[type]!!
    }

}

open class KodelisteFetcher(private val webClient: WebClient) {
    open fun fetchForKodeliste(uri: String): List<GeonorgeKodelisteItem> {
        return (webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(GeonorgeKodelisteResponse::class.java)
            .block(Duration.ofMillis(5_000))
            ?: throw RuntimeException("Fetch for $uri failed.")).containeditems
    }
}

data class GeonorgeKodelisteResponse(
    val status: String, val containeditems: List<GeonorgeKodelisteItem>
)

// API-typer

data class KodelisteItem(
    @Schema(description = "Angir typen av eksternt forvaltet kodeliste.")
    val type: KodelisteType,
    @Schema(description = "Inneholder kodelisteinnslaget fra Geonorge.")
    val item: GeonorgeKodelisteItem
)

data class GeonorgeKodelisteItem(
    @Schema(description = "Id er en URL til kodelisteinnslaget hos Geonorge.")
    val id: String,
    @Schema(description = "Kompakt tekst for framvisning.")
    val label: String,
    @Schema(description = "Angir språket, no for norsk.")
    val lang: String,
    @Schema(description = "Unik id.")
    val uuid: String,
    @Schema(description = "Beskriver om kodelisteinnslaget er gyldig.")
    val status: String,
    @Schema(description = "Beskrivelse av innslaget.")
    val description: String,
    @Schema(description = "Numerisk kode.")
    val codevalue: String
)

enum class KodelisteType {
    @Schema(description = "Kodeliste for gyldige kommunenumre.")
    KOMMUNENUMMER,

    @Schema(description = "Kodeliste for gyldige fylkesnumre.")
    FYLKESNUMMER,

    @Schema(description = "Kodeliste for målemetode-koder.")
    MAALEMETODE_KODE,
}
