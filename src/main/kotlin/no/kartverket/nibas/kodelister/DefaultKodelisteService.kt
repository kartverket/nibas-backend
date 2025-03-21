package no.kartverket.nibas.kodelister

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.logging.Logger

@Transactional(readOnly = true)
class DefaultKodelisteService(private val kodelisteFetcher: KodelisteFetcher) : KodelisteService {
    private val logger: Logger = Logger.getLogger(this::class.java.name)
    private val malemetodeKodeUri = "malemetode-kode.json?"
    private val kommunenummerUri = "inndelinger/inndelingsbase/kommunenummer.json?"
    private val fylkesnummerUri = "inndelinger/inndelingsbase/fylkesnummer.json?"
    private val kodelisteByTypeMap: MutableMap<KodelisteType, Map<String, CachetKodelisteItem>> = mutableMapOf()

    @Transactional(readOnly = false)
    @Scheduled(
        cron = "\${nibas.cron.kodeliste.invalidate}"
    )
    override fun clearCachedeKodelister() {
        logger.info("Invalidating cachede kodelister.")
        kodelisteByTypeMap.clear()
        //Henter kodelista på nytt, for å spare tid ved første hent av kodeliste
        getMaalemetodeKoderMap()
    }

    override fun getKommunenummerIdByCodeValue(codeValue: String): String {
        return findMatchingUUIDFor(KodelisteType.KOMMUNENUMMER, codeValue)
    }

    override fun getFylkesnummerIdByCodeValue(codeValue: String): String {
        return findMatchingUUIDFor(KodelisteType.FYLKESNUMMER, codeValue)
    }

    override fun getMalemetodeIdByCodeValue(codeValue: String): String {
        return findMatchingUUIDFor(KodelisteType.MAALEMETODE_KODE, codeValue)
    }

    override fun getMaalemetodeKoder(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.MAALEMETODE_KODE,
            items = toResponseListe(getMaalemetodeKoderMap())
        )
    }

    private fun toResponseListe(itemByUuidMap: Map<String, CachetKodelisteItem>) = itemByUuidMap.values.map {
        val geonorgeKodelisteItem = it.item
        KodelisteItem(
            id = geonorgeKodelisteItem.uuid,
            kode = geonorgeKodelisteItem.codevalue,
            label = geonorgeKodelisteItem.label
        )
    }

    private fun findMatchingUUIDFor(kodelisteType: KodelisteType, codeValue: String): String {
        val kodelisteItems: List<CachetKodelisteItem> = when (kodelisteType) {
            KodelisteType.KOMMUNENUMMER -> getKommunenummerMap().values.toList()
            KodelisteType.FYLKESNUMMER -> getFylkesnummerMap().values.toList()
            KodelisteType.MAALEMETODE_KODE -> getMaalemetodeKoderMap().values.toList()
        }
        return kodelisteItems.first { it.item.codevalue == codeValue }.item.uuid
    }

    private fun getKommunenummerMap(): Map<String, CachetKodelisteItem> {
        return getKodelisteItemByIdMap(kommunenummerUri, KodelisteType.KOMMUNENUMMER)
    }

    private fun getFylkesnummerMap(): Map<String, CachetKodelisteItem> {
        return getKodelisteItemByIdMap(fylkesnummerUri, KodelisteType.FYLKESNUMMER)
    }

    private fun getMaalemetodeKoderMap(): Map<String, CachetKodelisteItem> {
        return getKodelisteItemByIdMap(malemetodeKodeUri, KodelisteType.MAALEMETODE_KODE)
    }

    private fun getKodelisteItemByIdMap(uriPostfix: String, type: KodelisteType): Map<String, CachetKodelisteItem> {
        if (!kodelisteByTypeMap.containsKey(type)) {
            kodelisteByTypeMap[type] = kodelisteFetcher.fetchForKodeliste("/api/sosi-kodelister/$uriPostfix")
                .associate { item ->
                    item.uuid to CachetKodelisteItem(type, item)
                }
        }
        return kodelisteByTypeMap[type]!!
    }
}

open class KodelisteFetcher(private val webClient: WebClient) {

    private val blockMillis: Long = 5_000

    open fun fetchForKodeliste(uri: String): List<GeonorgeKodelisteItem> {
        return (webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(GeonorgeKodelisteResponse::class.java)
            .block(Duration.ofMillis(blockMillis))
            ?: throw RuntimeException("Fetch for $uri failed.")).containeditems
    }
}

class MissingKodelisteItemException(val kodelisteType: KodelisteType, val id: String)
    : RuntimeException("Kodeliste-innslag for: $id finnes ikke for Kodeliste: $kodelisteType")

