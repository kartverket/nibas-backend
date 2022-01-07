package no.kartverket.nibas.sosi

import no.kartverket.nibas.api.v1.ImportApi
import no.kartverket.nibas.sosi.sosiimport.FylkeImporter
import no.kartverket.nibas.sosi.sosiimport.KommuneImporter
import no.kartverket.nibas.sosi.sosiimport.NasjonImporter
import no.kartverket.nibas.sosi.common.SosiKoder
import no.kartverket.nibas.sosi.deserializer.NibasSosiDeserializer
import no.kartverket.nibas.sosi.model.SosiObjekt
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

    override fun importer(file: MultipartFile): String {
        importService.importStream(file.inputStream)
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

    fun importStream(stream: InputStream) {
        importerFlater(NibasSosiDeserializer().deserialize(stream))
    }

    private fun importerFlater(root: SosiObjekt) {
        val flater = root.getFlater()
        val nasjoner = flater.filter { it.getObjType() == SosiKoder.NASJON }
        val fylker = flater.filter { it.getObjType() == SosiKoder.FYLKE }
        val kommuner = flater.filter { it.getObjType() == SosiKoder.KOMMUNE }

        val millis = measureTimeMillis {
            nasjoner.forEachIndexed { index, nasjon ->
                logger.info("Importing nasjon ${index + 1} of ${nasjoner.size}")
                nasjonImporter.importerSosiNasjon(nasjon, root)
            }

            fylker.forEachIndexed { index, fylke ->
                logger.info("Importing fylke ${index + 1} of ${fylker.size}")
                fylkeImporter.importerSosiFylke(fylke, root)
            }

            kommuner.forEachIndexed { index, kommune ->
                logger.info("Importing kommune ${index + 1} of ${kommuner.size}")
                kommuneImporter.importerSosiKommune(kommune, root)
            }
        }
        logger.info("Took $millis ms to import")
    }
}
