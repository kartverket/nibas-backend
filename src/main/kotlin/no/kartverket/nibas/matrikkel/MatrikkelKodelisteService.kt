package no.kartverket.nibas.matrikkel

import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.MatrikkelBubbleObject
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.geometri.koder.MalemetodeKode
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.kodeliste.MatrikkelEnumKode
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.koder.AdministrativGrenseKode
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.koder.HjelpelinjetypeKode
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.koder.TerrengdetaljKode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

@Transactional(readOnly = true)
@Service
class MatrikkelKodelisteService @Autowired constructor(
    private val environment: Environment
) {
    private val logger: Logger = Logger.getLogger(this::class.java.name)
    private val cachedeMaalemetodeKoder: AtomicReference<List<KodelisteEntryDTO>> = AtomicReference()
    private val cachedeHjelpelinjetypeKoder: AtomicReference<List<KodelisteEntryDTO>> = AtomicReference()
    private val cachedeAdministrativGrenseKoder: AtomicReference<List<KodelisteEntryDTO>> = AtomicReference()
    private val cachedeTerrengdetaljKoder: AtomicReference<List<KodelisteEntryDTO>> = AtomicReference()

    @Transactional(readOnly = false)
    @Scheduled(
        cron = "\${nibas.cron.matrikkelkodeliste.invalidate}"
    )
    fun hentKodelisterFraMatrikkelen() {
        logger.info("Oppfrisker cachede matrikkel-kodelister.")

        val matrikkelenUrl = environment.getRequiredProperty("MATRIKKELEN_WFS_URL")
        val matrikkelenUsername = environment.getRequiredProperty("MATRIKKELEN_USERNAME")
        val matrikkelenPassword = environment.getRequiredProperty("MATRIKKELEN_PASSWORD")

        val matrikkelApi = MatrikkelApi(
            URI(matrikkelenUrl),
        ).withAuth(
            username = matrikkelenUsername,
            password = matrikkelenPassword
        )
        val kodelisteobjekter = matrikkelApi.kodelisteService().getKodelisterEnkel(matrikkelApi.matrikkelContext).bubbleObjects.item

        cachedeHjelpelinjetypeKoder.getAndSet(mapKodeliste(kodelisteobjekter.avType<HjelpelinjetypeKode>()))
        cachedeMaalemetodeKoder.getAndSet(mapKodeliste(kodelisteobjekter.avType<MalemetodeKode>()))
        cachedeAdministrativGrenseKoder.getAndSet(mapKodeliste(kodelisteobjekter.avType<AdministrativGrenseKode>()))
        cachedeTerrengdetaljKoder.getAndSet(mapKodeliste(kodelisteobjekter.avType<TerrengdetaljKode>()))

        logger.info("Hentet ${cachedeMaalemetodeKoder.get().size} målemetodekoder.")
        logger.info("Hentet ${cachedeHjelpelinjetypeKoder.get().size} hjelpelinjetyper.")
        logger.info("Hentet ${cachedeAdministrativGrenseKoder.get().size} administrativGrensekoder.")
        logger.info("Hentet ${cachedeTerrengdetaljKoder.get().size} terrengdetaljkoder.")
    }

    fun mapKodeliste(kodeliste: Map<Long, MatrikkelEnumKode>): List<KodelisteEntryDTO> =
        kodeliste.map { kodelisteEntry -> kodelisteEntry.value.mapKodelisteEntry() }

    fun MatrikkelEnumKode.mapKodelisteEntry() = KodelisteEntryDTO(id = this.id.value, kodeverdi = this.kodeverdi, navn = this.navn.entry[0].value)

    fun fetchMatrikkelKodelister(): MatrikkelKodelisterRespons {
        if (cachedeMaalemetodeKoder.get().isNullOrEmpty()) {
            hentKodelisterFraMatrikkelen()
        }
        val kodelister = MatrikkelKodelisterRespons(
            maalemetodeKodeliste = cachedeMaalemetodeKoder.get(),
            hjelpelinjetypeKodeliste = cachedeHjelpelinjetypeKoder.get(),
            administrativGrenseKodeliste = cachedeAdministrativGrenseKoder.get(),
            terrengdetaljKodeliste = cachedeTerrengdetaljKoder.get()
        )
        return kodelister
    }
}

inline fun <reified T : MatrikkelBubbleObject> List<MatrikkelBubbleObject>.avType(): Map<Long, T> =
    this.filterIsInstance<T>().associateBy {
        it.id.value
    }
