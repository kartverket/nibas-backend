package no.kartverket.nibas.sosi

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.deserializer.NibasSosiDeserializer
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.sosiimport.FylkeImporter
import no.kartverket.nibas.sosi.sosiimport.GrunnkretsImporter
import no.kartverket.nibas.sosi.sosiimport.KommuneImporter
import no.kartverket.nibas.sosi.sosiimport.NasjonImporter
import no.kartverket.nibas.sosi.sosiimport.StemmekretsImporter
import org.locationtech.jts.geom.Geometry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.io.File
import java.util.logging.Logger
import kotlin.system.measureTimeMillis


@Service
@Profile("import")
class SosiImportService {

    private final val TARGET_GRUNNKRETS = "grunnkrets"
    private final val TARGET_STEMMEKRETS = "stemmekrets"

    @Value("\${nibas.import.grunnkretsfil}")
    lateinit var grunnkretsfil: String

    @Value("\${nibas.import.admenhetfil}")
    lateinit var admenhetfil: String

    @Value("\${nibas.import.stemmekretsfil}")
    lateinit var stemmekretsfil: String

    @Value("#{'\${nibas.import.fylkesnumre}'.split(',')}")
    lateinit var fylkesnumre: List<String>

    @Value("#{'\${nibas.import.targets}'.split(',')}")
    lateinit var importTargets: List<String>

    val logger = Logger.getLogger(this::class.java.name)

    @Autowired
    lateinit var kommuneImporter: KommuneImporter

    @Autowired
    lateinit var fylkeImporter: FylkeImporter

    @Autowired
    lateinit var nasjonImporter: NasjonImporter

    @Autowired
    lateinit var grunnkretsImporter: GrunnkretsImporter

    @Autowired
    lateinit var stemmekretsImporter: StemmekretsImporter

    fun importNibas() {
        val (admEnhetRoot, grunnkretsRoot, stemmekretsRoot) = runBlocking {
            val admEnhetDeferred = async { NibasSosiDeserializer().deserialize(File(admenhetfil).inputStream()) }

            val grunnkretsDeferred = when(importTargets.contains(TARGET_GRUNNKRETS)) {
                true -> async { NibasSosiDeserializer().deserialize(File(grunnkretsfil).inputStream()) }
                false -> async { null }
            }

            val stemmekretsDeferred = when(importTargets.contains(TARGET_STEMMEKRETS)) {
                true -> async { NibasSosiDeserializer().deserialize(File(stemmekretsfil).inputStream()) }
                false -> async { null }
            }

            awaitAll(admEnhetDeferred, grunnkretsDeferred, stemmekretsDeferred)
        }

        admEnhetRoot?.let { importerAdministrativeEnheter(admEnhetRoot) }
        grunnkretsRoot?.let { importerGrunnkretser(grunnkretsRoot, admEnhetRoot!!, fylkesnumre) }
        stemmekretsRoot?.let { importerStemmekretser(stemmekretsRoot, admEnhetRoot!!) }
    }

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
        logger.info("Took $millis ms to import")
    }

    fun importerGrunnkretser(grunnkretsRoot: SosiObjekt, admenhetRoot: SosiObjekt, fylkesnumre: List<String>) {
        logger.info("Importing grunnkretser")
        val fylker = fylkesnumre.map { Fylkesnummer.fromValue(it) }
        val grunnkretser = filtrerKretser(grunnkretsRoot.getFlater(), fylker, SosiKoder.GRUNNKRETS)

        val kurver = grunnkretsRoot.getKurver()
        val grupperteGrunnkretser: Map<String?, List<SosiFlate>> = grunnkretser.groupBy { it.getGrunnkretsId()?.getGrunnkretsnummer() }
        grupperteGrunnkretser.entries.forEachIndexed { index, entry ->
            try {
                logger.info("Importing grunnkrets ${index + 1} of ${grupperteGrunnkretser.size} - LokalId = ${entry.value.first().getIdent()?.getLokalId()}")
                grunnkretsImporter.importerSosiGrunnkrets(entry.value, kurver, admenhetRoot)
            } catch(ex: Exception) {
                logger.warning("Klarte ikke å importere grunnkrets med lokalId ${entry.value.first().getIdent()?.getLokalId()}")
                ex.printStackTrace()
            }
        }
    }

    fun importerStemmekretser(stemmekretsRoot: SosiObjekt, admenhetRoot: SosiObjekt) {
        logger.info("Importing stemmekretser")
        data class KommunalKretsKey(val kommunenummer: String, val stemmekretsnummer: String)

        fun SosiFlate.toKommunalKretsKey() = KommunalKretsKey(getKommunenummer(), getStemmekretsId()!!.getStemmekretsnummer()!!)
        val fylker = fylkesnumre.map{Fylkesnummer.fromValue(it)}
        val stemmekretser = filtrerKretser(stemmekretsRoot.getFlater(), fylker, SosiKoder.STEMMEKRETS)
        val grupperteStemmekretser = stemmekretser.groupBy { it.toKommunalKretsKey() }

        val kurver = stemmekretsRoot.getKurver()
        grupperteStemmekretser.entries.forEachIndexed { index, entry ->
            try{
                logger.info("Importing stemmekrets ${index + 1} of ${grupperteStemmekretser.size} - LokalId = ${entry.value.first().getIdent()?.getLokalId()}")
                stemmekretsImporter.importerSosiStemmekrets(entry.value, kurver, admenhetRoot)
            } catch(ex: Exception) {
                logger.warning("Klarte ikke å importere stemmekrets med lokalId ${entry.value.first().getIdent()?.getLokalId()}")
                ex.printStackTrace()
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
}

enum class Fylkesnummer(val fylkesnummer: String) {
    VIKEN("30"),
    OSLO("03"),
    INNLANDET("34"),
    VESTFOLD_OG_TELEMARK("38"),
    AGDER("42"),
    ROGALAND("11"),
    VESTLAND("46"),
    MOERE_OG_ROMSDAL("15"),
    TROENDELAG("50"),
    NORDLAND("18"),
    TROMS_OG_FINNMARK("54");

    companion object {
        fun fromValue(value: String): Fylkesnummer = Fylkesnummer.values().first { it.fylkesnummer == value }
    }
}

class SosiImportException(val geometry: Geometry, override val message: String, override val cause: Throwable? = null) : RuntimeException(message, cause)
