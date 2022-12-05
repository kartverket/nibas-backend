package no.kartverket.nibas.sosi

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.runBlocking
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.deserializer.NibasSosiDeserializer
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.sosiimport.FylkeImporter
import no.kartverket.nibas.sosi.sosiimport.Fylkesnummer
import no.kartverket.nibas.sosi.sosiimport.GrunnkretsImporter
import no.kartverket.nibas.sosi.sosiimport.KommuneImporter
import no.kartverket.nibas.sosi.sosiimport.NasjonImporter
import no.kartverket.nibas.sosi.sosiimport.StemmekretsImporter
import org.locationtech.jts.geom.Geometry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.stereotype.Service
import java.io.InputStream
import kotlin.system.measureTimeMillis


@Service
@Profile("import")
class SosiImportService(
    private val kommuneImporter: KommuneImporter,
    private val fylkeImporter: FylkeImporter,
    private val nasjonImporter: NasjonImporter,
    private val grunnkretsImporter: GrunnkretsImporter,
    private val stemmekretsImporter: StemmekretsImporter,
    private val environment: Environment
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
    private final val targetGrunnkrets = "grunnkrets"
    private final val targetStemmekrets = "stemmekrets"

    @Value("\${nibas.import.grunnkretsfil:sosifiler/STAT_enheter_grunnkretser.sos}")
    lateinit var grunnkretsfil: String

    @Value("\${nibas.import.admenhetfil:sosifiler/ADM_Enheter_Norge_ny_inkl_nasjon.sos}")
    lateinit var admenhetfil: String

    @Value("\${nibas.import.stemmekretsfil:sosifiler/Stemmekretser_Norge.sos}")
    lateinit var stemmekretsfil: String

    private val importTargets: List<String> =
        environment.getProperty("nibas.import.targets")?.split(",")
            ?: listOf(targetGrunnkrets, targetStemmekrets)

    private val fylkesnumre: List<String> =
        environment.getProperty("nibas.import.fylkesnumre")?.split(",")
            ?: Fylkesnummer.values().map { it.fylkesnummer }.sorted()

    fun importNibas() {
        logger.info("\nKonfigurasjon for sosi-import: \n\n"
            + "Profiler: ${environment.activeProfiles.joinToString(",")} \n"
            + "Datasource url: ${environment.getProperty("spring.datasource.url")?.trim()} \n"
            + "Adm.enhet-fil: $admenhetfil \n"
            + "Grunnkretsfil: $grunnkretsfil \n"
            + "Stemmekretsfil: $stemmekretsfil \n"
            + "Fylkesnumre: ${fylkesnumre.joinToString(",")} \n"
            + "Import targets: ${importTargets.joinToString(",")} \n"
        )

        val (admEnhetRoot, grunnkretsRoot, stemmekretsRoot) = runBlocking {
            val admEnhetDeferred = async { NibasSosiDeserializer().deserialize(fileAsInputStream(admenhetfil)) }

            val grunnkretsDeferred = when (importTargets.contains(targetGrunnkrets)) {
                true -> async { NibasSosiDeserializer().deserialize(fileAsInputStream(grunnkretsfil)) }
                false -> async { null }
            }

            val stemmekretsDeferred = when (importTargets.contains(targetStemmekrets)) {
                true -> async { NibasSosiDeserializer().deserialize(fileAsInputStream(stemmekretsfil)) }
                false -> async { null }
            }

            awaitAll(admEnhetDeferred, grunnkretsDeferred, stemmekretsDeferred)
        }

        admEnhetRoot?.let { importerAdministrativeEnheter(admEnhetRoot) }
        val resultsGrunnkrets = grunnkretsRoot?.let { importerGrunnkretser(grunnkretsRoot, admEnhetRoot!!) }
        val resultsStemmekrets = stemmekretsRoot?.let { importerStemmekretser(stemmekretsRoot, admEnhetRoot!!) }
        logResults("Grunnkrets import", resultsGrunnkrets)
        logResults("Stemmekrets import", resultsStemmekrets)
    }

    private fun fileAsInputStream(filepath: String): InputStream = DefaultResourceLoader().getResource(filepath).file.inputStream()

    fun importerAdministrativeEnheter(root: SosiObjekt) {
        logger.info("Importing administrative enheter")
        val flater = root.getFlater()
        val nasjoner = flater.filter { it.getObjType() == SosiKoder.NASJON }
        val fylker = flater.filter { it.getObjType() == SosiKoder.FYLKE }
        val kommuner = flater.filter { it.getObjType() == SosiKoder.KOMMUNE }

        val millis = measureTimeMillis {
            nasjoner.forEachIndexed { index, nasjon ->
                logger.info("Importing nasjon ${index + 1} of ${nasjoner.size}")
                nasjonImporter.importerSosiNasjon(nasjon)
            }

            fylker.forEachIndexed { index, fylke ->
                logger.info("Importing fylke ${index + 1} of ${fylker.size}")
                fylkeImporter.importerSosiFylke(fylke)
            }

            val gruppertKommune: Map<String?, List<SosiFlate>> = kommuner.groupBy { it.getKommunenummer() }
            gruppertKommune.entries.forEachIndexed { index, entry ->
                logger.info("Importing kommune ${index + 1} of ${gruppertKommune.size}")
                kommuneImporter.importerSosiKommune(entry.value)
            }
        }
        logger.info("Took $millis ms to import Administrative enheter")
    }

    fun importerGrunnkretser(grunnkretsRoot: SosiObjekt, admenhetRoot: SosiObjekt): List<Result<Unit>> {
        logger.info("Importing grunnkretser")
        val fylker = fylkesnumre.map { Fylkesnummer.fromValue(it) }
        val grunnkretser = filtrerKretser(grunnkretsRoot.getFlater(), fylker, SosiKoder.GRUNNKRETS)

        val kurver = grunnkretsRoot.getKurver()
        val grupperteGrunnkretser: Map<String?, List<SosiFlate>> = grunnkretser.groupBy { it.getGrunnkretsId()?.getGrunnkretsnummer() }
        return grupperteGrunnkretser.entries.mapIndexed { index, entry ->
            try {
                logger.info("Importing grunnkrets ${index + 1} of ${grupperteGrunnkretser.size} - LokalId = ${entry.value.first().getIdent()?.getLokalId()}")
                grunnkretsImporter.importerSosiGrunnkrets(entry.value, kurver, admenhetRoot)
                Result.success(Unit)
            } catch (ex: Exception) {
                logger.warn("Klarte ikke å importere grunnkrets med lokalId ${entry.value.first().getIdent()?.getLokalId()}")
                Result.failure(ex)
            }
        }
    }

    fun importerStemmekretser(stemmekretsRoot: SosiObjekt, admenhetRoot: SosiObjekt): List<Result<Unit>> {
        logger.info("Importing stemmekretser")
        data class KommunalKretsKey(val kommunenummer: String, val stemmekretsnummer: String)

        fun SosiFlate.toKommunalKretsKey() = KommunalKretsKey(getKommunenummer(), getStemmekretsId()!!.getStemmekretsnummer()!!)
        val fylker = fylkesnumre.map { Fylkesnummer.fromValue(it) }
        val stemmekretser = filtrerKretser(stemmekretsRoot.getFlater(), fylker, SosiKoder.STEMMEKRETS)
        val grupperteStemmekretser = stemmekretser.groupBy { it.toKommunalKretsKey() }

        val kurver = stemmekretsRoot.getKurver()
        return grupperteStemmekretser.entries.mapIndexed { index, entry ->
            try {
                logger.info("Importing stemmekrets ${index + 1} of ${grupperteStemmekretser.size} - LokalId = ${entry.value.first().getIdent()?.getLokalId()}")
                stemmekretsImporter.importerSosiStemmekrets(entry.value, kurver, admenhetRoot)
                Result.success(Unit)
            } catch (ex: Exception) {
                logger.warn("Klarte ikke å importere stemmekrets med lokalId ${entry.value.first().getIdent()?.getLokalId()}", ex)
                Result.failure(ex)
            }
        }
    }

    private fun filtrerKretser(
        flater: List<SosiFlate>,
        fylkesnumre: List<Fylkesnummer>,
        kretskode: String
    ): List<SosiFlate> {
        if (listOf(SosiKoder.GRUNNKRETS, SosiKoder.STEMMEKRETS).contains(kretskode)) {
            var kretser = flater.filter { it.getObjType() == kretskode }
            if (fylkesnumre.isNotEmpty()) {
                logger.info("Antall flater av type $kretskode: ${kretser.size}")
                logger.info("Filtrerer på fylkene: [${fylkesnumre.joinToString(",")}]")

                kretser = kretser.filter {
                    fylkesnumre.contains(Fylkesnummer.fromValue(getKretsnummerMedFylkesidentifikator(it, kretskode)?.substring(0, 2)!!))
                }
                logger.info("Antall kretser etter filtrering: ${kretser.size}")
            }
            return kretser
        } else {
            throw RuntimeException("Kan ikke filtrere på noe annet enn stemmekrets og grunnkrets, forsøker å filtrere på $kretskode")
        }
    }

    private fun getKretsnummerMedFylkesidentifikator(flate: SosiFlate, kretskode: String): String? {
        return when (kretskode) {
            SosiKoder.GRUNNKRETS -> flate.getGrunnkretsId()?.getGrunnkretsnummer()
            else -> flate.getKommunenummer()
        }
    }

    private fun logResults(jobName: String, results: List<Result<Unit>>?) {
        if (results.isNullOrEmpty()) {
            logger.info("$jobName has no recorded results")
            return
        }
        val groupedResults = results.groupingBy {
            it.fold(
                onSuccess = { "Success" },
                onFailure = { ex -> ex.javaClass.name }
            )
        }.eachCount()
        logger.info("Results for $jobName:")
        logger.info("    Total: ${results.size}")
        groupedResults.entries.forEach { (key, count) -> logger.info("    $key: $count") }
    }
}

class SosiImportException(val geometry: Geometry, override val message: String, override val cause: Throwable? = null) : RuntimeException(message, cause)
