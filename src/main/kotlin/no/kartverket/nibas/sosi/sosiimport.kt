package no.kartverket.nibas.sosi

import no.kartverket.nibas.api.v1.ImportApi
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.deserializer.NibasSosiDeserializer
import no.kartverket.nibas.sosi.model.SosiFlate
import no.kartverket.nibas.sosi.model.SosiObjekt
import no.kartverket.nibas.sosi.sosiimport.FylkeImporter
import no.kartverket.nibas.sosi.sosiimport.GrunnkretsImporter
import no.kartverket.nibas.sosi.sosiimport.KommuneImporter
import no.kartverket.nibas.sosi.sosiimport.NasjonImporter
import no.kartverket.nibas.sosi.sosiimport.StemmekretsImporter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

/**
 * Api uten noe autentisering. Test-api for å importere sosi-data.
 * Dette apiet og endepunktet kan fjernes når vi har på plass s3-integrasjon
 */
@RestController
class ImportController @Autowired constructor(private val importService: SosiImportService) : ImportApi {

    override fun importer(file: MultipartFile, fylker: List<String>?): String {
        importService.importStream(file.inputStream, fylker?.map { Fylkesnummer.fromValue(it) } ?: emptyList())
        return "OK"
    }
}

@Service
class SosiImportService {

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

    fun importStream(stream: InputStream, fylkesnumre: List<Fylkesnummer>) {
        logger.info("Leser inn input stream.")
        val root: SosiObjekt
        val millisToRead = measureTimeMillis {
            root = NibasSosiDeserializer().deserialize(stream)
        }
        logger.info("Tok $millisToRead ms å lese inn.")
        importerFlater(root, fylkesnumre)
    }

    private fun importerFlater(root: SosiObjekt, fylkesnumre: List<Fylkesnummer>) {
        val flater = root.getFlater()
        val nasjoner = flater.filter { it.getObjType() == SosiKoder.NASJON }
        val fylker = flater.filter { it.getObjType() == SosiKoder.FYLKE }
        val kommuner = flater.filter { it.getObjType() == SosiKoder.KOMMUNE }
        val grunnkretser = filtrerGrunnkretser(flater, fylkesnumre)
        val stemmekretser = flater.filter { it.getObjType() == SosiKoder.STEMMEKRETS }

        val millis = measureTimeMillis {
            nasjoner.forEachIndexed { index, nasjon ->
                logger.info("Importing nasjon ${index + 1} of ${nasjoner.size}")
                nasjonImporter.importerSosiNasjon(nasjon, root)
            }

            fylker.forEachIndexed { index, fylke ->
                logger.info("Importing fylke ${index + 1} of ${fylker.size}")
                fylkeImporter.importerSosiFylke(fylke, root)
            }

            val gruppertKommune: Map<String?, List<SosiFlate>> = kommuner.groupBy { it.getKommunenummer() }
            gruppertKommune.entries.forEachIndexed { index, entry ->
                logger.info("Importing kommune ${index + 1} of ${gruppertKommune.size}")
                kommuneImporter.importerSosiKommune(entry.value, root)
            }

            val kurver = root.getKurver()
            val grupperteGrunnkretser: Map<String?, List<SosiFlate>> = grunnkretser.groupBy { it.getGrunnkretsId()?.getGrunnkretsnummer() }
            grupperteGrunnkretser.entries.forEachIndexed { index, entry ->
                logger.info("Importing grunnkrets ${index + 1} of ${grupperteGrunnkretser.size}")
                grunnkretsImporter.importerSosiGrunnkrets(entry.value, kurver)
            }

            data class KommunalKretsKey(val kommunenummer: String, val stemmekretsnummer: String)

            fun SosiFlate.toKommunalKretsKey() = KommunalKretsKey(getKommunenummer(), getStemmekretsId()!!.getStemmekretsnummer()!!)

            val grupperteStemmekretser = stemmekretser.groupBy { it.toKommunalKretsKey() }
            grupperteStemmekretser.entries.forEachIndexed { index, entry ->
                logger.info("Importing stemmekrets ${index + 1} of ${grupperteStemmekretser.size} (${entry.value.get(0).getId()})")
                stemmekretsImporter.importerSosiStemmekrets(entry.value, root)
            }

        }
        logger.info("Took $millis ms to import")
    }

    private fun filtrerGrunnkretser(
        flater: List<SosiFlate>,
        fylkesnumre: List<Fylkesnummer>
    ): List<SosiFlate> {
        var grunnkretser = flater.filter { it.getObjType() == SosiKoder.GRUNNKRETS }
        if (fylkesnumre.isNotEmpty()) {
            logger.info("Antall flater av type grunnkrets: ${grunnkretser.size}")
            logger.info("Filtrerer på fylkene: [${fylkesnumre.joinToString(",")}]")

            grunnkretser = grunnkretser.filter {
                fylkesnumre.contains(Fylkesnummer.fromValue(it.getGrunnkretsId()?.getGrunnkretsnummer()?.substring(0, 2)!!))
            }
            logger.info("Antall grunnkretser etter filtrering: ${grunnkretser.size}")
        }
        return grunnkretser
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
