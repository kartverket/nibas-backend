package no.kartverket.nibas.kodelister

import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

interface KodelisteService {
    /**
     * Throws NoSuchElementException hvis ikke match
     */
    fun getKommunenummerIdByCodeValue(codeValue: String): String

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

    fun getGyldigeKommunenumre(): List<KodelisteItem>
    fun getAlleKommunenumre(): List<KodelisteItem>
    fun getGyldigeFylkesnumre(): List<KodelisteItem>
    fun getAlleFylkesnumre(): List<KodelisteItem>
    fun getMalemetodeKode(): List<KodelisteItem>
    fun getMalemetode(): List<KodelisteItem>
}

class DefaultKodelisteService constructor(private val kodelisteFetcher: KodelisteFetcher) : KodelisteService {

    private val malemetodeKodeUri = "malemetode-kode.json?"
    private val malemetodeUri = "malemetode.json?"
    private val kommunenummerUri = "kommunenummer.json?"
    private val kommunenummerAlleUri = "kommunenummer-alle.json?"
    private val fylkesnummerUri = "fylkesnummer.json?"
    private val fylkesnummerAlleUri = "fylkesnummer-alle.json?"

    private val kodelisteByTypeMap: MutableMap<KodelisteType, Map<String, KodelisteItem>> = mutableMapOf()

    override fun getKommunenummerIdByCodeValue(codeValue: String): String {
        return findMatchingUUIDFor(getAlleKommunenumre(), codeValue)
    }

    override fun getFylkesnummerIdByCodeValue(codeValue: String): String {
        return findMatchingUUIDFor(getAlleFylkesnumre(), codeValue)
    }

    override fun getMalemetodeIdByCodeValue(codeValue: String): String {
        return findMatchingUUIDFor(getMalemetodeKode(), codeValue)
    }

    override fun kommunenummerIdExists(kommunenummerId: String): Boolean {
        return getAlleKommunenummerMap().containsKey(kommunenummerId)
    }

    override fun fylkesnummerIdExists(fylkesnummerId: String): Boolean {
        return getAlleFylkesnummerMap().containsKey(fylkesnummerId)
    }

    private fun findMatchingUUIDFor(kodeliste: List<KodelisteItem>, codeValue: String) =
        kodeliste.first { it.item.codevalue == codeValue }.item.uuid

    override fun getGyldigeKommunenumre(): List<KodelisteItem> {
        return getKommunenummerMap().values.toList()
    }

    override fun getAlleKommunenumre(): List<KodelisteItem> {
        return getAlleKommunenummerMap().values.toList()
    }

    override fun getGyldigeFylkesnumre(): List<KodelisteItem> {
        return getFylkesnummerMap().values.toList()
    }

    override fun getAlleFylkesnumre(): List<KodelisteItem> {
        return getAlleFylkesnummerMap().values.toList()
    }

    override fun getMalemetodeKode(): List<KodelisteItem> {
        return getKodelisteItemsForType(malemetodeKodeUri, KodelisteType.MALEMETODE_KODE)
    }

    override fun getMalemetode(): List<KodelisteItem> {
        return getKodelisteItemsForType(malemetodeUri, KodelisteType.MALEMETODE)
    }

    private fun getKommunenummerMap(): Map<String, KodelisteItem> {
        return getKodelisteItemByIdMap(kommunenummerUri, KodelisteType.KOMMUNENUMMER)
    }

    private fun getAlleKommunenummerMap(): Map<String, KodelisteItem> {
        return getKodelisteItemByIdMap(kommunenummerAlleUri, KodelisteType.KOMMUNENUMMER_ALLE)
    }

    private fun getFylkesnummerMap(): Map<String, KodelisteItem> {
        return getKodelisteItemByIdMap(fylkesnummerUri, KodelisteType.FYLKESNUMMER)
    }

    private fun getAlleFylkesnummerMap(): Map<String, KodelisteItem> {
        return getKodelisteItemByIdMap(fylkesnummerAlleUri, KodelisteType.FYLKESNUMMER_ALLE)
    }

    private fun getKodelisteItemsForType(uriPostfix: String, type: KodelisteType): List<KodelisteItem> =
        getKodelisteItemByIdMap(uriPostfix, type).values.toList()

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

data class GeonorgeKodelisteItem(
    val id: String,
    val label: String,
    val lang: String,
    val uuid: String,
    val status: String,
    val description: String,
    val codevalue: String
)

data class KodelisteItem(val type: KodelisteType, val item: GeonorgeKodelisteItem)

enum class KodelisteType {
    KOMMUNENUMMER, KOMMUNENUMMER_ALLE, FYLKESNUMMER, FYLKESNUMMER_ALLE, MALEMETODE_KODE, MALEMETODE
}
