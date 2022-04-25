package no.kartverket.nibas.kodelister

import no.kartverket.nibas.inndelinger.GrenseType
import no.kartverket.nibas.repository.FastsettingstypeRepository
import no.kartverket.nibas.repository.GrensestatusRepository
import no.kartverket.nibas.repository.LandKodeRepository
import no.kartverket.nibas.repository.NoeyaktighetsklassekodeRepository
import no.kartverket.nibas.repository.TerrengdetaljkodeRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.logging.Logger

class DefaultKodelisteService constructor(private val kodelisteFetcher: KodelisteFetcher,
                                          private val landKodeRepository: LandKodeRepository,
                                          private val terrengdetaljkodeRepository: TerrengdetaljkodeRepository,
                                          private val noeyaktighetsklassekodeRepository: NoeyaktighetsklassekodeRepository,
                                          private val fastsettingstypeRepository: FastsettingstypeRepository,
                                          private val grensestatusRepository: GrensestatusRepository) : KodelisteService {
    private val logger: Logger = Logger.getLogger(this::class.java.name)
    private val malemetodeKodeUri = "malemetode-kode.json?"
    private val kommunenummerUri = "kommunenummer.json?"
    private val fylkesnummerUri = "fylkesnummer.json?"
    private val kodelisteByTypeMap: MutableMap<KodelisteType, Map<String, CachetKodelisteItem>> = mutableMapOf()

    @Scheduled(
        cron = "\${nibas.cron.kodeliste.invalidate}"
    )
    override fun clearCachedeKodelister() {
        logger.info("Invalidating cachede kodelister.")
        kodelisteByTypeMap.clear()
    }

    override fun getCodeValueByKommunenummerId(kommunenummerId: String): String? {
        return getKommunenummerMap()[kommunenummerId]?.item?.codevalue
    }

    override fun getCodeValueByFylkesnummerId(fylkesnummerId: String): String? {
        return getFylkesnummerMap()[fylkesnummerId]?.item?.codevalue
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

    override fun getKommunenumre(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.KOMMUNENUMMER,
            items = toResponseListe(getKommunenummerMap())
        )
    }

    override fun getFylkesnumre(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.FYLKESNUMMER,
            items = toResponseListe(getFylkesnummerMap())
        )
    }

    override fun getMaalemetodeKoder(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.MAALEMETODE_KODE,
            items = toResponseListe(getMaalemetodeKoderMap())
        )
    }

    override fun getLandkoder(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.LAND,
            items = landKodeRepository.findAll()
                .map { KodelisteItem(id = it.id, label = it.description!!) }
                .sortedBy { it.id })
    }

    override fun getTerrengdetaljkoder(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.TERRENGDETALJ,
            items = terrengdetaljkodeRepository.findAll()
                .map { KodelisteItem(id = it.id, label = it.description!!) }
                .sortedBy { it.id })
    }

    override fun getNoeyaktighetsklasser(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.NOEYAKTIGHETSKLASSE,
            items = noeyaktighetsklassekodeRepository.findAll()
                .map { KodelisteItem(id = it.id, label = it.description!!) }
                .sortedBy { it.id })
    }

    override fun getGrensetyper(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.GRENSETYPE,
            items = GrenseType.values()
                .map { KodelisteItem(id = it.value, label = it.name) }
                .sortedBy { it.id })
    }

    override fun getFastsettingstyper(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.FASTSETTINGSTYPE,
            items = fastsettingstypeRepository.findAll()
                .map { KodelisteItem(id = it.id, label = it.description!!) }
                .sortedBy { it.id })
    }

    override fun getGrensestatuskoder(): KodelisteRespons {
        return KodelisteRespons(
            type = KodelisteType.GRENSESTATUS,
            items = grensestatusRepository.findAll()
                .map { KodelisteItem(id = it.id, label = it.description!!) }
                .sortedBy { it.id })
    }

    private fun toResponseListe(itemByUuidMap: Map<String, CachetKodelisteItem>) = itemByUuidMap.values.map {
        val geonorgeKodelisteItem = it.item
        KodelisteItem(
            id = geonorgeKodelisteItem.uuid,
            label = geonorgeKodelisteItem.label
        )
    }

    private fun findMatchingUUIDFor(kodelisteType: KodelisteType, codeValue: String): String {
        val kodelisteItems: List<CachetKodelisteItem> = when (kodelisteType) {
            KodelisteType.KOMMUNENUMMER -> getKommunenummerMap().values.toList()
            KodelisteType.FYLKESNUMMER -> getFylkesnummerMap().values.toList()
            KodelisteType.MAALEMETODE_KODE -> getMaalemetodeKoderMap().values.toList()
            else -> throw RuntimeException("Ugyldig kodelistetype: $kodelisteType")
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
